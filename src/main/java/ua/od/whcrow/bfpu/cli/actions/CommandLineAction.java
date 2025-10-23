package ua.od.whcrow.bfpu.cli.actions;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Component;
import ua.od.whcrow.bfpu.cli.Action;
import ua.od.whcrow.bfpu.cli.Setting;
import ua.od.whcrow.bfpu.cli._commons.ConditionalOnArrayPropertyContains;
import ua.od.whcrow.bfpu.cli.exceptions.ActionException;
import ua.od.whcrow.bfpu.cli.exceptions.ActionInitException;
import ua.od.whcrow.bfpu.cli.exceptions.ActionPropertyException;
import ua.od.whcrow.bfpu.cli.exceptions.ActionRuntimeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

@Component
@ConditionalOnArrayPropertyContains(
		name = CommandLineAction.PN_ACTION_NAME,
		containsValue = CommandLineAction.ACTION_NAME
)
class CommandLineAction implements Action {
	
	static final String ACTION_NAME = "command-line";
	
	private static final String PN_COMMAND = ACTION_NAME + ".command";
	private static final String PN_OUTPUT_LOG_LEVEL = ACTION_NAME + ".output-log-level";
	private static final String PN_IGNORE_EXIT_CODE = ACTION_NAME + ".ignore-exit-code";
	
	private static final String OUTPUT_LOG_MESSAGE = "Command for {} outputs:\n{}";
	
	private static final Logger LOG = LoggerFactory.getLogger(CommandLineAction.class);
	
	private final String commandFormat;
	private final LogLevel outputLogLevel;
	private final boolean ignoreExitCode;
	
	CommandLineAction(@Value("${" + PN_COMMAND + "}") String command,
			@Value("${" + PN_OUTPUT_LOG_LEVEL + ":OFF}") LogLevel outputLogLevel,
			@Value("${" + PN_IGNORE_EXIT_CODE + ":false}") boolean ignoreExitCode)
			throws ActionInitException {
		if (command == null) {
			throw new ActionPropertyException(getName(), PN_COMMAND, "not specified");
		}
		this.commandFormat = command;
		this.outputLogLevel = outputLogLevel;
		this.ignoreExitCode = ignoreExitCode;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return ACTION_NAME;
	}
	
	@Override
	public void run(@Nonnull Setting setting)
			throws Exception {
		try (Stream<Path> pathStream = createPathStream(setting)) {
			pathStream.forEach(sourceFile -> processFile(sourceFile, setting));
		}
	}
	
	@Nonnull
	private Stream<Path> createPathStream(@Nonnull Setting setting)
			throws IOException {
		PathMatcher fileNameMatcher = StringUtils.isBlank(setting.getGlob())
				? null : FileSystems.getDefault().getPathMatcher("glob:" + setting.getGlob());
		BiPredicate<Path,BasicFileAttributes> filePredicate = fileNameMatcher == null
				? (path, attr) -> attr.isRegularFile()
				: (path, attr) -> attr.isRegularFile() && fileNameMatcher.matches(path.getFileName());
		return setting.isRecursive()
				? Files.find(setting.getSource(), Integer.MAX_VALUE, filePredicate)
				: Files.find(setting.getSource(), 1, filePredicate);
	}
	
	private void processFile(@Nonnull Path sourceFile, @Nonnull Setting setting)
			throws ActionRuntimeException {
		Path fileRelPath = setting.getSource().relativize(sourceFile);
		Path targetFile = setting.getDestination().resolve(fileRelPath);
		if (Files.notExists(targetFile.getParent())) {
			try {
				Files.createDirectories(targetFile.getParent());
			} catch (IOException e) {
				String message = "Failed to create a target sub-dir/s for " + sourceFile;
				if (setting.isFailTolerant()) {
					LOG.warn(message, e);
					return;
				}
				throw new ActionRuntimeException(getName(), message, e);
			}
		}
		try {
			execCommand(sourceFile, targetFile);
		} catch (Exception e) {
			String message = "Failed to execute a command for " + sourceFile;
			if (setting.isFailTolerant()) {
				LOG.warn(message, e);
				return;
			}
			throw new ActionRuntimeException(getName(), message, e);
		}
	}
	
	private void execCommand(@Nonnull Path sourceFile, @Nonnull Path targetFile)
			throws IOException, InterruptedException, ActionException {
		long start = System.currentTimeMillis();
		String commandLine = commandFormat
				.replace("%source%", sourceFile.toAbsolutePath().toString())
				.replace("%target%", targetFile.toAbsolutePath().toString());
		LOG.debug("Running a command: {}", commandLine);
		// TODO: implement more sophisticated splitter for command
		Process process = new ProcessBuilder(commandLine.split("\\s+"))
				.redirectErrorStream(true)
				.start();
		handleOutput(process, sourceFile.toString());
		int exitCode = process.waitFor();
		if (exitCode == 0 || ignoreExitCode) {
			LOG.info("Command run for {} took {} ms", sourceFile, System.currentTimeMillis() - start);
			return;
		}
		throw new ActionException(getName(), "Command run for " + sourceFile + " exits with code " + exitCode);
	}
	
	private void handleOutput(@Nonnull Process process, @Nonnull String sourceFile)
			throws IOException {
		if (outputLogLevel == LogLevel.OFF) {
			return;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				switch (outputLogLevel) {
					case TRACE -> LOG.trace(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case DEBUG -> LOG.debug(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case INFO -> LOG.info(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case WARN -> LOG.warn(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case ERROR, FATAL -> LOG.error(OUTPUT_LOG_MESSAGE, sourceFile, line);
				}
			}
		}
	}
	
}
