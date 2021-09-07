package i5.las2peer.services.projectService.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	 * @return
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
	 * @param system Name of the system.
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
	 * @param system Name of the system.
	 * @return Value of "visibilityOfProjects" attribute set for this system.
	 */
	public ProjectVisibility getVisibilityOfProjectsBySystem(String systemName) {
		for(ProjectServiceSystem system : this.systems) {
			if(system.getName().equals(systemName)) return system.getVisibilityOfProjects();
		}
		return ProjectService.visibilityOfProjectsDefault;
	}
}
