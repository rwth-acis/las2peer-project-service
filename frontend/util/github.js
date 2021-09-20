import Auth from "./auth.js";

/**
 * Helper class for GitHub projects connection.
 * Helps to get the GitHub username and to inform the project-service about it.
 */
export default class GitHub {

  static GITHUB_ACCESS_TOKEN_URL = "https://api.learning-layers.eu/auth/realms/main/broker/github/token";
  static GITHUB_API_USER_URL = "https://api.github.com/user";
  static KEY_LOCALSTORAGE_USERNAME = "projectservice_github_username";

  static gitHubUsernameStored() {
    return localStorage.getItem(GitHub.KEY_LOCALSTORAGE_USERNAME) !== null;
  }

  static storeGitHubUsername(username) {
    localStorage.setItem(GitHub.KEY_LOCALSTORAGE_USERNAME, username);
  }

  static getGitHubUsername() {
    return localStorage.getItem(GitHub.KEY_LOCALSTORAGE_USERNAME);
  }

  /**
   * Sends the GitHub username (that is stored in localStorage) to the project-service.
   * @param projectServiceURL URL of the project service webconnector.
   * @param system Name of the system.
   */
  static sendGitHubUsernameToProjectService(projectServiceURL, system) {
    if(GitHub.gitHubUsernameStored()) {
      const username = GitHub.getGitHubUsername();
      fetch(projectServiceURL + "/projects/" + system + "/user/githubinfo", {
        method: "POST",
        headers: Auth.getAuthHeaderWithSub(),
        body: JSON.stringify({
          "gitHubUsername": username
        })
      });
    }
  }

  /**
   * Tries to get a GitHub access token for the user from keycloak.
   * If that works, the user has connected a GitHub account.
   * @returns {Promise<unknown>}
   */
  static hasUserGitHubAccountConnected() {
    return new Promise((resolve, reject) => fetch(GitHub.GITHUB_ACCESS_TOKEN_URL, {
      method: "GET",
      headers: {
        "Authorization": "Bearer " + Auth.getAccessToken()
      }
    }).then(response => {
      if(response.status == 200) {
        return response.text();
      } else {
        resolve([false]);
      }
    }).then(text => {
      const gitHubAccessToken = text.split("access_token=")[1].split("&")[0];
      return fetch(GitHub.GITHUB_API_USER_URL, {
        method: "GET",
        headers: {
          "Accept": "application/vnd.github.v3+json",
          "Authorization": "Bearer " + gitHubAccessToken
        }
      })
    }).then(response => {
      if(response.status == 200) {
        return response.json();
      } else {
        resolve([false])
      }
    }).then(json => {
      resolve([true, json.login]);
    }));
  }
}
