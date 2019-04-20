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

        final String lowerSrc = src.toLowerCase().trim();
        switch (lang) {
            case "zh":
                switch (lowerSrc) {
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
                    case "away":
                        return "离开";
                    case "connecting":
                        return "连接中";
                    case "network error":
                        return "联网失败";

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

    static public String get(final String lang, int k) {
        switch (k) {
            case PLAY:
                return lang.equals("zh") ? "出牌" : "Play";
            case PNAME:
                return lang.equals("zh") ? "必填" : "Required";
        }

        return "Unknown";
    }
}
