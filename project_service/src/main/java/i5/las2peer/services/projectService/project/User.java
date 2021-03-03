package i5.las2peer.services.projectService.project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import net.minidev.json.JSONObject;
/*
import i5.las2peer.services.projectManagementService.exception.GitHubException;
import i5.las2peer.services.projectManagementService.exception.UserNotFoundException;
import i5.las2peer.services.projectManagementService.github.GitHubHelper;
*/
/**
 * (Data-)Class for User. Provides means to convert Object
 * to JSON. Also provides means to persist the object to a database.
 */
// Currently left this class in, but dont know if needed as las2peer agents already kinda fulfill the role

public class User {
	
	/**
	 * Id of the user.
	 * Might be -1 if the user is not stored to the database yet.
	 */
	private int id = -1;
	
	/**
	 * Email of the user.
	 */
	private String email;
	
	/**
	 * Login name of the user.
	 */
	private String loginName;
	
	/**
	 * GitHub username of the user.
	 * This does not need to be set.
	 */
	private String gitHubUsername;
	
	/**
	 * Access Token used to communicate with the GitHub API, e.g. for GitHub projects.
	 */
	private String gitHubAccessToken;
	
	/**
	 * Sets parameters except for the id.
	 * Can be used before persisting the user.
	 * @param email Email of the user that should be created.
	 * @param loginName Login name of the user that should be created.
	 */
	public User(String email, String loginName) {
		this.email = email;
		this.loginName = loginName;
	}
	
	/**
	 * Method for storing the user object to the database.
	 * @param connection a Connection object
	 * @throws SQLException If something with database went wrong.
	 */
	/*
	public void persist(Connection connection) throws SQLException {
		PreparedStatement statement;
		// formulate empty statement for storing the user
		statement = connection.prepareStatement("INSERT INTO User (email, loginName) VALUES (?,?);", Statement.RETURN_GENERATED_KEYS);
		// set email and loginName of user
		statement.setString(1, this.email);
		statement.setString(2, this.loginName);
		
		// execute query
		statement.executeUpdate();
		
		// get the generated project id and close statement
		ResultSet genKeys = statement.getGeneratedKeys();
		genKeys.next();
		this.id = genKeys.getInt(1);
		statement.close();
	}
	*/
	
	/**
	 * Updates the GitHub username of the user in the database.
	 * Also grants the given username access to all the CAE projects where the user 
	 * is a member of.
	 * @param username GitHub username that should be set.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong.
	 * @throws GitHubException If something with the communication to the GitHub API went wrong.
	 */
	/*
	public void putUsername(String username, Connection connection) throws SQLException, GitHubException {
		this.gitHubUsername = username;
		
		// insert to database
		PreparedStatement statement = connection.prepareStatement("UPDATE User SET gitHubUsername = ? WHERE id = ?;");
		statement.setString(1, this.gitHubUsername);
		statement.setInt(2, this.id);
		
		// execute update
		statement.executeUpdate();
		statement.close();
		
		// grant access to every GitHub project for every CAE project where the user is a member of
		ArrayList<Project> projects = Project.getProjectsByUser(this.id, connection);
		for(Project project : projects) {
			GitHubHelper.getInstance().grantUserAccessToProject(username, project.getGitHubProject());
		}
	}
	*/
	
	/**
	 * Updates the GitHub access token of the user in the database.
	 * @param accessToken GitHub access token that should be stored into the database.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong.
	 */
	/*
	public void putGitHubAccessToken(String accessToken, Connection connection) throws SQLException {
		this.gitHubAccessToken = accessToken;
		
		// insert to database
		PreparedStatement statement = connection.prepareStatement("UPDATE User SET gitHubAccessToken = ? WHERE id = ?;");
		statement.setString(1, this.gitHubAccessToken);
		statement.setInt(2, this.id);
		
		// execute update
		statement.executeUpdate();
		statement.close();
	}
	*/
	/**
	 * Method for loading user by given email from database.
	 * @param email Email of user to search for.
	 * @param connection a Connection object
	 * @throws SQLException If something with the database went wrong (or UserNotFoundException if user not found).
	 */
	/*
	public User(String email, Connection connection) throws SQLException {
		this.email = email;
		
		// search for user with the given name
	    PreparedStatement statement = connection.prepareStatement("SELECT * FROM User WHERE email=?;");
		statement.setString(1, email);
		// execute query
	    ResultSet queryResult = statement.executeQuery();
	    
	    // check for results
		if (queryResult.next()) {
			this.id = queryResult.getInt(1);
			this.loginName = queryResult.getString("loginName");
			this.gitHubUsername = queryResult.getString("gitHubUsername");
			this.gitHubAccessToken = queryResult.getString("gitHubAccessToken");
		} else {
			// there does not exist a user with the given email in the database
			throw new UserNotFoundException();
		}
		statement.close();
	}
	*/
	/**
	 * Searches for a user with the given loginName.
	 * @param loginName Login name of the user to search for.
	 * @param connection Connection object
	 * @return User object
	 * @throws SQLException If something with the database went wrong (or UserNotFoundException).
	 */
	/*
	public static User loadUserByLoginName(String loginName, Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("SELECT * FROM User WHERE loginName=?;");
		statement.setString(1, loginName);
		// execute query
		ResultSet queryResult = statement.executeQuery();
		
		// check for results
		if(queryResult.next()) {
		    String email = queryResult.getString("email");
			statement.close();
			return new User(email, connection);
		} else {
			statement.close();
			throw new UserNotFoundException();
		}
	}
	*/
	/**
	 * Searches for users in the database where the login name is like the given one.
	 * @param loginName Login name to search for.
	 * @param connection Connection object
	 * @return ArrayList containing the User objects that were found.
	 * @throws SQLException If something with the database went wrong.
	 */
	/*
	public static ArrayList<User> searchUsers(String loginName, Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("SELECT * FROM User WHERE loginName LIKE ?;");
		statement.setString(1, "%" + loginName + "%");
		// execute query
		ResultSet queryResult = statement.executeQuery();
		
		ArrayList<User> users = new ArrayList<>();
		
		while(queryResult.next()) {
			String email = queryResult.getString("email");
			users.add(new User(email, connection));
		}
		
		return users;
	}*/
	
	/**
	 * Creates a JSON object from the user object.
	 * @param all Whether all information of the user is required, or e.g. email can be omitted.
	 * @return JSONObject containing the user information.
	 */
	/*
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject(boolean all) {
		JSONObject jsonUser = new JSONObject();
		
		// put attributes
		jsonUser.put("id", this.id);
		jsonUser.put("loginName", this.loginName);
		jsonUser.put("gitHubUsername", this.gitHubUsername);
		if(all) {
		    jsonUser.put("email", this.email);
		    jsonUser.put("gitHubAccessToken", this.gitHubAccessToken);
		}

		return jsonUser;
	}
	*/
	public String getLoginName() {
		return this.loginName;
	}
	
/*	public String getGitHubUsername() {
		return this.gitHubUsername;
	}
*/	
	public int getId() {
		return this.id;
	}
	
}
