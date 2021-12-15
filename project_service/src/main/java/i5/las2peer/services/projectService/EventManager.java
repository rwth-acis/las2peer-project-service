package i5.las2peer.services.projectService;

import java.io.Serializable;
import java.util.HashMap;

import org.json.simple.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAuthorizedException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;

/**
 * Helper class used to send messages to the configured event listener services on specific events.
 * @author Philipp
 *
 */
public class EventManager {
	
	private static final String EVENT_METHOD_PROJECT_CREATED = "_onProjectCreated";
	private static final String EVENT_METHOD_PROJECT_DELETED = "_onProjectDeleted";
	
	/**
	 * Map containing the name of the event listener service for every system.
	 * Name of the service is the one that should be called on specific events.
	 * Might be null for some systems if not set.
	 */
	private HashMap<String, String> eventListenerServiceMap;
	
	public EventManager(HashMap<String, String> eventListenerServiceMap) {
		this.eventListenerServiceMap = eventListenerServiceMap;
	}
	
	/**
	 * Sends the project-created event for the given project to the event listener service.
	 * @param context Context used for invoking the event listener service.
	 * @param system System that the project belongs to.
	 * @param projectJSON Project that got created as a JSONObject.
	 * @return If event listener is disabled, then always true. Otherwise only true, if event was sent successfully.
	 */
	public boolean sendProjectCreatedEvent(Context context, String system, JSONObject projectJSON) {
		return invokeEventListenerService(context, system, EVENT_METHOD_PROJECT_CREATED, projectJSON);
	}
	
	/**
	 * Sends the project-deleted event for the given project to the event listener service.
	 * @param context Context used for invoking the event listener service.
	 * @param system System that the project belongs to.
	 * @param projectJSON Project that got deleted as a JSONObject.
	 * @return If event listener is disabled, then always true. Otherwise only true, if event was sent successfully.
	 */
	public boolean sendProjectDeletedEvent(Context context, String system, JSONObject projectJSON) {
		return invokeEventListenerService(context, system, EVENT_METHOD_PROJECT_DELETED, projectJSON);
	}
	
	/**
	 * Helper method which uses the given context to invoke the given method of the event listener service (configured
	 * in properties file of service) using the given data.
	 * @param context Context used for invoking the event listener service.
	 * @param system System is required to find correct event listener service.
	 * @param method Method that should be called in the event listener service.
	 * @param data Data that should be used as parameters in the method call.
	 * @return If event listener is disabled, then always true. Otherwise only true, if method was called successfully.
	 */
	private boolean invokeEventListenerService(Context context, String system, String method, Serializable... data) {
		String eventListenerService = this.eventListenerServiceMap.get(system);
		boolean enabled = eventListenerService != null;
		if(!enabled) return true;
		
		try {
			context.invoke(eventListenerService, method, data);
			return true;
		} catch (ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException
				| ServiceMethodNotFoundException | ServiceInvocationFailedException | ServiceAccessDeniedException
				| ServiceNotAuthorizedException e) {
			return false;
		}
	}
	

}
