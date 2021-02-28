import {LitElement, html, css} from 'lit-element';

/**
 * The project list element provides the functionality to list existing 
 * projects and to create new ones.
 */
export class ProjectList extends LitElement {
  static get styles() {
    return css``;
  }

  static get properties() {
    return {};
  }

  constructor() {
    super();
  }

  render() {
    return html`
      <h1>Project List</h1>
    `;
  }
}

window.customElements.define('project-list', ProjectList);
