package ua.od.whcrow.bfpu.cli;

import jakarta.annotation.Nonnull;

public interface Action {
	
	@Nonnull
	String getName();
	
	void run(@Nonnull Setting setting)
			throws Exception;
	
}
