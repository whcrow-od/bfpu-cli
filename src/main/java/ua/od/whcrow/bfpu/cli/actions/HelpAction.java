package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import ua.od.whcrow.bfpu.cli.Action;
import ua.od.whcrow.bfpu.cli.Setting;

@Component
public class HelpAction implements Action {
	
	public static final String ACTION_NAME = "help";
	
	@Nonnull
	@Override
	public String getName() {
		return ACTION_NAME;
	}
	
	@Override
	public void run(@Nonnull Setting setting)
			throws Exception {
		//TODO: implement
	}
	
}
