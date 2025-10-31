package ua.od.whcrow.bfpu.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
record Properties (
		String[] actions,
		String source,
		String destination,
		boolean recursive,
		String glob,
		boolean skipOnExistingTarget,
		boolean failTolerant) {}
