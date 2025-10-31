package ua.od.whcrow.bfpu.cli;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;

public interface Setting {
	
	@Nonnull
	Path getSource();
	
	@Nonnull
	Path getDestination();
	
	boolean isRecursive();
	
	@Nullable
	String getGlob();
	
	boolean getSkipOnExistingTarget();
	
	boolean isFailTolerant();
	
}
