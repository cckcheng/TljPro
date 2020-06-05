package com.ccd.tljpro;

import com.codename1.components.ToastBar;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 *
 * @author ccheng
 */
public class GoogleData extends ConnectionRequest implements UserData {

    private Runnable callback;
    private Map<String, Object> parsedData;

    @Override
    public String getName() {
        return (String) parsedData.get("displayName");
    }

    @Override
    public String getId() {
        return parsedData.get("id").toString();
    }

    @Override
    public String getImage() {
        Map<String, Object> imageMeta = ((Map<String, Object>) parsedData.get("image"));
        return (String) imageMeta.get("url");
    }

    @Override
    public void fetchData(String token, Runnable callback) {
        this.callback = callback;
        addRequestHeader("Authorization", "Bearer " + token);
        setUrl("https://www.googleapis.com/plus/v1/people/me");
        setPost(false);
        NetworkManager.getInstance().addToQueue(this);
    }

    @Override
    protected void handleErrorResponseCode(int code, String message) {
        //access token not valid anymore
        if (code >= 400 && code <= 410) {
//            doLogin(MyGoogleLogin.getInstance(), this, true);
            ToastBar.showErrorMessage("Connection Error: " + message);
            return;
        }
        super.handleErrorResponseCode(code, message);
    }

    @Override
    protected void readResponse(InputStream input) throws IOException {
        JSONParser parser = new JSONParser();
        parsedData = parser.parseJSON(new InputStreamReader(input, "UTF-8"));
    }

    @Override
    protected void postResponse() {
        callback.run();
    }
}
