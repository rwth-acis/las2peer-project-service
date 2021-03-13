package i5.las2peer.services.projectService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import i5.las2peer.api.Context;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAuthorizedException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

import java.net.HttpURLConnection;

/**
 * The RMI test service is a RESTService used to test the methods that the project service provides via RMI.
 * Therefore, the RMI test service provides a RESTful interface that can be used by an agent during the tests.
 * The different methods of this RESTful service will then invoke the methods that the project service provides for RMI.
 * @author Philipp
 *
 */
@ServicePath("/rmitestservice")
public class RMITestService extends RESTService {
	
	@Override
	protected void initResources() {
		getResourceConfig().register(this);
	}
	
	/**
	 * Method that is used to test the hasAccessToProject method provided by the project service for RMI.
	 * It uses the current agent to invoke the hasAccessToProject method and uses the given project name.
	 * @param projectName Name of the project, where access of the used agent should be checked for.
	 * @return Response with status code 200 and content containing the result of the invoked method, or 500 on error.
	 */
	@GET
	@Path("/checkProjectAccess/{projectName}")
	public Response checkProjectAccess(@PathParam("projectName") String projectName) {
		boolean access;
		String serviceMethod = "hasAccessToProject";
		try {
			access = (boolean) Context.getCurrent().invoke("i5.las2peer.services.projectService.ProjectService@1.0.0", serviceMethod, projectName);
		} catch (ServiceNotFoundException | ServiceNotAvailableException e) {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Service not found or not available.").build();
		} catch (ServiceMethodNotFoundException e) {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity("Service has no method named " + serviceMethod + ".").build();
		} catch (InternalServiceException | ServiceInvocationFailedException | ServiceAccessDeniedException | ServiceNotAuthorizedException e) {
			return Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).build();
		}
	    return Response.status(200).entity(access).build();
	}

}
