package com.webforged.enforcer.management.data;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface BuildsRepository extends CrudRepository<Build, Long> {
	@Query(value="SELECT * FROM Builds WHERE component_id = :component_id ORDER BY build_ts DESC" )
	List<Build> findByComponentId(Long component_id );

	@Query(value="SELECT a.* FROM Builds a, Components b WHERE b.project_id = :project_id AND a.component_id = b.component_id ORDER BY a.build_ts DESC" )
	List<Build> findByProjectId(Long project_id );
	
	@Query(value="SELECT a.* FROM Builds a, Projects b, Components c WHERE b.acronym = :acronym and c.build_id = a.build_id and b.project_id = c.project_id ORDER BY a.build_ts DESC" )
	List<Build> findByProjectAcronym(String acronym );
	
	// TODO - not surfaced as a service
	@Modifying
	@Query(value="DELETE FROM Builds WHERE component_id IN (SELECT component_id FROM Components WHERE project_id = :projectId)" )
	int deleteByProjectId(Long projectId);
}