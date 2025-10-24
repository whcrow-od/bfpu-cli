package ua.od.whcrow.bfpu.cli.actions;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.od.whcrow.bfpu.cli.Action;
import ua.od.whcrow.bfpu.cli.Setting;
import ua.od.whcrow.bfpu.cli.exceptions.ActionRunException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public abstract class AbstractAction implements Action {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Nonnull
	protected Stream<Path> createPathStream(@Nonnull Setting setting)
			throws ActionRunException {
		PathMatcher fileNameMatcher = StringUtils.isBlank(setting.getGlob())
				? null : FileSystems.getDefault().getPathMatcher("glob:" + setting.getGlob());
		BiPredicate<Path,BasicFileAttributes> filePredicate = fileNameMatcher == null
				? (path, attr) -> attr.isRegularFile()
				: (path, attr) -> attr.isRegularFile() && fileNameMatcher.matches(path.getFileName());
		try {
			return setting.isRecursive()
					? Files.find(setting.getSource(), Integer.MAX_VALUE, filePredicate)
					: Files.find(setting.getSource(), 1, filePredicate);
		} catch (IOException e) {
			throw new ActionRunException(getName(), "Failed to find the files matching given setting", e);
		}
	}
	
	@Nonnull
	protected Path buildTargetFilePath(@Nonnull Path sourceDir, @Nonnull Path sourceFile, @Nonnull Path destinationDir)
			throws ActionRunException {
		Path fileRelPath = sourceDir.relativize(sourceFile);
		Path targetFile = destinationDir.resolve(fileRelPath);
		Path targetDirPath = targetFile.getParent();
		if (Files.notExists(targetDirPath)) {
			try {
				Files.createDirectories(targetDirPath);
			} catch (IOException e) {
				throw new ActionRunException(getName(), "Failed to create a target sub-dir/s " + targetDirPath
						+ " for " + sourceFile, e);
			}
		}
		return targetFile;
	}
	
	@Nonnull
	protected Path buildTargetFilePath(@Nonnull Path sourceFile, @Nonnull Setting setting)
			throws ActionRunException {
		return buildTargetFilePath(setting.getSource(), sourceFile, setting.getDestination());
	}
	
}
