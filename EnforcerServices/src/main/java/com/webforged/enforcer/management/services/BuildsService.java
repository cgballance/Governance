package com.webforged.enforcer.management.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.webforged.enforcer.management.data.BuildsRepository;
import com.webforged.enforcer.management.util.Jsr310NullConverters;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.BuildsApi;
import com.webforged.enforcer.openapi.api.BuildsApiDelegate;
import com.webforged.enforcer.openapi.model.Build;

@Service
@CrossOrigin
public class BuildsService implements BuildsApiDelegate {
	Logger logger = LoggerFactory.getLogger( BuildsService.class ) ;
	private final BuildsRepository repository;
	
	public BuildsService( BuildsRepository repository ) {
		this.repository = repository;
	}
	
    /**
     * POST /builds : Add a new build to the governance store
     *
     * @param build Build object that needs to be added to the store (required)
     * @return Invalid input (status code 405)
     * @see BuildsApi#addBuild
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Build> addBuild(Build build) {
        try {
        	com.webforged.enforcer.management.data.Build dtoBuild ;
        	Build apiBuild = null;
			// the id is supposed to be null on an insert.
			if( build.getBuildId() != null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.BAD_REQUEST.value() );
				e.setType( "Cannot supply an id to a new Build" );
				throw new WrappedErrorException( e ) ;
			}
	    	dtoBuild = convertBuildAPIToBuildDTO(build);
			dtoBuild = repository.save(dtoBuild);
			apiBuild = convertBuildDTOToBuildAPI(dtoBuild);
	        return new ResponseEntity<Build>(apiBuild, HttpStatus.CREATED);
		} catch( Exception others ) {
			logger.error( "addBuild " + build + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem adding build: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

    /**
     * PUT /builds : Update an existing build
     *
     * @param build Build object that needs to be updated in the store (required)
     * @return Invalid ID supplied (status code 400)
     *         or Build not found (status code 404)
     *         or Validation exception (status code 405)
     * @see BuildsApi#updateBuild
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Build> updateBuild(Build build) {
    	Build apiBuild;
    	try {
    		com.webforged.enforcer.management.data.Build dtoBuild ;
    		dtoBuild = convertBuildAPIToBuildDTO(build);
    		dtoBuild = repository.save(dtoBuild) ;
    		apiBuild = convertBuildDTOToBuildAPI(dtoBuild) ;
    		return new ResponseEntity<Build>(apiBuild, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "updateBuild " + build + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem updating build: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }
    
    /**
     * DELETE /builds/{build_id} : Deletes a build
     *
     * @param buildId Build id to delete (required)
     * @return Invalid ID supplied (status code 400)
     *         or build not found (status code 404)
     * @see BuildsApi#deleteBuild
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteBuild( Long buildId ) {
		try {
			repository.deleteById(buildId) ;
			return new ResponseEntity<>(HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "deleteBuild " + buildId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem deleting build: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
	   /**
     * GET /builds/findByComponentId : Find Builds by component id
     * Find builds given a comonent id
     *
     * @param componentId componentId value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     * @see BuildsApi#findBuildsByComponentId
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Build>> findBuildsByComponentId(Long componentId) {
    	try {
    		List<com.webforged.enforcer.management.data.Build> dtoBuild ;
    		List<Build> apiBuild = null ;
    		dtoBuild = repository.findByComponentId(componentId) ;
    		apiBuild = dtoBuild.stream()
				.map( BuildsService::convertBuildDTOToBuildAPI )
				.collect( Collectors.toList() );
    		return new ResponseEntity<List<Build>>(apiBuild, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findBuildsByComponentId " + componentId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }
    

    /**
     * GET /builds/findByProjectAcronym : Find Builds by project acronym
     * Find builds given a project acronym
     *
     * @param acronym acronym value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid project value (status code 400)
     * @see BuildsApi#findBuildsByProjectAcronym
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Build>> findBuildsByProjectAcronym(String acronym) {
    	try {
    		List<com.webforged.enforcer.management.data.Build> dtoBuild ;
    		List<Build> apiBuild = null ;
    		dtoBuild = repository.findByProjectAcronym(acronym) ;
    		if( dtoBuild == null ) {
    			return new ResponseEntity<List<Build>>( apiBuild, HttpStatus.NOT_FOUND) ;
    		}
    		apiBuild = dtoBuild.stream()
				.map( BuildsService::convertBuildDTOToBuildAPI )
				.collect( Collectors.toList() );
    		return new ResponseEntity<List<Build>>(apiBuild, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findBuildsByProjectAcronym " + acronym + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }

    /**
     * GET /builds/findByProjectId : Find Builds by project acronym
     * Find builds given a project id
     *
     * @param projectId projectId value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid project value (status code 400)
     * @see BuildsApi#findBuildsByProjectId
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Build>> findBuildsByProjectId(Long projectId) {
    	try {
    		List<com.webforged.enforcer.management.data.Build> dtoBuild ;
    		List<Build> apiBuild = null ;
    		dtoBuild = repository.findByProjectId(projectId) ;
    		if( dtoBuild == null ) {
    			return new ResponseEntity<List<Build>>( apiBuild, HttpStatus.NOT_FOUND) ;
    		}
    		apiBuild = dtoBuild.stream()
				.map( BuildsService::convertBuildDTOToBuildAPI )
				.collect( Collectors.toList() );
    		return new ResponseEntity<List<Build>>(apiBuild, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findBuildsByProjectId " + projectId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }

    /**
     * GET /builds/{build_id} : Find build by ID
     * Returns a single build
     *
     * @param buildId ID of build to return (required)
     * @return successful operation (status code 200)
     *         or Invalid ID supplied (status code 400)
     *         or Project not found (status code 404)
     * @see BuildsApi#findBuildById
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<Build> findBuildById(Long buildId) {
    	try {
    		com.webforged.enforcer.management.data.Build dtoBuild ;
    		Build apiBuild = null ;
    		dtoBuild = repository.findById(buildId).orElse(null);
    		if( dtoBuild == null ) {
    			return new ResponseEntity<Build>( apiBuild, HttpStatus.NOT_FOUND) ;
    		}
    		apiBuild = convertBuildDTOToBuildAPI(dtoBuild);
    		return new ResponseEntity<Build>(apiBuild, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findBuildById " + buildId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }

	private static Build convertBuildDTOToBuildAPI(com.webforged.enforcer.management.data.Build dtoBuild) {
		Build apiBuild;

		//
		// map DTO into API version of semantically the same animal.
		//
		apiBuild = new Build() ;
		apiBuild.setBuildId( dtoBuild.getBuild_id() );
		apiBuild.setProjectId( dtoBuild.getProject_id() );
		apiBuild.setComponentId( dtoBuild.getComponent_id() ) ;
		apiBuild.setComponentVersion( dtoBuild.getComponent_version() ) ;
		apiBuild.setInfractions( dtoBuild.getInfractions() );
		apiBuild.setBuildTs( Jsr310NullConverters.InstantToOffsetDateTimeConverter.INSTANCE.convert(dtoBuild.getBuild_ts()) );
		apiBuild.setSource( dtoBuild.getSource() ) ;

		return apiBuild;
	}

	private static com.webforged.enforcer.management.data.Build convertBuildAPIToBuildDTO(Build apiBuild) {
		com.webforged.enforcer.management.data.Build dtoBuild ;

		dtoBuild = new com.webforged.enforcer.management.data.Build() ;
		dtoBuild.setBuild_id( apiBuild.getBuildId() );
		dtoBuild.setProject_id( apiBuild.getProjectId() );
		dtoBuild.setComponent_id( apiBuild.getComponentId() );
		dtoBuild.setComponent_version( apiBuild.getComponentVersion() );
		dtoBuild.setInfractions( apiBuild.getInfractions() );
		dtoBuild.setBuild_ts( Jsr310NullConverters.OffsetDateTimeToInstantConverter.INSTANCE.convert(apiBuild.getBuildTs()) );
		dtoBuild.setSource( apiBuild.getSource() ) ;

		return dtoBuild;
	}
}
