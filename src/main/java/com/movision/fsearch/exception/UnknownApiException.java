package com.movision.fsearch.exception;

public class UnknownApiException extends ApiException {

	public UnknownApiException(Throwable t) {
		super(t, "unknown");
	}

	public UnknownApiException(String s) {
		super(new RuntimeException(s), "unknown");
	}

}
