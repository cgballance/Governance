package com.webforged.enforcer.management.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.webforged.enforcer.management.data.BuildItemsRepository;
import com.webforged.enforcer.management.data.BuildsRepository;
import com.webforged.enforcer.management.data.ComponentsRepository;
import com.webforged.enforcer.management.data.LicensedArtifactsRepository;
import com.webforged.enforcer.management.data.ProjectsRepository;
import com.webforged.enforcer.management.security.jwt.UserUtil;
import com.webforged.enforcer.management.util.Jsr310NullConverters;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.ProjectsApi;
import com.webforged.enforcer.openapi.api.ProjectsApiDelegate;
import com.webforged.enforcer.openapi.model.Project;


@Service
@CrossOrigin
public class ProjectsService implements ProjectsApiDelegate {
	Logger logger = LoggerFactory.getLogger( ProjectsService.class ) ;

	private TransactionTemplate transactionTemplate;
	private final ProjectsRepository repository;
	private final AllowedArtifactsRepository allowedRepo ;
	private final LicensedArtifactsRepository licensedRepo ;
	private final BuildItemsRepository builditemsRepo ;
	private final BuildsRepository buildsRepo ;
	private final ComponentsRepository componentsRepo ;

	public ProjectsService( ProjectsRepository repository,
			AllowedArtifactsRepository allowedRepo,
			LicensedArtifactsRepository licensedRepo,
			BuildItemsRepository builditemsRepo,
			BuildsRepository buildsRepo,
			ComponentsRepository componentsRepo,
			PlatformTransactionManager transactionManager ) {
		this.repository = repository;
		this.allowedRepo = allowedRepo;
		this.licensedRepo = licensedRepo ;
		this.builditemsRepo = builditemsRepo ;
		this.buildsRepo = buildsRepo;
		this.componentsRepo = componentsRepo ;
		
	    Assert.notNull(transactionManager, "The 'transactionManager' argument must not be null.");
	    this.transactionTemplate = new TransactionTemplate(transactionManager);
	}
	
