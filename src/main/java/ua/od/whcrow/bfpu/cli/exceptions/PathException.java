package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

import java.nio.file.Path;

public class PathException extends SettingException {
	
	public PathException(@Nonnull Path path, @Nonnull String error) {
		super(buildMessage(path, error));
	}
	
	public PathException(@Nonnull Path path, @Nonnull String error, @Nonnull Throwable cause) {
		super(buildMessage(path, error), cause);
	}
	
	@Nonnull
	private static String buildMessage(@Nonnull Path path, @Nonnull String error) {
		return "Path \"" + path.toAbsolutePath() + "\": " + error;
	}
	
}
