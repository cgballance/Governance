package com.webforged.enforcer.management.security.jwt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

abstract public class UserUtil {
	public static Set<String> getRoles() {
		Set<String> userRoles = new HashSet<String>();
		@SuppressWarnings("unchecked")
		Collection<GrantedAuthority> auths = (Collection<GrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
		for( GrantedAuthority auth : auths ) {
			String r = auth.getAuthority().substring( "ROLE_".length() ) ;
			userRoles.add(r);
		}
		return userRoles;
	}
}
