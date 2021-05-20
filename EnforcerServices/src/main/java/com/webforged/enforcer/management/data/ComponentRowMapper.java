package com.webforged.enforcer.management.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

public class ComponentRowMapper implements RowMapper<Component> {
	static Logger logger = LoggerFactory.getLogger( ComponentRowMapper.class ) ;

	@Override
	public Component mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "mapRow " + rowNum + " into an Project" );
		}
		
		Component dtoArtifact = new Component() ;

		dtoArtifact.setComponent_id( rs.getLong("component_id") );
		dtoArtifact.setProject_id( rs.getLong("project_id") );
		dtoArtifact.setName( rs.getString("name") );

		return dtoArtifact;
	}
}