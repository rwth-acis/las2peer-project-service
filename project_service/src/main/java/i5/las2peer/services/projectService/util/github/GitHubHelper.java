package i5.las2peer.services.projectService.util.github;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.projectService.util.SystemsConfig;


/**
 * Helper class for working with GitHub API.
 * Currently supports creating new GitHub projects and update their
 * visibility to public.
 * @author Philipp
 *
 */
public class GitHubHelper {
	
	private static GitHubHelper instance;
	private static final String API_BASE_URL = "https://api.github.com";
	
	// make sure that constructor cannot be accessed from outside
	private GitHubHelper() {}
	
	public static GitHubHelper getInstance() {
		if(GitHubHelper.instance == null) {
			GitHubHelper.instance = new GitHubHelper();
		}
		return GitHubHelper.instance;
	}
	
	/**
	 * Systems configuration (that also contains the GitHub config).
	 */
	private SystemsConfig systemsConfig = null;
	
	public void setSystemsConfig(SystemsConfig systemsConfig) {
		this.systemsConfig = systemsConfig;
	}
	
	/**
	 * Creates a public GitHub project with the given name.
	 * @param systemName Name of the system, for which the GitHub project should be created.
	 * @param projectName Name of the GitHub project which should be created.
	 * @return The newly created GitHubProject object.
	 * @throws GitHubException If something with the requests to the GitHub API went wrong.
	 */
	public GitHubProject createPublicGitHubProject(String systemName, String projectName) throws GitHubException {
		String gitHubOrganization = this.systemsConfig.getGitHubOrganizationBySystem(systemName);
		String gitHubPersonalAccessToken = this.systemsConfig.getGitHubPATBySystem(systemName);
		
		if(gitHubPersonalAccessToken == null || gitHubOrganization == null) {
			throw new GitHubException("One of the variables personal access token or organization are not set.");
		}
		
		GitHubProject gitHubProject = createGitHubProject(systemName, projectName);
		makeGitHubProjectPublic(systemName, gitHubProject.getId());
		
		// create some predefined columns
		createProjectColumn(systemName, gitHubProject.getId(), "To do");
		createProjectColumn(systemName, gitHubProject.getId(), "In progress");
		createProjectColumn(systemName, gitHubProject.getId(), "Done");
		
		return gitHubProject;
	}
	
