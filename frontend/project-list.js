import { LitElement, html, css } from 'lit-element';
import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-input/paper-input.js';
import '@polymer/paper-dialog/paper-dialog.js';
import '@polymer/paper-spinner/paper-spinner-lite.js';
import '@polymer/paper-dropdown-menu/paper-dropdown-menu.js';
import '@polymer/paper-item/paper-item.js';
import '@polymer/paper-listbox/paper-listbox.js';
import '@polymer/paper-tabs';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/social-icons.js';
import OnlineUserListHelper from './util/online-user-list-helper'

import Auth from './util/auth';


/**
 * The project list element provides the functionality to list existing projects and to create new ones.
 * It provides several possibilities for configuration. The URL of the las2peer project service should be configured.
 * It is also possible to disable the "All projects" tab, which allows hiding projects that the current user is no
 * member of.
 */
export class ProjectList extends LitElement {
  static get styles() {
    return css`
    .main {
      width: 100%;
      margin-top: 1em;
    }
    .paper-button-blue {
      color: rgb(240,248,255);
      background: rgb(30,144,255);
      max-height: 50px;
    }
    .button-create-project {
      margin-top: 0.5em;
      margin-bottom: 0.5em;
    }
    .paper-button-blue:hover {
      color: rgb(240,248,255);
      background: rgb(65,105,225);
    }
    .paper-button-blue[disabled] {
      background: #e1e1e1;
    }
    .button-danger {
      height: 2.5em;
      color: rgb(240,248,255);
      background: rgb(255,93,84);
    }
    .button-danger:hover {
      background: rgb(216,81,73);
    }
    .top-menu {
      display: flex;
      align-items: center;
    }
    .input-search-project {
      border-radius: 3px;
      border: thin solid #e1e1e1;
      margin-top: 0.5em;
      margin-bottom: 0.5em;
      margin-left: auto;
      height: 2.5em;
      padding-left:5px;
    }
    /* Set outline to none, otherwise the border color changes when clicking on input field. */
    .input-search-project:focus {
      outline: none;
    }
    .project-item-card {
      display: flex;
      width: 100%;
      margin-top: 1em;
    }
    .project-item-card:hover {
      background: #eeeeee;
    }
    .project-item-card-content {
      width: 100%;
      height: 100%;
      align-items: center;
      display: flex;
    }
    .project-item-name {
      margin-left: 1em;
      margin-top: 1em;
      margin-bottom: 1em;
    }
    .project-item-user-list {
      margin: 1em 1em 1em 0.5em;
    }
    .green-dot {
      background-color: #c5e686;
      height: 0.8em;
      width: 0.8em;
      border-radius: 50%;
      display: inline-block;
      margin-left: auto;
    }
    paper-tabs {
      --paper-tabs-selection-bar-color: rgb(30,144,255);
    }
    .icon {
      color: #000000;
    }
    .icon:hover {
      color: #7c7c7c;
    }
    `;
  }

  static get properties() {
    return {
      // l2p groups
      groups:{
        type: Array
      },
      /**
       * Array containing all the projects that were loaded from las2peer project service.
       */
      projects: {
        type: Array
      },
      /**
       * Array containing the projects that are currently listed/displayed in the frontend. This is used for the
       * implementation of the project search. If the user searches for projects by name, then listedProjects
       * only contains the projects that match the search input. If search is ended, listedProjects gets set to
       * all projects again.
       */
      listedProjects: {
        type: Array
      },
      /**
       * This property allows to disable the "All projects" tab. It can be set to "true" if only the projects where
       * the user is a member of should be listed. If set to "false", then all projects that are available will be
       * listed in the "All projects" tab.
       */
      disableAllProjects: {
        type: Boolean
      },
      /**
       * If disableAllProjects is set to false, then we have two tabs for listing the projects - "My projects" and
       * "All projects". This property stores the currently selected tab index and is either 0 (for "My projects")
       * or 1 (for "All projects").
       */
      tabSelected: {
        type: Number
      },
      // TODO
      projectsOnlineUser: {
        type: Map
      },
      /**
       * URL where the frontend can access the las2peer project service REST API.
       */
      projectServiceURL: {
        type: String
      },

      /**
      * URL where the frontend can access the las2peer contact service REST API.
      */
      contactServiceURL: {
        type: String
      },

      /**
       * Yjs address used for the online user list.
       * Only required if the online user list is used.
       */
      yjsAddress: {
        type: String
      },

      /**
       * Yjs resource path used for the online user list.
       * Only required if the online user list is used.
       */
      yjsResourcePath: {
        type: String
      },

      /**
       * If the user opens the project options dialog, then the project
       * for which the dialog is opened gets stored in this variable.
       */
      projectOptionsSelected: {
        type: Object
      }
    };
  }

