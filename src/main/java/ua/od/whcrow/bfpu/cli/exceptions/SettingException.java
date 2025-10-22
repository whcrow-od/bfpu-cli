package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

public class SettingException extends Exception {
	
	public SettingException(@Nonnull String message) {
		super(message);
	}
	
	public SettingException(@Nonnull String message, @Nonnull Throwable cause) {
		super(message, cause);
	}
	
}
