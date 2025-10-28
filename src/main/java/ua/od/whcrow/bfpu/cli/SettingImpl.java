package ua.od.whcrow.bfpu.cli;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import ua.od.whcrow.bfpu.cli.exceptions.PathException;
import ua.od.whcrow.bfpu.cli.exceptions.PathNotFoundException;
import ua.od.whcrow.bfpu.cli.exceptions.SettingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class SettingImpl implements Setting {
	
	private final Path source;
	private final Path destination;
	private final boolean recursive;
	private final String glob;
	private final boolean failTolerant;
	
	SettingImpl(@Nonnull Properties properties, boolean helpOnly)
			throws SettingException, IOException {
		if (StringUtils.isBlank(properties.source())) {
			if (!helpOnly) {
				throw new SettingException("Source cannot be blank");
			}
			source = Path.of("");
		} else {
			source = Path.of(properties.source());
		}
		if (Files.notExists(source)) {
			throw new PathNotFoundException(source, "source not found");
		}
		if (!Files.isDirectory(source)) {
			throw new PathException(source, "source isn't a directory");
		}
		destination = Path.of(properties.destination() == null ? "" : properties.destination());
		if (Files.notExists(destination)) {
			Files.createDirectory(destination);
		}
		recursive = properties.recursive();
		glob = properties.glob();
		failTolerant = properties.failTolerant();
	}
	
	@Nonnull
	@Override
	public Path getSource() {
		return source;
	}
	
	@Nonnull
	@Override
	public Path getDestination() {
		return destination;
	}
	
	@Override
	public boolean isRecursive() {
		return recursive;
	}
	
	@Nullable
	@Override
	public String getGlob() {
		return glob;
	}
	
	@Override
	public boolean isFailTolerant() {
		return failTolerant;
	}
	
	@Nonnull
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
	}
	
}
