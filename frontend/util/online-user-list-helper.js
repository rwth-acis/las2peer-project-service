import 'yjs/dist/y';
import 'y-memory/dist/y-memory';
import 'y-websockets-client/dist/y-websockets-client';
import 'y-map/dist/y-map';

export default class OnlineUserListHelper {

  /**
     * Load users that are online in the given Yjs room which is used by a SyncMeta instance.
     * @param yjsRoomName Name of the Yjs room which is used by SyncMeta.
     * @param yjsAddress
     * @param yjsResourcePath
     */
  static loadListOfSyncMetaOnlineUsers(yjsRoomName, yjsAddress, yjsResourcePath) {
    // get currently active users in yjs room
    return new Promise((resolve) => Y({
      db: {
        name: "memory" // store the shared data in memory
      },
      connector: {
        name: "websockets-client", // use the websockets connector
        room: yjsRoomName,
        options: { resource: yjsResourcePath },
        url: yjsAddress
      },
      share: { // specify the shared content
        userList: 'Map', // used to get full name of users
        join: 'Map' // used to get currently online users
      },
      type: ["Map"],
      sourceDir: "node_modules"
    }).then(function (y) {
      const userList = y.share.userList;

      let list = [];

      // Start observing for join events.
      // After that we will join the Yjs room with the username "invisible_user".
      // When we join the Yjs room, then all the other users send a join event back to us.
      // Thus, we wait for join events which tell us which users are online.
      // We use "invisible_user" as username, because this is the only username where SyncMeta's
      // activity list widget does not show the join/leave events for.
      y.share.join.observe(event => {
        if (userList.get(event.name)) {
          const userFullName = userList.get(event.name)["http://purl.org/dc/terms/title"];
          if (y.share.userList.get(event.name)) {
            if (!list.includes(userFullName)) {
              list.push(userFullName);
            }
          }
        }
      });
      // now join the Yjs room
      y.share.join.set("invisible_user", false);

      setTimeout(function () {
        y.close();
        resolve(list);
      }, 5000);
    }));
  }

}