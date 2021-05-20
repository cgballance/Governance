package com.webforged.enforcer.management.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

public class LicensedArtifactRowMapper implements RowMapper<LicensedArtifact> {
	static Logger logger = LoggerFactory.getLogger( LicensedArtifactRowMapper.class ) ;

	@Override
	public LicensedArtifact mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp ts;
		Instant i ;
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "mapRow " + rowNum + " into an LicensedArtifact" );
		}
		
		LicensedArtifact dtoLicensedArtifact = new LicensedArtifact() ;

		dtoLicensedArtifact.setLic_artifact_id( rs.getLong("lic_artifact_id" ) );
		dtoLicensedArtifact.setArtifact_id( rs.getLong( "artifact_id" ) );
		dtoLicensedArtifact.setProject_id( rs.getLong( "project_id" ) );
		dtoLicensedArtifact.setContract( rs.getString( "contract" ) );
		dtoLicensedArtifact.setVendor( rs.getString( "vendor" ) );
		dtoLicensedArtifact.setApproval_architect( rs.getString( "approval_architect" ) );
		ts = rs.getTimestamp( "approval_ts" ) ;
		if( ts != null ) {
			i = ts.toInstant() ;
		} else { i = null; }
		dtoLicensedArtifact.setApproval_ts(i);

		return dtoLicensedArtifact;
	}
}