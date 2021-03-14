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
