package i5.las2peer.services.projectService.project;

import java.io.Serializable;

import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.Agent;

/**
 * (Data-)Class for Projects. Provides means to convert JSON to Object and
 * Object to JSON. TODO: check if this javadoc is still correct later
 */
public class Project implements Serializable {

	/**
	 * Name of the project.
	 */
	private String name;

	/**
	 * Users that are part of the project.
	 */
	// private ArrayList<String> users;

	/**
	 * Group linked to Project.
	 */
	private String groupName;

	/**
	 * Identifier of the group linked to the project.
	 */
	private String groupIdentifier;

	/**
	 * String containing the JSON representation of the project metadata. This
	 * metadata can be used to store additional information on the project, that
	 * might be system-specific.
	 */
	private String metadata;

	public Project(String name, String groupName, String groupIdentifier, String metadata) {
		this.name = name;
		this.groupName = groupName;
		this.groupIdentifier = groupIdentifier;
		this.metadata = metadata;
	}

	/**
	 * Creates a project object from the given JSON string. This constructor should
	 * be used before storing new projects.
	 * 
	 * @param creator     User that creates the project.
	 * @param jsonProject JSON representation of the project to store.
	 * @throws ParseException If parsing went wrong or one of the keys is missing in
	 *                        the given JSON representation.
	 */
	public Project(Agent creator, String jsonProject) throws ParseException {
		JSONObject project = (JSONObject) JSONValue.parseWithException(jsonProject);

		this.containsKeyWithException(project, "name");
		this.name = (String) project.get("name");

		// this.users = new ArrayList<>();
		// this.users.add(creator);
		// group and users to project from said group
		this.containsKeyWithException(project, "linkedGroup");
		JSONObject linkedGroup = (JSONObject) project.get("linkedGroup");

		// get name of linked group
		this.containsKeyWithException(linkedGroup, "name");
		this.groupName = (String) linkedGroup.get("name");

		// get id of linked group
		this.containsKeyWithException(linkedGroup, "id");
		this.groupIdentifier = (String) linkedGroup.get("id");

		// check if jsonProject contains metadata
		if (project.containsKey("metadata")) {
			// try converting to JSONObject (to check if valid JSON)
			JSONObject metadataJSON = (JSONObject) project.get("metadata");
			this.metadata = metadataJSON.toJSONString();
		} else {
			// no metadata given, just store an empty object
			JSONObject empty = new JSONObject();
			this.metadata = empty.toJSONString();
		}

		if (project.containsKey("users")) {
			for (int i = 0; i < ((JSONArray) project.get("users")).size(); i++) {
				String userName = ((JSONArray) project.get("users")).get(i).toString();
				try {
					String userId = Context.get().getUserAgentIdentifierByLoginName(userName);
					System.out.println(userId);
					// this.users.add(userId);
				} catch (Exception q) {
					System.out.println(q + "User does not exist?");
				}
				/*
				 * if(user != true) {
				 * 
				 * }
				 */
			}
		}
	}

	/**
	 * Checks if the given JSONObject contains the given key. If key does not exist,
	 * then a ParseException is thrown.
	 * 
	 * @param json JSONObject where the key should be searched.
	 * @param key  Key that should be searched in given JSONObject.
	 * @throws ParseException If given JSONObject does not contain given key, a
	 *                        ParseException is thrown.
	 */
	private static void containsKeyWithException(JSONObject json, String key) throws ParseException {
		if (!json.containsKey(key))
			throw new ParseException(0, "Attribute '" + key + "' of project is missing.");
	}

	/**
	 * Changes linked group to given new group.
	 * 
	 * @param groupIdentifier Groupagent id of new group
	 * @param groupName       Groupname of new group
	 */
	public void changeGroup(String groupIdentifier, String groupName) {
		this.groupIdentifier = groupIdentifier;
		this.groupName = groupName;
	}

	/**
	 * Changes metadata of project.
	 * 
	 * @param newMetadata Metadata to replace the old one.
	 */
	public void changeMetadata(String newMetadata) {
		this.metadata = newMetadata;
	}

	/**
	 * Returns the JSON representation of this project.
	 * 
	 * @return a JSON object representing a project
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonProject = new JSONObject();

		// put attributes
		jsonProject.put("name", this.name);
		jsonProject.put("groupName", this.groupName);
		jsonProject.put("groupIdentifier", this.groupIdentifier);
		jsonProject.put("metadata", this.getMetadataAsJSONObject());

		return jsonProject;
	}

	/**
	 * Getter for the name of the project.
	 * 
	 * @return Name of the project.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Getter for the name of the group connected to the project.
	 * 
	 * @return Name of the group.
	 */
	public String getGroupName() {
		return this.groupName;
	}

	/**
	 * Getter for the identifier of the group connected to the project.
	 * 
	 * @return Identifier of the group.
	 */
	public String getGroupIdentifier() {
		return this.groupIdentifier;
	}

	/**
	 * Getter for the project metadata as a String.
	 * 
	 * @return JSON String representation of the project metadata.
	 */
	public String getMetadataString() {
		return this.metadata;
	}

	/**
	 * Getter for the project metadata as a JSONObject.
	 * 
	 * @return Project metadata converted to JSONObject.
	 */
	public JSONObject getMetadataAsJSONObject() {
		return (JSONObject) JSONValue.parse(this.metadata);
	}
}
