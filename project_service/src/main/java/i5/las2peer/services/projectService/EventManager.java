package i5.las2peer.services.projectService;

import java.io.Serializable;

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
 * Helper class used to send messages to the configured event listener service on specific events.
 * @author Philipp
 *
 */
public class EventManager {
	
	private static final String EVENT_METHOD_PROJECT_CREATED = "_onProjectCreated";
	private static final String EVENT_METHOD_PROJECT_DELETED = "_onProjectDeleted";
	
	/**
	 * Name of the service that should be called on specific events.
	 * Might be null if not set.
	 */
	private String eventListenerService;
	
	/**
	 * Whether the event listener is enabled.
	 * This depends on whether eventListenerService is null.
	 */
	private boolean eventListenerEnabled = false;
	
	public EventManager(String eventListenerService) {
		this.eventListenerService = eventListenerService;
		
		if(this.eventListenerService != null && !this.eventListenerService.isEmpty()) {
			this.eventListenerEnabled = true;
		}
	}
	
	/**
	 * Sends the project-created event for the given project to the event listener service.
	 * @param context Context used for invoking the event listener service.
	 * @param projectJSON Project that got created as a JSONObject.
	 * @return If event listener is disabled, then always true. Otherwise only true, if event was sent successfully.
	 */
	public boolean sendProjectCreatedEvent(Context context, JSONObject projectJSON) {
		return invokeEventListenerService(context, EVENT_METHOD_PROJECT_CREATED, projectJSON);
	}
	
	/**
	 * Sends the project-deleted event for the given project to the event listener service.
	 * @param context Context used for invoking the event listener service.
	 * @param projectJSON Project that got deleted as a JSONObject.
	 * @return If event listener is disabled, then always true. Otherwise only true, if event was sent successfully.
	 */
	public boolean sendProjectDeletedEvent(Context context, JSONObject projectJSON) {
		return invokeEventListenerService(context, EVENT_METHOD_PROJECT_DELETED, projectJSON);
	}
	
	/**
	 * Helper method which uses the given context to invoke the given method of the event listener service (configured
	 * in properties file of service) using the given data.
	 * @param context Context used for invoking the event listener service.
	 * @param method Method that should be called in the event listener service.
	 * @param data Data that should be used as parameters in the method call.
	 * @return If event listener is disabled, then always true. Otherwise only true, if method was called successfully.
	 */
	private boolean invokeEventListenerService(Context context, String method, Serializable... data) {
		if(!this.eventListenerEnabled) return true;
		try {
			context.invoke(this.eventListenerService, method, data);
			return true;
		} catch (ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException
				| ServiceMethodNotFoundException | ServiceInvocationFailedException | ServiceAccessDeniedException
				| ServiceNotAuthorizedException e) {
			return false;
		}
	}
	

}
