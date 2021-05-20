package com.webforged.enforcer.management.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.webforged.enforcer.management.data.Build;
import com.webforged.enforcer.management.data.BuildRowMapper;
import com.webforged.enforcer.management.data.Component;
import com.webforged.enforcer.management.data.ComponentRowMapper;
import com.webforged.enforcer.management.data.Project;
import com.webforged.enforcer.management.data.ProjectRowMapper;

@org.springframework.stereotype.Component
public class ExperimentalDAO {
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public ExperimentalDAO(JdbcTemplate jdbcTemplate) {
	    this.jdbcTemplate = jdbcTemplate;
	}
	
	
	/**
	 * Return the project/component/build information for the latest build of any project component that
	 * uses an artifact.  The build may not have succeeded and may not be the actual build in production,
	 * it is just what developers are currently trying to push through the system.
	 * 
	 * @param artifactId
	 * @return
	 */

	public List<Map<String,Object>> findProjectComponentBuildsByArtifactId(Long artifactId) {
	   String sql = "SELECT DISTINCT p.*, c.*, b.* " +
				"FROM components c, builds b, projects p, builditems i, artifacts a, " +
			    "(SELECT max(build_ts) build_ts from builds GROUP BY component_id) as j " +
				"WHERE " +
					"a.artifact_id = ? " +
					"and a.group_name = i.group_name " +
					"and a.artifact_name = i.artifact_name " +
					"and a.version_name = i.version_name " +
					"and i.build_id = b.build_id " +
					"and b.component_id = c.component_id " +
					"and c.project_id = p.project_id " +
					"and b.build_ts = j.build_ts " +
				"ORDER BY p.acronym, c.name" ;
	   ProjectRowMapper prm = new ProjectRowMapper();
	   ComponentRowMapper crm = new ComponentRowMapper();
	   BuildRowMapper brm = new BuildRowMapper();
	   RowMapper<Map<String,Object>> mapper = new RowMapper<Map<String,Object>>() {
	        public Map<String,Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
	        	Project p = prm.mapRow(rs, rowNum);
	        	Component c = crm.mapRow(rs, rowNum);
	        	Build b = brm.mapRow( rs, rowNum) ;
	        	Map<String,Object> map = new HashMap<String,Object>() ;
	        	map.put( "project", p ) ;
	        	map.put( "component", c ) ;
	        	map.put( "build", b );
	            return map ;
	        }
	    };
	    return (List<Map<String,Object>>) jdbcTemplate.query(sql, new Object[] {artifactId}, new int[] { java.sql.Types.INTEGER }, mapper);
	}
}
