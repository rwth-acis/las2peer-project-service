# Project-List Frontend
[![npmjs](https://img.shields.io/npm/v/@rwth-acis/las2peer-project-service-frontend?color=success)](https://www.npmjs.com/package/@rwth-acis/las2peer-project-service-frontend)

The project-list is a LitElement for listing projects given by the las2peer-project-service and for creating new projects.

Basic Usage
-------------------

To use the project-list LitElement in your project, just install it from [npmjs](https://www.npmjs.com/package/@rwth-acis/las2peer-project-service-frontend):

```
npm i @rwth-acis/las2peer-project-service-frontend
```

Then you can use the project-list element as follows:

```html
<project-list system="{SYSTEM_NAME}"
              projectServiceURL="{PROJECT_SERVICE_URL}"
              contactServiceURL="{CONTACT_SERVICE_URL}"
              yjsAddress="http://127.0.0.1:1234"
              yjsResourcePath="./socket.io"></project-list>
```

Here, SYSTEM_NAME needs to match with a system that is configured in the project-service.
For the configuration of systems, see the [main README](https://github.com/rwth-acis/las2peer-project-service).
Set PROJECT_SERVICE_URL and CONTACT_SERVICE_URL to the address of the webconnector, where the respective service can be found.

The project-list element uses [Yjs](https://github.com/yjs/yjs) to share the metadata of the currently selected/opened project with other project members that have selected the same project.
This ensures, that every project member always gets the latest changes to the metadata.
To configure the used y-websockets-server instance, use the attributes `yjsAddress` and `yjsResourcePath`.
Please note: To keep the metadata of projects secured, the project-list uses an extended version of y-websockets-server and y-websockets-client.
This extension allows to use Yjs rooms that only project members can access.
The extensions can be found in the rwth-acis forks [rwth-acis/y-websockets-server#project-service](https://github.com/rwth-acis/y-websockets-server/tree/project-service) and [rwth-acis/y-websockets-client#v8](https://github.com/rwth-acis/y-websockets-client/tree/v8).


You may also use the `disableAllProjects` attribute to disable the tab where all available projects are listed.

Events
-------------

**Events fired by the project-list element:**

`projects-loaded`:

The project-list element fires this event whenever the projects are reloaded from the project-service.
The event details contain a list of all projects that the current user can access.
Depending on how the visibility of all projects is configured in the project-service, also projects where the current user is no member of will be part of the list.

`project-selected`:

Whenever the user clicks on one of the projects in the list, this event will be fired.
It contains detailed information on the selected project and its metadata.

`metadata-changed`:

If the metadata of the currently selected/opened project got changed, this event will be fired.
It contains the updated project metadata.
If you display the metadata in the UI and want to keep it up-to-date, then you can use this event.

**Events that the project-list element listens for:**

`metadata-change-request`:

The project-list element listens to the event "metadata-change-request".
It can be used to update the metadata of the currently selected project.
If you send the event and set the event details to the updated metadata, the project-list element will send it to the project-service and after that the "metadata-changed" event will be fired.

`metadata-reload-request`:

If you updated the metadata without using the "metadata-change-request" event, as an example by using the RMI interface of the project-service, then you should use this event to inform the project-list element about it.
When receiving the event, the project-list will fetch the metadata from the project-service again and will also send a "metadata-changed" event.

`projects-reload-request`:

This event can be used to reload the list of projects.
It might be used after the user has logged in.

Extension: SyncMeta Online User List
-------------------------------------------

If your projects are using [SyncMeta](https://github.com/rwth-acis/syncmeta), you can use the online user list extension.
It allows to display the users that are currently online in a project / SyncMeta room.
To use the online user list, the project-list element needs to know which project is using which Yjs room(s) for modelling.
You can use the method "setOnlineUserListYjsRooms", that the project-list element provides, for this.
As a parameter you use a map that maps project names to a list of Yjs room names (which are used for modelling inside the project).

Development
-----------------------------
For testing the element during development, run `npm i` and  `npm run serve`.
There is a demo available that can be used during development.
