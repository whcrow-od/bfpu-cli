package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ua.od.whcrow.bfpu.cli.Action;
import ua.od.whcrow.bfpu.cli.Setting;
import ua.od.whcrow.bfpu.cli._commons.ConditionalOnArrayPropertyContains;

@Component
@ConditionalOnArrayPropertyContains(
		name = BySizeDeduplicator.PN_ACTION_NAME,
		containsValue = BySizeDeduplicator.ACTION_NAME
)
class BySizeDeduplicator implements Action {
	
	static final String ACTION_NAME = "deduplicate-by-size";
	
	private static final Logger LOG = LoggerFactory.getLogger(BySizeDeduplicator.class);
	
	@Nonnull
	@Override
	public String getName() {
		return ACTION_NAME;
	}
	
	@Override
	public void run(@Nonnull Setting setting)
			throws Exception {
		//TODO: implement
		LOG.warn("ACTION IS NOT IMPLEMENTED YET");
	}
	
}
