package githubissuesparser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author manee
 *
 */
public class GitHubIssuesParser {
	
	private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS issue_info( "
			+ "id int NOT NULL AUTO_INCREMENT, " + "issue_number int(32), " + "issue_title VARCHAR(510), "
			+ "merge_commit_sha VARCHAR(255), " + "merge_title VARCHAR(510), " + "issue_label VARCHAR(32),"
			+ "repository_url VARCHAR(510)," + "primary key (id)" + ");";
	private static final Logger LOGGER = Logger.getLogger(GitHubIssuesParser.class.getName());
	private static String clientId;
	private static String clientSecret;

	private GitHubIssuesParser() {
		Properties prop = new Properties();
		try{
		prop.load(new FileInputStream("config.properties"));
		clientId = prop.getProperty("clientid");
		clientSecret = prop.getProperty("clientsecret");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	public static void main(String args[]) throws Exception {
		GitHubIssuesParser newGitHubIssueParser = new GitHubIssuesParser();
		newGitHubIssueParser.iniDb();		
		String url = newGitHubIssueParser.generateApiUrl(args[0]);		
		int lastPageNo = newGitHubIssueParser.getLastPageNo(url);
		System.out.println("[INFO]: Number of pages = " + lastPageNo);
		for (int i = 1; i <= lastPageNo; i++) {
			String currUrlToRead = url + "&page=" + i;
			System.out.println("[INFO]: URl: " + currUrlToRead);
			JSONArray currentPageIssueList = new JSONArray(newGitHubIssueParser.getResponse(currUrlToRead));
			System.out.println("[INFO]: Number of issues on current page = " + currentPageIssueList.length());
			int issueWithPullRequestCount = 0;
			for (int j = 0; j < currentPageIssueList.length(); j++) {
				JSONObject currentPageIssueObject = currentPageIssueList.getJSONObject(j);
				if (currentPageIssueObject.has("pull_request")) {
					String urlForPullRequestInfo = ((JSONObject) currentPageIssueObject.get("pull_request")).getString("url") + "?client_id=" + clientId + "&client_secret=" + clientSecret;
					System.out.println("[DEBUG]: Pull request url " + urlForPullRequestInfo);
					issueWithPullRequestCount++;
					JSONArray currentPageIssueLabelObject = currentPageIssueObject.getJSONArray("labels");
					JSONObject pullRequestInfoObject = new JSONObject(newGitHubIssueParser.getResponse(urlForPullRequestInfo));
					for (int k = 0; k < currentPageIssueLabelObject.length(); k++) {
						JSONObject labelsInfoObject = (JSONObject) currentPageIssueLabelObject.get(k);
						performInsertOperation(currentPageIssueObject, pullRequestInfoObject, labelsInfoObject);
					}

				}

			}
			System.out.println("[INFO]: Total issues with pull request on current page = " + issueWithPullRequestCount);
		}
	}
	
	private void iniDb() {
		MySqlConn mysqlconn = new MySqlConn();
		Connection connection = mysqlconn.connect();
		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(CREATE_TABLE_QUERY);
			statement.execute();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		mysqlconn.disconnect();
	}	
	private String generateApiUrl(String url){		
		String apiUrl = url.replaceAll("github.com", "api.github.com/repos");
		apiUrl = apiUrl + "&client_id=" + clientId + "&client_secret=" + clientSecret;
		return apiUrl;
	}
	
	private int getLastPageNo(String urlToRead) {
		URL url;
		int lastPageNo = 0;
		try {
			url = new URL(urlToRead);
			HttpURLConnection conn;
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			Pattern p = Pattern.compile("page=(\\d+).*$");
			String[] link = conn.getHeaderField("link").split(",");
			for (String tmp : link) {
				if (tmp.contains("rel=\"last\"")) {
					Matcher m = p.matcher(tmp);
					if (m.find())
						lastPageNo = Integer.parseInt(m.group(1));
				}
			}
			conn.disconnect();
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, ex.toString(), ex);
		}
		return lastPageNo;
	}
	
	private String getResponse(String urlToRead) {
		StringBuilder result = new StringBuilder();
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, ex.toString(), ex);
		}
		return result.toString();
	}	
	private static void performInsertOperation(JSONObject currentPageIssueObject, JSONObject pullRequestInfoObject, JSONObject labelsInfoObject){
		MySqlConn mysqlconn = new MySqlConn();
		Connection connection = mysqlconn.connect();
		try {
			final PreparedStatement statement = connection.prepareStatement("INSERT INTO issue_info (issue_number, issue_title, merge_commit_sha, merge_title, issue_label, repository_url) VALUES (?, ?, ?, ?, ?, ?)");
			if(currentPageIssueObject.has("number") && !currentPageIssueObject.isNull("number"))
				statement.setInt(1, currentPageIssueObject.getInt("number"));
			else
				statement.setNull(1, Types.INTEGER);
			if(currentPageIssueObject.has("title") && !currentPageIssueObject.isNull("title"))
				statement.setString(2, currentPageIssueObject.getString("title"));
			else
				statement.setNull(2, Types.VARCHAR);			
			if(pullRequestInfoObject.has("merge_commit_sha") && !pullRequestInfoObject.isNull("merge_commit_sha"))
				statement.setString(3, pullRequestInfoObject.getString("merge_commit_sha"));
			else
				statement.setNull(3, Types.VARCHAR);
			if(pullRequestInfoObject.has("title") && !pullRequestInfoObject.isNull("title"))
				statement.setString(4, pullRequestInfoObject.getString("title"));
			else
				statement.setNull(4, Types.VARCHAR);
			if(labelsInfoObject.has("name") && !labelsInfoObject.isNull("name"))
				statement.setString(5, labelsInfoObject.getString("name"));
			else
				statement.setNull(5, Types.VARCHAR);
			if(currentPageIssueObject.has("repository_url") && !currentPageIssueObject.isNull("repository_url"))
				statement.setString(6, currentPageIssueObject.getString("repository_url"));
			else
				statement.setNull(6, Types.VARCHAR);
			statement.execute();
			statement.close();
		} catch (SQLException e) {
			System.out.println("[ERROR]" + pullRequestInfoObject.toString());
		}
		mysqlconn.disconnect();
	}
}
