package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ua.od.whcrow.bfpu.cli.Action;
import ua.od.whcrow.bfpu.cli.Setting;

@Component
class BySizeDeduplicator implements Action {
	
	private static final Logger LOG = LoggerFactory.getLogger(BySizeDeduplicator.class);
	
	@Nonnull
	@Override
	public String getName() {
		return "deduplicate-by-size";
	}
	
	@Override
	public void run(@Nonnull Setting setting)
			throws Exception {
		//TODO: implement
		LOG.warn("ACTION IS NOT IMPLEMENTED YET");
	}
	
}
