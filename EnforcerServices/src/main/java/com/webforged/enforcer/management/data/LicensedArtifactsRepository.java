package com.webforged.enforcer.management.data;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface LicensedArtifactsRepository extends CrudRepository<LicensedArtifact, Long> {
	@Query(value="SELECT * FROM LicensedArtifacts WHERE project_id = :projectId" )
	List<LicensedArtifact> findByProject(Long projectId );
	
	@Query(value="SELECT * FROM LicensedArtifacts WHERE artifact_id = :artifactId" )
	List<LicensedArtifact> findByArtifactId(Long artifactId );
	
	@Query(value="SELECT a.* FROM LicensedArtifacts a, Projects b WHERE b.acronym = :acronym and a.project_id = b.project_id" )
	List<LicensedArtifact> findByProject(String acronym );

	@Query(value="SELECT * FROM LicensedArtifacts WHERE vendor = :vendor" )
	List<LicensedArtifact> findByVendor(String vendor );

	@Query(value="SELECT * FROM LicensedArtifacts WHERE contract = :contract" )
	List<LicensedArtifact> findByContract(String contract );
	
	@Modifying
	@Query(value="DELETE FROM LicensedArtifacts WHERE project_id = :projectId AND artifact_id = :artifactId" )
	int deleteByProjectIdAndArtifactId(Long projectId, Long artifactId);
	
	@Modifying
	@Query(value="DELETE FROM LicensedArtifacts WHERE artifact_id = :artifactId" )
	int deleteByArtifactId(Long artifactId);
	
	// TODO - not surfaced as a service
	@Modifying
	@Query(value="DELETE FROM LicensedArtifacts WHERE project_id = :projectId" )
	int deleteByProjectId(Long projectId);
}