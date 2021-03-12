package i5.las2peer.services.projectService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import i5.las2peer.services.projectService.project.Project;

/**
 * This is an example object used to persist some data (in this case a simple
 * String) to the network storage. It can be replaced with any type of
 * Serializable or even with a plain String object.
 * 
 */
public class ProjectContainer implements Serializable {

	private static final long serialVersionUID = 1L;

	private HashSet<String> userProjects;

	private HashMap<String, Project> allProjects;

	public ProjectContainer() {
		userProjects = new HashSet<>();
		allProjects = new HashMap<>();
	}

	public HashSet<String> getUserProjects() {
		return userProjects;
	}

	public void addProject(Project p) {
		allProjects.put(p.getName(), p);
	}

	public void removeProject(Project p) {
		allProjects.remove(p.getName());
	}

	public List<Project> getAllProjects() {
		return new ArrayList<>(allProjects.values());
	}

}