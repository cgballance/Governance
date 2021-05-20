package com.webforged.enforcer.management.data;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface BuildItemsRepository extends CrudRepository<BuildItem, Long> {

	@Query(value="SELECT * FROM BuildItems WHERE build_id = :build_id ORDER BY group_name, artifact_name ASC" )
	List<BuildItem> findByBuildId(Long build_id );
	
	// TODO - not surfaced as a service
	@Modifying
	@Query(value="DELETE FROM BuildItems WHERE build_id IN (SELECT build_id from Builds WHERE project_id = :projectId) " )
	int deleteByProjectId(Long projectId);
}