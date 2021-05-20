package com.webforged.enforcer.management.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

public class BuildRowMapper implements RowMapper<Build> {
	static Logger logger = LoggerFactory.getLogger( BuildRowMapper.class ) ;

	@Override
	public Build mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp ts;
		Instant i;
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "mapRow " + rowNum + " into an Build" );
		}
		
		Build dtoBuild = new Build() ;
		
		dtoBuild.setBuild_id( rs.getLong("build_id") );
		dtoBuild.setProject_id( rs.getLong("project_id") );
		dtoBuild.setComponent_id( rs.getLong("component_id") );
		dtoBuild.setComponent_version( rs.getString( "component_version" ) );
		dtoBuild.setInfractions( rs.getString("infractions") );
		dtoBuild.setSource( rs.getString("source") );

		ts = rs.getTimestamp( "build_ts" ) ;
		if( ts != null ) {
			i = ts.toInstant() ;
		} else { i = null; }
		dtoBuild.setBuild_ts( i );

		return dtoBuild;
	}
}