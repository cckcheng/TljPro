package com.ccd.tljpro;

import com.codename1.ui.Dialog;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ccheng
 */
public class ServerMessage {
    private static Map<String, ServerMessage> messages = new HashMap<>();
    private String title;
    private String content;

    ServerMessage(String title, String content) {
        this.title = title;
        this.content = content;
    }

    static public void showMessage(TuoLaJiPro main, String type) {
        String key = type + main.lang;
        ServerMessage sm = messages.get(key);
        if (sm != null) {
            Dialog.show(sm.title, sm.content, Dict.get(main.lang, "OK"), null);
        } else {
            Player p = main.getPlayer();
            p.sendRequest(p.initRequest(Request.MESSAGE).append("type", type));
        }
    }

    static public void showMessage(Map<String, Object> data) {
        String lang = Func.trimmedString(data.get("lang"));
        String type = Func.trimmedString(data.get("type"));
        String key = type + lang;

        String title = Func.trimmedString(data.get("title"));
        String content = Func.trimmedString(data.get("content"));
        if (key.isEmpty() || title.isEmpty()) return;

        if (!type.isEmpty()) messages.put(key, new ServerMessage(title, content));
        Dialog.show(title, content, Dict.get(lang, "OK"), null);
    }
}
