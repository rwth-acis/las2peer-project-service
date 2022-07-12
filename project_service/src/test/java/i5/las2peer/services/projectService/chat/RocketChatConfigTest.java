package i5.las2peer.services.projectService.chat;

import org.json.simple.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class RocketChatConfigTest {

    private static String url = "http://url.com";
    private static String botAuthToken = "token";
    private static String botUserId = "id";

    @Test
    public void fromJSONTest() {
        JSONObject configJSON = new JSONObject();
        configJSON.put("url", url);
        configJSON.put("botAuthToken", botAuthToken);
        configJSON.put("botUserId", botUserId);

        RocketChatConfig chatConfig = RocketChatConfig.fromJSON(configJSON);
        assertEquals(chatConfig.getUrl(), url);
        assertEquals(chatConfig.getBotAuthToken(), botAuthToken);
        assertEquals(chatConfig.getBotUserId(), botUserId);
    }
}