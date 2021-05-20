package com.webforged.enforcer.management.data;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Table("artifacts")
public class Artifact {
	
	@Id
	public Long artifact_id ;
	private String status;
	private String group_name;
	private String artifact_name;
	private String version_name;
	private Boolean is_vendor_licensed;
	private LocalDateTime created_date;
	private LocalDateTime approval_date;
	private String approval_authorization;
	private Instant approval_ts;
	private LocalDateTime deprecation_date;
	private String deprecation_authorization;
	private Instant deprecation_ts;
	private LocalDateTime retirement_date;
	private String retirement_authorization;
	private Instant retirement_ts;
	
	public Artifact() {
	}
}
