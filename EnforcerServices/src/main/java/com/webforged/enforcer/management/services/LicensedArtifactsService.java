package com.webforged.enforcer.management.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.webforged.enforcer.management.data.LicensedArtifactsRepository;
import com.webforged.enforcer.management.util.Jsr310NullConverters;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.LicensedartifactsApiDelegate;
import com.webforged.enforcer.openapi.model.LicensedArtifact;
import com.webforged.enforcer.openapi.model.ProjectArtifactKeys;
import com.webforged.enforcer.openapi.api.LicensedartifactsApi;

@Service
@CrossOrigin
public class LicensedArtifactsService implements LicensedartifactsApiDelegate {
	Logger logger = LoggerFactory.getLogger( LicensedArtifactsService.class ) ;
	
	private final LicensedArtifactsRepository repository;
	
	public LicensedArtifactsService( LicensedArtifactsRepository repository ) {
		this.repository = repository;
	}

    /**
     * POST /licensedartifacts : Add a new licensed artifact to the governance store
     *
     * @param licensedArtifact LicensedArtifact object that needs to be added to the store (required)
     * @return Invalid input (status code 405)
     * @see LicensedartifactsApi#addLicensedArtifact
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<LicensedArtifact> addLicensedArtifact(LicensedArtifact artifact) {
		com.webforged.enforcer.management.data.LicensedArtifact dtoLicensedArtifact ;
		LicensedArtifact apiLicensedArtifact = null ;
		try {
			// the id is supposed to be null on an insert.
			if( artifact.getLicensedArtifactId() != null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.BAD_REQUEST.value() );
				e.setType( "Cannot supply id on a new LicensedArtifact" );
				throw new WrappedErrorException( e ) ;
			}
			dtoLicensedArtifact = convertLicensedArtifactAPIToLicensedArtifactDTO(artifact);
			dtoLicensedArtifact = repository.save(dtoLicensedArtifact);
			apiLicensedArtifact = convertLicensedArtifactDTOToLicensedArtifactAPI(dtoLicensedArtifact);
	        return new ResponseEntity<LicensedArtifact>(apiLicensedArtifact, HttpStatus.CREATED);
		} catch( Exception others ) { // 
			logger.error( "addLicensedArtifact " + artifact + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error inserting new LicensedArtifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	

    /**
     * POST /licensedartifacts/deleteLicensedArtifactsByArtifactId : Deletes licensedartifact(s) by artifact id
     *
     * @param artifactId kill all records referencing this (required)
     * @return OK (status code 200)
     *         or null (status code 500)
     * @see LicensedartifactsApi#deleteLicensedArtifactsByArtifactId
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteLicensedArtifactsByArtifactId(Long artifactId) {
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
     * DELETE /licensedartifacts/deleteLicensedArtifactByProjectIdAndArtifactId : Deletes an artifact by project/artifact ids
     *
     * @param projectId project id in scope (required)
     * @param artifactId artifact id in scope (required)
     * @return OK (status code 200)
     *         or null (status code 500)
     * @see LicensedartifactsApi#deleteLicensedArtifactByProjectIdAndArtifactId
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteLicensedArtifactByProjectIdAndArtifactId(ProjectArtifactKeys projectArtifactKeys) {
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
     * DELETE /licensedartifacts/{artifact_id} : Deletes a artifact
     *
     * @param artifact_id LicensedArtifact id to delete (required)
     * @param apiKey  (optional)
     * @return Invalid ID supplied (status code 400)
     *         or artifact not found (status code 404)
     * @see LicensedArtifactsApi#deleteArtifact
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteLicensedArtifact(Long licensedArtifactId) {
    	try {
    		repository.deleteById( licensedArtifactId );
    		return new ResponseEntity<>(HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "deleteLicensedArtifact " + licensedArtifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error inserting new LicensedArtifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }
	
    /**
     * PUT /licensedartifacts : Update an existing licensedartifact
     *
     * @param licensedArtifact Licensed Artifact object that needs to be updated in the store (required)
     * @return Invalid ID supplied (status code 400)
     *         or LicensedArtifact not found (status code 404)
     *         or Validation exception (status code 405)
     * @see LicensedartifactsApi#updateLicensedArtifact
     */
    @Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<LicensedArtifact> updateLicensedArtifact(LicensedArtifact licensedArtifact) {
    	LicensedArtifact apiArtifact;
    	
		try {
			com.webforged.enforcer.management.data.LicensedArtifact dtoLicensedArtifact ;
			dtoLicensedArtifact = convertLicensedArtifactAPIToLicensedArtifactDTO(licensedArtifact);
			if( dtoLicensedArtifact.getApproval_architect() != null && dtoLicensedArtifact.getApproval_architect().equals("") != true ) {
				if( dtoLicensedArtifact.getApproval_ts() == null ) {
					dtoLicensedArtifact.setApproval_ts( Instant.now() );
				}
			}
			
			dtoLicensedArtifact = repository.save(dtoLicensedArtifact);
			apiArtifact = convertLicensedArtifactDTOToLicensedArtifactAPI(dtoLicensedArtifact) ;
			
	        return new ResponseEntity<LicensedArtifact>(apiArtifact, HttpStatus.OK);
		} catch( IllegalArgumentException others ) {
			logger.error( "updateLicensedArtifact " + licensedArtifact + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error updating LicensedArtifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
    
    /**
     * GET /licensedartifacts/{artifact_id} : Find licensed artifact by ID
     * Returns a single licensed artifact
     *
     * @param licensedArtifactId ID of artifact to return (required)
     * @return successful operation (status code 200)
     *         or Invalid ID supplied (status code 400)
     *         or AllowedArtifact not found (status code 404)
     * @see LicensedartifactsApi#getAllowedArtifactById
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<LicensedArtifact> findLicensedArtifactById(Long licensedArtifactId) {
    	try {
    		com.webforged.enforcer.management.data.LicensedArtifact dtoLicensedArtifact ;
    		LicensedArtifact apiLicensedArtifact = null;
    		dtoLicensedArtifact = repository.findById(licensedArtifactId).orElse(null);
    		if( dtoLicensedArtifact == null ) {
    			return new ResponseEntity<LicensedArtifact>( apiLicensedArtifact, HttpStatus.NOT_FOUND) ;
    		}	
    		apiLicensedArtifact = convertLicensedArtifactDTOToLicensedArtifactAPI(dtoLicensedArtifact);
    		return new ResponseEntity<LicensedArtifact>(apiLicensedArtifact, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findLicensedArtifactById " + licensedArtifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding LicensedArtifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }
    
    /**
     * GET /licensedartifacts/findByArtifactId : Finds Licensed Artifacts by artifact id
     * Find licensedartifacts given a artifact id
     *
     * @param artifactId artifact id  value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or not found (status code 404)
     *         or null (status code 500)
     * @see LicensedartifactsApi#findLicensedArtifactsByArtifactId
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<LicensedArtifact>> findLicensedArtifactsByArtifactId(Long artifactId) {
    	try {
    		List<com.webforged.enforcer.management.data.LicensedArtifact> dtoList ;
    		List<LicensedArtifact> apiList = null ;
    		dtoList = repository.findByArtifactId(artifactId) ;
    		if( dtoList == null || dtoList.size() == 0 ) {
    			apiList = new ArrayList<LicensedArtifact>();
    		} else {	
    			apiList = dtoList.stream()
    					.map( LicensedArtifactsService::convertLicensedArtifactDTOToLicensedArtifactAPI )
    					.collect( Collectors.toList() );
    		}
    		return new ResponseEntity<List<LicensedArtifact>>(apiList, HttpStatus.OK);
    	} catch( Exception others ) {
    		logger.error( "findByArtifactId " + artifactId + " error: " + others.toString() );
    		com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
    		e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
    		e.setType( "Error finding LicensedArtifact: " + others.toString() );
    		throw new WrappedErrorException( e ) ;
    	}
    }
	
    /**
     * GET /licensedartifacts/findByProjectId : Finds Licensed Artifacts by project id
     * Find licensedartifacts given a project id
     *
     * @param projectId project id  value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid project value (status code 400)
     * @see LicensedartifactsApi#findLicensedArtifactsByProject
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<LicensedArtifact>> findLicensedArtifactsByProject(Long projectId) {
    	try {
    		List<com.webforged.enforcer.management.data.LicensedArtifact> dtoList ;
    		List<LicensedArtifact> apiList = null ;
    		dtoList = repository.findByProject(projectId) ;
    		if( dtoList == null || dtoList.size() == 0 ) {
    			apiList = new ArrayList<LicensedArtifact>();
    		} else {	
    			apiList = dtoList.stream()
    					.map( LicensedArtifactsService::convertLicensedArtifactDTOToLicensedArtifactAPI )
    					.collect( Collectors.toList() );
    		}
        	return new ResponseEntity<List<LicensedArtifact>>(apiList, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findLicensedArtifactsByProject " + projectId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding LicensedArtifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }

    /**
     * GET /licensedartifacts/findByProjectAcronym : Finds Licensed Artifacts by project acronym
     * Find licensedartifacts given a project acronym
     *
     * @param acronym acronym value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid project value (status code 400)
     * @see LicensedartifactsApi#findLicensedArtifactsByProjectAcronym
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<LicensedArtifact>> findLicensedArtifactsByProjectAcronym(String acronym) {
    	try {
    		List<com.webforged.enforcer.management.data.LicensedArtifact> dtoList ;
    		List<LicensedArtifact> apiList = null ;
    		dtoList = repository.findByProject(acronym) ;
    		if( dtoList == null ) {
    			return new ResponseEntity<List<LicensedArtifact>>( apiList, HttpStatus.NOT_FOUND) ;
    		}
    		apiList = dtoList.stream()
				.map( LicensedArtifactsService::convertLicensedArtifactDTOToLicensedArtifactAPI )
				.collect( Collectors.toList() );
    		return new ResponseEntity<List<LicensedArtifact>>(apiList, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findLicensedArtifactsByProjectAcronym " + acronym + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding LicensedArtifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }
    
    /**
     * GET /licensedartifacts/findByVendor : Finds Licensed Artifacts by vendor
     * Find licensedartifacts given a project acronym
     *
     * @param vendor vendor value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid project value (status code 400)
     * @see LicensedartifactsApi#findLicensedArtifactsByVendor
     */
    @Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<LicensedArtifact>> findLicensedArtifactsByVendor(String vendor) {
    	try {
    		List<com.webforged.enforcer.management.data.LicensedArtifact> dtoList ;
    		List<LicensedArtifact> apiList = null ;
    		dtoList = repository.findByVendor(vendor) ;
    		if( dtoList == null ) {
    			return new ResponseEntity<List<LicensedArtifact>>( apiList, HttpStatus.NOT_FOUND) ;
    		}
    		apiList = dtoList.stream()
				.map( LicensedArtifactsService::convertLicensedArtifactDTOToLicensedArtifactAPI )
				.collect( Collectors.toList() );
    		return new ResponseEntity<List<LicensedArtifact>>(apiList, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findLicensedArtifactsByVendor " + vendor + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding LicensedArtifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }

	private static LicensedArtifact convertLicensedArtifactDTOToLicensedArtifactAPI(com.webforged.enforcer.management.data.LicensedArtifact dtoLicensedArtifact) {
		LicensedArtifact apiLicensedArtifact;
		
		//
		// map DTO into API version of semantically the same animal.
		//
		apiLicensedArtifact = new LicensedArtifact() ;
		apiLicensedArtifact.setLicensedArtifactId( dtoLicensedArtifact.getLic_artifact_id() );
		apiLicensedArtifact.setArtifactId( dtoLicensedArtifact.getArtifact_id() );
		apiLicensedArtifact.setProjectId( dtoLicensedArtifact.getProject_id() );
		apiLicensedArtifact.setContract( dtoLicensedArtifact.getContract() );
		apiLicensedArtifact.setVendor( dtoLicensedArtifact.getVendor() );
		apiLicensedArtifact.setApprovalArchitect( dtoLicensedArtifact.getApproval_architect() );
		apiLicensedArtifact.setApprovalTs( Jsr310NullConverters.InstantToOffsetDateTimeConverter.INSTANCE.convert(dtoLicensedArtifact.getApproval_ts()) );

		return apiLicensedArtifact;
	}
	
	private static com.webforged.enforcer.management.data.LicensedArtifact convertLicensedArtifactAPIToLicensedArtifactDTO(LicensedArtifact apiLicensedArtifact) {
		com.webforged.enforcer.management.data.LicensedArtifact dtoLicensedArtifact ;
		
		dtoLicensedArtifact = new com.webforged.enforcer.management.data.LicensedArtifact() ;
		dtoLicensedArtifact.setLic_artifact_id( apiLicensedArtifact.getLicensedArtifactId() );
		dtoLicensedArtifact.setArtifact_id( apiLicensedArtifact.getArtifactId() );
		dtoLicensedArtifact.setProject_id( apiLicensedArtifact.getProjectId() );
		dtoLicensedArtifact.setContract( apiLicensedArtifact.getContract() );
		dtoLicensedArtifact.setVendor( apiLicensedArtifact.getVendor() ) ;
		dtoLicensedArtifact.setApproval_architect( apiLicensedArtifact.getApprovalArchitect() );
		dtoLicensedArtifact.setApproval_ts(Jsr310NullConverters.OffsetDateTimeToInstantConverter.INSTANCE.convert(apiLicensedArtifact.getApprovalTs())  );
		
		return dtoLicensedArtifact;
	}
}
