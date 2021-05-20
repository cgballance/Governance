package com.webforged.enforcer.management.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.webforged.enforcer.management.data.AllowedArtifactsRepository;
import com.webforged.enforcer.management.data.ArtifactsRepository;
import com.webforged.enforcer.management.data.LicensedArtifactsRepository;
import com.webforged.enforcer.management.util.Jsr310NullConverters;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.ArtifactsApi;
import com.webforged.enforcer.openapi.api.ArtifactsApiDelegate;
import com.webforged.enforcer.openapi.model.Artifact;

@Service
@CrossOrigin
public class ArtifactsService implements ArtifactsApiDelegate {
	
	private TransactionTemplate transactionTemplate;
	private final ArtifactsRepository repository;
	private final AllowedArtifactsRepository allowedRepo ;
	private final LicensedArtifactsRepository licensedRepo ;

	Logger logger = LoggerFactory.getLogger( ArtifactsService.class ) ;
	
	public ArtifactsService( ArtifactsRepository repository,
			AllowedArtifactsRepository allowedRepo,
			LicensedArtifactsRepository licensedRepo,
			PlatformTransactionManager transactionManager ) {
		this.repository = repository;
		this.allowedRepo = allowedRepo;
		this.licensedRepo = licensedRepo ;
		
	    Assert.notNull(transactionManager, "The 'transactionManager' argument must not be null.");
	    this.transactionTemplate = new TransactionTemplate(transactionManager);
	}
	
