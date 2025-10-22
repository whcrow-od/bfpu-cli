package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

public class ActionException extends Exception {
	
	public ActionException(@Nonnull String actionName, @Nonnull String error) {
		super(buildMessage(actionName, error));
	}
	
	public ActionException(@Nonnull String actionName, @Nonnull String error, @Nonnull Throwable cause) {
		super(buildMessage(actionName, error), cause);
	}
	
	@Nonnull
	private static String buildMessage(@Nonnull String actionName, @Nonnull String error) {
		return "Action \"" + actionName + "\": " + error;
	}
	
}
