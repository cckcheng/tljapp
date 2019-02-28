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
                    default:
                        return src;
                }
            case "en":
                return src;
            default:
                return src;
        }
    }
}
