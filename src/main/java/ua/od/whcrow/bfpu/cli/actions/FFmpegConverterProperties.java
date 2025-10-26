package ua.od.whcrow.bfpu.cli.actions;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.logging.LogLevel;

import java.util.Map;

@ConfigurationProperties(FFmpegConverter.ACTION_NAME)
record FFmpegConverterProperties(
		@DefaultValue(value = "OFF")
		LogLevel outputLogLevel,
		String fileExt,
		boolean skipFailFrame,
		Integer imageWidth,
		Integer imageHeight,
		Double aspectRatio,
		String format,
		String videoCodec,
		String videoCodecName,
		Map<String, String> option,
		Integer videoBitrate,
		Double videoQuality,
		Double frameRate,
		Double displayRotation,
		boolean skipVideoMetadata,
		String audioCodec,
		String audioCodecName,
		Integer audioBitrate,
		boolean skipAudioMetadata
) {
}
