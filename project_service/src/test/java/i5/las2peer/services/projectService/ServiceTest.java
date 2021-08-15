package i5.las2peer.services.projectService;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.Agent;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

/**
 * Test Class for las2peer-project-service.
 */
public class ServiceTest {

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgentImpl testAgentAdam;
	private static final String testPassAdam = "adamspass";
	private static UserAgentImpl testAgentEve;
	private static final String testPassEve = "evespass";

	// important: the system names need to match the systems that are configured
	// in the .properties file used for the ServiceTest
	public static final String system1 = "test";
	public static final String system2 = "othersystem";
	private static final String mainPath = "projects/" + system1 + "/";
	private static final String mainPath2 = "projects/" + system2 + "/";

	private String identifierGroupA;
	private static final String nameGroupA = "groupA";
	private String identifierGroup1;
	private static final String nameGroup1 = "group1";
	
	// the used .properties file can be found in project_service/properties folder
	private static final String projectServicePropertiesPath = "properties/i5.las2peer.services.projectService.ProjectService.properties";
	
	private final ServiceNameVersion rmiTestServiceName = new ServiceNameVersion(RMITestService.class.getName(), "1.0.0");
	
	/**
	 * Called before a test starts.
	 * 
	 * Sets up the node, initializes connector and adds user agent that can be used
	 * throughout the test.
	 * 
	 * @throws Exception
	 */
	@Before
	public void startServer() throws Exception {
		// start node
		node = new LocalNodeManager().newNode();
		node.launch();

		// MockAgentFactory provides 3 user agents: abel, adam, eve
		// MockAgentFactory provides 4 groups:
		// - Group1, Group2, Group3: all user agents are members
		// - GroupA: eve is no member, abel and adam are members

		// add user agents to node (currently only adam and eve are used for testing)
		testAgentAdam = MockAgentFactory.getAdam();
		testAgentAdam.unlock(testPassAdam); // agents must be unlocked in order to be stored
		node.storeAgent(testAgentAdam);

		testAgentEve = MockAgentFactory.getEve();
		testAgentEve.unlock(testPassEve);
		node.storeAgent(testAgentEve);
		
		// we only use abel as an admin for the service group which is used by the project service for storing envelopes
		UserAgentImpl serviceGroupAdmin = MockAgentFactory.getAbel();
		serviceGroupAdmin.unlock("abelspass");
		node.storeAgent(serviceGroupAdmin);

		// add group agent to node
		// use group A where adam is a member, but eve not
		GroupAgentImpl groupA = MockAgentFactory.getGroupA();
		this.identifierGroupA = groupA.getIdentifier();
		groupA.unlock(testAgentAdam);
		node.storeAgent(groupA);

		// use group 1 where adam and eve are member
		GroupAgentImpl group1 = MockAgentFactory.getGroup1();
		this.identifierGroup1 = group1.getIdentifier();
		group1.unlock(testAgentAdam);
		node.storeAgent(group1);
	
		// create group agent and add abel to this group agent
		// we will later add the project service to this group and the project service will use this
		// group agent for storing of envelopes
		Agent[] members = new Agent[1];
        members[0] = serviceGroupAdmin;
		GroupAgentImpl serviceGroup = GroupAgentImpl.createGroupAgent(members);
        serviceGroup.unlock(serviceGroupAdmin);
        node.storeAgent(serviceGroup);
        
        // now that we know the identifier of the group, we can set it in the properties file of the project service
        // as the serviceGroupId
        Properties props = new Properties();
        props.load(new FileInputStream(projectServicePropertiesPath));
        props.setProperty("serviceGroupId", serviceGroup.getIdentifier());
        props.store(new FileOutputStream(projectServicePropertiesPath), null);
		
        // start project service (which will automatically use the properties file)
     	// during testing, the specified service version does not matter
		ServiceAgentImpl projectService = node.startService(new ServiceNameVersion(ProjectService.class.getName(), "1.0.0"), "a pass");
        // add the service agent to the service group
		serviceGroup.addMember(projectService);
        node.storeAgent(serviceGroup);

        
		// also start RMI test service
		node.startService(rmiTestServiceName, "a pass");

		// start connector
		connector = new WebConnector(true, 0, false, 0); // port 0 means use system defined port
		logStream = new ByteArrayOutputStream();
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);
	}

	/**
	 * Called after the test has finished. Shuts down the server and prints out the
	 * connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@After
	public void shutDownServer() throws Exception {
		if (connector != null) {
			connector.stop();
			connector = null;
		}
		if (node != null) {
			node.shutDown();
			node = null;
		}
		if (logStream != null) {
			System.out.println("Connector-Log:");
			System.out.println("--------------");
			System.out.println(logStream.toString());
			logStream = null;
		}
	}

	/**
	 * Tests the method for fetching projects.
	 */
	@Test
	public void testGetProjects() {
		try {
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());

			// first try without agent (this should not be possible)
			ClientResponse result = client.sendRequest("GET", mainPath, "");
			Assert.assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, result.getHttpCode());

			// now use an agent
			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			result = client.sendRequest("GET", mainPath, "");
			// we should get 200 and an empty list
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			Assert.assertEquals("{\"projects\":[]}", result.getResponse().trim());

			// now add a project using adam and group A
			result = client.sendRequest("POST", mainPath,
					this.getProjectJSON("Project1_testGetProjects", this.nameGroupA, this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
			// get projects again
			result = client.sendRequest("GET", mainPath, "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			JSONObject resultJSON = (JSONObject) JSONValue.parse(result.getResponse().trim());
			JSONArray projectsJSON = (JSONArray) resultJSON.get("projects");
			int projectCountSystem1 = projectsJSON.size();
			Assert.assertEquals(1, projectCountSystem1);
			
			// now we are testing adding a project to the second system
			// we use the same name, as this should not be a problem in a different system
			result = client.sendRequest("POST", mainPath2,
					this.getProjectJSON("Project1_testGetProjects", this.nameGroupA, this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
			// now we also test it with a different name
			result = client.sendRequest("POST", mainPath2,
					this.getProjectJSON("Project1_testGetProjects2", this.nameGroupA, this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
			
			// now get projects of system1 again, to verify that no project was added there
			result = client.sendRequest("GET", mainPath, "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			resultJSON = (JSONObject) JSONValue.parse(result.getResponse().trim());
			projectsJSON = (JSONArray) resultJSON.get("projects");
			Assert.assertEquals(projectCountSystem1, projectsJSON.size());
			
			// now check if projects in system2 exists
			// here we also test the method getProjectByName
			result = client.sendRequest("GET", "projects/" + system2 + "/Project1_testGetProjects", "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			result = client.sendRequest("GET", "projects/" + system2 + "/Project1_testGetProjects2", "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			
			// now test getProjectByName with a non-existing project
			result = client.sendRequest("GET", "projects/" + system2 + "/does_not_exist", "");
			Assert.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, result.getHttpCode());
			
			// now test getProjectByName method with an agent who is no member of groupA
			client.setLogin(testAgentEve.getIdentifier(), testPassEve);
			result = client.sendRequest("GET", "projects/" + system2 + "/Project1_testGetProjects", "");
			Assert.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, result.getHttpCode());
			
			// test with non-existing system (both GET methods, the one for all projects and the one for a single project)
			result = client.sendRequest("GET", "projects/doesnotexist", "");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			result = client.sendRequest("GET", "projects/doesnotexist/Project", "");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostProject() {
		try {
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());

			// first try without agent (this should not be possible)
			ClientResponse result = client.sendRequest("POST", mainPath, "");
			Assert.assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, result.getHttpCode());

			// now use an agent
			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			result = client.sendRequest("POST", mainPath, "");
			// bad request because of no body is sent
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());

			// test with a group that does not exist
			result = client.sendRequest("POST", mainPath,
					this.getProjectJSON("Project1_testPostProject", "groupName", "doesNotExist"));
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());

			// test with an existing group and user but the user is no group member
			// in this case we use groupA and eve
			client.setLogin(testAgentEve.getIdentifier(), testPassEve);
			result = client.sendRequest("POST", mainPath,
					this.getProjectJSON("Project1_testPostProject", "groupName", this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());

			// now test with an existing group and a group member
			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			result = client.sendRequest("POST", mainPath,
					this.getProjectJSON("Project1_testPostProject", "groupName", this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
			
			// test again with same project name (should not be allowed, because project with that name already exists)
			result = client.sendRequest("POST", mainPath,
					this.getProjectJSON("Project1_testPostProject", "groupName", this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CONFLICT, result.getHttpCode());
			
			// test again with incorrect project json (only containing project name, other attributes missing)
			result = client.sendRequest("POST", mainPath, "{\"projectName\": \"should-fail\"}");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());

			// check if RMITestService event _onProjectCreated got called
			result = client.sendRequest("GET", "rmitestservice/onProjectCreated", "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());

			// test with metadata
			JSONObject metadata = new JSONObject();
			metadata.put("attr1", "value1");
			metadata.put("attr2", "value2");
			result = client.sendRequest("POST", mainPath, this.getProjectJSON("Project2_testPostProject", "groupName",
					this.identifierGroupA, metadata.toJSONString()));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
			
			// test with non-existing system
			result = client.sendRequest("POST", "projects/doesnotexist", "");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			
			// now stop the RMITestService once (and verify that it is stopped)
			node.stopService(rmiTestServiceName);
			Assert.assertEquals(false, node.hasService(rmiTestServiceName));
			
			// create a project (while event listener service or in this case RMITestService is not running anymore)
			// this should not be possible (because the event cannot be sent)
			result = client.sendRequest("POST", mainPath, 
					this.getProjectJSON("rmi-test-service-unavailable", "groupName", this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, result.getHttpCode());
			// if event listener is not available, the project should not be created => verify this
			result = client.sendRequest("GET", "projects/" + system1 + "/rmi-test-service-unavailable", "");
			Assert.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, result.getHttpCode());
			
			// start RMITestService again (maybe it is needed by other tests)
			node.startService(rmiTestServiceName, "a pass");
			Assert.assertEquals(true, node.hasService(rmiTestServiceName));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testRMIMethodsHasAccessToProject() {
		try {
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());

			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			String projectName = "project1_testRMIMethodHasAccessToProject";
			ClientResponse result = client.sendRequest("GET", "rmitestservice/checkProjectAccess/" + projectName, "");
			// there should not exist a project with the given name yet, so user cannot have
			// access to it
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			Assert.assertEquals("false", result.getResponse().trim());

			// create a project
			result = client.sendRequest("POST", mainPath,
					this.getProjectJSON(projectName, this.nameGroupA, this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());

			// now check if the agent has access to this existing project
			// test with adam first, adam is a member of group A
			result = client.sendRequest("GET", "rmitestservice/checkProjectAccess/" + projectName, "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			Assert.assertEquals("true", result.getResponse().trim());
			// now test with eve, eve is no member of group A
			client.setLogin(testAgentEve.getIdentifier(), testPassEve);
			result = client.sendRequest("GET", "rmitestservice/checkProjectAccess/" + projectName, "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			Assert.assertEquals("false", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testChangeGroup() {
		try {
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());

			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			// create project using adam and group A
			String projectName = "Project1_testGetChangeGroup";
			ClientResponse result = client.sendRequest("POST", mainPath,
					this.getProjectJSON(projectName, this.nameGroupA, this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());

			JSONObject o = new JSONObject();
			o.put("projectName", projectName);
			o.put("newGroupId", this.identifierGroup1);
			o.put("newGroupName", this.nameGroup1);
			
			// try chaning group but use incorrect system name
			result = client.sendRequest("POST", "projects/systemdoesnotexist/changeGroup", o.toJSONString());
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			
			// try changing group without being a logged in user
			client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());
			result = client.sendRequest("POST", mainPath + "changeGroup", o.toJSONString());
			Assert.assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, result.getHttpCode());

			// try changing group using eve (who is no project member)
			client.setLogin(testAgentEve.getIdentifier(), testPassEve);
			result = client.sendRequest("POST", mainPath + "changeGroup", o.toJSONString());
			Assert.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, result.getHttpCode());

			// now change group using adam (who is a project member)
			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			result = client.sendRequest("POST", mainPath + "changeGroup", o.toJSONString());
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			
			// now test with no valid json
			result = client.sendRequest("POST", mainPath + "changeGroup", "{fail");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
	
	@Test
	public void testChangeMetadata() {
		try {
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());
			
			// try with invalid system name
			ClientResponse result = client.sendRequest("POST", "projects/systemdoesnotexist/changeMetadata", "");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			
			// try to change metadata without being logged in
			result = client.sendRequest("POST", mainPath + "changeMetadata", "");
			Assert.assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, result.getHttpCode());

			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			
			// try to change metadata but let body empty
			result = client.sendRequest("POST", mainPath + "changeMetadata", "");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			
			// create a new project for testing
			String projectName = "testChangeMetadata_Project1";
			JSONObject metadata = new JSONObject();
			metadata.put("attr1", "value1");
			result = client.sendRequest("POST", mainPath, this.getProjectJSON(projectName, "groupName",
					this.identifierGroupA, metadata.toJSONString()));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
			
			// change metadata
			JSONObject body = new JSONObject();
			body.put("projectName", projectName);
			body.put("oldMetadata", metadata);
			JSONObject metadataUpdated = new JSONObject();
			metadataUpdated.put("attr1", "value2");
			body.put("newMetadata", metadataUpdated);
			result = client.sendRequest("POST", mainPath + "changeMetadata", body.toJSONString());
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			
			// get the project and check if metadata got really updated
			result = client.sendRequest("GET", mainPath, "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			JSONObject resultJSON = (JSONObject) JSONValue.parse(result.getResponse().trim());
			JSONArray projectsJSON = (JSONArray) resultJSON.get("projects");
			boolean containsProject = false;
			for(Object p : projectsJSON) {
				JSONObject projectJSON = (JSONObject) p;
				if(((String)projectJSON.get("name")).equals(projectName)) {
					String metadataString = ((JSONObject) projectJSON.get("metadata")).toJSONString();
					Assert.assertEquals(metadataUpdated.toJSONString(), metadataString);
					containsProject = true;
					break;
				}
			}
			Assert.assertEquals(true, containsProject);
			
			// try to change metadata with a non-member of the project
			client.setLogin(testAgentEve.getIdentifier(), testPassEve);
			result = client.sendRequest("POST", mainPath + "changeMetadata", body.toJSONString());
			Assert.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, result.getHttpCode());
			
			// test with incorrect oldMetadata value
			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			body.put("oldMetadata", "");
			result = client.sendRequest("POST", mainPath + "changeMetadata", body.toJSONString());
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
	
	@Test
	public void testDeleteProject() {
		try {
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());

			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			
			// try with non-existing system name
			ClientResponse result = client.sendRequest("DELETE", "projects/doesnotexist/not-existing-project", "");
			Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			
			// try to delete a non-existing project
			result = client.sendRequest("DELETE", mainPath + "not-existing-project", "");
			Assert.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, result.getHttpCode());
			
			// create project using adam and group A
			String projectName = "Project1_testDeleteProject";
			result = client.sendRequest("POST", mainPath,
					this.getProjectJSON(projectName, this.nameGroupA, this.identifierGroupA));
			Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
			
			// count number of projects that adam has access to
			result = client.sendRequest("GET", mainPath, "");
			JSONArray jsonProjects = (JSONArray) ((JSONObject) JSONValue.parse(result.getResponse().trim())).get("projects");
			int numProjects = jsonProjects.size();
			
			// now try to delete this project using a non-member (e.g. eve is no member of group A)
			// in this case, eve should not be allowed to delete the project
			client.setLogin(testAgentEve.getIdentifier(), testPassEve);
			result = client.sendRequest("DELETE", mainPath + projectName, "");
			Assert.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, result.getHttpCode());
			
			// now try to delete it again but now try with adam
			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			result = client.sendRequest("DELETE", mainPath + projectName, "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			
			// check if RMITestService event _onProjectCreated got called
			result = client.sendRequest("GET", "rmitestservice/onProjectDeleted", "");
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			
			// count number of projects that adam has access to again
			// should be one less than before
			result = client.sendRequest("GET", mainPath, "");
			JSONArray jsonProjects2 = (JSONArray) ((JSONObject) JSONValue.parse(result.getResponse().trim())).get("projects");
			int numProjects2 = jsonProjects2.size();
			Assert.assertEquals(numProjects-1, numProjects2);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	/**
	 * Helper method to get a JSON string representation of a project.
	 * 
	 * @param projectName     Name of the project.
	 * @param linkedGroupName Name of the group which gets linked to the project.
	 * @param linkedGroupId   Id of the group which gets linked to the project.
	 * @param metadata        JSON String representation of project metadata.
	 * @return JSON representation of project as string.
	 */
	private static final String getProjectJSON(String projectName, String linkedGroupName, String linkedGroupId,
			String metadata) {
		return "{\"name\": \"" + projectName + "\", \"linkedGroup\": { \"name\": \"" + linkedGroupName
				+ "\", \"id\": \"" + linkedGroupId + "\"}, \"users\": [], \"metadata\": " + metadata + "}";
	}

	/**
	 * Helper method to get a JSON string representation of a project.
	 * 
	 * @param projectName     Name of the project.
	 * @param linkedGroupName Name of the group which gets linked to the project.
	 * @param linkedGroupId   Id of the group which gets linked to the project.
	 * @return JSON representation of project as string. Does not use any project
	 *         metadata.
	 */
	private static final String getProjectJSON(String projectName, String linkedGroupName, String linkedGroupId) {
		return "{\"name\": \"" + projectName + "\", \"linkedGroup\": { \"name\": \"" + linkedGroupName
				+ "\", \"id\": \"" + linkedGroupId + "\"}, \"users\": []}";
	}
}
