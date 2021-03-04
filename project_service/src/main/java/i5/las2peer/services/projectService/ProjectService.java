package i5.las2peer.services.projectService;

import java.net.HttpURLConnection;
import java.util.Set;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONObject;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import org.json.simple.parser.ParseException;

import java.io.Serializable;



import javax.ws.rs.Consumes;

//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.Connection;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.Consumes;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.GitHubException;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.JSONObject;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.ParseException;
import i5.las2peer.services.projectService.project.Project;
import i5.las2peer.services.projectService.ProjectContainer;
import i5.las2peer.services.projectService.exception.ProjectNotFoundException;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.ReqBazException;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.SQLException;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.String;
import i5.las2peer.services.projectService.project.User;
//import project_management_service.src.main.java.i5.las2peer.services.projectManagementService.auth.Agent;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceNotAuthorizedException;

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
	 * If not, then the new project gets stored into the database.
	 * @param inputProject JSON representation of the project to store (containing name and access token of user needed to create Requirements Bazaar category).
	 * @return Response containing the status code (and a message or the created project).
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Creates a new project in the database if no project with the same name is already existing.")
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
            try {
            	Agent agent = Context.getCurrent().getMainAgent();
            	Envelope env = null;
    			Envelope env2 = null;
    			String id = "";
    			// didnt do much thinking in the following part but rather tried copying the code from contactservice just to make it work, will need to put some 
    			// more thought into it once it works :P
    			String identifier = projects_prefix + "_" + agent.toString();
    			String identifier2 = projects_prefix;
    			ProjectContainer cc = null;
				Project project = new Project(agent, inputProject);
				try {
					try {
						Context.get().requestEnvelope(identifier);
						return Response.status(Status.BAD_REQUEST).entity("Project already exists").build();
					} catch (EnvelopeNotFoundException e) {
						System.out.println("Enveleope did not exist creating...");
						cc = new ProjectContainer();
						// try to create group
						//groupAgent = Context.get().createGroupAgent(members, name);
						cc.addProject(project);
						System.out.println("Creating envelope");
						env = Context.get().createEnvelope(identifier, agent);
						System.out.println("Setting envelope content");
						env.setContent(cc);
						System.out.println("Storing emnvelope");
						Context.get().storeEnvelope(env);
						System.out.println("Storing complete");
					}
				} catch (Exception e) {
					// write error to logfile and console
				//	logger.log(Level.SEVERE, "Can't persist to network storage!", e);
				//	e.printStackTrace();
					return Response.status(Status.BAD_REQUEST).entity(e + "Error").build();
				}
				//pleasee ignore this for now :)
			} catch (ParseException | ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException e) {
			//	logger.printStackTrace(e);
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
			} 
		}
		return Response.status(Status.OK).entity("Added Project To l2p Storage").build();
	}
	
	
	/**
	 * Gets a user's projects
	 * Therefore, the user needs to be authorized.
	 * First, checks if a project with the given name already exists.
	 * If not, then the new project gets stored into the database.
	 * @param inputProject JSON representation of the project to store (containing name and access token of user needed to create Requirements Bazaar category).
	 * @return Response containing the status code (and a message or the created project).
	 */
	@GET
	@Path("/")
	@ApiOperation(value = "Creates a new project in the database if no project with the same name is already existing.")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, projects fetched."),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "User not authorized."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error.")
	})
	public Response getProjects() {
	    Agent agent = Context.getCurrent().getMainAgent();
		String identifier = projects_prefix + agent.toString();
		JSONObject result = new JSONObject();
		try {
			try {
				Envelope stored = Context.get().requestEnvelope(identifier, agent);
				ProjectContainer cc = (ProjectContainer) stored.getContent();
				/*	Set<String> groupNames = cc.getGroups().keySet();
				String groupId = "";
				for (String s : groupNames) {
					try {
						groupId = cc.getGroupId(s);
						Context.get().requestAgent(groupId);
						result.put(groupId, s);
					} catch (Exception e) {
						// Skip agents who are not known or groups wihtout access.
					}
				}*/
				result.put("projects", cc.getUserProjects());
				System.out.println(cc.getUserProjects());
				return Response.status(Status.OK).entity(result).build();
			} catch (EnvelopeNotFoundException e) {
				return Response.status(Status.OK).entity("No projects found").build();
			}
		} catch (Exception e) {
			// write error to logfile and console
			// Couldnt build due to logging error so just left it out for now...
			//logger.log(Level.SEVERE, "Can't persist to network storage!", e);
		}
		return Response.status(Status.BAD_REQUEST).entity("Unknown error occured.").build();
	}
}
