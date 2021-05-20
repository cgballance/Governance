package com.webforged.enforcer.management.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

public class ProjectRowMapper implements RowMapper<Project> {
	static Logger logger = LoggerFactory.getLogger( ProjectRowMapper.class ) ;

	@Override
	public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp ts;
		LocalDateTime ldt;
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "mapRow " + rowNum + " into an Project" );
		}
		
		Project dtoArtifact = new Project() ;
		
		dtoArtifact.setProject_id( rs.getLong("project_id") );
		dtoArtifact.setAcronym( rs.getString("acronym") );
		dtoArtifact.setBusiness_owner( rs.getString("business_owner") );
		dtoArtifact.setIt_owner( rs.getString("it_owner") );

		ts = rs.getTimestamp( "begin_date" ) ;
		if( ts != null ) {
			ldt = ts.toLocalDateTime();
		} else { ldt = null; }
		dtoArtifact.setBegin_date( ldt );

		ts = rs.getTimestamp( "end_date" ) ;
		if( ts != null ) {
			ldt = ts.toLocalDateTime();
		} else { ldt = null; }
		dtoArtifact.setEnd_date( ldt );

		return dtoArtifact;
	}
}