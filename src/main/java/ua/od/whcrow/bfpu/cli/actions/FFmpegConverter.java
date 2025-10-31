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
import java.nio.file.Files;
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
				Path targetFilePath = buildTargetFilePath(sourceFilePath, setting);
				if (setting.getSkipOnExistingTarget() && Files.exists(targetFilePath)) {
					logger.info("Skip converting of {} because target {} already exists", sourceFilePath,
							targetFilePath);
					continue;
				}
				try {
					processFile(sourceFilePath, targetFilePath, videoCodecId, audioCodecId);
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
		try (
				FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFilePath.toFile());
				FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(targetFilePath.toFile(), 0);
		) {
			grabber.start();
			populate(recorder, grabber, videoCodecId, audioCodecId);
			recorder.start();
			convert(grabber, recorder);
		}
		logger.info("Converted {} to {} in {} ms", sourceFilePath, targetFilePath, System.currentTimeMillis() - start);
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
			if (properties.frameRateMin() != null && grabber.getFrameRate() < properties.frameRateMin()) {
				recorder.setFrameRate(properties.frameRateMin());
			} else if (properties.frameRateMax() != null && grabber.getFrameRate() > properties.frameRateMax()) {
				recorder.setFrameRate(properties.frameRateMax());
			}
			else {
				recorder.setFrameRate(grabber.getFrameRate());
			}
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
		if (properties.sampleRate() == null) {
			if (properties.sampleRateMin() != null && grabber.getSampleRate() < properties.sampleRateMin()) {
				recorder.setSampleRate(properties.sampleRateMin());
			} else if (properties.sampleRateMax() != null && grabber.getSampleRate() > properties.sampleRateMax()) {
				recorder.setSampleRate(properties.sampleRateMax());
			} else {
				recorder.setSampleRate(grabber.getSampleRate());
			}
		} else {
			recorder.setSampleRate(properties.sampleRate());
		}
		if (!properties.skipAudioMetadata()) {
			recorder.setAudioMetadata(grabber.getAudioMetadata());
		}
	}
	
	private void convert(@Nonnull FFmpegFrameGrabber grabber, @Nonnull FFmpegFrameRecorder recorder)
			throws FFmpegFrameGrabber.Exception, FFmpegFrameRecorder.Exception {
		double grabberFrameRate = grabber.getFrameRate();
		logger.debug("Source frame rate is {}", grabberFrameRate);
		logger.debug("Target frame rate is {}", recorder.getFrameRate());
		double lengthInSec = (double) grabber.getLengthInTime() / 1000000L;
		logger.debug("Video length {} sec", lengthInSec);
		if (Math.floor(grabberFrameRate) == Math.floor(recorder.getFrameRate())) {
			if (grabberFrameRate == recorder.getFrameRate()) {
				logger.info("Difference between source/target frame rate is insufficient, "
						+ "therefore recording all frames (skipping the frame drop/duplicate procedure)");
			}
			recordAllFrames(grabber, recorder);
			return;
		}
		double totalFrameNum = grabberFrameRate * lengthInSec;
		logger.debug("Expecting {} image frames to be grabbed (approximately)", Math.round(totalFrameNum));
		double newFrameNum = lengthInSec * recorder.getFrameRate();
		logger.debug("Expecting {} image frames to be recorded (approximately)", Math.round(newFrameNum));
		double step = totalFrameNum / newFrameNum;
		logger.debug("Frame drop/duplicate step is {}", step);
		long recordedFrameNumber = 0, grabbedFrameIndex = 0;
		Frame frame;
		while ((frame = grabber.grab()) != null) {
			if (frame.image == null) {
				recorder.record(frame);
				continue;
			}
			long requiredFrameIndex;
			while ((requiredFrameIndex = Math.round(recordedFrameNumber  * step)) == grabbedFrameIndex) {
				recorder.record(frame);
				logger.trace("Recorded a grabbed frame #{} as frame#{}", requiredFrameIndex, recordedFrameNumber);
				recordedFrameNumber++;
			}
			grabbedFrameIndex++;
		}
		logger.debug("Grabbed {} frames, recorded {} frames", grabbedFrameIndex, recordedFrameNumber);
	}
	
	private static void recordAllFrames(@Nonnull FFmpegFrameGrabber grabber, @Nonnull FFmpegFrameRecorder recorder)
			throws FFmpegFrameGrabber.Exception, FFmpegFrameRecorder.Exception {
		Frame frame;
		while ((frame = grabber.grab()) != null) {
			recorder.record(frame);
		}
	}
	
}
