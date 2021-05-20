package com.webforged.enforcer.management.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.webforged.enforcer.management.dao.ExperimentalDAO;
import com.webforged.enforcer.management.data.Build;
import com.webforged.enforcer.management.data.Component;
import com.webforged.enforcer.management.data.Project;
import com.webforged.enforcer.management.security.jwt.UserUtil;
import com.webforged.enforcer.management.util.Jsr310NullConverters;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.ExperimentalApi;
import com.webforged.enforcer.openapi.api.ExperimentalApiDelegate;
import com.webforged.enforcer.openapi.model.ProjectComponentBuild;


@Service
@CrossOrigin
public class ExperimentalService implements ExperimentalApiDelegate {
	Logger logger = LoggerFactory.getLogger( ExperimentalService.class ) ;
	private final JdbcTemplate jdbcTemplate;
	
	public ExperimentalService( JdbcTemplate jdbcTemplate ) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	
    /**
     * GET /experimental/findProjectComponentBuildsByArtifactId : null
     * null
     *
     * @param artifactId artifactId value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     * @see ExperimentalApi#findProjectComponentByArtifactId
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity<List<ProjectComponentBuild>> findProjectComponentBuildsByArtifactId(Long artifactId) {
		try {
			Set<String> userRoles = UserUtil.getRoles() ;
			
			List<ProjectComponentBuild> pcs = new ArrayList<ProjectComponentBuild>() ;
			ExperimentalDAO dao = new ExperimentalDAO(jdbcTemplate);
			List<Map<String,Object>> rows = dao.findProjectComponentBuildsByArtifactId(artifactId) ;
			for( Map<String,Object> row : rows ) {
				Project p = (Project) row.get("project") ;
				//
				// ok, new security...only let the caller get to projects that they have rights to
				//
				if( !(userRoles.contains(p.getAcronym()+"_architect") || userRoles.contains("SUPERUSER_architect")) ) {
					continue;
				}
				
				Component c = (Component) row.get("component" ) ;
				ProjectComponentBuild pc = new ProjectComponentBuild();
				pc.setProject( convertProjectDTOToProjectAPI( p ) );
				pc.setComponent( convertComponentDTOToComponentAPI( c ) );
				pc.setBuild( convertBuildDTOToBuildAPI( (Build) row.get("build") ) );
				pcs.add( pc ) ;
			}
			return new ResponseEntity<List<ProjectComponentBuild>>(pcs, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findProjectComponentBuildsByArtifactId " + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error retrieving Components: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}
    
	private static com.webforged.enforcer.openapi.model.Project convertProjectDTOToProjectAPI(com.webforged.enforcer.management.data.Project dtoProject) {
		com.webforged.enforcer.openapi.model.Project apiProject;

		//
		// map DTO into API version of semantically the same animal.
		//
		apiProject = new com.webforged.enforcer.openapi.model.Project() ;
		apiProject.setProjectId( dtoProject.getProject_id() );
		apiProject.setAcronym( dtoProject.getAcronym() );
		apiProject.setBusinessOwner( dtoProject.getBusiness_owner() );
		apiProject.setItOwner( dtoProject.getIt_owner() );
		apiProject.setBeginDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoProject.getBegin_date()) );
		apiProject.setEndDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoProject.getBegin_date()) );

		return apiProject;
	}
	
	private static com.webforged.enforcer.openapi.model.Component convertComponentDTOToComponentAPI(com.webforged.enforcer.management.data.Component dtoComponent) {
		com.webforged.enforcer.openapi.model.Component apiComponent;

		//
		// map DTO into API version of semantically the same animal.
		//
		apiComponent = new com.webforged.enforcer.openapi.model.Component() ;
		apiComponent.setComponentId( dtoComponent.getComponent_id() );
		apiComponent.setProjectId( dtoComponent.getProject_id() );
		apiComponent.setName( dtoComponent.getName() );

		return apiComponent;
	}
	
	private static com.webforged.enforcer.openapi.model.Build convertBuildDTOToBuildAPI(com.webforged.enforcer.management.data.Build dtoBuild) {
		com.webforged.enforcer.openapi.model.Build apiBuild;

		//
		// map DTO into API version of semantically the same animal.
		//
		apiBuild = new com.webforged.enforcer.openapi.model.Build() ;
		apiBuild.setBuildId( dtoBuild.getBuild_id() );
		apiBuild.setBuildTs( Jsr310NullConverters.InstantToOffsetDateTimeConverter.INSTANCE.convert(dtoBuild.getBuild_ts()) );
		apiBuild.setComponentVersion( dtoBuild.getComponent_version() );
		apiBuild.setInfractions( dtoBuild.getInfractions() );
		apiBuild.setProjectId( dtoBuild.getProject_id() ) ;

		return apiBuild;
	}
	
}