	/**
	 * Gives the GitHub user with the given username access to the given GitHub project.
	 * @param systemName Name of the system, that the GitHub project belongs to.
	 * @param ghUsername Username of the GitHub user which should get access to the project.
	 * @param ghProject GitHubProject object
	 * @throws GitHubException If something with the API request went wrong
	 */
	public void grantUserAccessToProject(String systemName, String ghUsername, GitHubProject ghProject) throws GitHubException {
		if(ghUsername == null) return;
		
		String authStringEnc = getAuthStringEnc(systemName);
		
		URL url;
		try {
			url = new URL(API_BASE_URL + "/projects/" + ghProject.getId() + "/collaborators/" + ghUsername);

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Accept", "application/vnd.github.inertia-preview+json");
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			
			// forward (in case of) error
			if (connection.getResponseCode() != 204) {
				String message = getErrorMessage(connection);
				throw new GitHubException(message);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		}
	}
	
	/**
	 * Deletes the given GitHub project.
	 * @param systemName Name of the system, to which the GitHub project belongs to.
	 * @param ghProject GitHub project which should be deleted.
	 * @throws GitHubException If something with the request to the GitHub API went wrong.
	 */
	public void deleteGitHubProject(String systemName, GitHubProject ghProject) throws GitHubException {
        String authStringEnc = getAuthStringEnc(systemName);
		
		URL url;
		try {
			url = new URL(API_BASE_URL + "/projects/" + ghProject.getId());

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("DELETE");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Accept", "application/vnd.github.inertia-preview+json");
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			
			// forward (in case of) error
			if (connection.getResponseCode() != 204) {
				String message = getErrorMessage(connection);
				throw new GitHubException(message);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		}
	}
	
	/**
	 * Creates a GitHub project in the GitHub organization given by the properties file.
	 * @param systemName Name of the system for which the GitHub project should be created.
	 * @param projectName Name of the GitHub project.
	 * @return The newly created GitHubProject object.
	 * @throws GitHubException If something with creating the new project went wrong.
	 */
	private GitHubProject createGitHubProject(String systemName, String projectName) throws GitHubException {
		String body = getGitHubProjectBody(projectName);
		String authStringEnc = getAuthStringEnc(systemName);
		
		String gitHubOrganization = this.systemsConfig.getGitHubOrganizationBySystem(systemName);

		URL url;
		try {
			url = new URL(API_BASE_URL + "/orgs/" + gitHubOrganization + "/projects");

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Accept", "application/vnd.github.inertia-preview+json");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", String.valueOf(body.length()));
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);

			writeRequestBody(connection, body);
			
			// forward (in case of) error
			if (connection.getResponseCode() != 201) {
				String message = getErrorMessage(connection);
				throw new GitHubException(message);
			} else {
				// get response
				String response = getResponseBody(connection);
				
				// convert to JSONObject
				JSONObject json = (JSONObject) JSONValue.parseWithException(response);
				int gitHubProjectId = ((Long) json.get("id")).intValue();
				String gitHubProjectHtmlUrl = (String) json.get("html_url");
				return new GitHubProject(gitHubProjectId, gitHubProjectHtmlUrl);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		} catch (ParseException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		}
	}
	
	/**
	 * Changes the visibility of the GitHub project with the given id to "private: false".
	 * After calling this method, the GitHub project should be public and can be accessed by 
	 * every GitHub user (accessed only means read-access).
	 * @param systemName Name of the system, for which the GitHub project should be updated.
	 * @param gitHubProjectId Id of the GitHub project id, whose visibility should be updated.
	 * @throws GitHubException If something with the request to the GitHub API went wrong.
	 */
	private void makeGitHubProjectPublic(String systemName, int gitHubProjectId) throws GitHubException {
		String body = getVisibilityPublicBody();
		String authStringEnc = getAuthStringEnc(systemName);
		
		String url = API_BASE_URL + "/projects/" + gitHubProjectId;
		
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("PATCH", BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Accept", "application/vnd.github.inertia-preview+json")
                .header("Authorization", "Basic " + authStringEnc)
                .build();
		
		HttpResponse<String> response;
		try {
			response = client.send(request, BodyHandlers.ofString());
			if(response.statusCode() != 200) {
				String message = response.body();
				throw new GitHubException(message);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		}
	}
	
	/**
	 * Creates a new column with the given name in the GitHub project with the given id.
	 * @param systemName Name of the system, for which the column should be created.
	 * @param gitHubProjectId Id of the GitHub project, where the column should be added to.
	 * @param columnName Name of the column, which should be created.
	 * @throws GitHubException If something with the request to the GitHub API went wrong.
	 */
	private void createProjectColumn(String systemName, int gitHubProjectId, String columnName) throws GitHubException {
		String body = getCreateColumnBody(columnName);
		String authStringEnc = getAuthStringEnc(systemName);

		URL url;
		try {
			url = new URL(API_BASE_URL + "/projects/" + gitHubProjectId + "/columns");

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Accept", "application/vnd.github.inertia-preview+json");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", String.valueOf(body.length()));
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);

			writeRequestBody(connection, body);
			
			// forward (in case of) error
			if (connection.getResponseCode() != 201) {
				String message = getErrorMessage(connection);
				throw new GitHubException(message);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		}
	}

	/**
	 * Creates the body needed to create a new column in a GitHub project.
	 * @param columnName Name of the column that should be created.
	 * @return Body as string.
	 */
	private String getCreateColumnBody(String columnName) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", columnName);
		String body = JSONObject.toJSONString(jsonObject);
		return body;
	}
	
	/**
	 * Creates the body needed to update the visibility of the GitHub project.
	 * @return Body as String.
	 */
	private String getVisibilityPublicBody() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("private", false);
		String body = JSONObject.toJSONString(jsonObject);
		return body;
	}
	
	/**
	 * Creates the body needed for creating a new GitHub project.
	 * @param projectName Name of the project that should be created on GitHub.
	 * @return Body containing the information about the GitHub project which will be created.
	 */
	private String getGitHubProjectBody(String projectName) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", projectName);
		jsonObject.put("body", "This GitHub project was auto-generated by las2peer.");
		String body = JSONObject.toJSONString(jsonObject);
		return body;
	}
	
	/**
	 * Getter for encoded auth string.
	 * @param systemName Name of the system for which the request should be sent (relevant to choose correct PAT).
	 * @return Encoded auth string containing GitHub personal access token.
	 */
	private String getAuthStringEnc(String systemName) {
		String authString = this.systemsConfig.getGitHubPATBySystem(systemName);

		byte[] authEncBytes = Base64.getEncoder().encode(authString.getBytes());
		return new String(authEncBytes);
	}
	
	/**
	 * Extracts the error message from the response.
	 * @param connection HttpURLConnection object
	 * @return Error message as String.
	 * @throws IOException
	 */
	private String getErrorMessage(HttpURLConnection connection) throws IOException {
		String message = "Error creating GitHub project at: ";
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
		for (String line; (line = reader.readLine()) != null;) {
			message += line;
		}
		reader.close();
		return message;
	}
	
	/**
	 * Getter for the body of the response.
	 * @param connection HttpURLConnection object
	 * @return Body of the response as string.
	 * @throws IOException
	 */
	private String getResponseBody(HttpURLConnection connection) throws IOException {
		String response = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		for (String line; (line = reader.readLine()) != null;) {
			response += line;
		}
		reader.close();
		return response;
	}
	
	/**
	 * Writes the request body.
	 * @param connection HttpURLConnection object
	 * @param body Body that should be written to the request.
	 * @throws IOException
	 */
	private void writeRequestBody(HttpURLConnection connection, String body) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write(body);
		writer.flush();
		writer.close();
	}
	
}
