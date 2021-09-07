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
}
