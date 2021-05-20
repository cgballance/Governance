package com.webforged.enforcer.management.security.jwt;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class JwtWebSecurityConfig extends WebSecurityConfigurerAdapter {
	Logger logger = LoggerFactory.getLogger( JwtWebSecurityConfig.class ) ;

	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Autowired
	private UserDetailsService jwtUserDetailsService;

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		// configure AuthenticationManager so that it knows from where to load
		// user for matching credentials
		// Use BCryptPasswordEncoder
		auth.userDetailsService(jwtUserDetailsService).passwordEncoder(passwordEncoder());
	}
	
	@Value( "${cors.origins:#{null}}" )
	private String corsOriginsStr;
	
	@Value( "${cors.origins.patterns:#{null}}" )
	private String corsOriginsPatternStr;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {

	    httpSecurity.cors().configurationSource(corsConfigurationSource());
	    
		// We don't need CSRF for this example
		httpSecurity.csrf().disable()
		// dont authenticate this particular request
		.authorizeRequests().antMatchers( "/v1/authenticate", 
				"/**.html", 
				"/**.yaml", 
				"/**.js", 
				"/src/**.jsx", "/governance/**" ).permitAll().
		// all other requests need to be authenticated
		anyRequest().authenticated().and().
		// make sure we use stateless session; session won't be used to
		// store user's state.
		exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
		.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		// Add a filter to validate the tokens with every request
		httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		//
		// old version i think allowed this according to web...but no longer.  below configuration
		// does NOT allow an allowOrigins of '*'.  need to list domains! for this test, probably 
		// is closer to production anyway, although one has to know from where the resource will be accessed.
		// For generic web app on the net, what are the repercussions?...must find a way. I think an option is
		// to put a totally separate Servlet Filter into the chain that adds the following headers.
		//
		
        //.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Origin", "*"))
        //.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Methods", "POST, GET"))
       // .addHeaderWriter(new StaticHeadersWriter("Access-Control-Max-Age", "3600"))
        //.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Credentials", "true"))
        //.addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Headers", "Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization"));
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration configuration = new CorsConfiguration();

	    List<String> allowMethods = Arrays.asList( "GET", "POST", "PUT", "DELETE" ) ;
	    List<String> any = Arrays.asList("*");

	    if( !(corsOriginsStr == null || "".equals(corsOriginsStr)) ) {
	    	logger.info( "Security Configuration: corsOriginsStr=" + corsOriginsStr ) ;
	    	List<String> allowOrigins = Arrays.asList( corsOriginsStr ) ;
	    	configuration.setAllowedOrigins( allowOrigins );
	    }
	    if( !(corsOriginsPatternStr == null || "".equals(corsOriginsPatternStr)) ) {
		    logger.info( "Security Configuration: corsOriginsPatternStr=" + corsOriginsPatternStr ) ;
		    List<String> patterns = Arrays.asList( corsOriginsPatternStr );
		    for( String pattern : patterns ) {
		    	configuration.addAllowedOriginPattern( pattern ) ;
		    }
	    }
	    configuration.setAllowedMethods( allowMethods );
	    configuration.setAllowedHeaders( any );
	    //in case authentication is enabled this flag MUST be set, otherwise CORS requests will fail
	    configuration.setAllowCredentials(true);
	    
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", configuration);

	    return source;
	}
}
