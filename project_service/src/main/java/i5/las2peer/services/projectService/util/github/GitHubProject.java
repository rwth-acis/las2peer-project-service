package i5.las2peer.services.projectService.util.github;

/**
 * Contains the information about a GitHub project which is connected 
 * to a las2peer project.
 * @author Philipp
 *
 */
public class GitHubProject {

	/**
	 * The id of the GitHub project.
	 */
	private int id;
	
	/**
	 * The url to the GitHub project.
	 */
	private String url;
	
	public GitHubProject(int id, String url) {
		this.id = id;
		this.url = url;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getUrl() {
		return this.url;
	}
}