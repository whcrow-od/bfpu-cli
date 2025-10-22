package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

public class ActionInitException extends ActionException {
	
	public ActionInitException(@Nonnull String actionName, @Nonnull String error) {
		super(actionName, error);
	}
	
	public ActionInitException(@Nonnull String actionName, @Nonnull String error, @Nonnull Throwable cause) {
		super(actionName, error, cause);
	}
	
}
