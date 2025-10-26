package ua.od.whcrow.bfpu.cli.actions;

import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avutil.LogCallback;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_DEBUG;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_FATAL;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_INFO;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_PANIC;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_QUIET;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_TRACE;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_VERBOSE;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_WARNING;

class FFmpegConverterOutputLogCallback extends LogCallback {
	
	private static final Logger LOG = LoggerFactory.getLogger(FFmpegConverter.class.getName() + ":output");
	private static final FFmpegConverterOutputLogCallback instance =
			new FFmpegConverterOutputLogCallback().retainReference();
	
	private FFmpegConverterOutputLogCallback() {
	}
	
	public static FFmpegConverterOutputLogCallback getInstance() {
		return instance;
	}
	
	public static void set(@Nonnull LogLevel level) {
		avutil.av_log_set_level(
				switch (level) {
					case FATAL -> AV_LOG_FATAL;
					case ERROR -> AV_LOG_ERROR;
					case WARN -> AV_LOG_WARNING;
					case INFO -> AV_LOG_INFO;
					case DEBUG -> AV_LOG_VERBOSE;
					case TRACE -> AV_LOG_TRACE;
					case OFF -> AV_LOG_QUIET;
				}
		);
		avutil.setLogCallback(FFmpegConverterOutputLogCallback.getInstance());
	}
	
	@Override
	public void call(int level, BytePointer msg) {
		switch (level) {
			case AV_LOG_PANIC, AV_LOG_FATAL, AV_LOG_ERROR -> LOG.error(msg.getString());
			case AV_LOG_WARNING -> LOG.warn(msg.getString());
			case AV_LOG_INFO -> LOG.info(msg.getString());
			case AV_LOG_VERBOSE, AV_LOG_DEBUG -> LOG.debug(msg.getString());
			default -> LOG.trace(msg.getString());
		}
	}
	
}
