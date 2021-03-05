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
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.api.security.GroupAgent;
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

import javax.ws.rs.Consumes;

import i5.las2peer.services.projectService.project.Project;
import i5.las2peer.api.execution.ServiceNotFoundException;

/**
 * las2peer-project-service
 * 
 * A las2peer service for managing projects and their users.
 * 
 */
@Api
@SwaggerDefinition(
		info = @Info(
				title = "las2peer Project Service",
				version = "1.0.0",
				description = "A las2peer service for managing projects and their users."
				))
@ServicePath("/projects")
public class ProjectService extends RESTService {
	private final static String projects_prefix = "projects";
	
	@Override
	protected void initResources() {
		getResourceConfig().register(this);
	}
	
	/**
	 * Creates a new project in the pastry storage.
	 * Therefore, the user needs to be authorized.
	 * First, checks if a project with the given name already exists.
	 * If not, then the new project gets stored into the pastry storage.
	 * @param inputProject JSON representation of the project to store (containing name and access token of user needed to create Requirements Bazaar category).
	 * @return Response containing the status code (and a message or the created project).
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Creates a new project in the pastry storage if no project with the same name is already existing.")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, project created."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "User not authorized."),
			@ApiResponse(code = HttpURLConnection.HTTP_CONFLICT, message = "There already exists a project with the given name."),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Input project is not well formatted or some attribute is missing."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error.")
	})
	public Response postProject(String inputProject) throws ServiceNotFoundException {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "postProject: trying to store a new project");
		
		if(Context.getCurrent().getMainAgent() instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("User not authorized.").build();
		} else {
			Agent agent = Context.getCurrent().getMainAgent();
			Envelope env = null;
			Envelope env2 = null;
			//String id = "";
			Project project;
			
			try {
				project = new Project(agent, inputProject);
			} catch (ParseException e) {
				// JSON project given with the request is not well formatted or some attributes are missing
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(e.getMessage()).build();
			}
			
			String identifier = projects_prefix + "_" + project.getName();
			String identifier2 = projects_prefix;
			
			try {
				Context.get().requestEnvelope(identifier);
				// if requesting the envelope does not fail, then there already exists a project with the given name
				return Response.status(HttpURLConnection.HTTP_CONFLICT).entity("Project already exists").build();
			} catch (EnvelopeNotFoundException e) {
				// requesting the envelope failed, thus no project with the given name exists and we can create it
			} catch (EnvelopeAccessDeniedException | EnvelopeOperationFailedException e) {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
			}
			
			ProjectContainer cc = new ProjectContainer();
			
			// try to create group
			//groupAgent = Context.get().createGroupAgent(members, name);
			cc.addProject(project);
			try {
				System.out.println("Creating envelope");
				// create envelope for project using the ServiceAgent
				env = Context.get().createEnvelope(identifier, Context.get().getServiceAgent());
				System.out.println("Setting envelope content");
				// set the project container (which only contains the new project) as the envelope content
				env.setContent(cc);
				System.out.println("Storing envelope");
				// store envelope using ServiceAgent
				Context.get().storeEnvelope(env, Context.get().getServiceAgent());
				System.out.println("Storing complete");
				
				// writing to user
				try {
					// try to add project to project list
					env2 = Context.get().requestEnvelope(identifier2, Context.get().getServiceAgent());
					cc = (ProjectContainer) env2.getContent();
					cc.addProject(project);
					env2.setContent(cc);
					Context.get().storeEnvelope(env2, Context.get().getServiceAgent());
				} catch (EnvelopeNotFoundException e) {
					// create new project list
					cc = new ProjectContainer();
					env2 = Context.get().createEnvelope(identifier2, Context.get().getServiceAgent());
					env2.setPublic();
					cc.addProject(project);
					env2.setContent(cc);
					Context.get().storeEnvelope(env2, Context.get().getServiceAgent());
				}
			} catch (EnvelopeOperationFailedException | EnvelopeAccessDeniedException e1) {
				return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
			}
			
			return Response.status(HttpURLConnection.HTTP_OK).entity("Added Project To l2p Storage").build();
		}
	}
	
	
	/**
	 * Gets a user's projects
	 * Therefore, the user needs to be authorized.
	 * @return Response containing the status code
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Creates a new project in the database if no project with the same name is already existing.")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, projects fetched."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "User not authorized."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error.")
	})
	public Response getProjects() {
		Agent agent = Context.getCurrent().getMainAgent();
		if(agent instanceof AnonymousAgent) {
			return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).entity("User not authorized.").build();
		}
		
		String identifier = projects_prefix;
		JSONObject result = new JSONObject();
	    try {
			Envelope stored = Context.get().requestEnvelope(identifier, Context.get().getServiceAgent());
			ProjectContainer cc = (ProjectContainer) stored.getContent();
			// read all projects from the project list
			List<Project> projects = cc.getAllProjects();
			// create another hashmap for storing the projects, where the requesting agent has access to
			List<Project> projectsWithAccess = new ArrayList<>();
			
			// check which of all projects the user has access to
			for(Project project : projects) {
				//String projectJSON = entry.getValue();
				//JSONObject project = (JSONObject) JSONValue.parse(projectJSON);
				String groupId = project.getGroupIdentifier();
				// TODO: currently, the entries of the "projects" hashmap do not contain the group id (but only project name and group name)
				// To check whether the user is a member of the group, we need the group identifier
				try {
				    GroupAgent ga = (GroupAgent) Context.get().requestAgent(groupId, agent);
				    projectsWithAccess.add(project);
				} catch(AgentAccessDeniedException e) {
					// user is not allowed to access group agent => user is no group member
				}
				
			}
			
			List<JSONObject> projectsWithAccessJSON = new ArrayList<>();
			for(Project project : projectsWithAccess) {
				projectsWithAccessJSON.add(project.toJSONObject());
			}
			
			result.put("projects", projectsWithAccessJSON);
			//System.out.println(result);
			return Response.status(Status.OK).entity(result).build();
		} catch (EnvelopeNotFoundException e) {
			return Response.status(Status.OK).entity("No projects found").build();
		} catch (Exception e) {
			// write error to logfile and console
			// Couldnt build due to logging error so just left it out for now...
			//logger.log(Level.SEVERE, "Can't persist to network storage!", e);
			return Response.status(Status.BAD_REQUEST).entity("Unknown error occured: " + e.getMessage()).build();
		}
	}
}
