package com.webforged.enforcer.management.security.jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class JwtAuthenticationController {
	Logger logger = LoggerFactory.getLogger( JwtAuthenticationController.class ) ;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private JwtUserDetailsService userDetailsService;

	@RequestMapping(value = "/v1/authenticate", method = { RequestMethod.POST })
	public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
		logger.info( "SecurityEvent: createAuthenticationToken ..." + authenticationRequest.getUsername() ) ;
		authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
		logger.info( "SecurityEvent: authenticated " + authenticationRequest.getUsername() ) ;
		final UserDetails userDetails = userDetailsService
				.loadUserByUsername(authenticationRequest.getUsername());
		logger.info( "SecurityEvent: user details: " + userDetails.getAuthorities() ) ;
		final String token = jwtTokenUtil.generateToken(userDetails);

		return ResponseEntity.ok(new JwtResponse(token));
	}

	private void authenticate(String username, String password) throws Exception {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (DisabledException e) {
			logger.error( "SecurityEvent: User disabled: " + username );
			throw new Exception("USER_DISABLED", e);
		} catch (BadCredentialsException e) {
			logger.error( "SecurityEvent: Invalid credentials: " + username );
			throw new Exception("INVALID_CREDENTIALS", e);
		}
	}
}