package i5.las2peer.services.projectService;

import java.net.HttpURLConnection;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.UserAgent;
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

	/**
	 * Main endpoint of the project service.
	 * 
	 * @return Returns an HTTP response containing a message that the service is running.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Method for checking that the service is running.")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Project service is running.") })
	public Response getMain() {
		return Response.ok().entity("Project service is running.").build();
	}
}
