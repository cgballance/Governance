package com.webforged.enforcer.management.data;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Table("allowedartifacts")
public class AllowedArtifact {
	
	@Id
	public Long allowed_artifact_id ;
	private Long artifact_id;
	private Long project_id;
	private String approval_architect;
	private Instant approval_ts;
	
	public AllowedArtifact() {
	}
}
