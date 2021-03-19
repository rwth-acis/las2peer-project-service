package i5.las2peer.services.projectService;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.ServiceException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.api.security.ServiceAgent;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

import org.json.simple.JSONObject;

import org.json.simple.parser.ParseException;

import org.json.simple.JSONValue;

import javax.ws.rs.Consumes;

import i5.las2peer.services.projectService.project.Project;

/**
 * las2peer-project-service
 * 
 * A las2peer service for managing projects and their users.
 * 
 */
@Api
@SwaggerDefinition(info = @Info(title = "las2peer Project Service", version = "1.0.0", description = "A las2peer service for managing projects and their users."))
@ServicePath("/projects")
@ManualDeployment
public class ProjectService extends RESTService {
	private final static String projects_prefix = "projects";

	private String visibilityOfProjects;

	// service that should be called on specific events such as project creation
	private String eventListenerService;
	private EventManager eventManager;

	private String serviceGroupId;
	private String oldServiceAgentId;
	private String oldServiceAgentPw;

	@Override
	protected void initResources() {
		getResourceConfig().register(this);
	}

	public ProjectService() {
		super();
		setFieldValues(); // This sets the values of the configuration file
		System.out.println(serviceGroupId);
		this.eventManager = new EventManager(this.eventListenerService);
	}

	public GroupAgent getServiceGroupAgent() {
		try {
			return (GroupAgent) Context.get().requestAgent(this.serviceGroupId, Context.get().getServiceAgent());
		} catch (AgentAccessDeniedException | AgentNotFoundException | AgentOperationFailedException e) {
			// TODO: error handling
			try {
				// Dont know if this is the best solution, but works, the user just needs to
				// take care of the old service agent id field + pw
				// Note: when calling this method at the same time, sometimes a problem occurs
				// when trying to store groups at the same time

				System.out.println("Adding service agent " + Context.get().getServiceAgent().getIdentifier());

				ServiceAgent sAgent = (ServiceAgent) Context.get().fetchAgent(this.oldServiceAgentId);
				sAgent.unlock(this.oldServiceAgentPw);
				GroupAgent gAgent = (GroupAgent) Context.get().requestAgent(this.serviceGroupId, sAgent);
				gAgent.addMember(Context.get().getServiceAgent());
				Context.get().storeAgent(gAgent);
				return gAgent;
			} catch (Exception e1) {
				System.out.println("Getting Service Group Agent failed because of:" + e1);
				return null;
			}
			return null;
		}
	}

	/**
	 * This method can be used by other services, to verify if a user is allowed to
	 * write-access a project.
	 * 
	 * @param projectName Project where the permission should be checked for.
	 * @return True, if agent has access to project. False otherwise (or if project
	 *         with given name does not exist).
	 */
	public boolean hasAccessToProject(String projectName) {
		String identifier = projects_prefix + "_" + projectName;
		try {
			Context.getCurrent().requestEnvelope(identifier);
		} catch (EnvelopeAccessDeniedException e) {
			return false;
		} catch (EnvelopeNotFoundException | EnvelopeOperationFailedException e) {
			return false;
		}
		return true;
	}

