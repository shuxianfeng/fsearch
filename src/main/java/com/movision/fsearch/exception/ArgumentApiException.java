package com.movision.fsearch.exception;

public class ArgumentApiException extends ApiException {

	public ArgumentApiException(String key) {
		super("bad.arguments", null, key);
	}

}
