package com.ccd.tljpro;

/**
 * the message to send to server
 *
 * @author ccheng
 */
public class Request {
    static final String CREATE = "create";
    static final String JOIN = "join";
    static final String EXIT = "out";
    static final String ROBOT = "robot";
    static final String BID = "bid";
    static final String TRUMP = "trump";
    static final String BURY = "bury";
    static final String PLAY = "play";
    static final String PARTNER = "partner";
    static final String RE = "re";

    private String msg;
    private final String action;
    private final boolean checkReply;
    private boolean reSend;

    Request(String action, boolean reply) {
        this(action, reply, false);
    }

    Request(String action, boolean reply, boolean resend) {
        this.msg = "\"action\":\"" + action + "\"";
        this.checkReply = reply;
        this.action = action;
        this.reSend = resend;
    }

    static public Request create(String action, String key, String value) {
        Request req = new Request(action, true);
        return req.append(key, value);
    }

    static public Request create(String action, String key, int value) {
        Request req = new Request(action, true);
        return req.append(key, value);
    }

    static public Request create(String action, String key, char value) {
        Request req = new Request(action, true);
        return req.append(key, value);
    }

    public String getMsg() {
        return "{" + msg + "}";
    }

    public boolean isCheckReply() {
        return checkReply;
    }

    public boolean isReSend() {
        return reSend;
    }

    public Request setReSend(boolean reSend) {
        this.reSend = reSend;
        return this;
    }

    public Request append(String key, String value) {
        if (key == null || key.isEmpty()) return this;
        if (value == null) value = "";
        this.msg += ",\"" + key + "\":\"" + value + "\"";
        return this;
    }

    public Request append(String key, char value) {
        if (key == null || key.isEmpty()) return this;
        this.msg += ",\"" + key + "\":\"" + value + "\"";
        return this;
    }

    public Request append(String key, int value) {
        if (key == null || key.isEmpty()) return this;
        this.msg += ",\"" + key + "\":" + value;
        return this;
    }
}