	/**
	 * Creates a new project in the pastry storage. Therefore, the user needs to be
	 * authorized. First, checks if a project with the given name already exists. If
	 * not, then the new project gets stored into the pastry storage.
	 * 
	 * @param inputProject JSON representation of the project to store (containing
	 *                     name and access token of user needed to create
	 *                     Requirements Bazaar category).
	 * @return Response containing the status code (and a message or the created
	 *         project).
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Creates a new project in the pastry storage if no project with the same name is already existing.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, project created."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "User not authorized."),
			@ApiResponse(code = HttpURLConnection.HTTP_CONFLICT, message = "There already exists a project with the given name."),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Input project is not well formatted or some attribute is missing."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error.") })
	public Response postProject(String inputProject) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "postProject: trying to store a new project");

		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("User not authorized.").build();
		} else {
			GroupAgent serviceGroupAgent = getServiceGroupAgent();
			if (serviceGroupAgent == null)
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
						.entity("Cannot access service group agent.").build();

			Agent agent = Context.getCurrent().getMainAgent();
			Envelope env = null;
			Envelope env2 = null;
			// String id = "";
			Project project;

			try {
				project = new Project(agent, inputProject);
			} catch (ParseException e) {
				// JSON project given with the request is not well formatted or some attributes
				// are missing
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(e.getMessage()).build();
			}

			String identifier = projects_prefix + "_" + project.getName();
			String identifier2 = projects_prefix;

			try {
				Context.get().requestEnvelope(identifier);
				// if requesting the envelope does not fail, then there already exists a project
				// with the given name
				return Response.status(HttpURLConnection.HTTP_CONFLICT).entity("Project already exists").build();
			} catch (EnvelopeNotFoundException e) {
				// requesting the envelope failed, thus no project with the given name exists
				// and we can create it
			} catch (EnvelopeAccessDeniedException | EnvelopeOperationFailedException e) {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
			}

			GroupAgent groupAgent;
			try {
				// use main agent (user) to request the group agent
				groupAgent = (GroupAgent) Context.get().requestAgent(project.getGroupIdentifier(),
						Context.get().getMainAgent());
			} catch (AgentAccessDeniedException e) {
				// could not unlock group agent => user is no group member
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
						.entity("User is no member of the group linked to the given project.").build();
			} catch (AgentNotFoundException e) {
				// could not find group agent
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
						.entity("The group linked to the given project cannot be found.").build();
			} catch (AgentOperationFailedException e) {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
			}

			ProjectContainer cc = new ProjectContainer();

			// try to create group
			// groupAgent = Context.get().createGroupAgent(members, name);
			cc.addProject(project);
			try {
				System.out.println("Creating envelope");
				// create envelope for project using the group agent
				env = Context.get().createEnvelope(identifier, groupAgent);
				System.out.println("Setting envelope content");
				// set the project container (which only contains the new project) as the
				// envelope content
				env.setContent(cc);
				System.out.println("Storing envelope");
				// store envelope using the group agent
				Context.get().storeEnvelope(env, groupAgent);
				System.out.println("Storing complete");

				// writing to user
				try {
					// try to add project to project list (with ServiceAgent)
					System.out.println("A");
					env2 = Context.get().requestEnvelope(identifier2, serviceGroupAgent);
					cc = (ProjectContainer) env2.getContent();
					cc.addProject(project);
					env2.setContent(cc);
					Context.get().storeEnvelope(env2, serviceGroupAgent);
					System.out.println("B");
				} catch (EnvelopeNotFoundException e) {
					// create new project list (with ServiceAgent)
					System.out.println("C");
					cc = new ProjectContainer();
					env2 = Context.get().createEnvelope(identifier2, serviceGroupAgent);
					env2.setPublic();
					cc.addProject(project);
					env2.setContent(cc);
					Context.get().storeEnvelope(env2, serviceGroupAgent);
				}
			} catch (EnvelopeOperationFailedException | EnvelopeAccessDeniedException e1) {
				System.out.println(e1);
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
			}

			if (this.eventManager.sendProjectCreatedEvent(Context.get(), project.toJSONObject())) {
				return Response.status(HttpURLConnection.HTTP_CREATED).entity("Added Project To l2p Storage").build();
			} else {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR)
						.entity("Sending event to event listener service failed.").build();
			}
		}
	}

	/**
	 * Gets a user's projects Therefore, the user needs to be authorized.
	 * 
	 * @return Response containing the status code
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Creates a new project in the database if no project with the same name is already existing.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, projects fetched."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "User not authorized."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error.") })
	public Response getProjects() {
		System.out.println("sasas" + visibilityOfProjects);
		Agent agent = Context.getCurrent().getMainAgent();
		if (agent instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("User not authorized.").build();
		}

		GroupAgent serviceGroupAgent = getServiceGroupAgent();
		if (serviceGroupAgent == null)
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Cannot access service group agent.")
					.build();

		String identifier = projects_prefix;
		JSONObject result = new JSONObject();
		try {
			Envelope stored = Context.get().requestEnvelope(identifier, serviceGroupAgent);
			ProjectContainer cc = (ProjectContainer) stored.getContent();
			// read all projects from the project list
			List<Project> projects = cc.getAllProjects();
			// create another list for storing the projects that should be returned as JSON
			// objects
			List<JSONObject> projectsJSON = new ArrayList<>();

			for (Project project : projects) {
				// To check whether the user is a member of the project/group, we need the group
				// identifier
				String groupId = project.getGroupIdentifier();
				JSONObject projectJSON = project.toJSONObject();
				try {
					GroupAgent ga = (GroupAgent) Context.get().requestAgent(groupId, agent);
					// user is allowed to access group agent => user is a project/group member
					// add attribute to project JSON which tells that the user is a project member
					projectJSON.put("is_member", true);
					projectsJSON.add(projectJSON);
				} catch (AgentAccessDeniedException e) {
					// user is not allowed to access group agent => user is no project/group member
					// only return this project if the service is configured that all projects are
					// readable by any user
					if (visibilityOfProjects.equals("all")) {
						projectJSON.put("is_member", false);
						projectsJSON.add(projectJSON);
					}
				}
			}

			result.put("projects", projectsJSON);
			// System.out.println(result);
			return Response.status(Status.OK).entity(result).build();
		} catch (EnvelopeNotFoundException e) {
			// return empty list of projects
			result.put("projects", new ArrayList<>());
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			// write error to logfile and console
			// Couldnt build due to logging error so just left it out for now...
			// logger.log(Level.SEVERE, "Can't persist to network storage!", e);
			return Response.status(Status.BAD_REQUEST).entity("Unknown error occured: " + e.getMessage()).build();
		}
	}

	/**
	 * Changes the group linked to an existing project in the pastry storage.
	 * Therefore, the user needs to be authorized.
	 * 
	 * @param body JSON representation of the project to store (containing name and
	 *             access token of user needed to create Requirements Bazaar
	 *             category).
	 * @return Response containing the status code (and a message or the created
	 *         project).
	 */
	@POST
	@Path("/changeGroup")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Creates a new project in the pastry storage if no project with the same name is already existing.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, group changed."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "User not authorized."),
			@ApiResponse(code = HttpURLConnection.HTTP_CONFLICT, message = "The given group is already linked to the project."),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Input project is not well formatted or some attribute is missing."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error.") })
	public Response changeGroup(String body) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "changeGroup: trying to change group of project");

		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("User not authorized.").build();
		} else {
			Agent agent = Context.getCurrent().getMainAgent();
			try {
				JSONObject jsonBody = (JSONObject) JSONValue.parseWithException(body);

				String projectName = (String) jsonBody.get("projectName");
				String newGroupId = (String) jsonBody.get("newGroupId");
				String newGroupName = (String) jsonBody.get("newGroupName");
				String identifier = projects_prefix;

				// check if user currently has access to project
				if (!this.hasAccessToProject(projectName)) {
					return Response.status(HttpURLConnection.HTTP_FORBIDDEN)
							.entity("User is no member of the project and thus not allowed to edit its linked group.")
							.build();
				}

				try {
					Envelope stored = Context.get().requestEnvelope(identifier, Context.get().getServiceAgent());
					ProjectContainer cc = (ProjectContainer) stored.getContent();
					// read all projects from the project list
					List<Project> projects = cc.getAllProjects();

					for (Project project : projects) {
						// To check whether the user is a member of the project/group, we need the group
						// identifier
						String groupId = project.getGroupIdentifier();
						// Search correct project
						if (projectName.equals(project.getName())) {
							// check if new group actually differs from old group
							if (!newGroupId.equals(groupId)) {
								try {
									GroupAgent ga = (GroupAgent) Context.get().requestAgent(newGroupId, agent);
									// user is allowed to access group agent => user is a project/group member
									cc.removeProject(project);
									project.changeGroup(newGroupId, newGroupName);
									cc.addProject(project);
									stored.setContent(cc);
									Context.get().storeEnvelope(stored, Context.get().getServiceAgent());
									JSONObject response = new JSONObject();
									response.put("project", project);
									return Response.status(Status.OK).entity("Group successfully changed!")
											.entity(response).build();
								} catch (AgentAccessDeniedException e) {
									// user is not allowed to access group agent => user is no project/group member
									// cant use group which user is not a part of
									return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
											.entity("You are not a part of this group!").build();

								} catch (AgentNotFoundException e) {
									// or: group does not exist
									return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
											.entity("Non-existing group").build();
								} catch (AgentOperationFailedException e) {
									return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e).build();
								}
							}
						}
					}

					// create another list for storing the projects that should be returned as JSON
					// objects
					List<JSONObject> projectsJSON = new ArrayList<>();
				} catch (EnvelopeNotFoundException e) {

					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("No projects available.").build();
				} catch (EnvelopeAccessDeniedException | EnvelopeOperationFailedException e) {
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
				}
			} catch (ParseException e) {
				// JSON project given with the request is not well formatted or some attributes
				// are missing
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(e.getMessage()).build();
			}

			return Response.status(HttpURLConnection.HTTP_CREATED).entity("Added Project To l2p Storage").build();
		}
	}

	/**
	 * Changes the group linked to an existing project in the pastry storage.
	 * Therefore, the user needs to be authorized.
	 * 
	 * @param body JSON representation of the project to store (containing name and
	 *             access token of user needed to create Requirements Bazaar
	 *             category).
	 * @return Response containing the status code (and a message or the created
	 *         project).
	 */
	@POST
	@Path("/changeMetadata")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Change metadata corresponding to project.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, metadata changed."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "User not authorized."),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Input project is not well formatted or some attribute is missing."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error.") })
	public Response changeMetadata(String body) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "changeGroup: trying to change group of project");

		if (Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("User not authorized.").build();
		} else {
			Agent agent = Context.getCurrent().getMainAgent();
			try {
				JSONObject jsonBody = (JSONObject) JSONValue.parseWithException(body);
				String projectName = (String) jsonBody.get("projectName");
				String oldMetadata = jsonBody.get("oldMetadata").toString();
				String newMetadata = jsonBody.get("newMetadata").toString();
				String identifier = projects_prefix;
				String newGroupName = "ss";
				// check if user currently has access to project
				if (!this.hasAccessToProject(projectName)) {
					return Response.status(HttpURLConnection.HTTP_FORBIDDEN)
							.entity("User is no member of the project and thus not allowed to edit its linked group.")
							.build();
				}

				try {
					Envelope stored = Context.get().requestEnvelope(identifier, Context.get().getServiceAgent());
					ProjectContainer cc = (ProjectContainer) stored.getContent();
					// read all projects from the project list
					List<Project> projects = cc.getAllProjects();

					for (Project project : projects) {
						// To check whether there is an inconsistency, we compare the old metadata given
						// as a parameter;
						// Search correct project
						if (projectName.equals(project.getName())) {
							// check if new group actually differs from old group
							if (oldMetadata.equals(project.getMetadataString())) {
								try {
									GroupAgent ga = (GroupAgent) Context.get()
											.requestAgent(project.getGroupIdentifier(), agent);
									// user is allowed to access group agent => user is a project/group member
									cc.removeProject(project);
									project.changeMetadata(newMetadata);
									cc.addProject(project);
									stored.setContent(cc);
									Context.get().storeEnvelope(stored, Context.get().getServiceAgent());
									JSONObject response = new JSONObject();
									response.put("project", project);
									return Response.status(Status.OK).entity("Metadata successfully changed!")
											.entity(response).build();
								} catch (AgentAccessDeniedException e) {
									// user is not allowed to access group agent => user is no project/group member
									// cant use group which user is not a part of
									return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
											.entity("You are not a part of this group!").build();

								} catch (AgentNotFoundException e) {
									// or: group does not exist
									return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
											.entity("Non-existing group").build();
								} catch (AgentOperationFailedException e) {
									return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e).build();
								}
							} else {
								return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
										.entity("Inconsistency with old metadata, please reload page and try again!")
										.build();
							}
						}
					}

					// create another list for storing the projects that should be returned as JSON
					// objects
					List<JSONObject> projectsJSON = new ArrayList<>();
				} catch (EnvelopeNotFoundException e) {

					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("No projects available.").build();
				} catch (EnvelopeAccessDeniedException | EnvelopeOperationFailedException e) {
					return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
				}
			} catch (ParseException e) {
				// JSON project given with the request is not well formatted or some attributes
				// are missing
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(e.getMessage()).build();
			}

			return Response.status(HttpURLConnection.HTTP_CREATED).entity("Added Project To l2p Storage").build();
		}
	}
}
