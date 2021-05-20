package com.webforged.enforcer.management.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Table("components")
public class Component {
	
	@Id
	public Long component_id ;
	public Long project_id ;
	public String name;
	
	public Component() {
	}
}