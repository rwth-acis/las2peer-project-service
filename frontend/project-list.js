import { LitElement, html, css } from 'lit-element';
import '@polymer/paper-card/paper-card.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-input/paper-input.js';
import '@polymer/paper-dialog/paper-dialog.js';
import '@polymer/paper-spinner/paper-spinner-lite.js';
import '@polymer/paper-tabs';

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
    `;
  }

  static get properties() {
    return {
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
      }
    };
  }

  constructor() {
    super();
    this.projects = [];
    this.listedProjects = [];
    this.projectsOnlineUser = new Object();
    // use a default value for project service URL for local testing
    this.projectServiceURL = "127.0.0.1:8080";

    this.disableAllProjects = false;

    this.showProjects(!this.disableAllProjects);
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
            <paper-card class="project-item-card" @click="${() => this._onProjectItemClicked(project.id)}">
              <div class="project-item-card-content">
                <p class="project-item-name">${project.name}</p>
                <div style="margin-left: auto; display: flex">
                  ${this.getListOfProjectOnlineUsers(project.id) ? html`<span class="green-dot" style="margin-top: auto; margin-bottom: auto"></span>` : html``}
                  <p class="project-item-user-list">${this.getListOfProjectOnlineUsers(project.id)}</p>
                </div>
              </div>
            </paper-card>
        `)}
      </div>
      
      <!-- Dialog for creating new projects. -->
      <!-- TODO: this does not contain groups yet -->
      <paper-dialog id="dialog-create-project">
        <h2>Create a Project</h2>
        
        <paper-input id="input-project-name" @input="${(e) => this._onInputProjectNameChanged(e.target.value)}" 
            placeholder="Project Name"></paper-input>
        
        <div class="buttons">
          <paper-button @click="${this._closeCreateProjectDialogClicked}" dialog-dismiss>Cancel</paper-button>
          <paper-button id="dialog-button-create" @click="${this._createProject}" dialog-confirm>Create</paper-button>
        </div>
      </paper-dialog>
      
      <!-- Dialog showing a loading bar -->
      <paper-dialog id="dialog-loading" modal>
        <paper-spinner-lite active></paper-spinner-lite>
      </paper-dialog>
      
      <!-- Toasts -->
      <!-- Toast for successful creation of project -->
      <paper-toast id="toast-success" text="Project created!"></paper-toast>
      
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
    this.shadowRoot.getElementById("dialog-create-project").open();
    // disable create button until user entered a project name
    this.shadowRoot.getElementById("dialog-button-create").disabled = true;
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

    // clear current project list
    this.projects = [];
    this.listedProjects = [];

    // Following code is used for testing only
    this.projectsLoading = false;
    let data = [
      {
        "id": 1,
        "name": "Project 1"
      },
      {
        "id": 2,
        "name": "Project 2"
      }
    ];
    this.projects = data;
    this.listedProjects = data;
    /*
    // only send authHeader when not all projects should be shown, but only the
    // one from the current user
    const headers = allProjects? undefined : Auth.getAuthHeader();

    fetch(this.projectServiceURL + "/projects", {
      method: "GET",
      headers: headers
    }).then(response => {
      if(!response.ok) throw Error(response.status);
      return response.json();
    }).then(data => {
      // set loading to false, then the spinner gets hidden
      this.projectsLoading = false;

      // store loaded projects
      this.projects = data;
      // set projects that should be shown (currently all)
      this.listedProjects = data;

      // load online users
      for(let i in this.projects) {
        this.loadListOfProjectOnlineUsers(this.projects[i].id);
      }
    }).catch(error => {
      if(error.message == "401") {
        // user is not authorized
        // maybe the access token has expired
        Auth.removeAuthDataFromLocalStorage();
        location.reload();
      } else {
        console.log(error);
      }
    });*/
  }

  /**
   * Gets called when the user clicks on a project in the project list. Fires an event that notifies the parent
   * elements that a project got selected.
   * @param projectId Id of the project that got selected in the project list.
   * @private
   */
  _onProjectItemClicked(projectId) {
    // TODO: give full information on the project and whether the user is a member of it
    let event = new CustomEvent("project-selected", {
      detail: {
        message: "Selected project in project list.",
        project: this.getProjectById(projectId)
      },
      bubbles: true
    });
    this.dispatchEvent(event);
  }

  getProjectById(id) {
    return this.listedProjects.find(x => x.id == id);
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

    // close dialog (then also the button is not clickable and user cannot create project twice or more often)
    // important: get projectName before closing dialog, because when closing the dialog the input field gets cleared
    this._closeCreateProjectDialogClicked();

    // show loading dialog
    this.shadowRoot.getElementById("dialog-loading").open();

    if(projectName) {
      /*fetch(this.projectServiceURL + "/projects", {
        method: "POST",
        headers: Auth.getAuthHeader(),
        body: JSON.stringify({
          "name": projectName,
          "access_token": Auth.getAccessToken()
        })
      }).then(response => {
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
          location.reload();
        }
      });*/
    }
  }

  /**
   * Creates a string which contains a list of the users that are online in the
   * project with the given id.
   * @param projectId
   * @returns {string} String containing a list of online users in the given project.
   */
  getListOfProjectOnlineUsers(projectId) {
    let s = "";
    for(let i in this.projectsOnlineUser[projectId]) {
      s += this.projectsOnlineUser[projectId][i] + ",";
    }
    if(s) {
      s = s.substr(0,s.length-1);
    }
    return s;
  }
}

window.customElements.define('project-list', ProjectList);
