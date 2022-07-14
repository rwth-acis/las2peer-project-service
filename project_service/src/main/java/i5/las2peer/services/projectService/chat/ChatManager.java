package i5.las2peer.services.projectService.chat;

import i5.las2peer.services.projectService.project.Project;
import org.json.simple.JSONObject;

/**
 * Super class for all chat managers.
 * Currently, only RocketChat is implemented.
 */
public abstract class ChatManager {

    protected ChatConfig config;

    public ChatManager(ChatConfig config) {
        this.config = config;
    }

    public abstract JSONObject createProjectChannel(Project project, String systemName);
    public abstract JSONObject getChannelInfoForExistingChannel(String channelName);

}
