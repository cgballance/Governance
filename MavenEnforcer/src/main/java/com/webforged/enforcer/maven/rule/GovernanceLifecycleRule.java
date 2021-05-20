package com.webforged.enforcer.maven.rule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class GovernanceLifecycleRule implements EnforcerRule {
	static final String IDENTITY = "MavenEnforcer";
	public enum STATUS_TYPES {
		CREATED ("CREATED"),
		GA ("GA"),
		DEPRECATED ("DEPRECATED"),
		LIMITED ("LIMITED"),
		LIMITED_DEPRECATED ("LIMITED_DEPRECATED"),
		RETIRED ("RETIRED") ;
		public final String label;
		private STATUS_TYPES(String label) {
			this.label = label;
		}
	} ;
	
    private boolean failFlag = false;
    
    private String jdbcUrl = "";
    private String jdbcDriverClass = "";
    private String jdbcUser = "";
    private String jdbcPassword = "" ;
    private BasicDataSource bds;
    
    private static Log log;

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        log = helper.getLog();

        try {
        	
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            MavenSession session = (MavenSession) helper.evaluate( "${session}" );
            String acronym = (String) helper.evaluate( "${acronym}" ) ;
            String target = (String) helper.evaluate( "${project.build.directory}" );
            String groupId = (String) helper.evaluate( "${project.groupId}" ) ;
            String artifactId = (String) helper.evaluate( "${project.artifactId}" );
            String version = (String) helper.evaluate( "${project.version}" );
            
            if( acronym == null || "".equals(acronym) ) {
            	//
            	// without an acronym, can't lookup any rules.
            	//
            	String msg = "Missing governance required property: 'acronym'";
            	log.error( msg ) ;
            	throw new EnforcerRuleException( msg );
            }
            
            if( log.isDebugEnabled() ) {
            	log.debug( "TARGET PATH: " + target );
            	log.debug( "Retrieved Target Folder: " + target );
            	log.debug( "Retrieved Group: " + groupId );
            	log.debug( "Retrieved ArtifactId: " + artifactId );
            	log.debug( "Retrieved Version: " + version );
            	log.debug( "Retrieved Project: " + project );
            	log.debug( "Retrieved Session: " + session );
            }
            
            initDatabase();

            Integer[] info = initProject( acronym, artifactId ) ;
            Integer project_id = info[0] ;
            Integer component_id = info[1] ;
            
			java.util.Map<String,Artifact> map = project.getArtifactMap() ;
            java.util.Set<String> keys = map.keySet();
            java.util.Iterator<String> keysIt = keys.iterator();
            StringBuilder infractions = new StringBuilder() ;
            List<Map<String,String>> bom = new ArrayList<Map<String,String>>() ;
            
            while( keysIt.hasNext() ) {
            	String key = keysIt.next();
            	Artifact value = map.get(key) ;
            	if( log.isDebugEnabled() ) {
            		log.debug( "ARTIFACT: " + key +
            				" GroupId: " + value.getGroupId() +
            				" Id: " + value.getId() +
            				" Version: " + value.getVersion() +
            				" Classifier: " + value.getClassifier() +
            				" Scope: " + value.getScope() +
            				" Type: " + value.getType()
            				);
            	}
                java.util.List<String> trails = value.getDependencyTrail() ;
                for( String crumb : trails ) {
                	String[] parts = crumb.split(":") ;
                	String grp = parts[0] ;
                	String id = parts[1] ;
                	String tp = parts[2] ;
                	String ver = parts[3] ;
                	if( groupId.equals(grp) && artifactId.equals(id) && version.equals(ver) ) {
                		continue; // skip myself
                	}

                	if( trails.size() > 2 ) {
                		if( crumb.equals(trails.get(1)) ) {
                			continue;
                		}
                		if( log.isDebugEnabled() ) {
                			log.debug( "\tTransitive Dependency: " + crumb + " parent: " + trails.get(1) );
                		}
                	} else {
                		if( log.isDebugEnabled() ) {
                			log.debug( "\tDirect Dependency: " + crumb );
                			log.debug( "VALIDATE USAGE OF: " + crumb );
                		}
                		StringBuilder msg = new StringBuilder() ;
                		StringBuilder ss = new StringBuilder() ;
                		boolean allowed = validateUsage(project_id, acronym, version, grp, id, tp, ver, msg, ss) ;
                		if( allowed == false ) {
                			failFlag = true;
                			msg.insert(0,"Unauthorized Library Usage: " + crumb + "\n" );
                			if( log.isDebugEnabled() ) {
                				log.debug( msg );
                			}
                    		infractions.append( "\t" + msg + "\n") ;
                		}
                		
            			//
            			// ADD TO A BILL OF MATERIALS.  stashBOM() INTO THE DATABASE LATER.
            			//
                		// get a snapshot of the status of this artifact as it stands currently.
                		//
                		
                		Map<String,String> item = new HashMap<String,String>() ;
                		item.put( "acronym",  acronym ) ;
                		item.put( "component",  artifactId ) ;
                		item.put( "artifact",  artifactId ) ;
                		item.put( "group", grp ) ;
                		item.put( "id", id ) ;
                		item.put( "version", ver ) ;
                		item.put( "status", ss.toString() ) ;
                		item.put( "allowed", String.valueOf(allowed) ) ;
            			bom.add( item ) ;
                	}
                }
            }
            
            stashBOM( project_id, component_id, version, bom, infractions ) ;
            
            if ( this.failFlag ) {
                throw new EnforcerRuleException( infractions.toString() );
            }
        } catch ( Exception e ) {
            throw new EnforcerRuleException( e.getLocalizedMessage(), e );
        }
    }
    
    private void initDatabase() throws ClassNotFoundException {
    	Class.forName( jdbcDriverClass ) ;
        bds = new BasicDataSource() ;
        bds.setDriverClassName( jdbcDriverClass );
        bds.setUrl( jdbcUrl ) ;
        bds.setUsername( jdbcUser ) ;
        bds.setPassword( jdbcPassword ) ;
        bds.setInitialSize(1);
    }
    
    private Integer[] initProject( String acronym, String artifact ) {
    	String query ;
    	query = "SELECT * FROM Projects WHERE acronym = ?" ;

    	Connection conn = null ;
    	PreparedStatement ps = null ;
    	ResultSet rs = null ;
		Integer project_id = null;
		Integer component_id = null;
    	
    	try {
    		conn = bds.getConnection() ;
    		ps = conn.prepareStatement( query ) ;
    		ps.setString(1, acronym );
    		rs = ps.executeQuery();

    		while( rs.next() ) {
    			project_id = rs.getInt("project_id") ;
    		}
    		rs.close();
    		ps.close();
    		rs = null;
    		ps = null;
    		if( project_id == null ) {
    			//
    			// no project/component yet there...
    			//
    			query = "INSERT INTO Projects(acronym,begin_date) VALUES(?,?)" ;
    			ps = conn.prepareStatement( query, PreparedStatement.RETURN_GENERATED_KEYS ) ;
    			ps.setString(1, acronym);
    			ps.setTimestamp(2, getCurrentTimestamp());
    			ps.executeUpdate();
    			rs = ps.getGeneratedKeys() ;
    			while( rs.next() ) {
    				project_id = rs.getInt(1);
    			}
    		}
        	query = "SELECT * FROM Components WHERE project_id = ? and name = ?" ;
        	ps = conn.prepareStatement( query ) ;
    		ps.setInt(1, project_id );
    		ps.setString( 2, artifact );
    		rs = ps.executeQuery();

    		while( rs.next() ) {
    			component_id = rs.getInt("component_id") ;
    		}
    		rs.close();
    		ps.close();
    		rs = null;
    		ps = null;
    		if( component_id == null ) {
    			//
    			// no component yet there...
    			//
    			query = "INSERT INTO Components(project_id,name) VALUES(?,?)" ;
    			ps = conn.prepareStatement( query, PreparedStatement.RETURN_GENERATED_KEYS ) ;
    			ps.setInt(1, project_id);
    			ps.setString(2, artifact);
    			ps.executeUpdate();
    			rs = ps.getGeneratedKeys() ;
    			while( rs.next() ) {
    				component_id = rs.getInt(1);
    			}
    		}
        	
    	} catch( Exception others ) {
    		others.printStackTrace();
    	} finally {
    		try { rs.close(); } catch(Exception others) {}
    		try { ps.close(); } catch(Exception others) {}
    		try { conn.close(); } catch(Exception others) {}
    	}
    	return new Integer[] { project_id, component_id } ;
    }

	private static java.sql.Timestamp getCurrentTimestamp() {
		java.util.Date now = new java.util.Date();
		return new java.sql.Timestamp(now.getTime());
	}

	/**
	 * Validates whether or not a project is permitted to use a library.  The library may be vendor supplied, which must be indicated in the artifacts table of the database.
	 * Always valid versions will either be LIMITED and GA.  DEPRECATED and LIMITED_DEPRECATED are valid but should have a warning.
	 * @param project_id
	 * @param acronym
	 * @param build_version
	 * @param groupId
	 * @param artifactId
	 * @param type
	 * @param version
	 * @param msg
	 * @return
	 */
    private boolean validateUsage( Integer project_id, String acronym, String build_version, String groupId, String artifactId, String type, String version,
    			StringBuilder msg, StringBuilder statusSnapshot  ) {
    	//
    	// Validate whether or not application(acronym) is allowed to use the artifact.
    	// Can be: OSS or licensed. various levels of approval and overrides.
    	// If missing, then not approved.
    	//
    	Connection conn = null ;
    	PreparedStatement ps = null ;
    	PreparedStatement limitedps = null;
    	PreparedStatement licensedps = null;
    	PreparedStatement insps = null ;
    	ResultSet rs = null ;
    	ResultSet limitedrs = null;
    	ResultSet licensedrs = null;
		boolean isAllowed = false;
    	
    	try {
    		conn = bds.getConnection() ;
    		ps = conn.prepareStatement(
    				"SELECT * FROM Artifacts WHERE group_name = ? AND artifact_name = ? AND version_name = ?" ) ;
    		ps.setString(1, groupId );
    		ps.setString(2, artifactId );
    		ps.setString(3, version );
    		rs = ps.executeQuery();
    		boolean foundArtifact = rs.next();
    		if( foundArtifact ) {
    			String status = rs.getString("status") ;
    			statusSnapshot.setLength(0);
    			statusSnapshot.append(status);
    			Integer artifact_id = rs.getInt("artifact_id");
    			Boolean is_vendor_licensed = rs.getBoolean("is_vendor_licensed") ;

    			STATUS_TYPES t = STATUS_TYPES.valueOf( status ) ;
    			switch( t ) {
    				case GA:
    				case DEPRECATED:
    					isAllowed = true;
    					break;
    				case LIMITED:
    				case LIMITED_DEPRECATED:
    					if( Boolean.FALSE.equals(is_vendor_licensed) ) {
    						if( limitedps == null ) {
    							limitedps = conn.prepareStatement(
    									"SELECT * FROM AllowedArtifacts WHERE artifact_id = ? AND project_id = ?" ) ;
    						}
    						limitedps.setInt(1, artifact_id);
    						limitedps.setInt(2, project_id);
    						limitedrs = limitedps.executeQuery();
    						while( limitedrs.next() ) {
    							isAllowed = true;
    						}
    						limitedrs.close();
    						limitedrs = null;
            				limitedps.clearParameters();
    					} else {
    						if( licensedps == null ) {
            					licensedps = conn.prepareStatement(
            						"SELECT * FROM LicensedArtifacts WHERE artifact_id = ? AND project_id = ?" ) ;
            				}
            				licensedps.setInt(1, artifact_id);
            				licensedps.setInt(2, project_id);
            				licensedrs = licensedps.executeQuery();
            				while( licensedrs.next() ) {
            					isAllowed = true;
            				}
            				licensedrs.close();
            				licensedrs = null;
            				licensedps.clearParameters();
    					}
    					break;
    				case RETIRED:
    				case CREATED:
    					isAllowed = false;
    					break;
    				default:
    					break;
    			}
    		}
    		rs.close();
    		rs = null;
    		ps.close();
    		ps = null;
    		
    		if( foundArtifact == false ) {
    			//
    			// insert placeholder record....periodically we will prune these if never used.
    			// inserting them makes it easier to approve if warranted.
    			//
    			String query = "INSERT INTO Artifacts(group_name,artifact_name,version_name,status,created_date) VALUES (?,?,?,?,?)" ;
    			insps = conn.prepareStatement( query, Statement.RETURN_GENERATED_KEYS ) ;
    			insps.setString( 1, groupId );
    			insps.setString( 2, artifactId );
    			insps.setString( 3, version ) ;
    			insps.setString( 4, STATUS_TYPES.CREATED.label ) ;
    			insps.setTimestamp( 5, getCurrentTimestamp() );
    			insps.executeUpdate();
    			rs = insps.getGeneratedKeys() ;
    			Integer artifact_id = null;
    			while( rs.next() ) {
    				artifact_id = rs.getInt(1);
    				String amsg = String.format( "***NOTICE***:  Created placeholder Artifact for  %s :: %s :: %s that must be approved. Artifact Id is %d",
    						groupId, artifactId, version,artifact_id ) ;
    				log.warn( amsg ) ;
    			}
    			rs.close();
    			insps.close();
    		}
    	} catch( Exception any ) {
    		msg.append( "??? " + any.toString() );
    		log.error( any.toString(), any );
    		return false;
    	} finally {
    		try { if( limitedps != null ) limitedps.close(); } catch(Exception others) {}
    		try { if( licensedps != null ) licensedps.close(); } catch(Exception others) {}
    		try { if( insps != null ) insps.close(); } catch(Exception others) {}
    		try { if( ps != null ) ps.close(); } catch(Exception others) {}
    		try { if( rs != null ) rs.close(); } catch(Exception others) {}
    		try { if( conn != null ) conn.close(); } catch(Exception others) {}
    	}
		return isAllowed;
    }
    
    private void stashBOM( Integer project_id, Integer component_id, String component_version, List<Map<String,String>> bom, StringBuilder infractions ) {
        //
        // insert/update/delete build artifact BOM and errors
        //
    	
    	Connection conn = null ;
    	PreparedStatement ps = null ;
    	ResultSet rs = null ;
    	String query ;
    	Long build_id = null;

    	try {
    		conn = bds.getConnection() ;
    		query = "INSERT INTO Builds(build_ts, project_id, component_id, component_version, infractions, source) VALUES(?,?,?,?,?,?)"  ;
    		ps = conn.prepareStatement( query, PreparedStatement.RETURN_GENERATED_KEYS ) ;
    		ps.setTimestamp(1, getCurrentTimestamp() );
    		ps.setInt(2, project_id );
    		ps.setInt(3, component_id );
    		ps.setString(4,  component_version );
    		ps.setString(5, infractions.toString() );
			ps.setString(6,  IDENTITY );
    		ps.executeUpdate();
    		rs = ps.getGeneratedKeys() ;
			while( rs.next() ) {
				build_id = rs.getLong(1);
			}
			rs.close(); rs = null;
			ps.close(); ps = null;
			query = "INSERT INTO BuildItems(build_id,group_name,artifact_name,version_name,artifact_status_snapshot,allowed) VALUES(?,?,?,?,?,?)" ;
			ps = conn.prepareStatement( query ) ;
			for( Map<String,String> map : bom ) {
				ps.setLong(1, build_id );
				ps.setString(2, map.get("group") );
				ps.setString(3, map.get("id") );
				ps.setString(4, map.get("version" ) );
				ps.setString(5, map.get("status") );
				ps.setBoolean(6, Boolean.valueOf(map.get("allowed")) );
				ps.execute();
				ps.clearParameters();
			}
			ps.close(); ps = null;
    	} catch( Exception any ) {
    		log.error( any.toString(), any );
    		return ;
    	} finally {
    		try { if( rs != null ) rs.close(); } catch(Exception others) {}
    		try { if( ps != null ) ps.close(); } catch(Exception others) {}
    		try { if( conn != null ) conn.close(); } catch(Exception others) {}
    	}
    }

    public String getCacheId() {
        return "";
    }

    public boolean isCacheable() {
        return false;
    }

    public boolean isResultValid( EnforcerRule arg0 ) {
        return false;
    }
}
