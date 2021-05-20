package com.webforged.enforcer.management.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

public class BuildItemRowMapper implements RowMapper<BuildItem> {
	static Logger logger = LoggerFactory.getLogger( BuildItemRowMapper.class ) ;

	@Override
	public BuildItem mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "mapRow " + rowNum + " into an BuildItem" );
		}
		
		BuildItem dtoBuild = new BuildItem() ;
		
		dtoBuild.setBuilditem_id( rs.getLong("builditem_id") );
		dtoBuild.setBuild_id( rs.getLong("build_id") );
		dtoBuild.setGroup_name( rs.getString( "group_name") );
		dtoBuild.setArtifact_name( rs.getString( "artifact_name") );
		dtoBuild.setVersion_name( rs.getString( "version_name") );
		dtoBuild.setArtifact_status_snapshot( rs.getString( "artifact_status_snapshot") );
		dtoBuild.setAllowed( rs.getBoolean( "allowed" ) );

		return dtoBuild;
	}
}