package com.webforged.enforcer.management.data;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Table("projects")
public class Project {
	
	@Id
	public Long project_id ;
	private String acronym;
	private String business_owner;
	private String it_owner;
	private LocalDateTime begin_date;
	private LocalDateTime end_date;
	
	public Project() {
	}
}