package com.webforged.enforcer.management.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

public class ArtifactRowMapper implements RowMapper<Artifact> {
	static Logger logger = LoggerFactory.getLogger( ArtifactRowMapper.class ) ;

	@Override
	public Artifact mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp ts;
		LocalDateTime ldt;
		Instant i ;
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "mapRow " + rowNum + " into an Artifact" );
		}
		
		Artifact dtoArtifact = new Artifact() ;

		dtoArtifact.setApproval_authorization( rs.getString( "approval_authorization" ) );
		
		ts = rs.getTimestamp( "approval_date" ) ;
		if( ts != null ) {
			ldt = ts.toLocalDateTime() ;
		} else { ldt = null; }	
		dtoArtifact.setApproval_date( ldt );
		
		ts = rs.getTimestamp( "approval_ts" ) ;
		if( ts != null ) {
			i = ts.toInstant() ;
		} else { i = null; }
		dtoArtifact.setApproval_ts( i );
		
		dtoArtifact.setArtifact_id (rs.getLong("artifact_id") );
		
		dtoArtifact.setArtifact_name( rs.getString( "artifact_name" ) );
		
		ts = rs.getTimestamp( "created_date" ) ;
		if( ts != null ) {
			ldt = ts.toLocalDateTime();
		} else { ldt = null; }
		dtoArtifact.setCreated_date( ldt );
		
		dtoArtifact.setDeprecation_authorization( rs.getString( "deprecation_authorization" ) );
		
		ts = rs.getTimestamp( "deprecation_date" ) ;
		if( ts != null ) {
			ldt = ts.toLocalDateTime();
		} else { ldt = null; }
		dtoArtifact.setDeprecation_date( ldt );

		ts = rs.getTimestamp( "deprecation_ts" ) ;
		if( ts != null ) {
			i = ts.toInstant() ;
		} else { i = null; }
		dtoArtifact.setDeprecation_ts( i );
		
		dtoArtifact.setGroup_name( rs.getString( "group_name" ) );

		dtoArtifact.setIs_vendor_licensed( rs.getBoolean( "is_vendor_licensed" ) );
		
		dtoArtifact.setRetirement_authorization( rs.getString( "retirement_authorization" ) );
		
		ts = rs.getTimestamp( "retirement_date" ) ;
		if( ts != null ) {
			ldt = ts.toLocalDateTime();
		} else { ldt = null; }
		dtoArtifact.setRetirement_date( ldt );
		
		ts = rs.getTimestamp( "retirement_ts" ) ;
		if( ts != null ) {
			i = ts.toInstant() ;
		} else { i = null; }
		dtoArtifact.setRetirement_ts( i );
		
		dtoArtifact.setStatus( rs.getString( "status" ) );
		
		dtoArtifact.setVersion_name( rs.getString( "version_name" ) );

		return dtoArtifact;
	}
}