  constructor() {
    super();
    this.groups = [];
    this.projects = [];
    this.listedProjects = ["sss"];
    this.projectsOnlineUser = new Object();
    // use a default value for project service URL for local testing
    this.projectServiceURL = "http://127.0.0.1:8080";
    this.contactServiceURL = "http://127.0.0.1:8080/contactservice";
    window.addEventListener('metadata-changed', this._changeMetadata);
    this.disableAllProjects = false;
    this.showProjects(false);
    this.yjsAddress = "http://127.0.0.1:1234";
    this.yjsResourcePath = "./socket.io";
  }

  _changeMetadata(event){
    console.log("Project is: " + event.detail.project);
    console.log("New Metadata is: " + event.detail.newMetadata);
    var project = JSON.parse(event.detail.project);
    var projectName =  project.name;
    var oldMetadata =  project.metadata;
    var newMetadata =  event.detail.newMetadata;
    // due to my lack of experience in frontend programming, I didnt know how to access the this.projectserviceurl var :( 
    fetch("http://127.0.0.1:8080"+ "/projects/changeMetadata/", {
      method: "POST",
      headers: Auth.getAuthHeaderWithSub(),
      body: JSON.stringify({
        "access_token": Auth.getAccessToken(),
        "projectName": projectName,
        "oldMetadata": oldMetadata,
        "newMetadata": newMetadata
      })
    }).then( response => {
        if(!response.ok) throw Error(response.status);
          return response.json();
    }).then(data => {
        console.log(data);
    }).catch(error => {
      if(error.message == "401") {
        // user is not authorized
        // maybe the access token has expired
        Auth.removeAuthDataFromLocalStorage();
     //   location.reload();
      } else {
        console.log(error);
      }
    });
  }

