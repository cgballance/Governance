package com.webforged.enforcer.management.data;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Table("licensedartifacts")
public class LicensedArtifact {
	
	@Id
	public Long lic_artifact_id ;
	private Long artifact_id;
	private Long project_id;
	private String contract;
	private String vendor;
	private String approval_architect;
	private Instant approval_ts;
	
	public LicensedArtifact() {
	}
}