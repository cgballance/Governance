package com.webforged.enforcer.management.services;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Let's get rid of the default, white-label, error page.  turn what we can into an Error JSON object.
 * probably have to revisit and put in a constant http status that is defined in the api.  For now, just
 * transferring what is there.
 * 
 * @author chas
 *
 */

@RestController
@RequestMapping("/error")
public class SpringErrorController extends AbstractErrorController {
	static final String ERROR_PATH = "/error";

    public SpringErrorController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}


   // public SuperHeroErrorController(final ErrorAttributes errorAttributes) {
   //     super(errorAttributes, Collections.emptyList());
   // }

    @RequestMapping
    public ResponseEntity<Object> error(HttpServletRequest request) {
    	ErrorAttributeOptions options = ErrorAttributeOptions.of(
    			ErrorAttributeOptions.Include.EXCEPTION,
    			ErrorAttributeOptions.Include.MESSAGE,
    			ErrorAttributeOptions.Include.BINDING_ERRORS,
    			ErrorAttributeOptions.Include.STACK_TRACE
    			);
    	//
    	// this spring error handling is unstable.  manifest constants must be known.
    	//
		Map<String, Object> body = this.getErrorAttributes(request, options) ;
    	
        HttpStatus status = this.getStatus(request);
        HttpHeaders headers = new HttpHeaders() ;
		headers.setContentType( MediaType.APPLICATION_JSON );
		headers.setCacheControl( CacheControl.noStore() ) ;

    	com.webforged.enforcer.openapi.model.Error err = new com.webforged.enforcer.openapi.model.Error() ;
    	err.setStatus( status.value() );
    	err.setTitle( "Spring Error" );
    	err.setDetail( (String) body.get("message") );
    	if( body.get("errors")  != null ) {
    		err.setType( (String) body.get("errors") );
    	} else {
    		err.setType( "Internal" );
    	}
		
		return new ResponseEntity<Object>( (Object)err, headers, status ) ;
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }
}
