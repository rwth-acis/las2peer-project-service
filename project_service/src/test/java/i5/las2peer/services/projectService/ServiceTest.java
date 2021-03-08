package i5.las2peer.services.projectService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;

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
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

/**
 * Test Class for las2peer-project-service.
 */
public class ServiceTest {


		private static LocalNode node;
		private static WebConnector connector;
		private static ByteArrayOutputStream logStream;

		private static UserAgentImpl testAgent;
		private static final String testPass = "adamspass";

		private static final String mainPath = "projects/";

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

			// add agent to node
			testAgent = MockAgentFactory.getAdam();
			testAgent.unlock(testPass); // agents must be unlocked in order to be stored
			node.storeAgent(testAgent);

			// start project service
			// during testing, the specified service version does not matter
			node.startService(new ServiceNameVersion(ProjectService.class.getName(), "1.0.0"), "a pass");

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
				client.setLogin(testAgent.getIdentifier(), testPass);
				result = client.sendRequest("GET", mainPath, "");
				// we should get 200 and an empty list
				Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getHttpCode());
				Assert.assertEquals("{\"projects\":[]}", result.getResponse().trim());
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.toString());
			}
		}
	}
