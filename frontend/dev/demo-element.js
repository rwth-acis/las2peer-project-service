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

  //  this.storeEmptyModelingInfo();
  //  this.updateMenu();

    // after login, project management is shown, thus this menu item should be underlined
  //  this.underlineMenuItem("menu-project-management");

    // notify project management service about user login
    // if the user is not yet registered, then the project management service will do this
    // add loadcurrentuserat a later time
    
      var url = localStorage.userinfo_endpoint + '?access_token=' + localStorage.access_token;
      console.log(url);
      fetch(url, {method: "GET"}).then(response => {
        if(response.ok) {
          return response.json();
        }
      }).then(data => {
        console.log(data.name);
       // const userInfo = Common.getUserInfo();
        //userInfo.sub = data.sub;
        Common.storeUserInfo(data);
      });
   

    // show statusbar again
  //  this.getCaeStatusbar().removeAttribute("hidden");

    // set project-management as current page
    // Reason: when the user logged out in modeling, then after login the user
    // should start with project management page again
  //  this.set("route.path", "/");

    // when removing this line, we get a problem because some
    // user services used by the las2peer-frontend-statusbar cannot be accessed
   // location.reload();

    // since location.reload() is not called anymore, it is necessary
    // to reload the project management manually, since otherwise the "Please login"
    // message does not disappear.
   // this.shadowRoot.getElementById("project-management").requestUpdate();
  }

  handleLogout() {
    Auth.removeAuthDataFromLocalStorage();

    // hide cae statusbar
   // this.getCaeStatusbar().setAttribute("hidden", "");

    // update project management, because then it shows the login hint
    //this.shadowRoot.getElementById("project-management").requestUpdate();

    // remove userInfo from localStorage
    Common.removeUserInfoFromStorage();

    // redirect to landing page (because there the login-hint is shown)
   // this.set("route.path", "/");
  }

  render() {
    return html`
    <las2peer-frontend-statusbar
    id="statusBar"
    service="Project List Demo"
    oidcpopupsigninurl="/callbacks/popup-signin-callback.html"
    oidcpopupsignouturl="/callbacks/popup-signout-callback.html"
    oidcsilentsigninturl="/callbacks/silent-callback.html"
    oidcclientid="d8e6c0d3-fb09-49cc-9a6d-f1763d39a0a7"
    suppresswidgeterror="true"
    autoAppendWidget=true
    ></las2peer-frontend-statusbar>
      <h2>Project list with "All Projects" enabled</h2>
      <div style="display: flex">
        <project-list id="pl1" @projects-loaded=${this._onProjectsLoaded} @project-selected=${this._onProjectSelected} style="flex: 2"></project-list>
        <div style="flex: 1; margin-left: 1em">
          <h1>Demo information:</h1>
          <h3>Selected project:</h3>
          <p style="max-width: 200px" @click=${this.logx}>${this.selectedProject}</p>
          <input id="metadataInput" placeholder="Random Test Metadata"></input>
          <button style="max-width: 200px" @click=${this._triggerChange}>Change Metadata</button>
        </div>
      </div>
      
      <h2>Project list with "All Projects" disabled</h2>
    <!--  <project-list disableAllProjects="true"></project-list> -->
    `;
  }

  getStatusBarElement() {
    return this.shadowRoot.querySelector("#statusBar");
  }

  _triggerChange(event){
    let events = new CustomEvent("metadata-changed", {
      detail: {
        message: "Changed Project",
        project: this.selectedProject,
        newMetadata: {"random":this.shadowRoot.querySelector("#metadataInput").value}
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
    
    // uncomment this, if you want to test the online user list
    /*let mapProjectRooms = {};
    for(let project of projects) {
        mapProjectRooms[project.name] = ["exampleYjsRoom"];
    }
    this.shadowRoot.getElementById("pl1").setOnlineUserListYjsRooms(mapProjectRooms);*/
  }
}

window.customElements.define('demo-element', DemoElement);
