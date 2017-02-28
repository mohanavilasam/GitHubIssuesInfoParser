package githubissuesparsertest;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.junit.Test;

import githubissuesparser.MySqlConn;

public class MySqlConnTest {

	@Test
	public void testConnect() throws SQLException {
		try {
			PrintWriter p = new PrintWriter("config.properties", "UTF-8");
			p.println("databaseurl=jdbc:mysql://localhost:3306/codequalitymeasurements");
			p.println("user=root");
			p.println("password=root");
			p.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		MySqlConn m = new MySqlConn();
		Connection c = m.connect();
		String url = null;
		String user = null;
		try {
			DatabaseMetaData dmd = c.getMetaData();;
			url = dmd.getURL();
			user = dmd.getUserName();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		assertNotNull(c);
		assertNotNull(user);
		assertNotNull(url);
		assertEquals(url, "jdbc:mysql://localhost:3306/codequalitymeasurements");
		assertEquals(user, "root@localhost");
		assertTrue(c.isValid(1));		
		m.disconnect();		
	}

	@Test
	public void testDisconnect() throws SQLException {
		MySqlConn m = new MySqlConn();
		Connection c = m.connect();
		assertNotNull(c);
		assertTrue(c.isValid(1));		
		m.disconnect();	
		assertTrue(c.isClosed());
	}

}
