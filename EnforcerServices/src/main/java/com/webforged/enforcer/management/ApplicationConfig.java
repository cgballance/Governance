package com.webforged.enforcer.management;

import java.util.ArrayList;
import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.QueryMappingConfiguration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.DefaultQueryMappingConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.webforged.enforcer.management.data.AllowedArtifact;
import com.webforged.enforcer.management.data.AllowedArtifactRowMapper;
import com.webforged.enforcer.management.data.Artifact;
import com.webforged.enforcer.management.data.ArtifactRowMapper;
import com.webforged.enforcer.management.data.Build;
import com.webforged.enforcer.management.data.BuildItem;
import com.webforged.enforcer.management.data.BuildItemRowMapper;
import com.webforged.enforcer.management.data.BuildRowMapper;
import com.webforged.enforcer.management.data.Component;
import com.webforged.enforcer.management.data.ComponentRowMapper;
import com.webforged.enforcer.management.data.LicensedArtifact;
import com.webforged.enforcer.management.data.LicensedArtifactRowMapper;
import com.webforged.enforcer.management.data.Project;
import com.webforged.enforcer.management.data.ProjectRowMapper;
import com.webforged.enforcer.management.util.Jsr310NullConverters;


@Configuration
@PropertySource("classpath:application.properties")
@EnableJdbcRepositories("com.webforged.enforcer.management.data")
public class ApplicationConfig extends AbstractJdbcConfiguration {

	@Value("${spring.datasource.driver-class-name}")
	private String dbdriver;

	@Value("${spring.datasource.url}")
	private String url;

	@Value("${spring.datasource.username}")
	private String username;

	@Value("${spring.datasource.password}")
	private String password;

	public ApplicationConfig() {
		super();
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL) ;
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName( dbdriver );
		dataSource.setUrl( url );
		dataSource.setUsername( username );
		dataSource.setPassword( password );
		
		transactionManager(dataSource);

		return dataSource;
	}

	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
		return new NamedParameterJdbcTemplate(dataSource);
	}

	@Bean
	TransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public JdbcCustomConversions jdbcCustomConversions() {
		Collection<Converter<?,?>> custom = Jsr310NullConverters.getConvertersToRegister() ;
		ArrayList<Converter<?,?>> all = new ArrayList<Converter<?,?>>() ;
		all.addAll(custom);
		return new JdbcCustomConversions( all );
	}

	@Bean
	QueryMappingConfiguration rowMappers() {
		return new DefaultQueryMappingConfiguration()
				.registerRowMapper(Project.class, new ProjectRowMapper() )
				.registerRowMapper(Component.class, new ComponentRowMapper() )
				.registerRowMapper(Artifact.class, new ArtifactRowMapper() )
				.registerRowMapper(AllowedArtifact.class, new AllowedArtifactRowMapper() )
				.registerRowMapper(LicensedArtifact.class, new LicensedArtifactRowMapper() )
				.registerRowMapper(Build.class, new BuildRowMapper() )
				.registerRowMapper(BuildItem.class, new BuildItemRowMapper() )
				;
	}
}
