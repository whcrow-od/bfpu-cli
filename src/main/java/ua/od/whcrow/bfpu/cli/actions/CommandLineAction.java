package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Component;
import ua.od.whcrow.bfpu.cli.Setting;
import ua.od.whcrow.bfpu.cli._commons.ConditionalOnArrayPropertyContains;
import ua.od.whcrow.bfpu.cli.exceptions.ActionInitException;
import ua.od.whcrow.bfpu.cli.exceptions.ActionPropertyException;
import ua.od.whcrow.bfpu.cli.exceptions.ActionRunException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
@ConditionalOnArrayPropertyContains(
		name = CommandLineAction.PN_ACTION_NAME,
		containsValue = CommandLineAction.ACTION_NAME
)
class CommandLineAction extends AbstractAction {
	
	static final String ACTION_NAME = "command-line";
	
	private static final String PN_COMMAND = ACTION_NAME + ".command";
	private static final String PN_OUTPUT_LOG_LEVEL = ACTION_NAME + ".output-log-level";
	private static final String PN_IGNORE_EXIT_CODE = ACTION_NAME + ".ignore-exit-code";
	
	private static final String OUTPUT_LOG_MESSAGE = "Command for {} outputs:\n{}";
	
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
			throws ActionRunException {
		try (Stream<Path> pathStream = createPathStream(setting)) {
			for (Path sourceFilePath : (Iterable<Path>) pathStream::iterator) {
				processFile(sourceFilePath, setting);
			}
		}
	}
	
	private void processFile(@Nonnull Path sourceFilePath, @Nonnull Setting setting)
			throws ActionRunException {
		Path targetFilePath = buildTargetFilePath(sourceFilePath, setting);
		try {
			execCommand(sourceFilePath, targetFilePath);
		} catch (IOException | InterruptedException e) {
			String message = "Failed to execute a command for " + sourceFilePath;
			if (setting.isFailTolerant()) {
				logger.warn(message, e);
				return;
			}
			throw new ActionRunException(getName(), message, e);
		}
	}
	
	private void execCommand(@Nonnull Path sourceFilePath, @Nonnull Path targetFilePath)
			throws IOException, InterruptedException, ActionRunException {
		long start = System.currentTimeMillis();
		String commandLine = commandFormat
				.replace("%source%", sourceFilePath.toAbsolutePath().toString())
				.replace("%target%", targetFilePath.toAbsolutePath().toString());
		logger.debug("Running a command: {}", commandLine);
		// TODO: implement more sophisticated splitter for command
		Process process = new ProcessBuilder(commandLine.split("\\s+"))
				.redirectErrorStream(true)
				.start();
		handleOutput(process, sourceFilePath.toString());
		int exitCode = process.waitFor();
		if (exitCode == 0 || ignoreExitCode) {
			logger.info("Command run for {} took {} ms", sourceFilePath, System.currentTimeMillis() - start);
			return;
		}
		throw new ActionRunException(getName(), "Command run for " + sourceFilePath + " exits with code " + exitCode);
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
					case TRACE -> logger.trace(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case DEBUG -> logger.debug(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case INFO -> logger.info(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case WARN -> logger.warn(OUTPUT_LOG_MESSAGE, sourceFile, line);
					case ERROR, FATAL -> logger.error(OUTPUT_LOG_MESSAGE, sourceFile, line);
				}
			}
		}
	}
	
}
