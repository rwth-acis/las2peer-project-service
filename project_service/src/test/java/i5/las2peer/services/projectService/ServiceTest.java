package i5.las2peer.services.projectService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
		 * Tests the main endpoint method.
		 */
		@Test
		public void testGetMain() {
			try {
				MiniClient client = new MiniClient();
				client.setConnectorEndpoint(connector.getHttpEndpoint());
				// no agent is required for this method
				
				ClientResponse result = client.sendRequest("GET", mainPath, "");
				Assert.assertEquals(200, result.getHttpCode());
				Assert.assertEquals("Project service is running.", result.getResponse().trim());
				System.out.println("Result of 'testGetMain': " + result.getResponse().trim());
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.toString());
			}
		}
	}
