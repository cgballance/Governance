package com.webforged.enforcer.management.util;

import com.webforged.enforcer.openapi.model.Error;

public class WrappedErrorException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	Error apiError ;
	public WrappedErrorException( Error e ) {
		apiError = e;
	}
	
	public Error getError() { return apiError; }
}
