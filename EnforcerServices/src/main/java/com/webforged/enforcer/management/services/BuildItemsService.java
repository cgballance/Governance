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

import com.webforged.enforcer.management.data.BuildItemsRepository;
import com.webforged.enforcer.management.util.WrappedErrorException;

import com.webforged.enforcer.openapi.api.BuilditemsApi;
import com.webforged.enforcer.openapi.api.BuilditemsApiDelegate;
import com.webforged.enforcer.openapi.model.BuildItem;

@Service
@CrossOrigin
public class BuildItemsService implements BuilditemsApiDelegate {
	Logger logger = LoggerFactory.getLogger( BuildItemsService.class ) ;
	
	private final BuildItemsRepository repository;

	public BuildItemsService( BuildItemsRepository repository ) {
		this.repository = repository;
	}

    /**
     * POST /builditems : Add a new builditem to the governance store
     *
     * @param buildItem BuildItem object that needs to be added to the store (required)
     * @return Invalid input (status code 405)
     * @see BuilditemsApi#addBuildItem
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<BuildItem> addBuildItem(BuildItem buildItem) {
        try {
    		com.webforged.enforcer.management.data.BuildItem dtoBuildItem ;
    		BuildItem apiBuildItem = null;
			// the id is supposed to be null on an insert.
			if( buildItem.getBuilditemId() != null ) {
				com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
				e.setStatus( HttpStatus.BAD_REQUEST.value() );
				e.setType( "Cannot supply an id to a new BuildItem" );
				throw new WrappedErrorException( e ) ;
			}
	    	dtoBuildItem = convertBuildItemAPIToBuildItemDTO(buildItem);
			dtoBuildItem = repository.save(dtoBuildItem);
			apiBuildItem = convertBuildItemDTOToBuildItemAPI(dtoBuildItem);
	        return new ResponseEntity<BuildItem>(apiBuildItem, HttpStatus.CREATED);
		} catch( Exception others ) {
			logger.error( "addBuildItem " + buildItem + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem adding..." + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

    /**
     * DELETE /builditems/findByBuildId : Deletes a build item
     *
     * @param buildId BuildItem id to delete (required)
     * @param apiKey  (optional)
     * @return Invalid ID supplied (status code 400)
     *         or build not found (status code 404)
     * @see BuilditemsApi#deleteBuildItem
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<Void> deleteBuildItem(Long builditem_id) {
		try {
			repository.deleteById(builditem_id) ;
			return new ResponseEntity<>(HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "deleteBuildItem " + builditem_id + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem deleting..." + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }
	
    /**
     * GET /builditems/{builditem_id} : Find builditem by ID
     * Returns a single build
     *
     * @param builditemId ID of builditem to return (required)
     * @return successful operation (status code 200)
     *         or null (status code 400)
     *         or null (status code 404)
     * @see BuilditemsApi#findBuildItemById
     */
    public ResponseEntity<BuildItem> findBuildItemById(Long builditemId) {
    	try {
    		com.webforged.enforcer.management.data.BuildItem dtoBuildItem ;
    		BuildItem apiBuildItem = null ;
    		dtoBuildItem = repository.findById(builditemId).orElse(null);
    		if( dtoBuildItem == null ) {
    			return new ResponseEntity<BuildItem>( apiBuildItem, HttpStatus.NOT_FOUND) ;
    		}
    		apiBuildItem = convertBuildItemDTOToBuildItemAPI(dtoBuildItem);
    		return new ResponseEntity<BuildItem>(apiBuildItem, HttpStatus.OK);
    	} catch( Exception others ) {
			logger.error( "findBuildItemById " + builditemId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem finding build items: " + others.toString() );
			throw new WrappedErrorException( e ) ;
    	}
    }

    /**
     * GET /builditems/findByBuildId : Find BuildItems by build id
     * Find builditems given a build id
     *
     * @param buildId buildId value that needs to be considered for filter (required)
     * @return successful operation (status code 200)
     *         or Invalid buildId value (status code 400)
     * @see BuilditemsApi#findBuildItemsByBuildId
     */
	@Override
	@PreAuthorize("hasRole('read_governance')")
    public ResponseEntity<List<BuildItem>> findBuildItemsByBuildId(Long buildId) {
		try {
			List<com.webforged.enforcer.management.data.BuildItem> dtoBuildItem ;
			List<BuildItem> apiBuildItem = null ;
			dtoBuildItem = repository.findByBuildId(buildId) ;
			if( dtoBuildItem == null ) {
				return new ResponseEntity<List<BuildItem>>( apiBuildItem, HttpStatus.NOT_FOUND) ;
			}
			apiBuildItem = dtoBuildItem.stream()
				.map( BuildItemsService::convertBuildItemDTOToBuildItemAPI )
				.collect( Collectors.toList() );
			return new ResponseEntity<List<BuildItem>>(apiBuildItem, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "findBuildItemsByBuildId " + buildId + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem finding build items: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

    /**
     * PUT /builditems : Update an existing build item
     *
     * @param buildItem BuildItem object that needs to be updated in the store (required)
     * @return Invalid ID supplied (status code 400)
     *         or BuildItem not found (status code 404)
     *         or Validation exception (status code 405)
     * @see BuilditemsApi#updateBuildItem
     */
	@Override
	@PreAuthorize("hasRole('write_governance')")
    public ResponseEntity<BuildItem> updateBuildItem(BuildItem buildItem) {
		BuildItem apiBuildItem ;
		try {
			com.webforged.enforcer.management.data.BuildItem dtoBuildItem ;
			dtoBuildItem = convertBuildItemAPIToBuildItemDTO(buildItem);
			dtoBuildItem = repository.save(dtoBuildItem) ;
			apiBuildItem = convertBuildItemDTOToBuildItemAPI(dtoBuildItem);
			return new ResponseEntity<BuildItem>(apiBuildItem, HttpStatus.OK);
		} catch( Exception others ) {
			logger.error( "updateBuildItem " + buildItem + " error: " + others.toString() );
			com.webforged.enforcer.openapi.model.Error e = new com.webforged.enforcer.openapi.model.Error();
			e.setStatus( HttpStatus.INTERNAL_SERVER_ERROR.value() );
			e.setType( "Problem updating buildItem: " + others.toString() );
			throw new WrappedErrorException( e ) ;
		}
    }

	private static BuildItem convertBuildItemDTOToBuildItemAPI(com.webforged.enforcer.management.data.BuildItem dtoBuildItem) {
		BuildItem apiBuildItem;

		//
		// map DTO into API version of semantically the same animal.
		//
		apiBuildItem = new BuildItem() ;
		apiBuildItem.setBuilditemId( dtoBuildItem.getBuilditem_id() );
		apiBuildItem.setBuildId( dtoBuildItem.getBuild_id() );
		apiBuildItem.setGroupName( dtoBuildItem.getGroup_name() );
		apiBuildItem.setArtifactName( dtoBuildItem.getArtifact_name() );
		apiBuildItem.setVersionName( dtoBuildItem.getVersion_name() );
		apiBuildItem.setArtifactStatusSnapshot( dtoBuildItem.getArtifact_status_snapshot() );
		apiBuildItem.setAllowed( dtoBuildItem.getAllowed() );

		return apiBuildItem;
	}

	private static com.webforged.enforcer.management.data.BuildItem convertBuildItemAPIToBuildItemDTO(BuildItem apiBuildItem) {
		com.webforged.enforcer.management.data.BuildItem dtoBuildItem ;

		dtoBuildItem = new com.webforged.enforcer.management.data.BuildItem() ;
		dtoBuildItem.setBuilditem_id( apiBuildItem.getBuilditemId() );
		dtoBuildItem.setBuild_id( apiBuildItem.getBuildId() );
		dtoBuildItem.setGroup_name( apiBuildItem.getGroupName() );
		dtoBuildItem.setArtifact_name( apiBuildItem.getArtifactName() );
		dtoBuildItem.setVersion_name( apiBuildItem.getVersionName() );
		dtoBuildItem.setArtifact_status_snapshot( apiBuildItem.getArtifactStatusSnapshot() );
		dtoBuildItem.setAllowed( apiBuildItem.getAllowed() );

		return dtoBuildItem;
	}
}