    /**
     * GET /projects : retrieve all projects
     * Retrieve all Projects. A project&#39;s acronym and component name provide the key for dealing with projects.
     *
     * @return null (status code 201)
     *         or null (status code 405)
     * @see ProjectsApi#findProjects
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Project>> findProjects() {
		try {
			Set<String> userRoles = UserUtil.getRoles() ;
			
			Iterable<com.webforged.enforcer.management.data.Project> dtoProject ;
			List<Project> apiProject = null ;
			dtoProject = repository.findAll() ;
			// convert to list and do some filtering
			ArrayList<com.webforged.enforcer.management.data.Project> newList = new ArrayList<com.webforged.enforcer.management.data.Project>();
			dtoProject.forEach( newList::add ) ;
			apiProject = newList.stream()
					.filter( p -> userRoles.contains(p.getAcronym()+"_architect") || userRoles.contains("SUPERUSER_architect") )
					.map( ProjectsService::convertProjectDTOToProjectAPI )
					.collect( Collectors.toList() );
			return new ResponseEntity<List<Project>>(apiProject, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findProjects " + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding Projects: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
    /**
     * POST /projects : Add a new project to the governance store
     *
     * @param project Project object that needs to be added to the store (required)
     * @return Invalid input (status code 405)
     * @see ProjectsApi#addProject
     */
	@Override
	@PreAuthorize("hasRole('write_governance') and hasRole('SUPERUSER_architect')")
    public ResponseEntity<Project> addProject(Project project) {
		//
		// before proceeding, make sure this is SUPERUSER_architect
		//
        try {
        	com.webforged.enforcer.management.data.Project dtoProject ;
        	Project apiProject = null;
			if( project.getProjectId() != null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.BAD_REQUEST.value() );
				e.setType( "Cannot supply id on a new Project" );
				e.setDetail( "resubmit without including an id, the service will create a new one." );
				throw new WrappedErrorException( e ) ;
			}
	    	dtoProject = convertProjectAPIToProjectDTO(project);
			dtoProject = repository.save(dtoProject);
			apiProject = convertProjectDTOToProjectAPI(dtoProject);
	        return new ResponseEntity<Project>(apiProject, HttpStatus.CREATED);
		} catch( Exception others ) {
			logger.error( "addProject " + project + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error inserting new project" );
			e.setDetail( others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

    /**
     * PUT /projects : Update an existing project
     *
     * @param project Project object that needs to be updated in the store (required)
     * @return Invalid ID supplied (status code 400)
     *         or Project not found (status code 404)
     *         or Validation exception (status code 405)
     * @see ProjectsApi#updateProject
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Project> updateProject(Project project) {
		Project updatedProject ;
		try {
			Set<String> userRoles = UserUtil.getRoles() ;
			if( !(userRoles.contains(project.getAcronym()+"_architect") || userRoles.contains("SUPERUSER_architect")) ) {
				throw new Exception( "Caller does not have permission to " + project.getAcronym() ) ;
			}
			
			com.webforged.enforcer.management.data.Project dtoProject ;
			dtoProject = convertProjectAPIToProjectDTO(project);
			//
			// validation goes here...
			//
			dtoProject = repository.save(dtoProject) ;
			updatedProject = convertProjectDTOToProjectAPI( dtoProject ) ;
			return new ResponseEntity<Project>(updatedProject,HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "updateProject " + project + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error updating project" );
			e.setDetail( others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
    /**
     * DELETE /projects/{project_id} : Deletes a project
     *
     * @param projectId Project id to delete (required)
     * @return OK (status code 200)
     *         or null (status code 404)
     *         or null (status code 500)
     * @see ProjectsApi#deleteProject
     */
	@Override
	@PreAuthorize("hasRole('write_governance') and hasRole('SUPERUSER_architect')")
    public ResponseEntity<Void> deleteProject(Long projectId) {
		try {
			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			    @Override
			    public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
			    	// kill builditems
			    	builditemsRepo.deleteByProjectId( projectId ) ;
			    	// kill builds
			    	buildsRepo.deleteByProjectId( projectId ) ;
			    	// kill components
			    	componentsRepo.deleteByProjectId( projectId ) ;
			    	// kill allowed artifacts
			    	allowedRepo.deleteByProjectId( projectId ) ;
			    	// kill licensed artifacts
			    	licensedRepo.deleteByProjectId( projectId ) ;
			    	// kill project
					repository.deleteById( projectId );
			    }
			});
			return new ResponseEntity<>(HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "deleteProject " + projectId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem deleting Project" ) ;
			e.setDetail( others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

	/**
	 * GET /projects/findByProjectAcronym : Finds Projects by project acronym
	 * Find projects given a project acronym
	 *
	 * @param acronym acronym value that needs to be considered for filter (required)
	 * @return successful operation (status code 200)
	 *         or Invalid project value (status code 400)
	 * @see ProjectsApi#findProjectsByProjectAcronym
	 */	
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity<List<Project>> findProjectsByAcronym(String acronym) {
		try {
			Set<String> userRoles = UserUtil.getRoles() ;
			if( !(userRoles.contains(acronym+"_architect") || userRoles.contains("SUPERUSER_architect")) ) {
				throw new Exception( "User does not have permission to this project" ) ;
			}
			
			List<com.webforged.enforcer.management.data.Project> dtoProject ;
			List<Project> apiProject = null ;
			dtoProject = repository.findByAcronym(acronym) ;
			apiProject = dtoProject.stream()
				.map( ProjectsService::convertProjectDTOToProjectAPI )
				.collect( Collectors.toList() );
			return new ResponseEntity<List<Project>>(apiProject, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findProjectsByAcronym " + acronym + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding projects" );
			e.setDetail( others.toString() ) ;
			throw new WrappedErrorException( e ) ;
		}
	}
	
	/**
	 * GET /projects/findByBusinessOwner : Finds Projects by business owner
	 * Find projects given a business owner
	 *
	 * @param businessOwner acronym value that needs to be considered for filter (required)
	 * @return successful operation (status code 200)
	 *         or Invalid project value (status code 400)
	 * @see ProjectsApi#findProjectsByBusinessOwner
	 */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity<List<Project>> findProjectsByBusinessOwner(String businessOwner) {
		try {
			List<com.webforged.enforcer.management.data.Project> dtoProject ;
			List<Project> apiProject = null ;
			dtoProject = repository.findByBusinessOwner(businessOwner) ;
			apiProject = dtoProject.stream()
				.map( ProjectsService::convertProjectDTOToProjectAPI )
				.collect( Collectors.toList() );
			return new ResponseEntity<List<Project>>(apiProject, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findProjectsByBusinessOwner " + businessOwner + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding projects" );
			e.setDetail( others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}

	/**
	 * GET /projects/findByITOwner : Finds Projects by it owner
	 * Find projects given a it owner
	 *
	 * @param itOwner acronym value that needs to be considered for filter (required)
	 * @return successful operation (status code 200)
	 *         or Invalid project value (status code 400)
	 * @see ProjectsApi#findProjectsByITOwner
	 */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity<List<Project>> findProjectsByITOwner(String itOwner) {
		try {
			List<com.webforged.enforcer.management.data.Project> dtoProject ;
			List<Project> apiProject = null ;
			dtoProject = repository.findByITOwner(itOwner) ;
			apiProject = dtoProject.stream()
				.map( ProjectsService::convertProjectDTOToProjectAPI )
				.collect( Collectors.toList() );
			return new ResponseEntity<List<Project>>(apiProject, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findProjectsByITOwner " + itOwner + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding projects" );
			e.setDetail( others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}
	
    /**
     * GET /projects/{project_id} : Find project by ID
     * Returns a single project
     *
     * @param projectId ID of project to return (required)
     * @return successful operation (status code 200)
     *         or Invalid ID supplied (status code 400)
     *         or Project not found (status code 404)
     * @see ProjectsApi#getProjectById
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<Project> findProjectById(Long projectId) {
		try {
			com.webforged.enforcer.management.data.Project dtoProject ;
			Project apiProject = null ;
			dtoProject = repository.findById(projectId).orElse(null);
			if( dtoProject == null ) {
				return new ResponseEntity<Project>( apiProject, HttpStatus.NOT_FOUND) ;
			}
			apiProject = convertProjectDTOToProjectAPI(dtoProject);
			return new ResponseEntity<Project>(apiProject, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findProjectById " + projectId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding projects" );
			e.setDetail( others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
    /**
     * GET /projects/findPermittedProjectsByArtifactId : null
     * null
     *
     * @param artifactId artifactId value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or null (status code 500)
     * @see ProjectsApi#findPermittedProjectsByArtifactId
     */
    public ResponseEntity<List<Project>> findPermittedProjectsByArtifactId(Long artifactId) {
		try {
			List<com.webforged.enforcer.management.data.Project> dtoProject ;
			List<Project> apiProject = null ;
			dtoProject = repository.findPermittedProjectsByArtifactId(artifactId) ;
			apiProject = dtoProject.stream()
				.map( ProjectsService::convertProjectDTOToProjectAPI )
				.collect( Collectors.toList() );
			return new ResponseEntity<List<Project>>(apiProject, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findPermittedProjectsByArtifactId " + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error finding projects" );
			e.setDetail( others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

	public static Project convertProjectDTOToProjectAPI(com.webforged.enforcer.management.data.Project dtoProject) {
		Project apiProject;

		//
		// map DTO into API version of semantically the same animal.
		//
		apiProject = new Project() ;
		apiProject.setProjectId( dtoProject.getProject_id() );
		apiProject.setAcronym( dtoProject.getAcronym() );
		apiProject.setBusinessOwner( dtoProject.getBusiness_owner() );
		apiProject.setItOwner( dtoProject.getIt_owner() );
		apiProject.setBeginDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoProject.getBegin_date()) );
		apiProject.setEndDate( Jsr310NullConverters.LocalDateTimeToOffsetDateTimeConverter.INSTANCE.convert(dtoProject.getEnd_date()) );

		return apiProject;
	}

	public static com.webforged.enforcer.management.data.Project convertProjectAPIToProjectDTO(Project apiProject) {
		com.webforged.enforcer.management.data.Project dtoProject ;

		dtoProject = new com.webforged.enforcer.management.data.Project() ;
		dtoProject.setProject_id( apiProject.getProjectId() );
		dtoProject.setAcronym( apiProject.getAcronym() );
		dtoProject.setBusiness_owner( apiProject.getBusinessOwner() );
		dtoProject.setIt_owner( apiProject.getItOwner() );
		dtoProject.setBegin_date( Jsr310NullConverters.OffsetDateTimeToLocalDateTimeConverter.INSTANCE.convert(apiProject.getBeginDate()) );
		dtoProject.setEnd_date( Jsr310NullConverters.OffsetDateTimeToLocalDateTimeConverter.INSTANCE.convert(apiProject.getEndDate()) );

		return dtoProject;
	}
}