  render() {
    return html`
      <div class="main">
        <div class="top-menu">
          <paper-button class="button-create-project paper-button-blue" @click="${this._onCreateProjectButtonClicked}">Create Project</paper-button>
          <input class="input-search-project" @input="${(e) => this._onSearchInputChanged(e.target.value)}" 
              placeholder="Search Projects"></input>
        </div>
        <div>
          ${this.disableAllProjects ? html`` : html`
            <paper-tabs id="my-and-all-projects" selected="0">
              <paper-tab @click="${() => this._onTabChanged(0)}">My Projects</paper-tab>
              <paper-tab @click="${() => this._onTabChanged(1)}">All Projects</paper-tab>
            </paper-tabs>
          `}
        </div>
        <!-- show spinner if projects are loading -->
        ${this.projectsLoading ? html`
          <div style="width: 100%; display: flex">
            <paper-spinner-lite style="margin-top: 4em; margin-left: auto; margin-right: auto" active></paper-spinner-lite>
          </div>
        ` : html``}
        ${this.listedProjects.map(project => html`
            <paper-card class="project-item-card" @click="${() => this._onProjectItemClicked(project.name)}">
              <div class="project-item-card-content">
                <p class="project-item-name">${project.name}</p>
                <div style="margin-left: auto; display: flex">
                ${this.getListOfProjectOnlineUsers(project.name) ? html`<span class="green-dot" style="margin-top: auto; margin-bottom: auto"></span>` : html``}
                  <p class="project-item-user-list">${this.getListOfProjectOnlineUsers(project.name)}</p>
                  <slot name="project-${project.id}"></slot>
                  <iron-icon icon="icons:more-vert" class="icon" style="margin-top: auto; margin-bottom: auto; margin-right: 1em"
                    @click=${() => this.openProjectOptionsDialog(project)}></iron-icon>
                </div>
              </div>
            </paper-card>
        `)}
      </div>
      
      <!-- Dialog for creating new projects. -->
      <paper-dialog id="dialog-create-project">
        <h2>Create a Project</h2>
        
        <paper-input id="input-project-name" @input="${(e) => this._onInputProjectNameChanged(e.target.value)}" 
            placeholder="Project Name"></paper-input>
        <paper-dropdown-menu id="input-group-name" label="Link Group to Project:" 
        ><paper-listbox slot="dropdown-content" class="dropdown-content">
          ${this.groups.map(group => html`
          <paper-item value="${group.id}">${group.name}</paper-item>
            `)}
        </paper-listbox>
        </paper-dropdown-menu>
        <div class="buttons">
          <paper-button @click="${this._closeCreateProjectDialogClicked}" dialog-dismiss>Cancel</paper-button>
          <paper-button id="dialog-button-create" @click="${this._createProject}" dialog-confirm>Create</paper-button>
        </div>
      </paper-dialog>
      
      <paper-dialog id="dialog-project-options">
        <h2>Connected Group</h2>
        <p>The project <span id="connected-group-project-name">Project name</span> is connected to the las2peer group:</p>
        <div style="display: flex">
          <p><span id="connected-group-name">Group name</span></p>
          <paper-dropdown-menu style="display: none" id="input-edit-group-name" label="Link Group to Project:" 
        ><paper-listbox slot="dropdown-content" class="dropdown-content">
          ${this.groups.map(group => html`
          <paper-item value="${group.id}">${group.name}</paper-item>
            `)}
        </paper-listbox>
        </paper-dropdown-menu>
          <iron-icon icon="editor:mode-edit" class="icon" style="margin-top: auto; margin-bottom: auto; margin-left: 1em"
            @click=${this._onEditConnectedGroupClicked}></iron-icon>
        </div>

        <h2>Danger Zone</h2>
        <div style="display: flex; margin-top: 0">
          <p>Delete this project. Please note that a project cannot be restored after deletion.</p>
          <paper-button class="button-danger" @click=${this.showDeleteProjectDialog} style="margin-top: auto; margin-bottom: auto">Delete</paper-button>
        </div>
        <div class="buttons">
          <paper-button dialog-confirm @click=${this._onGroupChanged}>OK</paper-button>
        </div>
      </paper-dialog>

      <!-- Dialog: Are you sure to delete the project? -->
      <paper-dialog id="dialog-delete-project" modal>
        <h4>Delete Project</h4>
        <div>
        Are you sure that you want to delete the project?
        </div>
        <div class="buttons">
          <paper-button dialog-dismiss>Cancel</paper-button>
          <paper-button @click=${this._deleteProject} dialog-confirm autofocus>Yes</paper-button>
        </div>
      </paper-dialog>
      
      <!-- Dialog showing a loading bar -->
      <paper-dialog id="dialog-loading" modal>
        <paper-spinner-lite active></paper-spinner-lite>
      </paper-dialog>
      
      <!-- Toasts -->
      <!-- Toast for successful creation of project -->
      <paper-toast id="toast-success" text="Project created!"></paper-toast>

      <!-- Toast for successful deletion of project -->
      <paper-toast id="toast-success-deletion" text="Project deleted!"></paper-toast>
      
      <!-- Toast for creation fail because of project with same name already existing -->
      <custom-style><style is="custom-style">
        #toast-already-existing {
          --paper-toast-background-color: red;
          --paper-toast-color: white;
        }
      </style></custom-style>
      <paper-toast id="toast-already-existing" text="A project with the given name already exists!"></paper-toast>
    `;
  }

