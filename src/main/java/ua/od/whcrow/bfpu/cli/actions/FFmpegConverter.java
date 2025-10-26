package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
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
import java.util.Hashtable;
import java.util.Objects;
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
	private static final NamedMap<String,Integer> CODEC_MAP = new NamedMap<>("list of codec");
	
	static {
		Arrays.stream(avcodec.class.getFields())
				.filter(f -> Modifier.isStatic(f.getModifiers()))
				.filter(f -> f.getName().startsWith(AV_CODEC_ID_PREFIX))
				.filter(f -> f.getType() == int.class)
				.forEach(f -> CODEC_MAP.put(f.getName().substring(AV_CODEC_ID_PREFIX.length()),
						(Integer) ExceptionUtil.sneakySupply(() -> f.get(null))));
	}
	
	private final FFmpegConverterProperties properties;
	
	FFmpegConverter(@Nonnull FFmpegConverterProperties properties) {
		this.properties = properties;
		FFmpegConverterOutputLogCallback.set(properties.outputLogLevel());
	}
	
	@Nonnull
	@Override
	public String getName() {
		return ACTION_NAME;
	}
	
	@Override
	public void run(@Nonnull Setting setting)
			throws ActionRunException {
		try (Stream<Path> pathStream = createPathStream(setting)) {
			for (Path sourceFilePath : (Iterable<Path>) pathStream::iterator) {
				try {
					processFile(sourceFilePath, setting);
				} catch (Exception e) {
					String message = "Failed to convert the source file " + sourceFilePath;
					if (setting.isFailTolerant()) {
						logger.warn(message, e);
						continue;
					}
					if (e instanceof ActionRunException) {
						throw (ActionRunException) e;
					}
					throw new ActionRunException(getName(), message, e);
				}
			}
		}
	}
	
	private void processFile(@Nonnull Path sourceFilePath, @Nonnull Setting setting)
			throws ActionRunException, FrameGrabber.Exception, FrameRecorder.Exception {
		Path targetFilePath = buildTargetFilePath(sourceFilePath, setting);
		if (properties.fileExt() != null) {
			targetFilePath = withExtension(targetFilePath, properties.fileExt());
		}
		logger.info("Converting {}", sourceFilePath);
		long start = System.currentTimeMillis();
		long skippedFrameCount = 0;
		try (
				FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFilePath.toFile());
				FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(targetFilePath.toFile(), 0);
		) {
			grabber.start();
			populate(recorder, grabber);
			recorder.start();
			Frame frame = null;
			do {
				try {
					frame = grabber.grab();
					recorder.record(frame);
				} catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
					if (properties.skipFailFrame()) {
						skippedFrameCount++;
						continue;
					}
					throw e;
				}
			} while (frame != null);
		}
		if (skippedFrameCount == 0) {
			logger.info("Converted {} in {} ms", sourceFilePath, System.currentTimeMillis() - start);
			return;
		}
		logger.warn("Converted {} in {} ms, {} frames skipped", sourceFilePath, System.currentTimeMillis() - start,
				skippedFrameCount);
	}
	
	private void populate(@Nonnull FFmpegFrameRecorder recorder, @Nonnull FFmpegFrameGrabber grabber)
			throws ActionRunException {
		recorder.setImageWidth(Objects.requireNonNullElse(properties.imageWidth(), grabber.getImageWidth()));
		recorder.setImageHeight(Objects.requireNonNullElse(properties.imageHeight(), grabber.getImageHeight()));
		if (properties.aspectRatio() != null) {
			recorder.setAspectRatio(properties.aspectRatio());
		}
		if (properties.format() != null) {
			recorder.setFormat(properties.format());
		}
		if (properties.videoCodec() != null) {
			recorder.setVideoCodec(getValue(CODEC_MAP, properties.videoCodec()));
		}
		if (properties.videoCodecName() != null) {
			recorder.setVideoCodecName(properties.videoCodecName());
		}
		if (properties.option() != null) {
			properties.option().forEach(recorder::setOption);
		}
		if (properties.videoBitrate() != null) {
			recorder.setVideoBitrate(properties.videoBitrate());
		}
		if (properties.videoQuality() != null) {
			recorder.setVideoQuality(properties.videoQuality());
		}
		if (properties.frameRate() != null) {
			recorder.setFrameRate(properties.frameRate());
		}
		recorder.setDisplayRotation(Objects.requireNonNullElse(properties.displayRotation(),
				grabber.getDisplayRotation()));
		if (!properties.skipVideoMetadata()) {
			recorder.setVideoMetadata(grabber.getVideoMetadata());
		}
		if (grabber.getAudioChannels() < 1) {
			return;
		}
		recorder.setAudioChannels(grabber.getAudioChannels());
		recorder.setSampleRate(grabber.getSampleRate());
		if (properties.audioCodec() != null) {
			recorder.setAudioCodec(getValue(CODEC_MAP, properties.audioCodec()));
		}
		if (properties.audioCodecName() != null) {
			recorder.setAudioCodecName(properties.audioCodecName());
		}
		if (properties.audioBitrate() != null) {
			recorder.setAudioBitrate(properties.audioBitrate());
		}
		if (!properties.skipAudioMetadata()) {
			recorder.setAudioMetadata(grabber.getAudioMetadata());
		}
	}
	
	@Nonnull
	private <T> T getValue(@Nonnull NamedMap<String,T> map, @Nonnull String name)
			throws ActionRunException {
		T value = map.get(name.toUpperCase());
		if (value == null) {
			throw new ActionRunException(getName(), "No \"" + name + "\" found in " + map.name);
		}
		return value;
	}
	
	private static final class NamedMap<K, V> extends Hashtable<K,V> {
		
		final String name;
		
		NamedMap(@Nonnull String name) {
			this.name = name;
		}
		
	}
	
}
