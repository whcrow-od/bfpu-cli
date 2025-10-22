package ua.od.whcrow.bfpu.cli;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ua.od.whcrow.bfpu.cli.exceptions.ActionNotFoundException;

import java.util.Set;

@SpringBootApplication
@EnableConfigurationProperties(Properties.class)
public class Application implements CommandLineRunner {
	
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	
	private final Properties properties;
	private final Set<Action> actions;
	
	private Application(@Nonnull Properties properties, @Nonnull Set<Action> actions) {
		this.properties = properties;
		this.actions = actions;
	}
	
	public static void main(@Nonnull String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Override
	public void run(@Nonnull String... args)
			throws Exception {
		Setting setting = new SettingImpl(properties);
		LOG.info("{}", setting);
		for (String actionName : properties.actions()) {
			long start = System.currentTimeMillis();
			actions.stream()
					.filter(a -> a.getName().equals(actionName))
					.findFirst()
					.orElseThrow(() -> new ActionNotFoundException(actionName))
					.run(setting);
			long duration = System.currentTimeMillis() - start;
			LOG.info("Action \"{}\" finished in {} ({} ms)", actionName,
					DurationFormatUtils.formatDurationWords(duration, true, true), duration);
		}
	}
	
}
