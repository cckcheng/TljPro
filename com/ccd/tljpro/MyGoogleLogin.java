/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ccd.tljpro;

import com.codename1.social.Login;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Oauth2;
import java.util.Hashtable;

/**
 *
 * @author ccheng
 */
public class MyGoogleLogin extends Login {

    private static String tokenURL = "https://www.googleapis.com/oauth2/v3/token";
//    private static String tokenURL = "https://oauth2.googleapis.com/token";

    private static MyGoogleLogin instance;
    static Class implClass;

    MyGoogleLogin() {
    }

    private static String oauth2URL = "https://accounts.google.com/o/oauth2/auth";
    private static String clientId = Card.GOOGLE_CLIENT_ID;
    private static String clientSecret = Card.GOOGLE_CLIENT_SECRET;
    private static String redirectURI = TuoLaJiPro.DEBUG ? "http://localhost" : "urn:ietf:wg:oauth:2.0:oob";
    private static String scope = "profile email";

    public void init() {
        setOauth2URL(oauth2URL);
        setScope(scope);
        setClientId(clientId);
        setClientSecret(clientSecret);
        setRedirectURI(redirectURI);
    }

    /**
     * Gets the MyGoogleLogin singleton instance .
     *
     * @return the MyGoogleLogin instance
     */
    public static MyGoogleLogin getInstance() {
        if (instance == null) {
            if (implClass != null) {
                try {
                    instance = (MyGoogleLogin) implClass.newInstance();
                } catch (Throwable t) {
                    instance = new MyGoogleLogin();
                }
            } else {
                instance = new MyGoogleLogin();
            }
        }
        return instance;
    }

    @Override
    public boolean isNativeLoginSupported() {
        return false;
    }

    @Override
    protected Oauth2 createOauth2() {
        Hashtable params = new Hashtable();
        params.put("approval_prompt", "force");
        params.put("access_type", "offline");

        Oauth2 auth = new Oauth2(oauth2URL, clientId, redirectURI, scope, tokenURL, clientSecret, params);
        return auth;
    }

    @Override
    protected boolean validateToken(String token) {
        //make a call to the API if the return value is 40X the token is not
        //valid anymore
        final boolean[] retval = new boolean[1];
        retval[0] = true;
        ConnectionRequest req = new ConnectionRequest() {
            @Override
            protected void handleErrorResponseCode(int code, String message) {
                //access token not valid anymore
                if (code >= 400 && code <= 410) {
                    retval[0] = false;
                    return;
                }
                super.handleErrorResponseCode(code, message);
            }

        };
        req.setPost(false);
        req.setUrl("https://www.googleapis.com/plus/v1/people/me");
        req.addRequestHeader("Authorization", "Bearer " + token);
        NetworkManager.getInstance().addToQueueAndWait(req);
        return retval[0];
    }
}
