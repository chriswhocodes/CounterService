package com.chrisnewland.rest.counterservice.dto;

import java.sql.*;

public class DatabaseManager
{
	private static final String jdbcUrl = System.getProperty("jdbcUrl", "jdbc:postgresql://127.0.0.1/counter");
	private static final String user = System.getProperty("user", "counter");
	private static final String password = System.getProperty("password", "counter");

	static Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection(jdbcUrl, user, password);
	}

	static PreparedStatement prepare(Connection connection, String sql) throws SQLException
	{
		return connection.prepareStatement(sql);
	}
}