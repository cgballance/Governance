package com.webforged.enforcer.management.services;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.webforged.enforcer.management.data.AllowedArtifactsRepository;
import com.webforged.enforcer.management.util.Jsr310NullConverters;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.AllowedartifactsApi;
import com.webforged.enforcer.openapi.api.AllowedartifactsApiDelegate;
import com.webforged.enforcer.openapi.model.AllowedArtifact;
import com.webforged.enforcer.openapi.model.ProjectArtifactKeys;

@Service
@CrossOrigin
public class AllowedArtifactsService implements AllowedartifactsApiDelegate {
	Logger logger = LoggerFactory.getLogger( AllowedArtifactsService.class ) ;
	private final AllowedArtifactsRepository repository;
	
	public AllowedArtifactsService( AllowedArtifactsRepository repository ) {
		this.repository = repository;
	}
	
    /**
     * GET /allowedartifacts/{allowed_artifact_id} : Find allowed artifact by ID
     * Returns a single allowed artifact
     *
     * @param allowedArtifactId ID of artifact to return (required)
     * @return successful operation (status code 200)
     *         or Invalid ID supplied (status code 400)
     *         or AllowedArtifact not found (status code 404)
     * @see AllowedartifactsApi#findAllowedArtifactById
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<AllowedArtifact> findAllowedArtifactById(Long allowedArtifactId) {
    	try {
        	com.webforged.enforcer.management.data.AllowedArtifact dtoAllowedArtifact ;
        	AllowedArtifact apiAllowedArtifact = null;
    		dtoAllowedArtifact = repository.findById(allowedArtifactId).orElse(null);
    		if( dtoAllowedArtifact == null ) {
    			return new ResponseEntity<AllowedArtifact>( apiAllowedArtifact, HttpStatus.NOT_FOUND) ;
    		}
    		apiAllowedArtifact = convertAllowedArtifactDTOToAllowedArtifactAPI(dtoAllowedArtifact);
    		return new ResponseEntity<AllowedArtifact>(apiAllowedArtifact, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findAllowedArtifactById " + allowedArtifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact insert error " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }


	/**
     * GET /allowedartifacts/findAllowedArtifactsByProjectAcronym?acronym=x : Find artifact by acronym
     * Returns a single artifact
     *
     * @param artifactId ID of artifact to return (required)
     * @return successful operation (status code 200)
     *         or Invalid ID supplied (status code 400)
     *         or Artifact not found (status code 404)
     * @see AllowedartifactsApi#getArtifactById
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity< List<AllowedArtifact>> findAllowedArtifactsByProjectAcronym(String acronym) {
		try {
			List<com.webforged.enforcer.management.data.AllowedArtifact> dtoArtifact ;
			List<AllowedArtifact> apiArtifact = null ;
			dtoArtifact = repository.findByProject(acronym) ;
			if( dtoArtifact == null ) {
				return new ResponseEntity<List<AllowedArtifact>>( apiArtifact, HttpStatus.NOT_FOUND) ;
			}	
			apiArtifact = dtoArtifact.stream()
				.map( AllowedArtifactsService::convertAllowedArtifactDTOToAllowedArtifactAPI )
				.collect( Collectors.toList() );

			return new ResponseEntity<List<AllowedArtifact>>(apiArtifact, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findAllowedArtifactsByProjectAcronym " + acronym + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact insert error " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}
	
    /**
     * GET /allowedartifacts/findByArtifactId : Finds Allowed Artifacts by artifact id
     * Find allowedartifacts given a artifact id
     *
     * @param artifactId artifact id  value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or not found (status code 404)
     *         or null (status code 500)
     * @see AllowedartifactsApi#findAllowedArtifactsByArtifactId
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<AllowedArtifact>> findAllowedArtifactsByArtifactId(Long artifactId) {
		List<com.webforged.enforcer.management.data.AllowedArtifact> dtoArtifact ;
		List<AllowedArtifact> apiArtifact = null ;
		try {
			dtoArtifact = repository.findByArtifactId(artifactId) ;
			if( dtoArtifact == null ) {
				return new ResponseEntity<List<AllowedArtifact>>( apiArtifact, HttpStatus.NOT_FOUND) ;
			}
			apiArtifact = dtoArtifact.stream()
				.map( AllowedArtifactsService::convertAllowedArtifactDTOToAllowedArtifactAPI )
				.collect( Collectors.toList() );

			return new ResponseEntity<List<AllowedArtifact>>(apiArtifact, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findAllowedArtifactsByArtifactId " + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact insert error " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
	/**
     * GET /allowedartifacts/findAllowedArtifactsByProject?pjoject_id=x : Find artifact by ID
     * Returns a single artifact
     *
     * @param artifactId ID of artifact to return (required)
     * @return successful operation (status code 200)
     *         or Invalid ID supplied (status code 400)
     *         or Artifact not found (status code 404)
     * @see AllowedartifactsApi#getArtifactById
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity< List<AllowedArtifact>> findAllowedArtifactsByProject(Long projectId) {
		List<com.webforged.enforcer.management.data.AllowedArtifact> dtoArtifact ;
		List<AllowedArtifact> apiArtifact = null ;
		try {
			dtoArtifact = repository.findByProject(projectId) ;
			if( dtoArtifact == null ) {
				return new ResponseEntity<List<AllowedArtifact>>( apiArtifact, HttpStatus.NOT_FOUND) ;
			}
			apiArtifact = dtoArtifact.stream()
				.map( AllowedArtifactsService::convertAllowedArtifactDTOToAllowedArtifactAPI )
				.collect( Collectors.toList() );

			return new ResponseEntity<List<AllowedArtifact>>(apiArtifact, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findAllowedArtifactsByProject " + projectId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact insert error " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}
	
    /**
     * POST /allowedartifacts : Add a new artifact to the governance store
     *
     * @param artifact Artifact object that needs to be added to the store (required)
     * @return Invalid input (status code 405)
     * @see AllowedartifactsApi#addArtifact
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<AllowedArtifact> addAllowedArtifact(AllowedArtifact artifact) {
		com.webforged.enforcer.management.data.AllowedArtifact dtoArtifact ;
		AllowedArtifact apiArtifact = null ;
		
		logger.info( getClass().getName() + " addAllowedArtifact " + artifact );
		try {
			// the id is supposed to be null on an insert.
			if( artifact.getAllowedArtifactId() != null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.BAD_REQUEST.value() );
				e.setType( "Cannot supply an id to a new AllowedArtifact" );
				throw new WrappedErrorException( e ) ;
			}
			dtoArtifact = convertAllowedArtifactAPIToAllowedArtifactDTO(artifact);
			dtoArtifact = repository.save(dtoArtifact);
			apiArtifact = convertAllowedArtifactDTOToAllowedArtifactAPI(dtoArtifact);
	        return new ResponseEntity<AllowedArtifact>(apiArtifact, HttpStatus.CREATED);
		} catch( Exception others ) {
			logger.error( "addAllowedArtifact " + artifact + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact insert error " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
    /**
     * DELETE /artifacts/{artifact_id} : Deletes a artifact
     *
     * @param artifactId Artifact id to delete (required)
     * @param apiKey  (optional)
     * @return Invalid ID supplied (status code 400)
     *         or artifact not found (status code 404)
     * @see AllowedartifactsApi#deleteArtifact
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteAllowedArtifact(Long allowedArtifactId) {
    	try {
    		repository.deleteById( allowedArtifactId );
    		return new ResponseEntity<>(HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "deleteAllowedArtifact " + allowedArtifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact delete error " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }
    
    /**
     * POST /allowedartifacts/deleteAllowedArtifactsByArtifactId : Deletes allowedartifact(s) by artifact id
     *
     * @param artifactId kill all records referencing this (required)
     * @return OK (status code 200)
     *         or null (status code 500)
     * @see AllowedartifactsApi#deleteAllowedArtifactsByArtifactId
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteAllowedArtifactsByArtifactId(Long artifactId) {
    	try {
    		repository.deleteByArtifactId( artifactId );
    		return new ResponseEntity<>(HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "deleteLicensedArtifactsByArtifactId " + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact delete error " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}

    }
    
    /**
     * DELETE /allowedartifacts/deleteAllowedArtifact : Deletes an artifact by project/artifact ids
     *
     * @param projectId project id in scope (required)
     * @param artifactId artifact id in scope (required)
     * @return OK (status code 200)
     *         or null (status code 500)
     * @see AllowedartifactsApi#deleteAllowedArtifactByProjectIdAndArtifactId
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteAllowedArtifactByProjectIdAndArtifactId(ProjectArtifactKeys projectArtifactKeys) {
    	Long projectId = projectArtifactKeys.getProjectId() ;
        Long artifactId = projectArtifactKeys.getArtifactId() ;
    	try {
    		repository.deleteByProjectIdAndArtifactId(projectId, artifactId);
    		return new ResponseEntity<>(HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "deleteAllowedArtifactByProjectIdAndArtifactId " + projectId + "," + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "AllowedArtifact delete error " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}

    }
	
    /**
     * PUT /artifacts : Update an existing artifact
     *
     * @param artifact Artifact object that needs to be updated in the store (required)
     * @return Invalid ID supplied (status code 400)
     *         or Artifact not found (status code 404)
     *         or Validation exception (status code 405)
     * @see AllowedartifactsApi#updateArtifact
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<AllowedArtifact> updateAllowedArtifact(AllowedArtifact artifact) {
		AllowedArtifact apiArtifact;
		
		try {
			com.webforged.enforcer.management.data.AllowedArtifact dtoArtifact ;
			dtoArtifact = convertAllowedArtifactAPIToAllowedArtifactDTO(artifact);
			if( dtoArtifact.getApproval_architect() != null && dtoArtifact.getApproval_architect().equals("") != true ) {
				if( dtoArtifact.getApproval_ts() == null ) {
					dtoArtifact.setApproval_ts( Instant.now() );
				}
			}
			
			dtoArtifact = repository.save(dtoArtifact);
			apiArtifact = convertAllowedArtifactDTOToAllowedArtifactAPI(dtoArtifact);
			
	        return new ResponseEntity<AllowedArtifact>(apiArtifact, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "updateAllowedArtifact " + artifact + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "update of AllowedArtifact failed: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
	private static AllowedArtifact convertAllowedArtifactDTOToAllowedArtifactAPI(com.webforged.enforcer.management.data.AllowedArtifact dtoArtifact) {
		AllowedArtifact apiArtifact;
		
		//
		// map DTO into API version of semantically the same animal.
		//
		apiArtifact = new AllowedArtifact() ;
		apiArtifact.setAllowedArtifactId( dtoArtifact.getAllowed_artifact_id() );
		apiArtifact.setArtifactId( dtoArtifact.getArtifact_id() );
		apiArtifact.setProjectId( dtoArtifact.getProject_id() );
		apiArtifact.setApprovalArchitect( dtoArtifact.getApproval_architect() );
		apiArtifact.setApprovalTs( Jsr310NullConverters.InstantToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getApproval_ts()) );

		return apiArtifact;
	}
	
	private static com.webforged.enforcer.management.data.AllowedArtifact convertAllowedArtifactAPIToAllowedArtifactDTO(AllowedArtifact apiArtifact) {
		com.webforged.enforcer.management.data.AllowedArtifact dtoArtifact ;
		
		dtoArtifact = new com.webforged.enforcer.management.data.AllowedArtifact() ;
		dtoArtifact.setAllowed_artifact_id( apiArtifact.getAllowedArtifactId() );
		dtoArtifact.setArtifact_id( apiArtifact.getArtifactId() );
		dtoArtifact.setApproval_architect( apiArtifact.getApprovalArchitect() );
		dtoArtifact.setProject_id( apiArtifact.getProjectId() ) ;
		dtoArtifact.setApproval_ts(Jsr310NullConverters.OffsetDateTimeToInstantConverter.INSTANCE.convert(apiArtifact.getApprovalTs())  );
		
		return dtoArtifact;
	}
}
