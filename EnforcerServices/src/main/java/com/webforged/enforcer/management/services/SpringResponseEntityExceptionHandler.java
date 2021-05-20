package com.webforged.enforcer.management.services;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.model.Error;

@ControllerAdvice
public class SpringResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { Exception.class, Exception.class })
    protected ResponseEntity<Object> handleConflict( RuntimeException ex, WebRequest request) {
    	ResponseEntity<Object> response;
    	Error err;
    	
    	if( ex instanceof WrappedErrorException ) {
    		err = ((WrappedErrorException)ex).getError();
    	} else {
    		//
    		// since the api really wants a json Error, let's translate what we got.  In our case, we always send back a particular error
    		// status, so the api spec should take this into account. 
    		//
    		err = new Error();
    		err.setType( "Generic" );
    		err.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
    		err.setTitle( "Error" );
    		err.setDetail( ex.toString() );
    		
    		//return handleExceptionInternal(ex, "runtime exception " + ex.toString(), 
    		//			new HttpHeaders(), HttpStatus.CONFLICT, request);
    	}
		HttpHeaders headers = new HttpHeaders() ;
		headers.setContentType( MediaType.APPLICATION_JSON );
		headers.setCacheControl( CacheControl.noStore() ) ;
		response = new ResponseEntity<Object>( (Object)err, headers, HttpStatus.valueOf( err.getStatus() ) ) ;
		
    	return response;
    }
}