  /**
   * Gets called by the "Create Project" button. Opens the dialog for creating a project, which then lets the user
   * select a name for the project and a group that should be connected to the project.
   * @private
   */
  _onCreateProjectButtonClicked() {
    // clear input fields of dialog
    this.resetCreateProjectDialog();

    // add statusbar to be able to get user infos for this step
    console.log(this.contactServiceURL);
    fetch(this.contactServiceURL + "/groups", {
      method: "GET",
      headers: Auth.getAuthHeaderWithSub()
    }).then(response => {
      if(!response.ok) throw Error(response.status);
      console.log(typeof response)
      console.log("ssssssss" + Object.keys(response));
      return response.json();
    }).then(data => {
      // store loaded groups
      // groups given by contact service as a JSONObject with key = group agent id and value = group name
      // we create an array of objects with id and name attribute out of it
      this.groups = [];
      for(let key of Object.keys(data)) {
        let group = {
          "id": key,
          "name": data[key]
        };
        this.groups.push(group);
      }

      console.log(this.groups);
      // only open popup once group loaded
      this.shadowRoot.getElementById("dialog-create-project").open();
      // disable create button until user entered a project name
      this.shadowRoot.getElementById("dialog-button-create").disabled = true;
    }).catch(error => {
      console.log("ssdlkjidhaidjkol" + error.message);
      if(error.message == "401") {
        // user is not authorized
        // maybe the access token has expired
        Auth.removeAuthDataFromLocalStorage();
    //    location.reload();
      } else {
        console.log(error);
        // in case of contactservice not running, which should not happen in real deployment
        this.groups = [];
        // only open popup once group loaded
        this.shadowRoot.getElementById("dialog-create-project").open();
        // disable create button until user entered a project name
        this.shadowRoot.getElementById("dialog-button-create").disabled = true;
      }

    });
  }
  

  /**
   * Gets called when the search input gets updated by the user. Updates listedProjects array correspondingly.
   * @param searchInput Input from the user entered in the input field for searching projects by name.
   * @private
   */
  _onSearchInputChanged(searchInput) {
    if(searchInput) {
      this.listedProjects = this.projects.filter(project => {
        return project.name.toLowerCase().includes(searchInput.toLowerCase());
      });
    } else {
      // no search input, show all projects that were loaded
      this.listedProjects = this.projects;
    }
  }

  /**
   * Gets called when the user switches the current tab. Depending on which tab is selected, "My projects" or
   * "All projects" are loaded.
   * @param tabIndex 0 = My Projects, 1 = All Projects
   * @private
   */
  _onTabChanged(tabIndex) {
    this.tabSelected = tabIndex;
    if(tabIndex == 0) {
      // show users projects / projects where the user is a member of
      this.showProjects(false);
    } else {
      // show all projects
      this.showProjects(true);
    }
  }

  /**
   * Loads and shows the projects that the user is a member of, or all existing projects.
   * @param allProjects If all projects should be shown or only the ones where the
   * current user is a member of.
   */
  showProjects(allProjects) {
    // set loading to true
    this.projectsLoading = true;
    console.log("sasaq");
    // clear current project list
    this.projects = [];
    this.listedProjects = [];

    /*
    // only send authHeader when not all projects should be shown, but only the
    // one from the current user
    const headers = allProjects? undefined : Auth.getAuthHeader();
*/
    fetch(this.projectServiceURL + "/projects", {
      method: "GET",
      headers: Auth.getAuthHeaderWithSub()
    }).then(response => {
      if(!response.ok) throw Error(response.status);
      return response.json();
    }).then(data => {
      console.log("data");
      console.log(data);
      console.log("Projects are", data.projects);
      // set loading to false, then the spinner gets hidden
      this.projectsLoading = false;

      // if we only want to show the projects, where the user is a member of, we need to filter out some projects
      if(!allProjects) data.projects = data.projects.filter(project => project.is_member);

      // store loaded projects
      this.projects = data.projects;
      // set projects that should be shown (currently all)
      this.listedProjects = data.projects;

      let event = new CustomEvent("projects-loaded", {
        detail: {
          projects: this.projects
        },
        bubbles: true
      });
      this.dispatchEvent(event);

      // load online users
  /*    for(let i in this.projects) {
        this.loadListOfProjectOnlineUsers(this.projects[i].id);
      }*/
    }).catch(error => {
      if(error.message == "401") {
        // user is not authorized
        // maybe the access token has expired
        Auth.removeAuthDataFromLocalStorage();
     //   location.reload();
      } else {
        console.log(error);
      }
    });
  }

