package com.ccd.tlj;

/**
 *
 * @author ccheng
 */
public class Dict {
    static public String get(final String lang, final String src) {
        if (src == null || src.isEmpty()) return "";
        if (src.equals(TuoLaJi.title)) {
            if (lang.equals("zh")) return "兰里拖拉机";
            return src;
        }

        final String lowerSrc = src.toLowerCase();
        switch (lang) {
            case "zh":
                switch (lowerSrc) {
                    case "ok":
                        return "确定";
                    case "cancel":
                        return "取消";
                    case "error":
                        return "错误";

                    case "play":
                        return "开局";
                    case "help":
                        return "帮助";
                    case "settings":
                        return "设置";
                    case "version":
                        return "版本";
                    case "player name":
                        return "玩家";
                    case "your name":
                        return "昵称";
                    case "exit":
                        return "退出";
                    case "connecting":
                        return "连接中";
                    case "network error":
                        return "联网失败";

                    case "bury":
                        return "扣底";
                    case "pass":
                        return "不叫";
                    case "nt":
                        return "无将";
                    case "1 vs 5":
                    case "1vs5":
                        return "一打五";

                    case "1st":
                        return "第一";
                    case "2nd":
                        return "第二";
                    case "3rd":
                        return "第三";
                    case "4th":
                        return "第四";

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

    static public String get(final String lang, int k) {
        switch (k) {
            case PLAY:
                return lang.equals("zh") ? "出牌" : "Play";
        }

        return "Unknown";
    }
}
