package githubissuesinfoparser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySqlConn {

	private static final String DATABASE_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/codequalitymeasurements"; //129.123.7.126
	private static final String USERNAME = "root";
	private static final String PASSWORD = "root";
	private static final String MAX_POOL = "250";
	private Connection connection;
	private Properties properties;

	private Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
			properties.setProperty("user", USERNAME);
			properties.setProperty("password", PASSWORD);
			properties.setProperty("MaxPooledStatements", MAX_POOL);
		}
		return properties;
	}

	public Connection connect() {
		if (connection == null) {
			try {
				Class.forName(DATABASE_DRIVER);
				connection = DriverManager.getConnection(DATABASE_URL, getProperties());
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
		}
		return connection;
	}

	public void disconnect() {
		if (connection != null) {
			try {
				connection.close();
				connection = null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
