package i5.las2peer.services.projectService.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import i5.las2peer.services.projectService.chat.ChatManager;
import org.json.simple.JSONObject;

import i5.las2peer.services.projectService.ProjectService;

public class SystemsConfig {

	private List<ProjectServiceSystem> systems;
	
	public SystemsConfig(JSONObject systemsJSON) {
		this.systems = new ArrayList<>();
		for(Object systemName : systemsJSON.keySet()) {
			JSONObject systemJSON = (JSONObject) systemsJSON.get(systemName);
			systems.add(new ProjectServiceSystem((String) systemName, systemJSON));
		}
	}
	
	/**
	 * Returns a map consisting for every system (key) the corresponding event listener service name (as value).
	 * If the event listener service was not set in the properties file, then it is null.
	 * @return Map that maps system names to event listener service names.
	 */
	public HashMap<String, String> getSystemEventListenerServiceMap() {
		HashMap<String, String> map = new HashMap<>();
		for(ProjectServiceSystem system : this.systems) {
			map.put(system.getName(), system.getEventListenerService());
		}
		return map;
	}
	
	/**
	 * Checks if the given system name is valid, i.e. if it is part of the systems JSON given as a system property.
	 * @param systemName Name of the system.
	 * @return Whether the given system name is valid.
	 */
	public boolean isValidSystemName(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return true;
		}
		return false;
	}
	
	/**
	 * Returns the value of the "visibilityOfProjects" attribute of the given system.
	 * @param systemName Name of the system.
	 * @return Value of "visibilityOfProjects" attribute set for this system.
	 */
	public ProjectVisibility getVisibilityOfProjectsBySystem(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return system.getVisibilityOfProjects();
		}
		return ProjectService.visibilityOfProjectsDefault;
	}
	
	/**
	 * Whether the GitHub projects connection is enabled for the system with the given name.
	 * @param systemName Name of the system.
	 * @return Whether the GitHub projects connection is enabled for the system with the given name.
	 */
	public boolean gitHubProjectsEnabled(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return system.gitHubProjectsEnabled();
		}
		return false;
	}
	
	/**
	 * Returns the name of the GitHub organization that is connected to the system.
	 * @param systemName System to search GitHub organization for.
	 * @return Name of the GitHub organization that is connected to the system.
	 */
	public String getGitHubOrganizationBySystem(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return system.getGitHubOrganization();
		}
		return null;
	}
	
	/**
	 * Returns the personal access token for GitHub related to the system.
	 * @param systemName System to search GitHub personal access token for.
	 * @return Personal access token for GitHub related to the system.
	 */
	public String getGitHubPATBySystem(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return system.getGitHubPersonalAccessToken();
		}
		return null;
	}

	public boolean isChannelConnectionEnabled(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return system.isChannelConnectionEnabled();
		}
		return false;
	}

	public ChatManager getChatManager(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return system.getChatManager();
		}
		return null;
	}
}