	/**
     * GET /artifacts/{artifact_id} : Find artifact by ID
     * Returns a single artifact
     *
     * @param artifactId ID of artifact to return (required)
     * @return successful operation (status code 200)
     *         or Invalid ID supplied (status code 400)
     *         or Artifact not found (status code 404)
     * @see ArtifactsApi#findArtifactById
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity<Artifact> findArtifactById(Long artifactId) {
		try {
			com.webforged.enforcer.management.data.Artifact dtoArtifact ;
			Artifact apiArtifact = null ;
			dtoArtifact = repository.findById(artifactId).orElse(null);
			if( dtoArtifact == null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.NOT_FOUND.value() );
				e.setType( "Artifact having artifact id " + artifactId + " not found" );
				throw new WrappedErrorException( e ) ;
			}
			apiArtifact = convertArtifactDTOToArtifactAPI(dtoArtifact);
			return new ResponseEntity<Artifact>(apiArtifact, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findArtifactById " + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching..." + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}
	
    /**
     * POST /artifacts : Add a new artifact to the governance store
     *
     * @param artifact Artifact object that needs to be added to the store (required)
     * @return Invalid input (status code 405)
     * @see ArtifactsApi#addArtifact
     */
	@Override
	@PreAuthorize("hasRole('write_governance') and hasRole('SUPERUSER_architect')")
    public ResponseEntity<Artifact> addArtifact(Artifact artifact) {
		try {
			com.webforged.enforcer.management.data.Artifact dtoArtifact ;
			Artifact apiArtifact = null;
			// the id is supposed to be null on an insert.
			if( artifact.getArtifactId() != null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.BAD_REQUEST.value() );
				e.setType( "artifact id should not be supplied for a new Artifact." );
				throw new WrappedErrorException( e ) ;
			}
			dtoArtifact = convertArtifactAPIToArtifactDTO(artifact);
			dtoArtifact = repository.save(dtoArtifact);
			apiArtifact = convertArtifactDTOToArtifactAPI(dtoArtifact);
			return new ResponseEntity<Artifact>(apiArtifact, HttpStatus.CREATED);
		} catch( Exception others ) {
			logger.error( "addArtifact error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem adding..." + others.toString() );
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
     * @see ArtifactsApi#deleteArtifact
     */
	@Override
	@PreAuthorize("hasRole('write_governance') and hasRole('SUPERUSER_architect')")
    public ResponseEntity<Void> deleteArtifact(Long artifactId) {
		try {
			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			    @Override
			    public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
			    	allowedRepo.deleteByArtifactId( artifactId ) ;
			    	licensedRepo.deleteByArtifactId( artifactId ) ;
					repository.deleteById( artifactId );
			    }
			});
			return new ResponseEntity<>(HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "deleteArtifact " + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem deleting..." + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
    /**
     * GET /artifacts/findByProjectId : Finds Artifacts by project id
     * Find artifacts given a status
     *
     * @param status Status value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     * @see ArtifactsApi#findArtifactsByProjectId
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Artifact>> findArtifactsByProjectId(Long projectId) {
		ArrayList<Artifact> apiArtifacts = new ArrayList<Artifact>() ;
		try {
			List<com.webforged.enforcer.management.data.Artifact> dtoArtifacts = repository.findByProjectId(projectId);
			for( com.webforged.enforcer.management.data.Artifact dtoArtifact : dtoArtifacts ) {
				apiArtifacts.add( ArtifactsService.convertArtifactDTOToArtifactAPI(dtoArtifact)) ;
			}
			return new ResponseEntity<List<Artifact>>(apiArtifacts, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findArtifactsByProjectId " + projectId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem finding artifacts: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
	/**
     * GET /artifacts/findByStatus : Finds Artifacts by status
     * Multiple status values can be provided with comma separated strings
     *
     * @param status Status values that need to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid status value (status code 400)
     * @see ArtifactsApi#findArtifactsByStatus
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Artifact>> findArtifactsByStatus(String status) {
		try {
			ArrayList<Artifact> apiArtifacts = new ArrayList<Artifact>() ;
			List<com.webforged.enforcer.management.data.Artifact> dtoArtifacts = repository.findByStatus(status);
			for( com.webforged.enforcer.management.data.Artifact dtoArtifact : dtoArtifacts ) {
				apiArtifacts.add( ArtifactsService.convertArtifactDTOToArtifactAPI(dtoArtifact)) ;
			}
			return new ResponseEntity<List<Artifact>>(apiArtifacts, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findArtifactsByStatus " + status + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem finding artifacts: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}
	
    /**
     * GET /artifacts/findByApprover : Finds Artifacts by approver
     * find artifacts given an approver id/name
     *
     * @param approver approver value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid approver/not found (status code 400)
     * @see ArtifactsApi#findArtifactsByApprover
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Artifact>> findArtifactsByApprover(String approver) {
		try {
			ArrayList<Artifact> apiArtifacts = new ArrayList<Artifact>() ;
			List<com.webforged.enforcer.management.data.Artifact> dtoArtifacts = repository.findByApprover(approver);
			for( com.webforged.enforcer.management.data.Artifact dtoArtifact : dtoArtifacts ) {
				apiArtifacts.add( ArtifactsService.convertArtifactDTOToArtifactAPI(dtoArtifact)) ;
			}
			return new ResponseEntity<List<Artifact>>(apiArtifacts, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findArtifactsByApprover " + approver + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching..." + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
    
    /**
     * GET /artifacts/findByGroupNameAndArtifactName : Finds Artifacts by group name and artifact name
     * find artifacts given an group name + artifact name
     *
     * @param groupName group name used by filter (required)
     * @param artifactName artifact name used by filter (optional)
     * @return successful operation (status code 200)
     *         or Invalid something (status code 400)
     * @see ArtifactsApi#findArtifactsByGroupNameAndArtifactName
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Artifact>> findArtifactsByGroupNameAndArtifactName(String groupName,
        String artifactName) {
		try {
			ArrayList<Artifact> apiArtifacts = new ArrayList<Artifact>() ;
			List<com.webforged.enforcer.management.data.Artifact> dtoArtifacts = repository.findByGroupAndArtifact(groupName, artifactName);
			for( com.webforged.enforcer.management.data.Artifact dtoArtifact : dtoArtifacts ) {
				apiArtifacts.add( ArtifactsService.convertArtifactDTOToArtifactAPI(dtoArtifact)) ;
			}
			return new ResponseEntity<List<Artifact>>(apiArtifacts, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findArtifactsByGroupNameAndArtifactName " + groupName + ", " + artifactName + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching..." + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
    
    /**
     * GET /artifacts/findApprovedByDateRange : Finds Artifacts by a date range.
     * find artifacts given an date range.
     *
     * @param fromDate starting date used by filter (required)
     * @param toDate ending date used by filter (required)
     * @return successful operation (status code 200)
     *         or Invalid something (status code 400)
     * @see ArtifactsApi#findArtifactsByDateRange
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Artifact>> findArtifactsByDateRange(LocalDate fromDate, LocalDate toDate) {
		try {
			ArrayList<Artifact> apiArtifacts = new ArrayList<Artifact>() ;
			List<com.webforged.enforcer.management.data.Artifact> dtoArtifacts = repository.findApprovedByDateRange(fromDate, toDate);
			for( com.webforged.enforcer.management.data.Artifact dtoArtifact : dtoArtifacts ) {
				apiArtifacts.add( ArtifactsService.convertArtifactDTOToArtifactAPI(dtoArtifact)) ;
			}
			return new ResponseEntity<List<Artifact>>(apiArtifacts, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findArtifactsByDateRange " + fromDate + "-" + toDate + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem searching..." + others.toString() );
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
     * @see ArtifactsApi#updateArtifact
     */
	@Override
	@PreAuthorize("hasRole('write_governance') and hasRole('SUPERUSER_architect')")
    public ResponseEntity<Artifact> updateArtifact(Artifact artifact) {
		com.webforged.enforcer.management.data.Artifact updatedArtifact = null;
		
		//
		// if the is_vendor_licensed is changed, then migrate related records
		// from before change to the 'changed' state.  information loss may occur.
		// Specifically, moving from licensed to allowed will lose the vendor and contract
		// information.  Thus moving back from allowed to licensed will result in empty field
		// values.  Only known change to avert this would be to consolidate both tables into one,
		// leaving values intact, but invisible in any interim state.
		//
		try {
			com.webforged.enforcer.management.data.Artifact dtoArtifact ;
			com.webforged.enforcer.management.data.Artifact dtoPreviousArtifact ;
			
			dtoArtifact = convertArtifactAPIToArtifactDTO(artifact);
			
			dtoPreviousArtifact = repository.findById( artifact.getArtifactId() ).orElse(null);
			if( dtoPreviousArtifact != null ) {
				if( dtoPreviousArtifact.getStatus().equals( dtoArtifact.getStatus()) != true ) {
					//
					// Is this a valid lifecycle change
					//
					if( Artifact.StatusEnum.LIMITED.getValue().equals(dtoPreviousArtifact.getStatus()) ) {
						if( !( Artifact.StatusEnum.LIMITED_DEPRECATED.getValue().equals(dtoArtifact.getStatus()) ||
								Artifact.StatusEnum.GA.getValue().equals(dtoArtifact.getStatus()) ||
								Artifact.StatusEnum.RETIRED.getValue().equals(dtoArtifact.getStatus()) ) ) { // LIMITED => LIMITED_DEPRECATED | RETIRED | GA
							throw new Exception( "LIMITED may progress to LIMITED_DEPRECATED or RETIRED." ) ;
						}
					} else if( Artifact.StatusEnum.LIMITED_DEPRECATED.getValue().equals(dtoPreviousArtifact.getStatus()) ) { // LIMITED_DEPRECATED => LIMITED | RETIRED
						if( !( Artifact.StatusEnum.LIMITED.getValue().equals(dtoArtifact.getStatus()) ||
								Artifact.StatusEnum.RETIRED.getValue().equals(dtoArtifact.getStatus()) ) ) {

							throw new Exception( "LIMITED_DEPRECATED may progress to LIMITED or RETIRED." ) ;
						}
					} else if( Artifact.StatusEnum.CREATED.getValue().equals( dtoPreviousArtifact.getStatus()) ) {
						if( !( Artifact.StatusEnum.LIMITED.getValue().equals(dtoArtifact.getStatus()) ||
								Artifact.StatusEnum.GA.getValue().equals(dtoArtifact.getStatus()) ||
								Artifact.StatusEnum.RETIRED.getValue().equals(dtoArtifact.getStatus()) ) ) { // CREATED => LIMITED | GA | RETIRED
							throw new Exception( "CREATED may progress to LIMITED, GA or RETIRED." ) ;
						}
					} else if( Artifact.StatusEnum.GA.getValue().equals( dtoPreviousArtifact.getStatus()) ) {
						if( !( Artifact.StatusEnum.DEPRECATED.getValue().equals(dtoArtifact.getStatus()) ||
								Artifact.StatusEnum.RETIRED.getValue().equals(dtoArtifact.getStatus()) ) ) { // GA => DEPRECATED | RETIRED
							throw new Exception( "GA may progress to DEPRECATED or RETIRED." ) ;
						}
					} else if( Artifact.StatusEnum.RETIRED.getValue().equals( dtoPreviousArtifact.getStatus()) ) {  // RETIRED => ***
							throw new Exception( "RETIRED may not progress." ) ;
					}
					
					//  Authorization documentation requirements here...
					if( Artifact.StatusEnum.LIMITED_DEPRECATED.getValue().equals(dtoArtifact.getStatus()) ||
							Artifact.StatusEnum.DEPRECATED.getValue().equals(dtoArtifact.getStatus()) ) { 
						if( dtoArtifact.getDeprecation_authorization() == null || 
								"".equals(dtoArtifact.getDeprecation_authorization()) ) {
							throw new Exception( "Must provide Deprecation Authorization" ) ;
						}
						if( dtoArtifact.getDeprecation_date() == null ) {
							dtoArtifact.setDeprecation_date( LocalDateTime.now().plusMonths(6) ); // TODO...externalize 6
						}
						dtoArtifact.setDeprecation_ts( Instant.now() );
					} else if( Artifact.StatusEnum.GA.getValue().equals(dtoArtifact.getStatus()) ) {
						if( dtoArtifact.getApproval_authorization() == null || 
								"".equals(dtoArtifact.getApproval_authorization()) ) {
							throw new Exception( "Must provide Approval Authorization" ) ;
						}
						if( dtoArtifact.getApproval_date() == null ) {
							dtoArtifact.setApproval_date( LocalDateTime.now() );
						}
						dtoArtifact.setApproval_ts( Instant.now() );
					} else if( Artifact.StatusEnum.LIMITED.getValue().equals(dtoArtifact.getStatus()) ) {
						if( dtoArtifact.getApproval_authorization() == null || 
								"".equals(dtoArtifact.getApproval_authorization()) ) {
							throw new Exception( "Must provide Approval Authorization" ) ;
						}
						if( dtoArtifact.getApproval_date() == null ) {
							dtoArtifact.setApproval_date( LocalDateTime.now() );
						}
						dtoArtifact.setApproval_ts( Instant.now() );
					} else if( Artifact.StatusEnum.RETIRED.getValue().equals(dtoArtifact.getStatus()) ) {
						if( dtoArtifact.getRetirement_authorization() == null || 
								"".equals(dtoArtifact.getRetirement_authorization()) ) {
							throw new Exception( "Must provide Retirement Authorization" ) ;
						}
						if( dtoArtifact.getRetirement_date() == null ) {
							dtoArtifact.setRetirement_date( LocalDateTime.now() );
						}
						dtoArtifact.setRetirement_ts( Instant.now() );
					}
					//  END OF Authorization documentation requirements here...
				}
				//
				// enrichment for changes to authorizers.  if name changed, then update the corresponding timestamp.
				// Set the corresponding effective date to now if not provided too. Note that status change above may
				// update various timestamps and datetimes, so may get updated again.  all good, just be aware of that.
				//
				if( dtoArtifact.getApproval_authorization() != null ) {
					if( dtoArtifact.getApproval_authorization().equals(dtoPreviousArtifact.getApproval_authorization() ) != true ) {
						dtoArtifact.setApproval_ts( Instant.now() );
						if( dtoArtifact.getApproval_date() == null ) {
							dtoArtifact.setApproval_date( LocalDateTime.now() );
						}
					}
				}
				if( dtoArtifact.getDeprecation_authorization() != null ) {
					if( dtoArtifact.getDeprecation_authorization().equals(dtoPreviousArtifact.getDeprecation_authorization() ) != true ) {
						dtoArtifact.setDeprecation_ts( Instant.now() );
						if( dtoArtifact.getDeprecation_date() == null ) {
							dtoArtifact.setDeprecation_date( LocalDateTime.now().plusMonths(6) ); // TODO...externalize 6
						}
					}
				}
				if( dtoArtifact.getRetirement_authorization() != null ) {
					if( dtoArtifact.getRetirement_authorization().equals(dtoPreviousArtifact.getRetirement_authorization() ) != true ) {
						dtoArtifact.setRetirement_ts( Instant.now() );
						if( dtoArtifact.getRetirement_date() == null ) {
							dtoArtifact.setRetirement_date( LocalDateTime.now() );
						}
					}
				}
				
				//
				// be aware that this statement is going to do multiple things as a transaction. The else statement
				// If vendor license flag did not change, then the dto's save will be done as a simple repository save.
				// point is, all validation should be done prior to this part of the code.
				//
				if( dtoPreviousArtifact.getIs_vendor_licensed() != dtoArtifact.getIs_vendor_licensed() ) {
					System.err.println( "Updating is_vendor_licensed change detected...") ;
					if ( dtoArtifact.getIs_vendor_licensed() ) { // move from allowedartifacts to licensedartifacts
						transactionTemplate.execute(new TransactionCallbackWithoutResult() {
						    @Override
						    public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
						    	// transfer
						    	repository.copyAllowedToLicensed( dtoArtifact.getArtifact_id() ) ;
						    	// delete
						    	allowedRepo.deleteByArtifactId( dtoArtifact.getArtifact_id() ) ;
								repository.save(dtoArtifact);
						    }
						});
					} else { // move from licensedartifacts to allowedartifacts
						transactionTemplate.execute(new TransactionCallbackWithoutResult() {
						    @Override
						    public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
						    	// transfer
						    	repository.copyLicensedToAllowed( dtoArtifact.getArtifact_id() ) ;
						    	// delete
						    	licensedRepo.deleteByArtifactId( dtoArtifact.getArtifact_id() ) ;
								repository.save(dtoArtifact);
						    }
						});
					}
				} else {
					updatedArtifact = repository.save(dtoArtifact);
				}
			}
			if( updatedArtifact == null ) {
				updatedArtifact = repository.findById( artifact.getArtifactId() ).orElse(null);
			}
			Artifact apiArtifact = convertArtifactDTOToArtifactAPI(updatedArtifact) ;
			
	        return new ResponseEntity<Artifact>(apiArtifact, HttpStatus.OK);
		} catch( Exception others ) {
			others.printStackTrace();
			logger.error( "updateArtifact " + artifact + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem updating artifact: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
	private static Artifact convertArtifactDTOToArtifactAPI(com.webforged.enforcer.management.data.Artifact dtoArtifact) {
		Artifact apiArtifact;
		
		//
		// map DTO into API version of semantically the same animal.
		//
		apiArtifact = new Artifact() ;
		apiArtifact.setArtifactId( dtoArtifact.getArtifact_id() );
		apiArtifact.setGroupName( dtoArtifact.getGroup_name() );
		apiArtifact.setStatus( Artifact.StatusEnum.fromValue(dtoArtifact.getStatus()) );
		apiArtifact.setArtifactName( dtoArtifact.getArtifact_name() );
		apiArtifact.setVersionName( dtoArtifact.getVersion_name() );
		apiArtifact.setCreatedDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getCreated_date()) );
		apiArtifact.setApprovalAuthorization(dtoArtifact.getApproval_authorization());
		apiArtifact.setApprovalDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getApproval_date()) );
		apiArtifact.setApprovalTs( Jsr310NullConverters.InstantToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getApproval_ts()) );
		apiArtifact.setDeprecationAuthorization(dtoArtifact.getDeprecation_authorization() );
		apiArtifact.setDeprecationDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getDeprecation_date()) );
		apiArtifact.setDeprecationTs( Jsr310NullConverters.InstantToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getDeprecation_ts()) );
		apiArtifact.setIsVendorLicensed(dtoArtifact.getIs_vendor_licensed() );
		apiArtifact.setRetirementAuthorization(dtoArtifact.getRetirement_authorization() );
		apiArtifact.setRetirementDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getRetirement_date()) );
		apiArtifact.setRetirementTs( Jsr310NullConverters.InstantToOffsetDateTimeConverter.INSTANCE.convert(dtoArtifact.getRetirement_ts()) );

		return apiArtifact;
	}
	
	private static com.webforged.enforcer.management.data.Artifact convertArtifactAPIToArtifactDTO(Artifact apiArtifact) {
		com.webforged.enforcer.management.data.Artifact dtoArtifact ;
		
		dtoArtifact = new com.webforged.enforcer.management.data.Artifact() ;
		dtoArtifact.setArtifact_id( apiArtifact.getArtifactId() );
		dtoArtifact.setGroup_name( apiArtifact.getGroupName() );
		dtoArtifact.setStatus( apiArtifact.getStatus().getValue() );
		dtoArtifact.setArtifact_name( apiArtifact.getArtifactName() );
		dtoArtifact.setVersion_name( apiArtifact.getVersionName() );
		dtoArtifact.setCreated_date( Jsr310NullConverters.OffsetDateTimeToLocalDateTimeConverter.INSTANCE.convert(apiArtifact.getCreatedDate()) );
		dtoArtifact.setApproval_authorization(apiArtifact.getApprovalAuthorization());
		dtoArtifact.setApproval_date( Jsr310NullConverters.OffsetDateTimeToLocalDateTimeConverter.INSTANCE.convert(apiArtifact.getApprovalDate()) );
		dtoArtifact.setApproval_ts( Jsr310NullConverters.OffsetDateTimeToInstantConverter.INSTANCE.convert(apiArtifact.getApprovalTs()) );
		dtoArtifact.setDeprecation_authorization(apiArtifact.getDeprecationAuthorization() );
		dtoArtifact.setDeprecation_date( Jsr310NullConverters.OffsetDateTimeToLocalDateTimeConverter.INSTANCE.convert(apiArtifact.getDeprecationDate()) );
		dtoArtifact.setDeprecation_ts( Jsr310NullConverters.OffsetDateTimeToInstantConverter.INSTANCE.convert(apiArtifact.getDeprecationTs()) );
		dtoArtifact.setIs_vendor_licensed(apiArtifact.getIsVendorLicensed() );
		dtoArtifact.setRetirement_authorization(apiArtifact.getRetirementAuthorization() );
		dtoArtifact.setRetirement_date( Jsr310NullConverters.OffsetDateTimeToLocalDateTimeConverter.INSTANCE.convert(apiArtifact.getRetirementDate()) );
		dtoArtifact.setRetirement_ts( Jsr310NullConverters.OffsetDateTimeToInstantConverter.INSTANCE.convert(apiArtifact.getRetirementTs()) );
		
		return dtoArtifact;
	}
}
