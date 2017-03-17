package com.movision.fsearch.core;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.movision.fsearch.L;
import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;

public class DataSourceManager {
	private static JdbcTemplate jdbcTemplate;
	static {
		try {
			PropertiesConfig config = new PropertiesConfig(
					L.class.getResourceAsStream("conf/datasource.ini"), false);
			ComboPooledDataSource ds = new com.mchange.v2.c3p0.ComboPooledDataSource();
			ds.setDriverClass("com.mysql.jdbc.Driver");
			ds.setJdbcUrl(config.getString("db.url"));
			ds.setUser(config.getString("db.username"));
			ds.setPassword(config.getString("db.password"));
			ds.setInitialPoolSize(config.getInt("db.initialPoolSize"));
			ds.setMinPoolSize(config.getInt("db.minPoolSize"));
			ds.setMaxPoolSize(config.getInt("db.maxPoolSize"));
			ds.setAcquireIncrement(config.getInt("db.acquireIncrement"));
			ds.setAcquireRetryAttempts(config.getInt("db.acquireRetryAttempts"));
			ds.setMaxIdleTime(config.getInt("db.maxIdleTime"));
			jdbcTemplate = new JdbcTemplate();
			jdbcTemplate.setDataSource(ds);
		} catch (Throwable e) {
			L.error("Failed to load dataSource", e);
			System.exit(-1);
		}
	}

	public static JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

}
