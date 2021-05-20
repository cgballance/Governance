package com.webforged.enforcer.management.data;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Table("builds")
public class Build {
	
	@Id
	public Long build_id ;
	private Long project_id;
	private Long component_id;
	private String component_version;
	private Instant build_ts;
	private String infractions;
	private String source;
	
	public Build() {
	}
}