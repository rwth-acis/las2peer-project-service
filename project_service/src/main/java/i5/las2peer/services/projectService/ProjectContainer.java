package i5.las2peer.services.projectService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import i5.las2peer.services.projectService.project.Project;

/**
 * This is an example object used to persist some data (in this case a simple String) to the network storage. It can be
 * replaced with any type of Serializable or even with a plain String object.
 * 
 */
public class ProjectContainer implements Serializable {

	private static final long serialVersionUID = 1L;

	private HashSet<Project> userProjects;

	public ProjectContainer() {
		userProjects = new HashSet<Project>();
	}

	public void addProject(Project p) {
		userProjects.add(p);
	}

	public HashSet<Project> getUserProjects() {
		return userProjects;
	}

	public boolean removeProject(Project p) {
		return userProjects.remove(p);
	}

}