  /**
   * Gets called when the user clicks on a project in the project list. Fires an event that notifies the parent
   * elements that a project got selected.
   * @param projectName Name of the project that got selected in the project list.
   * @private
   */
  _onProjectItemClicked(projectName) {
    // TODO: give full information on the project and whether the user is a member of it
    let event = new CustomEvent("project-selected", {
      detail: {
        message: "Selected project in project list.",
        project: this.getProjectByName(projectName)
      },
      bubbles: true
    });
    this.dispatchEvent(event);
  }
  

  getProjectByName(name) {
    return this.listedProjects.find(x => x.name === name);
  }

    /**
   * Gets called when the user clicks on a project in the project list. Fires an event that notifies the parent
   * elements that a project got selected.
   * @param projectName Name of the project that got selected in the project list.
   * @private
   */
     _onGroupChangeDone(project) {
      // TODO: give full information on the project and whether the user is a member of it
      let event = new CustomEvent("project-selected", {
        detail: {
          message: "Selected project in project list.",
          project: project
        },
        bubbles: true
      });
      this.dispatchEvent(event);
    }

  /**
   * Gets called when the user clicks on the "Close" button in the create project dialog.
   * @private
   */
  _closeCreateProjectDialogClicked() {
    this.shadowRoot.getElementById("dialog-create-project").close();

    // clear input field for project name in the dialog
    this.shadowRoot.getElementById("input-project-name").value = "";
  }

  /**
   * Gets called when the user changes the input of the project name input field in the create project dialog.
   * Enables/disables the creation of projects depending on whether the name input is empty or not.
   * @param projectName Input
   * @private
   */
  _onInputProjectNameChanged(projectName) {
    if(projectName) {
      this.shadowRoot.getElementById("dialog-button-create").disabled = false;
    } else {
      this.shadowRoot.getElementById("dialog-button-create").disabled = true;
    }
  }

  /**
   * Get called when the user click on "create" in the create project dialog.
   */
  _createProject() {
    const projectName = this.shadowRoot.getElementById("input-project-name").value;
    const linkedGroupName = this.shadowRoot.getElementById("input-group-name").value;
    const linkedGroup = this.groups.find(group => group.name == linkedGroupName);

    // close dialog (then also the button is not clickable and user cannot create project twice or more often)
    // important: get projectName before closing dialog, because when closing the dialog the input field gets cleared
    this._closeCreateProjectDialogClicked();

    // show loading dialog
    this.shadowRoot.getElementById("dialog-loading").open();

    // currently fetches members from contact service but does not check whether project already exists (code is there but commented)
    if(projectName) {
      fetch(this.contactServiceURL + "/groups/" + linkedGroupName + "/member", {
        method: "GET",
        headers: Auth.getAuthHeaderWithSub() 
      }).then( response => {
          if(!response.ok) throw Error(response.status);
          console.log(typeof response)
          console.log("ssssssss" + Object.keys(response));
          return response.json();
      }).then(data => {
        console.log(data);
        const users = Object.values(data);
        //const newProject = {"id":this.projects.length, "name":projectName, "Linked Group":linkedGroup, "Group Members":users};
          fetch(this.projectServiceURL + "/projects", {
          method: "POST",
          headers:  Auth.getAuthHeaderWithSub(),
          body: JSON.stringify({
            "name": projectName,
            "access_token": Auth.getAccessToken(),
            "linkedGroup": linkedGroup,
            "users": users
          })
        }).then(response => {
          console.log(response);
          // close loading dialog
          this.shadowRoot.getElementById("dialog-loading").close();

          if(response.status == 201) {
            // project got created successfully
            this.shadowRoot.getElementById("toast-success").show();

            // clear input field for project name in the dialog
            this.shadowRoot.getElementById("input-project-name").value = "";

            // since a new project exists, reload projects from server
            this.showProjects(false);
            // switch to tab "My Projects"
            this.tabSelected = 0;
            this.shadowRoot.getElementById("my-and-all-projects").selected = 0;
          } else if(response.status == 409) {
            // a project with the given name already exists
            this.shadowRoot.getElementById("toast-already-existing").show();
          } else if(response.status == 401) {
            Auth.removeAuthDataFromLocalStorage();
       //     location.reload();
          }
          // TODO: check what happens when access_token is missing in localStorage
        });
      });
    }
  }

