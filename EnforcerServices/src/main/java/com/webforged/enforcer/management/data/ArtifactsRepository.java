package com.webforged.enforcer.management.data;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface ArtifactsRepository extends CrudRepository<Artifact, Long> {

	@Query(value="SELECT * FROM Artifacts WHERE group_name = :groupName AND (artifact_name = :artifactName OR :artifactName IS NULL) ORDER BY artifact_name, group_name, version_name ASC" )
	List<Artifact> findByGroupAndArtifact(String groupName, String artifactName );

	@Query(value="SELECT * FROM Artifacts WHERE approval_authorization = :name" )
	List<Artifact> findByApprover(String name);
	
	@Query(value="SELECT * FROM Artifacts WHERE approval_date >= :d1 AND approval_date < :d2" )
	List<Artifact> findApprovedByDateRange(LocalDate d1, LocalDate d2);

	@Query(value="SELECT * FROM Artifacts WHERE status = :status ORDER BY group_name,artifact_name,version_name ASC" )
	List<Artifact> findByStatus(String status);
	
	@Query(value="SELECT a.* FROM Artifacts a, AllowedArtifacts b WHERE (a.artifact_id = b.artifact_id and b.project_id = :projectId) UNION ALL " +
				"SELECT a.* FROM Artifacts a, LicensedArtifacts b WHERE (a.artifact_id = b.artifact_id and b.project_id = :projectId) " )
	List<Artifact> findByProjectId(Long projectId);
	
	@Modifying
	@Query(value="INSERT INTO LicensedArtifacts(artifact_id,project_id,approval_architect,approval_ts) " +
			"SELECT artifact_id,project_id,approval_architect,approval_ts FROM AllowedArtifacts WHERE artifact_id = :artifactId" )
	int copyAllowedToLicensed(Long artifactId);

	@Modifying
	@Query(value="INSERT INTO AllowedArtifacts(artifact_id,project_id,approval_architect,approval_ts) " +
			"SELECT artifact_id,project_id,approval_architect,approval_ts FROM LicensedArtifacts WHERE artifact_id = :artifactId" )
	int copyLicensedToAllowed(Long artifactId);
}