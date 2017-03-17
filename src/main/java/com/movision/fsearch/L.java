package com.movision.fsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.movision.fsearch.exception.ApiException;
import com.movision.fsearch.exception.UnknownApiException;

/**
 * 日志类
 */
public class L {
	private static final Logger DEFAULT_LOGGER = LoggerFactory
			.getLogger(L.class);

	public static boolean isInfoEnabled() {
		return DEFAULT_LOGGER.isInfoEnabled();
	}

	public static void info(String msg) {
		DEFAULT_LOGGER.info(msg);
	}

	public static void warn(String msg) {
		DEFAULT_LOGGER.warn(msg);
	}

	public static void error(String msg) {
		DEFAULT_LOGGER.error(msg);
	}

	public static void error(Throwable ex) {
		error(null, ex);
	}

	public static void error(String msg, Throwable ex) {
		if (ex instanceof ApiException) {
			ApiException apiError = (ApiException) ex;
			if (apiError instanceof UnknownApiException) {
				Throwable tt = ((UnknownApiException) apiError).getCause();
				if (tt != null) {
					DEFAULT_LOGGER.error(null, ex);
					error(msg, tt);
				} else if (apiError.getMessage() != null) {
					DEFAULT_LOGGER.error(apiError.getMessage());
				}
			}
		} else {
			DEFAULT_LOGGER.error(msg, ex);
		}
	}

}
