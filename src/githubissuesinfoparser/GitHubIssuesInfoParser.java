package githubissuesinfoparser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author manee
 *
 */
public class GitHubIssuesInfoParser {
	private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS issue_info( "
			+ "id int NOT NULL AUTO_INCREMENT, " + "issue_number int(32), " + "issue_title VARCHAR(510), "
			+ "merge_commit_sha VARCHAR(255), " + "merge_title VARCHAR(510), " + "issue_label VARCHAR(32),"
			+ "repository_url VARCHAR(510)," + "primary key (id)" + ");";
	private static final Logger LOGGER = Logger.getLogger(GitHubIssuesInfoParser.class.getName());
	private static final String CLIENT_ID = "3b6a2ef01a4ccf230ddd";
	private static final String CLIENT_SECRET = "358324ef3b6f407054fdf6c736e322934b610964";
	private static MySqlConn mysqlconn;

	public static void main(String args[]) throws Exception {
		iniDb();
		// URL that gets all issues that are closed
		String url = "https://api.github.com/repos/wordpress-mobile/WordPress-Android/issues?state=closed";
		// Append url with client id and secret to increase the number of
		url = url + "&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET;
		// Get the last page number
		int lastPageNo = getLastPageNo(url);
		// Parse each page
		System.out.println("[INFO]: Number of pages = " + lastPageNo);
		for (int i = 1; i <= lastPageNo; i++) {
			String currUrlToRead = url + "&page=" + i;
			System.out.println("[INFO]: URl: " + currUrlToRead);
			// Get a response using GitHub api and parse the JSON
			parseJSONDataToDb(currUrlToRead);
		}
	}

	private static void parseJSONDataToDb(String currUrlToRead) {
		JSONArray currentPageIssueList = new JSONArray(getResponse(currUrlToRead));
		MySqlConn mysqlconn = new MySqlConn();
		Connection connection = mysqlconn.connect();
		System.out.println("[INFO]: Number of issues on current page = " + currentPageIssueList.length());
		int issueWithPullRequestCount = 0;
		for (int j = 0; j < currentPageIssueList.length(); j++) {
			JSONObject currentPageIssueObject = currentPageIssueList.getJSONObject(j);
			if (currentPageIssueObject.has("pull_request")) {
				issueWithPullRequestCount++;
				JSONArray currentPageIssueLabelObject = currentPageIssueObject.getJSONArray("labels");
				JSONObject pullRequestInfoObject = new JSONObject(getResponse(((JSONObject) currentPageIssueObject.get("pull_request")).getString("url") + "?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET));
				for (int k = 0; k < currentPageIssueLabelObject.length(); k++) {
					JSONObject labelsInfoObject = (JSONObject) currentPageIssueLabelObject.get(k);
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
						/*
						Integer number = gitIssueQueryResponseObject.getInt("number");
						if (number != null) {
							statement.setInt(1, number);
						} else {
							statement.setNull(1, Types.INTEGER);
						}
						String title = gitIssueQueryResponseObject.getString("title");
						if (title != null) {
							statement.setString(2, title);
						} else {
							statement.setNull(2, Types.VARCHAR);
						}
						String mergeCommitSha = pullRequestInfoObject.getString("merge_commit_sha");
						if (mergeCommitSha != null) {
							statement.setString(3, mergeCommitSha);
						} else {
							statement.setNull(3, Types.VARCHAR);
						}
						String mergeTitle = pullRequestInfoObject.getString("title");
						if (mergeTitle != null) {
							statement.setString(4, mergeTitle);
						} else {
							statement.setNull(4, Types.VARCHAR);
						}
						String issueLabel = labelsInfoObject.getString("name");
						if (issueLabel != null) {
							statement.setString(5, issueLabel);
						} else {
							statement.setNull(5, Types.VARCHAR);
						}
						String repositoryUrl = gitIssueQueryResponseObject.getString("repository_url");
						if (repositoryUrl != null) {
							statement.setString(6, repositoryUrl);
						} else {
							statement.setNull(6, Types.VARCHAR);
						}
						*/
						statement.execute();
						statement.close();
						// statement.setInt(1,
						// gitIssueQueryResponseObject.getInt("number"));
						// statement.setString(2,
						// gitIssueQueryResponseObject.getString("title"));

						// statement.setString(3,
						// pullRequestInfoObject.getString("merge_commit_sha"));
						// statement.setString(4,
						// pullRequestInfoObject.getString("title"));
						// statement.setString(5,
						// labelsInfoObject.getString("name"));
						// statement.setString(6,
						// gitIssueQueryResponseObject.getString("repository_url"));
					} catch (SQLException e) {
						System.out.println("[ERROR]" + pullRequestInfoObject.toString());
					}
				}

			}

		}
		System.out.println("[INFO]: Total issues with pull request on current page = " + issueWithPullRequestCount);
		mysqlconn.disconnect();
	}

	private static void iniDb() {
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

	public static String getResponse(String urlToRead) {
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

	public static int getLastPageNo(String urlToRead) {
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
}
