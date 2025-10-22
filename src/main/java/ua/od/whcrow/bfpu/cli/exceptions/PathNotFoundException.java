package ua.od.whcrow.bfpu.cli.exceptions;

import jakarta.annotation.Nonnull;

import java.nio.file.Path;

public class PathNotFoundException extends PathException {
	
	private static final String ERROR = "not found";
	
	public PathNotFoundException(@Nonnull Path path, @Nonnull String error) {
		super(path, error);
	}
	
	public PathNotFoundException(@Nonnull Path path, @Nonnull String error, @Nonnull Throwable cause) {
		super(path, error, cause);
	}
	
	public PathNotFoundException(@Nonnull Path path) {
		this(path, ERROR);
	}
	
	public PathNotFoundException(@Nonnull Path path, @Nonnull Throwable cause) {
		this(path, ERROR, cause);
	}
	
}
