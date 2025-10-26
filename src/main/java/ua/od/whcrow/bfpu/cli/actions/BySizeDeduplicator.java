package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import ua.od.whcrow.bfpu.cli.Setting;
import ua.od.whcrow.bfpu.cli._commons.ConditionalOnArrayPropertyContains;
import ua.od.whcrow.bfpu.cli.exceptions.ActionRunException;

@Component
@ConditionalOnArrayPropertyContains(
		name = BySizeDeduplicator.PN_ACTION_NAME,
		containsValue = BySizeDeduplicator.ACTION_NAME
)
class BySizeDeduplicator extends AbstractAction {
	
	static final String ACTION_NAME = "deduplicate-by-size";
	
	@Nonnull
	@Override
	public String getName() {
		return ACTION_NAME;
	}
	
	@Override
	public void run(@Nonnull Setting setting)
			throws ActionRunException {
		//TODO: implement
		logger.warn("ACTION IS NOT IMPLEMENTED YET");
	}
	
}
