package com.movision.fsearch.exception;

public class ApiException extends Exception {
	private String errorCode;
	private Object[] errorParams;
	private Object errorData;

	public ApiException(Throwable t, String errorCode, Object[] errorParams,
			Object errorData) {
		super(t);
		this.errorCode = errorCode;
		this.errorParams = errorParams;
		this.errorData = errorData;
	}

	public ApiException(String errorCode, Object[] errorParams, Object errorData) {
		this.errorCode = errorCode;
		this.errorParams = errorParams;
		this.errorData = errorData;
	}

	public ApiException(Throwable t, String errorCode, Object[] errorParams) {
		this(t, errorCode, errorParams, null);
	}

	public ApiException(String errorCode, Object[] errorParams) {
		this(errorCode, errorParams, null);
	}

	public ApiException(Throwable t, String errorCode) {
		this(t, errorCode, null, null);
	}

	public ApiException(String errorCode) {
		this(errorCode, null, null);
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Object[] getErrorParams() {
		return errorParams;
	}

	public Object getErrorData() {
		return errorData;
	}

}
