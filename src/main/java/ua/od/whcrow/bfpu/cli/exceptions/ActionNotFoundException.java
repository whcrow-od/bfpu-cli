package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

public class ActionNotFoundException extends ActionException {
	
	private static final String ERROR = "not found";
	
	public ActionNotFoundException(@Nonnull String actionName, @Nonnull String error) {
		super(actionName, error);
	}
	
	public ActionNotFoundException(@Nonnull String actionName, @Nonnull String error, @Nonnull Throwable cause) {
		super(actionName, error, cause);
	}
	
	public ActionNotFoundException(@Nonnull String actionName) {
		super(actionName, ERROR);
	}
	
	public ActionNotFoundException(@Nonnull String actionName, @Nonnull Throwable cause) {
		super(actionName, ERROR, cause);
	}
	
}