  /**
   * Call this method with a map, mapping project names to lists of Yjs room names, and then these Yjs room names
   * will be used for the online user list.
   * @param mapProjectRooms Map, mapping project names to lists of Yjs room names, where SyncMeta is running.
   */
  setOnlineUserListYjsRooms(mapProjectRooms) {
    this.projectsOnlineUser = {};

    for(let projectName of Object.keys(mapProjectRooms)) {
      let roomNames = mapProjectRooms[projectName];
      this.projectsOnlineUser[projectName] = [];
      for(let roomName of roomNames) {
        OnlineUserListHelper.loadListOfSyncMetaOnlineUsers(roomName, this.yjsAddress, this.yjsResourcePath).then(list => {
          for(let username of list) {
            if(!this.projectsOnlineUser[projectName].includes(username)) this.projectsOnlineUser[projectName].push(username);
          }
          this.requestUpdate();
        });
      }
    }
  }


  /**
   * Creates a string which contains a list of the users that are online in the
   * project with the given name.
   * @param projectName
   * @returns {string} String containing a list of online users in the given project.
   */
  getListOfProjectOnlineUsers(projectName) {
    let s = "";
    for(let i in this.projectsOnlineUser[projectName]) {
      s += this.projectsOnlineUser[projectName][i] + ",";
    }
    if(s) {
      s = s.substr(0,s.length-1);
    }
    return s;
  }

  /**
   * Gets called when the user clicks on the edit-button for the group name in the "connected-group" dialog.
   * @private
   */
  _onEditConnectedGroupClicked() {
    
    fetch(this.contactServiceURL + "/groups", {
      method: "GET",
      headers: Auth.getAuthHeaderWithSub()
    }).then(response => {
      if(!response.ok) throw Error(response.status);
      return response.json();
    }).then(data => {
      // store loaded groups
      // groups given by contact service as a JSONObject with key = group agent id and value = group name
      // we create an array of objects with id and name attribute out of it
      this.groups = [];
      for(let key of Object.keys(data)) {
        let group = {
          "id": key,
          "name": data[key]
        };
        this.groups.push(group);
      }
        // hide current group name paragraph element
        this.shadowRoot.getElementById("connected-group-name").style.setProperty("display", "none");

        // show dropdown menu to select a different group, therefore remove display: none
        this.shadowRoot.getElementById("input-edit-group-name").style.removeProperty("display");
      });
    
    


    /*const projectName = this.shadowRoot.getElementById("connected-group-project-name").value;
    const newLinkedGroupName = this.shadowRoot.getElementById("input-edit-group-name").value;

    fetch(this.projectServiceURL + "/projects/changeGroup/", {
      method: "POST",
      headers: Auth.getAuthHeaderWithSub(),
      body: JSON.stringify({
        "name": projectName,
        "access_token": Auth.getAccessToken(),
        "newGroupNameId": newLinkedGroupName
      })
    }).then( response => {
        if(!response.ok) throw Error(response.status);
        console.log(typeof response)
        console.log("ssssssss" + Object.keys(response));
        return response.json();
    })*/

  }

