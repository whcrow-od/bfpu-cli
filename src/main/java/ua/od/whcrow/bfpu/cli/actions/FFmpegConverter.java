package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import ua.od.whcrow.bfpu.cli.Setting;
import ua.od.whcrow.bfpu.cli._commons.ConditionalOnArrayPropertyContains;
import ua.od.whcrow.bfpu.cli._commons.ExceptionUtil;
import ua.od.whcrow.bfpu.cli.exceptions.ActionRunException;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@ConditionalOnArrayPropertyContains(
		name = FFmpegConverter.PN_ACTION_NAME,
		containsValue = FFmpegConverter.ACTION_NAME
)
@EnableConfigurationProperties(FFmpegConverterProperties.class)
class FFmpegConverter extends AbstractAction {
	
	static final String ACTION_NAME = "ffmpeg-converter";
	
	private static final String AV_CODEC_ID_PREFIX = "AV_CODEC_ID_";
	private static final Map<String,Integer> CODEC_MAP;
	
	static {
		CODEC_MAP = Arrays.stream(avcodec.class.getFields())
				.filter(f -> Modifier.isStatic(f.getModifiers()))
				.filter(f -> f.getName().startsWith(AV_CODEC_ID_PREFIX))
				.filter(f -> f.getType() == int.class)
				.collect(Collectors.toMap(
						f -> f.getName().substring(AV_CODEC_ID_PREFIX.length()),
						f -> (Integer) ExceptionUtil.sneakySupply(() -> f.get(null))
				));
	}
	
	private final FFmpegConverterProperties properties;
	
	FFmpegConverter(@Nonnull FFmpegConverterProperties properties) {
		this.properties = properties;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return ACTION_NAME;
	}
	
