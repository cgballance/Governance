package com.webforged.enforcer.management.data;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
public interface AllowedArtifactsRepository extends CrudRepository<AllowedArtifact, Long> {

	@Query(value="SELECT * FROM AllowedArtifacts WHERE project_id = :projectId" )
	List<AllowedArtifact> findByProject(Long projectId );

	@Query(value="SELECT * FROM AllowedArtifacts WHERE artifact_id = :artifactId" )
	List<AllowedArtifact> findByArtifactId(Long artifactId );
	
	@Query(value="SELECT a.* FROM AllowedArtifacts a, Projects b WHERE b.acronym = :acronym and a.project_id = b.project_id" )
	List<AllowedArtifact> findByProject(String acronym );
	
	@Modifying
	@Query(value="DELETE FROM AllowedArtifacts WHERE project_id = :projectId AND artifact_id = :artifactId" )
	int deleteByProjectIdAndArtifactId(Long projectId, Long artifactId);

	@Modifying
	@Query(value="DELETE FROM AllowedArtifacts WHERE artifact_id = :artifactId" )
	int deleteByArtifactId(Long artifactId);
	
	// TODO - not surfaced as a service
	@Modifying
	@Query(value="DELETE FROM AllowedArtifacts WHERE project_id = :projectId" )
	int deleteByProjectId(Long projectId);
}