    /**
   * Gets called when the "Group" icon of one of the displayed projects gets clicked and opens a dialog with
   * information on the group which is currently connected to the project.
   * @param project
   */
    _onGroupChanged() {
      
      const projectName = this.shadowRoot.getElementById("connected-group-project-name").innerText;
      const newLinkedGroupName = this.shadowRoot.getElementById("input-edit-group-name").value;
      if(newLinkedGroupName == undefined){
        return;
      }

      var newLinkedGroupId = "";
      for(let key of Object.keys(this.groups)) {
        if(this.groups[key].name == newLinkedGroupName){
            newLinkedGroupId = this.groups[key].id;
            break;
        }
      }
      fetch(this.projectServiceURL + "/projects/changeGroup/", {
        method: "POST",
        headers: Auth.getAuthHeaderWithSub(),
        body: JSON.stringify({
          "name": projectName,
          "access_token": Auth.getAccessToken(),
          "projectName": projectName,
          "newGroupName": newLinkedGroupName,
          "newGroupId": newLinkedGroupId
        })
      }).then( response => {
          if(!response.ok) throw Error(response.status);
            return response.json();
      }).then(data => {
          this._onGroupChangeDone(data);
      }).catch(error => {
        if(error.message == "401") {
          // user is not authorized
          // maybe the access token has expired
          Auth.removeAuthDataFromLocalStorage();
       //   location.reload();
        } else {
          console.log(error);
        }
      });
    }

  /**
   * Gets called when the "options" icon of one of the displayed projects gets clicked and opens a dialog with
   * information on the group which is currently connected to the project and the possibility to delete the project.
   * @param project
   */
  openProjectOptionsDialog(project) {
    this.projectOptionsSelected = project;

    // reset the dialog
    this.shadowRoot.getElementById("connected-group-name").style.removeProperty("display");
    this.shadowRoot.getElementById("input-edit-group-name").style.setProperty("display", "none");

    this.shadowRoot.getElementById("connected-group-project-name").innerText = project.name;
    this.shadowRoot.getElementById("connected-group-name").innerText = project.groupName;

    // open the dialog
    this.shadowRoot.getElementById("dialog-project-options").open();
  }

  /**
   * Gets called when the "delete project" button in the project option dialog is clicked.
   * Opens another dialog to verify whether the project should really be deleted.
   */
  showDeleteProjectDialog() {
    // hide project options dialog
    this.shadowRoot.getElementById("dialog-project-options").close();

    // open delete dialog
    this.shadowRoot.getElementById("dialog-delete-project").open();
  }

  /**
   * Gets called if the user has verified that the project should be deleted.
   */
  _deleteProject() {
    let projectToDelete = this.projectOptionsSelected;
    fetch(this.projectServiceURL + "/projects/" + projectToDelete.name, {
      method: "DELETE",
      headers: Auth.getAuthHeaderWithSub(),
      body: JSON.stringify({
        "access_token": Auth.getAccessToken()
      })
    }).then(response => {
      if(response.ok) {
        this.shadowRoot.getElementById("toast-success-deletion").show();

        // since a project got deleted, reload projects from server
        this.showProjects(false);
        // switch to tab "My Projects"
        this.tabSelected = 0;
        this.shadowRoot.getElementById("my-and-all-projects").selected = 0;
      }
      else throw Error(response.status);
    }).catch(error => {
      
    });

  }

  /**
   * Resets the input fields of the "Create Project" dialog.
   * Clears the input for the project name and resets the dropdown menu for the group selection.
   */
  resetCreateProjectDialog() {
    // clear previous input (might not exist yet if dialog was never opened before, but just clear it anyway)
    this.shadowRoot.getElementById("input-project-name").value = "";
    this.shadowRoot.getElementById("input-group-name")._setSelectedItem(null);
  }
}

window.customElements.define('project-list', ProjectList);
