package i5.las2peer.services.projectService.util;

import org.json.simple.JSONObject;

import i5.las2peer.services.projectService.ProjectService;

/**
 * Data class to store information about one system that the project service
 * handles projects for. Examples for a system could be the Social Bot Framework
 * or the Community Application Editor.
 * @author Philipp
 *
 */
public class ProjectServiceSystem {
	
	private static final String JSON_KEY_VISIBILITY_OF_PROJECTS = "visibilityOfProjects";
	private static final String JSON_KEY_EVENT_LISTENER_SERVICE = "eventListenerService";
	private static final String JSON_KEY_GITHUB_PROJECTS_ENABLED = "gitHubProjectsEnabled";
    private static final String JSON_KEY_GITHUB_ORGANIZATION = "gitHubOrganization";
    private static final String JSON_KEY_GITHUB_PERSONAL_ACCESS_TOKEN = "gitHubPersonalAccessToken";
	
	/**
	 * Name of the system. Example: SBF
	 */
	private String name;
	
	/**
	 * Whether projects of this system (and their metadata) can be read by everyone
	 * or only by project members.
	 */
	private ProjectVisibility visibilityOfProjects;
	
	/**
	 * Name of the event listener service of this system.
	 * This service will be notified about specific events (e.g., project-creation).
	 */
	private String eventListenerService;
	
	/**
	 * Whether the connection of las2peer projects within this system to GitHub 
	 * projects should be enabled.
	 */
	private boolean gitHubProjectsEnabled = false;
	
	/**
	 * Name of the GitHub organization where GitHub projects (corresponding 
	 * to projects of this system) should be stored. 
	 */
	private String gitHubOrganization;
	
	/**
	 * Personal access token from GitHub that allows to use the API
	 * to create new GitHub projects in the used GitHub organization.
	 */
	private String gitHubPersonalAccessToken;
	
	public ProjectServiceSystem(String systemName, JSONObject systemJSON) {
		this.name = systemName;
		
		if (systemJSON.containsKey(JSON_KEY_VISIBILITY_OF_PROJECTS)) {
			String visibility = (String) systemJSON.get(JSON_KEY_VISIBILITY_OF_PROJECTS);
			if (visibility.equals("all")) {
				this.visibilityOfProjects = ProjectVisibility.ALL;
			} else {
				this.visibilityOfProjects = ProjectVisibility.OWN;
			}
		} else {
			// use default value
			this.visibilityOfProjects = ProjectService.visibilityOfProjectsDefault;
		}
		
		if (systemJSON.containsKey(JSON_KEY_GITHUB_PROJECTS_ENABLED)) {
			this.gitHubProjectsEnabled = (boolean) systemJSON.get(JSON_KEY_GITHUB_PROJECTS_ENABLED);
			this.gitHubOrganization = (String) systemJSON.get(JSON_KEY_GITHUB_ORGANIZATION);
			this.gitHubPersonalAccessToken = (String) systemJSON.get(JSON_KEY_GITHUB_PERSONAL_ACCESS_TOKEN);
		}
		
		this.eventListenerService = (String) systemJSON.getOrDefault(JSON_KEY_EVENT_LISTENER_SERVICE, null);
	}
	
	
	public String getName() {
		return this.name;
	}
	
	public ProjectVisibility getVisibilityOfProjects() {
		return this.visibilityOfProjects;
	}
	
	public String getEventListenerService() {
		return this.eventListenerService;
	}
	
	public boolean hasEventListenerService() {
		return this.eventListenerService != null;
	}
	
	public boolean gitHubProjectsEnabled() {
		return this.gitHubProjectsEnabled;
	}
	
	public String getGitHubOrganization() {
		return this.gitHubOrganization;
	}
	
	public String getGitHubPersonalAccessToken() {
		return this.gitHubPersonalAccessToken;
	}
}
