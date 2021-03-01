import { LitElement, html } from 'lit-element';
import '../project-list.js';

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

  render() {
    return html`
      <h2>Project list with "All Projects" enabled</h2>
      <div style="display: flex">
        <project-list projectServiceURL="test" @project-selected=${this._onProjectSelected} style="flex: 2"></project-list>
        <div style="flex: 1; margin-left: 1em">
          <h1>Demo information:</h1>
          <h3>Selected project:</h3>
          <p>${this.selectedProject}</p>
        </div>
      </div>
      
      <h2>Project list with "All Projects" disabled</h2>
      <project-list disableAllProjects="true"></project-list>
    `;
  }

  /**
   * For testing the "project-selected" event of the project list.
   * @param event Event that contains the information on the selected project.
   * @private
   */
  _onProjectSelected(event) {
    this.selectedProject = JSON.stringify(event.detail.project);
  }
}

window.customElements.define('demo-element', DemoElement);
