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

| Property (Docker env variable)                | Possible values | Default          | Description |
|-----------------------------------------------|-----------------|------------------|-------------|
| visibilityOfProjects (VISIBILITY_OF_PROJECTS) | all, own        | own              | Whether users are able to read-access all projects or only the ones they are a member of.|
| eventListenerService (EVENT_LISTENER_SERVICE) | Service names   | -                | May be used to set a service as an event listener. This service will then be called on specified events, such as project creation. |
| serviceGroupId | | - | Needs to be set to the identifier of a group agent where the service agent is a member of. This group will be used to store envelopes. |

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
| boolean hasAccessToProject(String projectName) | This method may be used to verify if a user is allowed to write-access a project. Returns true, if the calling agent has access to project. Returns false otherwise (or if project with given name does not exist). |


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
