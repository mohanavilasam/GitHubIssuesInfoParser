package githubissuesinfoparser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class MySqlConn {
	/*
	 * Let us try and convert this class to a singleton?
	 */
	private Connection con;

	public boolean connToDb() {
		boolean status = false;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/codequalitymeasurements?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance", "root", "root");
			// here sonoo is database name, root is username and password
			status = true;
		} catch (Exception e) {
			System.out.println(e);
		}
		return status;
	}
	public boolean executeDmlStmt(String query){
		boolean status = false;
		try {
			Statement stmt = con.createStatement();
			int rs = stmt.executeUpdate(query);
			status = true;
		} catch (Exception e) {
			System.out.println(e);
		}
		return status;
	}

}
