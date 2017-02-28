package githubissuesparser;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySqlConn {

	private static final String DATABASE_DRIVER = "com.mysql.jdbc.Driver";
	private String databaseUrl;
	private Connection connection;
	private Properties dbProperties;
	
	public MySqlConn(){
		dbProperties = new Properties();
		
		try {
			dbProperties.load(new FileInputStream("config.properties"));
			databaseUrl = dbProperties.getProperty("databaseurl");
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public Connection connect() {
		if (connection == null) {
			try {
				Class.forName(DATABASE_DRIVER);
				connection = DriverManager.getConnection(databaseUrl, dbProperties);
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
