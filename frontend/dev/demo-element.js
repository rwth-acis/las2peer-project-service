import { LitElement, html } from 'lit-element';
import '../project-list.js';
import Auth from "../util/auth";
import Common from "../util/common";
import 'las2peer-frontend-statusbar/las2peer-frontend-statusbar.js';

export class DemoElement extends LitElement {

  static get properties() {
    return {
      selectedProject: {
        type: Object
      }
    }
  }

 constructor() {
    super();

  }
  // I didnt get how to use ready, so simply used firstUpdated which is always called after render...
  firstUpdated(changedProperties){
    console.log("sas");
    const statusBar = this.shadowRoot.querySelector("#statusBar");
    // in the following we use (event) => this.method(event) in order to be able to access
    // this.shadowRoot in the handleLogin and handleLogout methods
    statusBar.addEventListener('signed-in', (event) => this.handleLogin(event));
    statusBar.addEventListener('signed-out', (event) => this.handleLogout(event));
  }

  handleLogin(event) {
    console.log("swsqwsw");
    console.log(event.detail.access_token);
    Auth.setAuthDataToLocalStorage(event.detail.access_token);

      var url = "https://api.learning-layers.eu/auth/realms/main/protocol/openid-connect/userinfo";
      console.log(url);
      fetch(url, {method: "GET", headers: {
        "Authorization": "Bearer " + Auth.getAccessToken()
      }}).then(response => {
        if(response.ok) {
          return response.json();
        }
      }).then(data => {
        console.log(data.name);
       // const userInfo = Common.getUserInfo();
        //userInfo.sub = data.sub;
        Common.storeUserInfo(data);
      });
  }

  handleLogout() {
    Auth.removeAuthDataFromLocalStorage();

    // remove userInfo from localStorage
    Common.removeUserInfoFromStorage();
  }

  render() {
    return html`
    <las2peer-frontend-statusbar
    id="statusBar"
    service="Project List Demo"
    oidcpopupsigninurl="/callbacks/popup-signin-callback.html"
    oidcpopupsignouturl="/callbacks/popup-signout-callback.html"
    oidcsilentsigninurl="/callbacks/silent-callback.html"
    oidcclientid="localtestclient"
    suppresswidgeterror="true"
    autoAppendWidget=true
    ></las2peer-frontend-statusbar>
      <h2>Project list with "All Projects" enabled</h2>
      <div style="display: flex">
        <project-list system="test" id="pl1" @projects-loaded=${this._onProjectsLoaded} @project-selected=${this._onProjectSelected} style="flex: 2"></project-list>
        <div style="flex: 1; margin-left: 1em">
          <h1>Demo information:</h1>
          <h3>Selected project:</h3>
          <p style="max-width: 200px" @click=${this.logx}>${this.selectedProject}</p>
          <input id="metadataInput" placeholder="Random Test Metadata"></input>
          <button style="max-width: 200px" @click=${this._triggerChange}>Change Metadata</button>
        </div>
      </div>
      
      <h2>Project list with "All Projects" disabled</h2>
    <!--  <project-list system="test" disableAllProjects="true"></project-list> -->
    `;
  }

  getStatusBarElement() {
    return this.shadowRoot.querySelector("#statusBar");
  }

  _triggerChange(event){
    let events = new CustomEvent("metadata-change-request", {
      detail: {
        "random": this.shadowRoot.querySelector("#metadataInput").value
      },
      bubbles: true
    });
    window.dispatchEvent(events);
  }

  /**
   * For testing the "project-selected" event of the project list.
   * @param event Event that contains the information on the selected project.
   * @private
   */
  _onProjectSelected(event) {
    console.log("onProjectSelected called");
    this.selectedProject = JSON.stringify(event.detail.project);
    console.log(this.selectedProject);
  }

  /**
   * Example for using the online user list.
   * Make sure that _onProjectsLoaded is called on the projects-loaded event of the project-list.
   */
  _onProjectsLoaded(event) {
    let projects = event.detail.projects;

    window.addEventListener("metadata-changed", e => {
        console.log("metadata has changed", e.detail);
        const project = JSON.parse(this.selectedProject);
        project.metadata = e.detail;
        this.selectedProject = JSON.stringify(project);
    });
    
    // uncomment this, if you want to test the online user list
    /*let mapProjectRooms = {};
    for(let project of projects) {
        mapProjectRooms[project.name] = ["exampleYjsRoom"];
    }
    this.shadowRoot.getElementById("pl1").setOnlineUserListYjsRooms(mapProjectRooms);*/
  }
}

window.customElements.define('demo-element', DemoElement);