	@Override
	public void run(@Nonnull Setting setting)
			throws ActionRunException {
		logger.info("Setting: {}", properties);
		FFmpegConverterOutputLogCallback.set(properties.outputLogLevel());
		Integer videoCodecId = getCodecId(properties.videoEncoder(), properties.videoCodec(), "video");
		Integer audioCodecId = getCodecId(properties.audioEncoder(), properties.audioCodec(), "audio");
		try (Stream<Path> pathStream = createPathStream(setting)) {
			for (Path sourceFilePath : (Iterable<Path>) pathStream::iterator) {
				try {
					processFile(sourceFilePath, buildTargetFilePath(sourceFilePath, setting), videoCodecId,
							audioCodecId);
				} catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
					String message = "Failed to convert the source file " + sourceFilePath;
					if (setting.isFailTolerant()) {
						logger.warn(message, e);
						continue;
					}
					throw new ActionRunException(getName(), message, e);
				}
			}
		}
	}
	
	@Nullable
	private Integer getCodecId(@Nullable String encoder, @Nullable String codec, @Nonnull String codecType)
			throws ActionRunException {
		if (encoder == null && codec == null) {
			return null;
		}
		try (AVCodec avCodec = encoder == null
				? avcodec.avcodec_find_encoder(getCodecId(codec, codecType))
				: avcodec.avcodec_find_encoder_by_name(encoder)
		) {
			if (avCodec == null) {
				throw new ActionRunException(getName(), "Required " + codecType + " codec "
						+ Objects.requireNonNullElse(encoder, codec) + " is not found");
			}
			logger.info("Found required {} codec {}", codecType, avCodec.long_name().getString());
			return avCodec.id();
		}
	}
	
	private int getCodecId(@Nonnull String codec, @Nonnull String codecType)
			throws ActionRunException {
		Integer value = CODEC_MAP.get(codec.toUpperCase());
		if (value == null) {
			throw new ActionRunException(getName(), "Required " + codecType + " codec " + codec + " is not registered");
		}
		return value;
	}
	
	@Nonnull
	@Override
	protected Path buildTargetFilePath(@Nonnull Path sourceFilePath, @Nonnull Setting setting)
			throws ActionRunException {
		Path targetFilePath = super.buildTargetFilePath(sourceFilePath, setting);
		if (properties.fileExt() != null) {
			targetFilePath = withExtension(targetFilePath, properties.fileExt());
		}
		return targetFilePath;
	}
	
	private void processFile(@Nonnull Path sourceFilePath, @Nonnull Path targetFilePath,
			@Nullable Integer videoCodecId, @Nullable Integer audioCodecId)
			throws FrameGrabber.Exception, FrameRecorder.Exception {
		logger.info("Converting {} to {}", sourceFilePath, targetFilePath);
		long start = System.currentTimeMillis();
		long imageFrameNum;
		try (
				FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFilePath.toFile());
				FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(targetFilePath.toFile(), 0);
		) {
			grabber.start();
			populate(recorder, grabber, videoCodecId, audioCodecId);
			recorder.start();
			Frame frame;
			while ((frame = grabber.grab()) != null) {
				recorder.record(frame);
			}
			imageFrameNum = recorder.getFrameNumber();
		}
		logger.info("Converted {} to {} ({} frames) in {} ms", sourceFilePath, targetFilePath, imageFrameNum,
				System.currentTimeMillis() - start);
	}
	
	private void populate(@Nonnull FFmpegFrameRecorder recorder, @Nonnull FFmpegFrameGrabber grabber,
			@Nullable Integer videoCodecId, @Nullable Integer audioCodecId) {
		recorder.setImageWidth(Objects.requireNonNullElse(properties.imageWidth(), grabber.getImageWidth()));
		recorder.setImageHeight(Objects.requireNonNullElse(properties.imageHeight(), grabber.getImageHeight()));
		recorder.setAudioChannels(grabber.getAudioChannels());
		if (properties.aspectRatio() != null) {
			recorder.setAspectRatio(properties.aspectRatio());
		}
		if (!properties.skipMetadata()) {
			recorder.setMetadata(grabber.getMetadata());
		}
		if (properties.format() != null) {
			recorder.setFormat(properties.format());
		}
		if (properties.option() != null) {
			properties.option().forEach(recorder::setOption);
		}
		if (videoCodecId != null) {
			recorder.setVideoCodec(videoCodecId);
		}
		if (properties.videoEncoder() != null) {
			recorder.setVideoCodecName(properties.videoEncoder());
		}
		if (properties.videoOption() != null) {
			properties.videoOption().forEach(recorder::setVideoOption);
		}
		if (properties.videoBitrate() != null) {
			recorder.setVideoBitrate(properties.videoBitrate());
		}
		if (properties.videoQuality() != null) {
			recorder.setVideoQuality(properties.videoQuality());
		}
		if (properties.frameRate() == null) {
			recorder.setFrameRate(grabber.getFrameRate());
		} else {
			recorder.setFrameRate(properties.frameRate());
		}
		recorder.setDisplayRotation(Objects.requireNonNullElse(properties.displayRotation(),
				grabber.getDisplayRotation()));
		if (!properties.skipVideoMetadata()) {
			recorder.setVideoMetadata(grabber.getVideoMetadata());
		}
		if (grabber.getAudioChannels() == 0) {
			return;
		}
		recorder.setAudioChannels(grabber.getAudioChannels());
		if (audioCodecId != null) {
			recorder.setAudioCodec(audioCodecId);
		}
		if (properties.audioEncoder() != null) {
			recorder.setAudioCodecName(properties.audioEncoder());
		}
		if (properties.audioOption() != null) {
			properties.audioOption().forEach(recorder::setAudioOption);
		}
		if (properties.audioBitrate() != null) {
			recorder.setAudioBitrate(properties.audioBitrate());
		}
		if (properties.sampleRate() != null) {
			recorder.setSampleRate(properties.sampleRate());
		}
		if (!properties.skipAudioMetadata()) {
			recorder.setAudioMetadata(grabber.getAudioMetadata());
		}
	}
	
}
