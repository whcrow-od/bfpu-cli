package ua.od.whcrow.bfpu.cli;

import jakarta.annotation.Nonnull;

public interface Action {
	
	String PN_ACTION_NAME = "actions";
	
	@Nonnull
	String getName();
	
	void run(@Nonnull Setting setting)
			throws Exception;
	
}
