/**
 * Helper class for storing/loading of user info.
 */
export default class Common {

  /**
   * Key used to store the information about the currently logged in user.
   * @type {string}
   */
  static KEY_USER_INFO = "userInfo";

  /**
   * Returns the information about the currently logged in user.
   * @returns {string}
   */
  static getUserInfo() {
    return JSON.parse(localStorage.getItem(this.KEY_USER_INFO));
  }

  /**
   * Stores the information about the currently logged in user.
   * @param userInfo Info to store in localStorage.
   */
  static storeUserInfo(userInfo) {
    localStorage.setItem(this.KEY_USER_INFO, JSON.stringify(userInfo));
  }


  /**
   * Removes the userInfo from localStorage.
   * This method may be used when user has logged out.
   */
  static removeUserInfoFromStorage() {
    localStorage.removeItem(this.KEY_USER_INFO);
  }
}

