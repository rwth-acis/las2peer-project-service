package i5.las2peer.services.projectService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.security.GroupAgentImpl;
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

		private static final String mainPath = "projects/";
		
		private String identifierGroupA;
		private static final String nameGroupA = "groupA";
		
		

		/**
		 * Called before a test starts.
		 * 
		 * Sets up the node, initializes connector and adds user agent that can be used throughout the test.
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
			
			// add group agent to node
			// use group A where adam is a member, but eve not
			GroupAgentImpl groupA = MockAgentFactory.getGroupA();
			this.identifierGroupA = groupA.getIdentifier();
			groupA.unlock(testAgentAdam);
			node.storeAgent(groupA);
			
			// start project service
			// during testing, the specified service version does not matter
			node.startService(new ServiceNameVersion(ProjectService.class.getName(), "1.0.0"), "a pass");
			
			// also start RMI test service
			node.startService(new ServiceNameVersion(RMITestService.class.getName(), "1.0.0"), "a pass");

			// start connector
			connector = new WebConnector(true, 0, false, 0); // port 0 means use system defined port
			logStream = new ByteArrayOutputStream();
			connector.setLogStream(new PrintStream(logStream));
			connector.start(node);
		}

		/**
		 * Called after the test has finished. Shuts down the server and prints out the connector log file for reference.
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
				result = client.sendRequest("POST", mainPath, this.getProjectJSON("Project1_testGetProjects", this.nameGroupA, this.identifierGroupA));
				Assert.assertEquals(HttpURLConnection.HTTP_CREATED, result.getHttpCode());
				// get projects again
				result = client.sendRequest("GET", mainPath, "");
				Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
				JSONObject resultJSON = (JSONObject) JSONValue.parse(result.getResponse().trim());
				JSONArray projectsJSON = (JSONArray) resultJSON.get("projects");
				Assert.assertEquals(1, projectsJSON.size());
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
				result = client.sendRequest("POST", mainPath, this.getProjectJSON("Project1_testPostProject", "groupName", "doesNotExist"));
			    Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			    
			    // test with an existing group and user but the user is no group member
			    // in this case we use groupA and eve
			    client.setLogin(testAgentEve.getIdentifier(), testPassEve);
			    result = client.sendRequest("POST", mainPath,  this.getProjectJSON("Project1_testPostProject", "groupName", this.identifierGroupA));
			    Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, result.getHttpCode());
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.toString());
			}
		}
		
		@Test
		public void testRMIMethodHasAccessToProject() {
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(connector.getHttpEndpoint());
			
			client.setLogin(testAgentAdam.getIdentifier(), testPassAdam);
			String projectName = "project1_testRMIMethodHasAccessToProject";
			ClientResponse result = client.sendRequest("GET", "rmitestservice/checkProjectAccess/" + projectName, "");
			// there should not exist a project with the given name yet, so user cannot have access to it
			Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
			Assert.assertEquals("false", result.getResponse().trim());
			
			// create a project
			result = client.sendRequest("POST", mainPath, this.getProjectJSON(projectName, this.nameGroupA, this.identifierGroupA));
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
		}
		
		/**
		 * Helper method to get a JSON string representation of a project.
		 * @param projectName Name of the project.
		 * @param linkedGroupName Name of the group which gets linked to the project.
		 * @param linkedGroupId Id of the group which gets linked to the project.
		 * @return JSON representation of project as string.
		 */
		private static final String getProjectJSON(String projectName, String linkedGroupName, String linkedGroupId) {
			return "{\"name\": \"" + projectName + "\", \"linkedGroup\": { \"name\": \"" + 
				    linkedGroupName + "\", \"id\": \"" + linkedGroupId + "\"}, \"users\": []}";
		}
	}
