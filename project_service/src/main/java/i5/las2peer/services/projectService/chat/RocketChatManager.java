package i5.las2peer.services.projectService.chat;

import i5.las2peer.services.projectService.project.Project;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * ChatManager for RocketChat. Allows to create channels for las2peer projects.
 */
public class RocketChatManager extends ChatManager {

    public RocketChatManager(ChatConfig config) {
        super(config);
    }

    @Override
    public JSONObject createProjectChannel(Project project, String systemName) {
        String channelName = project.getName().replaceAll(" ", "") + "_" + systemName;

        JSONObject body = new JSONObject();
        body.put("name", channelName);

        HttpResponse<String> response = Unirest.post(getConfig().getUrl() + "/api/v1/channels.create")
                .header("X-Auth-Token", getConfig().getBotAuthToken())
                .header("X-User-Id", getConfig().getBotUserId())
                .header("Content-Type", "application/json")
                .body(body.toJSONString())
                .asString();

        if(!response.isSuccess()) {
            System.out.println("RocketChat channel creation failed with status code: " + response.getStatus());
            return null;
        }

        JSONObject res = (JSONObject) JSONValue.parse(response.getBody());
        JSONObject resChannel = (JSONObject) res.get("channel");

        JSONObject channelInfo = new JSONObject();
        channelInfo.put("type", "RocketChat");
        channelInfo.put("url", getConfig().getUrl());
        channelInfo.put("channelId", resChannel.get("_id"));
        channelInfo.put("chatUrl", getConfig().getUrl() + "/channel/" + resChannel.get("_id"));
        return channelInfo;
    }

    @Override
    public JSONObject getChannelInfoForExistingChannel(String channelName) {
        String channelId = getChannelIdByName(channelName);
        JSONObject channelInfo = new JSONObject();
        channelInfo.put("type", "RocketChat");
        channelInfo.put("url", getConfig().getUrl());
        channelInfo.put("channelId", channelId);
        channelInfo.put("chatUrl", getConfig().getUrl() + "/channel/" + channelId);
        return channelInfo;
    }

    private String getChannelIdByName(String channelName) {
        HttpResponse<String> response = Unirest.get(getConfig().getUrl() + "/api/v1/channels.info")
                .header("X-Auth-Token", getConfig().getBotAuthToken())
                .header("X-User-Id", getConfig().getBotUserId())
                .queryString("roomName", channelName)
                .asString();

        if(!response.isSuccess()) {
            System.out.println("RocketChat channel info request failed.");
            return null;
        }

        JSONObject res = (JSONObject) JSONValue.parse(response.getBody());
        JSONObject resChannel = (JSONObject) res.get("channel");
        return (String) resChannel.get("_id");
    }

    private RocketChatConfig getConfig() {
        return (RocketChatConfig) this.config;
    }
}
