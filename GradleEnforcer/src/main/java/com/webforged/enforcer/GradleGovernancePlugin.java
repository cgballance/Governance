package com.webforged.enforcer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

class GovernanceExtension {
	String jdbcDriverClass ;
	String jdbcUrl ;
	String jdbcUser ;
	String jdbcPassword ;
	
	public GovernanceExtension() {}
}

public class GradleGovernancePlugin implements Plugin<Project> {
	@Override
    public void apply(Project gradleProject) {
    	GovernanceTask gt = gradleProject.getTasks().create( "governance", GovernanceTask.class );
    	gt.getExtensions().create( "GovernanceExtension",  GovernanceExtension.class ) ;
    }
}
