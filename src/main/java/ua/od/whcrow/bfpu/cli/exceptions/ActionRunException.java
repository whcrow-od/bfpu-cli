package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

public class ActionRunException extends ActionException {
	
	public ActionRunException(@Nonnull String actionName, @Nonnull String error) {
		super(actionName, error);
	}
	
	public ActionRunException(@Nonnull String actionName, @Nonnull String error, @Nonnull Throwable cause) {
		super(actionName, error, cause);
	}
	
}
