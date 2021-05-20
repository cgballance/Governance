package com.webforged.enforcer.management.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Table("builditems")
public class BuildItem {
	
	@Id
	public Long builditem_id ;
	public Long build_id ;
	private String group_name;
	private String artifact_name;
	private String version_name;
	private String  artifact_status_snapshot;
	private Boolean allowed;
	
	public BuildItem() {
	}
}