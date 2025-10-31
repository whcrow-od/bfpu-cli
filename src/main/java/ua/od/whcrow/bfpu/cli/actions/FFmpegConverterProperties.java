package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.logging.LogLevel;

import java.util.Map;

@ConfigurationProperties(FFmpegConverter.ACTION_NAME)
record FFmpegConverterProperties(
		@DefaultValue(value = "OFF")
		LogLevel outputLogLevel,
		String fileExt,
		
		Integer imageWidth,
		Integer imageHeight,
		Double aspectRatio,
		boolean skipMetadata,
		String format,
		Map<String,String> option,
		
		String videoEncoder,
		Map<String,String> videoOption,
		String videoCodec,
		Integer videoBitrate,
		Double videoQuality,
		Integer frameRate,
		Integer frameRateMin,
		Integer frameRateMax,
		Double displayRotation,
		boolean skipVideoMetadata,
		
		String audioEncoder,
		Map<String,String> audioOption,
		String audioCodec,
		Integer audioBitrate,
		Integer sampleRate,
		Integer sampleRateMin,
		Integer sampleRateMax,
		boolean skipAudioMetadata
) {
	
	@Nonnull
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
	}
	
}
