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

import com.webforged.enforcer.management.data.ComponentsRepository;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.ComponentsApi;
import com.webforged.enforcer.openapi.api.ComponentsApiDelegate;
import com.webforged.enforcer.openapi.model.Component;

@Service
@CrossOrigin
public class ComponentsService implements ComponentsApiDelegate {
	Logger logger = LoggerFactory.getLogger( ComponentsService.class ) ;
	
	private final ComponentsRepository repository;

	public ComponentsService( ComponentsRepository repository ) {
		this.repository = repository;
	}
	

    /**
     * POST /components : Add a new Component to a Project
     * Add a new Component. This is typically done automatically through the first build that is executed for a distributable component. A project&#39;s acronym and component name provide the key for dealing with projects.
     *
     * @param component Component object that needs to be added (required)
     * @return null (status code 201)
     *         or null (status code 405)
     * @see ComponentsApi#addComponent
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Component> addComponent(Component component) {
        try {
    		com.webforged.enforcer.management.data.Component dtoComponent ;
    		Component apiComponent = null;
			if( component.getComponentId() != null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.BAD_REQUEST.value() );
				e.setType( "Cannot supply id on a new Component" );
				throw new WrappedErrorException( e ) ;
			}
	    	dtoComponent = convertComponentAPIToComponentDTO(component);
			dtoComponent = repository.save(dtoComponent);
			apiComponent = convertComponentDTOToComponentAPI(dtoComponent);
	        return new ResponseEntity<Component>(apiComponent, HttpStatus.CREATED);
		} catch( IllegalArgumentException others ) {
			logger.error( "addComponent " + component + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error inserting new Project: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

    /**
     * DELETE /components/{component_id} : Deletes a component
     *
     * @param componentId Component id to delete (required)
     * @return null (status code 400)
     *         or null (status code 404)
     * @see ComponentsApi#deleteComponent
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteComponent(Long componentId) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
	

    /**
     * GET /components/findByArtifactId : Finds Components by artifact id
     * Find components given an artifact id
     *
     * @param artifactId artifact id value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     * @see ComponentsApi#findComponentsByArtifactId
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity<List<Component>> findComponentsByArtifactId(Long artifactId) {
		try {
			List<com.webforged.enforcer.management.data.Component> dtoComponent ;
			List<Component> apiComponent = null ;
			dtoComponent = repository.findByArtifactId(artifactId) ;
			if( dtoComponent == null ) {
				return new ResponseEntity<List<Component>>( apiComponent, HttpStatus.NOT_FOUND) ;
			}
			apiComponent = dtoComponent.stream()
					.map( ComponentsService::convertComponentDTOToComponentAPI )
					.collect( Collectors.toList() );

			return new ResponseEntity<List<Component>>(apiComponent, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findComponentsByArtifactId " + artifactId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error retrieving Components: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}
    

    /**
     * GET /components/findByProjectAcronym : Finds Components by project acronym
     * Find components given a project acronym
     *
     * @param acronym acronym value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     * @see ComponentsApi#findComponentsByProjectAcronym
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Component>> findComponentsByProjectAcronym(String acronym) {
		try {
			List<com.webforged.enforcer.management.data.Component> dtoComponent ;
			List<Component> apiComponent = null ;
			dtoComponent = repository.findByAcronym(acronym) ;
			if( dtoComponent == null ) {
				return new ResponseEntity<List<Component>>( apiComponent, HttpStatus.NOT_FOUND) ;
			}
			apiComponent = dtoComponent.stream()
				.map( ComponentsService::convertComponentDTOToComponentAPI )
				.collect( Collectors.toList() );
			return new ResponseEntity<List<Component>>(apiComponent, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findComponentsByProjectAcronym " + acronym + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error retrieving Components: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
    /**
     * GET /components/findByProjectId : Finds Components by project id
     * Find components given a project id
     *
     * @param projectId id value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     * @see ComponentsApi#findComponentsByProjectId
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<Component>> findComponentsByProjectId(Long projectId) {
		try {
			List<com.webforged.enforcer.management.data.Component> dtoComponent ;
			List<Component> apiComponent = null ;
			dtoComponent = repository.findByProjectId(projectId) ;
			if( dtoComponent == null ) {
				return new ResponseEntity<List<Component>>( apiComponent, HttpStatus.NOT_FOUND) ;
			}
			apiComponent = dtoComponent.stream()
				.map( ComponentsService::convertComponentDTOToComponentAPI )
				.collect( Collectors.toList() );
			return new ResponseEntity<List<Component>>(apiComponent, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findComponentsByProjectId " + projectId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error retrieving Components: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

    /**
     * GET /components/{component_id} : Find component by ID
     * Returns a single component
     *
     * @param componentId ID of component to return (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     *         or null (status code 404)
     * @see ComponentsApi#getComponentById
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
	public ResponseEntity<Component> findComponentById(Long componentId) {
		try {
			com.webforged.enforcer.management.data.Component dtoComponent ;
			Component apiComponent = null ;
			dtoComponent = repository.findById(componentId).orElse(null);
			if( dtoComponent == null ) {
				return new ResponseEntity<Component>( apiComponent, HttpStatus.NOT_FOUND) ;
			}
			apiComponent = convertComponentDTOToComponentAPI(dtoComponent);
			return new ResponseEntity<Component>(apiComponent, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findComponentById " + componentId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error retrieving Component: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
	}

    /**
     * PUT /components : Update an existing component
     * Update an existing Component
     *
     * @param component Component object that needs to be updated in the store (required)
     * @return null (status code 400)
     *         or null (status code 404)
     *         or null (status code 405)
     * @see ComponentsApi#updateComponent
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Component> updateComponent(Component component) {
		Component apiComponent;
		
		try {
			com.webforged.enforcer.management.data.Component dtoComponent ;
			dtoComponent = convertComponentAPIToComponentDTO(component);
			dtoComponent = repository.save(dtoComponent) ;
			apiComponent = convertComponentDTOToComponentAPI( dtoComponent ) ;
			
			return new ResponseEntity<Component>(apiComponent, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "updateComponent " + component + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Error updating Component: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

	private static Component convertComponentDTOToComponentAPI(com.webforged.enforcer.management.data.Component dtoComponent) {
		Component apiComponent;

		//
		// map DTO into API version of semantically the same animal.
		//
		apiComponent = new Component() ;
		apiComponent.setComponentId( dtoComponent.getComponent_id() );
		apiComponent.setProjectId( dtoComponent.getProject_id() );
		apiComponent.setName( dtoComponent.getName() );

		return apiComponent;
	}

	private static com.webforged.enforcer.management.data.Component convertComponentAPIToComponentDTO(Component apiComponent) {
		com.webforged.enforcer.management.data.Component dtoComponent ;

		dtoComponent = new com.webforged.enforcer.management.data.Component() ;
		dtoComponent.setComponent_id( apiComponent.getComponentId() );
		dtoComponent.setProject_id( apiComponent.getProjectId() );
		dtoComponent.setName( apiComponent.getName() );

		return dtoComponent;
	}
}
