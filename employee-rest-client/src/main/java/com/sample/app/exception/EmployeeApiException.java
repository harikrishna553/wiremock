package com.sample.app.exception;

public class EmployeeApiException extends RuntimeException {

	private static final long serialVersionUID = 119874212393098L;

	public EmployeeApiException(String msg) {
		super(msg);
	}

}
