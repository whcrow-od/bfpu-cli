package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

public class ActionPropertyException extends ActionInitException {
	
	public ActionPropertyException(@Nonnull String actionName, @Nonnull String propertyName, @Nonnull String error) {
		super(actionName, buildError(propertyName, error));
	}
	
	public ActionPropertyException(@Nonnull String actionName, @Nonnull String propertyName, @Nonnull String error,
			@Nonnull Throwable cause) {
		super(actionName, buildError(propertyName, error), cause);
	}
	
	@Nonnull
	private static String buildError(@Nonnull String propertyName, @Nonnull String error) {
		return "property/argument \"" + propertyName + "\": " + error;
	}
	
}
