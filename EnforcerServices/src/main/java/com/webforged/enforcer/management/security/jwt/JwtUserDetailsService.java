package com.webforged.enforcer.management.security.jwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtUserDetailsService implements UserDetailsService {
	//
	// simple data structure for linear search for known principals with plaintext passwords...obviously not production stuff.
	//
    private static List<UserObject> users = new ArrayList<UserObject>();

    public JwtUserDetailsService() {
        users.add(new UserObject("chas", "chas", new String[]{ "read_governance", "write_governance", "SUPERUSER_architect" }));
        users.add(new UserObject("dev", "dev", new String[]{ "read_governance", "write_governance", "SUPERUSER_architect" }));
        users.add(new UserObject("nathan", "nathan", new String[]{ "read_governance", "write_governance", "FOO_architect", "BAR_architect" }));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserObject> user = users.stream()
                                         .filter(u -> u.name.equals(username))
                                         .findAny();
        if (!user.isPresent()) {
            throw new UsernameNotFoundException("User not found by name: " + username);
        }
        return toUserDetails(user.get());
    }

    private UserDetails toUserDetails(UserObject userObject) {
    	UserBuilder builder = null ;

        builder = org.springframework.security.core.userdetails.User.withUsername( userObject.name );
        builder.password(new BCryptPasswordEncoder().encode( userObject.password ) );
        builder.roles(userObject.role);
        return builder.build();
    }

    private static class UserObject {
        private String name;
        private String password;
        private String[] role;

        public UserObject(String name, String password, String[] role) {
            this.name = name;
            this.password = password;
            this.role = role;
        }
    }
}