package githubissuesinfoparser;

import java.net.HttpURLConnection;
import java.net.URL;
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
	
	private static final Logger LOGGER = Logger.getLogger( GitHubIssuesInfoParser.class.getName() );
	private static final String CLIENT_ID = "xxx";  
	private static final String CLIENT_SECRET = "xxx";
	private static MySqlConn mysqlconn;
	public static void main(String args[]) throws Exception {
		mysqlconn = iniDb();
		// URL that gets all issues that are closed
		String url = "https://api.github.com/repos/wordpress-mobile/WordPress-Android/issues?state=closed";
		// Append url with client id and secret to increase the number of queries		
		url = url + "&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET;
		// Get the last page number
		int lastPageNo = getLastPageNo(url);
		// Parse each page
		for (int i = 1; i <= lastPageNo; i++) {
			String currUrlToRead = url + "&page=" + i;
			// Get a response using GitHub api and parse the JSON
			parseJSONDataToDb(currUrlToRead);
			break;
		}

	}
	
	private static void parseJSONDataToDb(String currUrlToRead){
		JSONArray gitIssueQueryResponseList = new JSONArray(getResponse(currUrlToRead));
		for (int j = 0; j < gitIssueQueryResponseList.length(); j++) {
			JSONObject gitIssueQueryResponseObject = gitIssueQueryResponseList.getJSONObject(j);
			if (gitIssueQueryResponseObject.has("pull_request")) {
				JSONArray labelsInfoList = gitIssueQueryResponseObject.getJSONArray("labels");
				JSONObject pullRequestInfoObject = new JSONObject(getResponse(((JSONObject) gitIssueQueryResponseObject.get("pull_request")).getString("url") + "?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET));
				for (int k = 0; k < labelsInfoList.length(); k++) {
					JSONObject labelsInfoObject = (JSONObject) labelsInfoList.get(k);
					String issueInfoInsQuery = 
							"INSERT INTO IssueInfo(IssueNumber, IssueTitle, MergeCommitSha, MergeTitle, IssueLabel) VALUES ("
							+ gitIssueQueryResponseObject.getInt("number") 											+ " , "
							+ " ' " + gitIssueQueryResponseObject.getString("title") .replaceAll("'", "") 			+ " ' " + " , "  
							+ " ' " + pullRequestInfoObject.getString("merge_commit_sha").replaceAll("'", "") 		+ " ' " + " , "  
							+ " ' " + pullRequestInfoObject.getString("title").replaceAll("'", "") 					+ " ' " + " , "
							+ " ' " + labelsInfoObject.getString("name").replaceAll("'", "") 						+ " ' " 
							+ " );";
					if (mysqlconn.connToDb()) {
						mysqlconn.executeDmlStmt(issueInfoInsQuery);
					}
				}
			}				
		}		
	}
	private static MySqlConn iniDb(){
		MySqlConn mysqlconn = new MySqlConn();
		if (mysqlconn.connToDb()) {
			mysqlconn.executeDmlStmt(
					"CREATE TABLE IF NOT EXISTS IssueInfo "
					+ "("
					+ "ID int NOT NULL AUTO_INCREMENT, "
					+ "IssueNumber int(32), "
					+ "IssueTitle VARCHAR(510), "
					+ "MergeCommitSha VARCHAR(255), "
					+ "MergeTitle VARCHAR(510), "
					+ "IssueLabel VARCHAR(32),"
					+ "primary key (ID)"
					+ ");");
		} 	
		return mysqlconn;
	}
	
	public static String getResponse(String urlToRead)  {
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
			LOGGER.log( Level.SEVERE, ex.toString(), ex );
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
			LOGGER.log( Level.SEVERE, ex.toString(), ex );
		}
		return lastPageNo;
	}
}
