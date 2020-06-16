package com.ccd.tljpro;

import com.codename1.util.regex.RE;

/**
 *
 * @author ccheng
 */
public class Dict {
    static public String get(final String lang, final String src) {
        if (src == null || src.isEmpty()) return "";
        if (src.equals(TuoLaJiPro.title)) {
            if (lang.equals("zh")) {
                return "兰里拖拉机专业版";
            }
            return src;
        }

        final String lowerSrc = src.toLowerCase().trim();
        switch (lang) {
            case "zh":
                switch (lowerSrc) {
                    case "background":
                        return "背景";
                    case "match":
                        return "比赛";
                    case "practice":
                        return "练习";
                    case "tutorial":
                        return "入门";
                    case "ok":
                        return "确定";
                    case "done":
                        return "完成";
                    case "next":
                        return "继续";
                    case "cancel":
                        return "取消";
                    case "no":
                        return "否";
                    case "error":
                        return "错误";
                    case "score":
                        return "得分";

                    case "password":
                        return "密码";
                    case "refresh":
                        return "刷新";
                    case "play":
                        return "开局";
                    case "browse":
                        return "浏览";
                    case "help":
                        return "帮助";
                    case "settings":
                        return "设置";
                    case "version":
                        return "版本";
                    case "account":
                        return "账户";
                    case "player name":
                        return "玩家";
                    case "your name":
                        return "昵称";
                    case "exit":
                        return "退出";
                    case "away":
                        return "离开";
                    case "connecting":
                        return "连接中";
                    case "network error":
                        return "联网失败";
                    case "start over":
                        return "重新开始";

                    case "submit":
                        return "提交";
                    case "save":
                        return "保存";
                    case "join":
                        return "加入";
                    case "robot":
                        return "托管";
                    case "bury":
                        return "扣底";
                    case "pass":
                        return "不叫";
                    case "nt":
                        return "无将";
                    case "1 vs 5":
                    case "1vs5":
                        return "一打五";

                    case "partner":
                        return "找朋友";
                    case "first":
                    case "1st":
                        return "第一";
                    case "second":
                    case "2nd":
                        return "第二";
                    case "third":
                    case "3rd":
                        return "第三";
                    case "fourth":
                    case "4th":
                        return "第四";
                    case "fifth":
                    case "5th":
                        return "第五";
                    case "sixth":
                    case "6th":
                        return "第六";

                    case "points":
                        return "分";

                    case "hold seat":
                        return "是否保留座位";
                    case "minutes":
                        return "分钟";

                    default:
                        return src;
                }
            case "en":
                return src;
            default:
                return src;
        }
    }

    public static final int PLAY = 1;
    public static final int PNAME = 2;
    public static final int INPUT_EMAIL = 5;
    public static final int AUTH_CODE = 6;
    public static final int REGISTER = 8;
    public static final int AUTH = 9;

    public static final int QUICK_JOIN = 11;
    public static final int NEW_TABLE = 12;
    public static final int PRIVATE_TABLE = 13;

    public static final int TABLE_CODE = 21;

    public static final int SIGNIN_APPLE = 31;
    public static final int SIGNIN_GOOGLE = 32;
    public static final int SIGNIN_FACEBOOK = 33;
    public static final int SIGNIN_EMAIL = 35;
    public static final int PLEASE_WAIT = 36;

    public static final int PLAYER_NAME_REQUIRED = 51;
    public static final int INVALID_PLAYER_NAME = 52;
    public static final int INVALID_EMAIL = 54;
    public static final int UPGRADE_IOS = 55;

    public static final int RESEND = 61;
    public static final int VERIFY_AUTHCODE = 62;
    public static final int CORRECT_EMAIL = 63;
    public static final int MISSING_AUTHCODE = 64;
    public static final int VERIFY_INSTRUCTION = 65;

    public static final int FAIL_CONNECT_SERVER = 99;

    static public String get(final String lang, int k) {
        switch (k) {
            case PLAY:
                return lang.equals("zh") ? "出牌" : "Play";
            case PNAME:
                return lang.equals("zh") ? "必填" : "Required";

            case QUICK_JOIN:
                return lang.equals("zh") ? "快速加入" : "Quick Join";
            case NEW_TABLE:
                return lang.equals("zh") ? "开新桌" : "New Table";
            case PRIVATE_TABLE:
                return lang.equals("zh") ? "私有桌" : "Private Table";
            case TABLE_CODE:
                return lang.equals("zh") ? "本桌密码" : "Table Pass";

            case SIGNIN_APPLE:
                return lang.equals("zh") ? "使用Apple登录" : "Sign in with Apple";
            case SIGNIN_GOOGLE:
                return lang.equals("zh") ? "使用Google登录" : "Sign in with Google";
            case SIGNIN_FACEBOOK:
                return lang.equals("zh") ? "使用Facebook登录" : "Sign in with Facebook";
            case SIGNIN_EMAIL:
                return lang.equals("zh") ? "使用Email登录" : "Sign in with Your Email";

            case PLAYER_NAME_REQUIRED:
                return lang.equals("zh") ? "请填写昵称" : "Player name is required";
            case INVALID_PLAYER_NAME:
                return lang.equals("zh") ? "昵称含有非法字符" : "Invalid player name";
            case INVALID_EMAIL:
                return lang.equals("zh") ? "非法Email" : "Invalid Email address";

            case REGISTER:
                return lang.equals("zh") ? "注册" : "Register";
            case AUTH:
                return lang.equals("zh") ? "验证" : "Verify";
            case AUTH_CODE:
                return lang.equals("zh") ? "验证码" : "Verification Code";
            case INPUT_EMAIL:
                return lang.equals("zh") ? "您的邮箱" : "Your Email";

            case RESEND:
                return lang.equals("zh") ? "重新发送" : "Resend";
            case VERIFY_AUTHCODE:
                return lang.equals("zh") ? "验证" : "Verify";

            case VERIFY_INSTRUCTION:
                return lang.equals("zh") ? "验证码已发至您的邮箱" : "The verification code has been emailed to you";
            case CORRECT_EMAIL:
                return lang.equals("zh") ? "请输入正确的Email：" : "Please input your correct Email address:";
            case MISSING_AUTHCODE:
                return lang.equals("zh") ? "未收到验证码! 请确认邮箱无误" : "Verification Code not received! Please correct your email address";

            case PLEASE_WAIT:
                return lang.equals("zh") ? "请稍候..." : "Please wait...";
            case FAIL_CONNECT_SERVER:
                return lang.equals("zh") ? "服务器连接失败" : "Failed to connection server";
            case UPGRADE_IOS:
                return lang.equals("zh") ? "无法登录，请升级到IOS 13"
                        : "Native login not supported, please upgrade to IOS 13";
        }

        return "Unknown";
    }

//    static Pattern ptnEmail = Pattern.compile("^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$");
    static RE reEmail = new RE("^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$");
    static public boolean validEmail(String email) {
//        return ptnEmail.matcher(email).matches();
        return reEmail.match(email);
    }
}
