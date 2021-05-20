package com.webforged.enforcer.management.data;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface ComponentsRepository extends CrudRepository<Component, Long> {
	@Query(value="SELECT a.* FROM Components a, Projects b WHERE a.project_id = b.project_id AND b.acronym = :acronym" )
	List<Component> findByAcronym(String acronym );
	
	@Query(value="SELECT * FROM Components WHERE project_id = :projectId" )
	List<Component> findByProjectId(Long projectId);
	
	/** Looking into how to use embeddable/embedded with OpenAPI and Spring REST Data.
	 *  This is replaced by ExperimentalService + DAO's implementation, as this will not scale
	 *  with marrying back up with project info(same query, just p.* used).
	 * @param artifactId
	 * @return
	 */
	@Query(value="SELECT p.acronym, c.* "
			+ "FROM components c, builds b, projects p, builditems i, artifacts a, " +
			"   (SELECT component_id, MAX(build_ts) build_ts from builds group by component_id) foo "
			+ "WHERE "
			+ "	a.artifact_id = :artifactId "
			+ "	and a.group_name = i.group_name "
			+ "	and a.artifact_name = i.artifact_name "
			+ "	and a.version_name = i.version_name "
			+ "	and i.build_id = b.build_id "
			+ "	and b.component_id = c.component_id "
			+ "	and c.project_id = p.project_id "
			+ "    and (b.component_id = foo.component_id and b.build_ts = foo.build_ts) "
			+ "ORDER BY p.acronym, c.name")
	List<Component> findByArtifactId(Long artifactId) ;
	
	// TODO - not surfaced as a service
	@Modifying
	@Query(value="DELETE FROM Components WHERE project_id = :projectId" )
	int deleteByProjectId(Long projectId);
}