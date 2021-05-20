package com.webforged.enforcer.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
 
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan({"com.webforged.enforcer.openapi", "com.webforged.enforcer.management", "com.webforged.enforcer.management.dao", "com.webforged.enforcer.management.services"})
public class GovernanceManagement {
	public static void main(String[] args) {
		SpringApplication.run(GovernanceManagement.class, args);
	}
}