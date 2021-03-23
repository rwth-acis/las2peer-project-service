# las2peer-project-service

[![Java CI with Gradle](https://github.com/rwth-acis/las2peer-project-service/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/rwth-acis/las2peer-project-service/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/rwth-acis/las2peer-project-service/branch/main/graph/badge.svg)](https://codecov.io/gh/rwth-acis/las2peer-project-service)

A [las2peer](https://github.com/rwth-acis/las2peer) service for managing projects and their members. We provide a project-list [LitElement](/frontend) which can be used as a frontend for this service.

Build
--------
Execute the following command on your shell:
```shell
gradle clean build 
```

Service Properties
--------
The project service uses a group agent to store envelopes containing the lists of available projects.
This allows, to access these envelopes using different service agents.
As an example, when updating your service, you do not loose access to the projects, but a new service agent can be added to the group agent instead (which will grant access to the envelopes).

Depending on whether you are starting the service for the first time (and do not have a service group agent yet) or whether you are starting an updated version of the service, different properties (or environment variables) have to be set.

When you are starting the service for the first time, you need to set the docker environment variable "NEW_GROUP_AGENT". Then on the first start of the service, a new group agent is generated for the service. In case you want to update your service at a later point and still want to have access to your projects, please note down the agent id of your service, the password for the service agent and also the id of the service group agent.
You can find the id of the service group agent in the etc/startup/group.xml file.

When you want to restart your service after an update, you need to remove the "NEW_GROUP_AGENT" environment variable.
Instead, you now need to set the variables "SERVICE_GROUP_ID", "OLD_SERVICE_AGENT_ID" and "OLD_SERVICE_AGENT_PW" accordingly.
When using the service, your new service agent will be automatically added to the existing group by using the old service agent and it's password.

Besides that, you need to configure the systems that the project service should handle projects for.
Therefore, you may use the "SYSTEMS" environment variable.
It should be set to a JSON object which could look as follows, if you are using two systems:
```
{
  "SBF": {
    "visibilityOfProjects": "own"
  },
  "CAE": {
    "visibilityOfProjects": "all",
    "eventListenerService": "i5.las2peer.services.modelPersistenceService.ModelPersistenceService@0.1"
  }
}
```
In this example, we are supporting two systems, namely the "SBF" and "CAE".
In the CAE it is possible to view all projects, even the ones where a user is no member of.
The SBF only allows to read those projects, where the user is a member of.
Besides that, the ModelPersistenceService is called in the CAE, if specific events (such as project-creation) occur. For more information, see the section on the event listener service.

Event Listener Service
--------
The project service provides the possibility to set another las2peer service as an event listener service.
On specific events (such as project-creation), the project service calls the configured event listener service via RMI.
In order to work properly, the event listener service needs to implement the following public methods for the specific events:

| Event             | Method                                    | Description |
|-------------------|-------------------------------------------|----|
| Project creation  | _onProjectCreated(JSONObject projectJSON) | Event gets fired whenever a new project gets created. The JSONObject will then be a JSON representation of the created project. |
| Project deletion  | _onProjectDeleted(JSONObject projectJSON) | Event gets fired whenever a project gets deleted. The JSONObject will then be a JSON representation of the deleted project. |

RMI Methods
--------
Besides the event listener service, other services in general have the possibility to communicate with the project service via RMI.
Therefore, the project service provides the following methods:

| Method                                         | Description |
|------------------------------------------------|-------------|
| boolean hasAccessToProject(String system, String projectName) | This method may be used to verify if a user is allowed to write-access a project. Returns true, if the calling agent has access to project. Returns false otherwise (or if project with given name does not exist). |


Start
--------

First of all make sure that you have a running instance of the [Contact Service](https://github.com/rwth-acis/las2peer-contact-service).

To start the Project Service, use one of the available start scripts:

Windows:

```shell
bin/start_network.bat
```

Unix/Mac:
```shell
bin/start_network.sh
```

After successful start, Project Service is available under

[http://localhost:8080/projects/](http://localhost:8080/projects/)
