package com.webforged.enforcer.management.data;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
public interface ProjectsRepository extends CrudRepository<Project, Long> {

	@Query(value="SELECT * FROM Projects WHERE acronym = :acronym ORDER BY acronym, component ASC" )
	List<Project> findByAcronym(String acronym );

	@Query(value="SELECT * FROM Projects WHERE it_owner = :it_owner ORDER BY acronym, component ASC" )
	List<Project> findByITOwner(String it_owner );
	
	@Query(value="SELECT * FROM Projects WHERE business_owner = :business_owner ORDER BY acronym, component ASC" )
	List<Project> findByBusinessOwner(String business_owner );
	
	@Query(value="SELECT a.* FROM Projects a, AllowedArtifacts b WHERE (b.artifact_id = :artifactId AND a.project_id = b.project_id) "
			+ "UNION ALL SELECT a.* FROM Projects a, LicensedArtifacts b WHERE (b.artifact_id = :artifactId AND a.project_id = b.project_id)" )
	List<Project> findPermittedProjectsByArtifactId(Long artifactId );
}