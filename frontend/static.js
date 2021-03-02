
export default class Static {
}
// these ids are used for yjs room names
Static.FrontendSpaceId = 'frontend-modeling';
Static.MicroserviceSpaceId = 'microservice-modeling';
Static.ApplicationSpaceId = 'application-modeling';

// store the URL to the project management service, model persistence service and webhost (for widgets)
// this gets updated when using docker env variables
// and should not end with a "/"
Static.ProjectManagementServiceURL = 'http://localhost:8081/project-management';
Static.WebhostURL = 'http://localhost:8070';
Static.ModelPersistenceServiceURL = 'http://localhost:8081/CAE';
Static.CodeGenServiceURL = 'http://localhost:8081/CodeGen';
// Yjs configuration
Static.YjsAddress = "http://localhost:1234";
Static.YjsResourcePath = "/socket.io";

Static.ContactServiceUrl = 'http://localhost:8080/contactservice';

// URL where the deployed application can be seen
Static.DeploymentURL = "http://localhost:8087";
// name of the GitHub organization where the source code is hosted
Static.GitHubOrg = "CAETESTRWTH";
Static.GitHubOAuthClientId = "e36f1d4edfc6ee7ff9c7";

// the following links are not edited through the docker container
Static.las2peerURL = "https://las2peer.org";
Static.ExternalDependenciesWiki = "https://github.com/rwth-acis/CAE/wiki/External-Dependencies";

Static.ReqBazBackend = "https://requirements-bazaar.org/bazaar";
Static.ReqBazFrontend = "https://requirements-bazaar.org";