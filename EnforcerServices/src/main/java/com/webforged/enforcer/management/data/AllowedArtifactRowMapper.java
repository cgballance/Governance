package com.webforged.enforcer.management.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

public class AllowedArtifactRowMapper implements RowMapper<AllowedArtifact> {
	static Logger logger = LoggerFactory.getLogger( AllowedArtifactRowMapper.class ) ;

	@Override
	public AllowedArtifact mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp ts;
		Instant i ;
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "mapRow " + rowNum + " into an Artifact" );
		}
		
		AllowedArtifact dtoArtifact = new AllowedArtifact() ;

		dtoArtifact.setAllowed_artifact_id( rs.getLong( "allowed_artifact_id" ) );
		dtoArtifact.setArtifact_id( rs.getLong( "artifact_id" ) );
		dtoArtifact.setProject_id( rs.getLong( "project_id" ) );
		dtoArtifact.setApproval_architect( rs.getString( "approval_architect" ) );
		ts = rs.getTimestamp( "approval_ts" ) ;
		if( ts != null ) {
			i = ts.toInstant() ;
		} else { i = null; }
		dtoArtifact.setApproval_ts( i );

		return dtoArtifact;
	}
}