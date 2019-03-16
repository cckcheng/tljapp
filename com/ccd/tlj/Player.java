package com.ccd.tlj;

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.DynamicImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.Base64;
import com.codename1.util.regex.StringReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ccheng
 */
public class Player {

    static final String BIDDING_STAGE = "bid";
    static final String PLAYING_STAGE = "play";

    static final String CONTRACTOR = "庄";
    static final String PARTNER = "帮";

//    static final int POINT_COLOR = 0xd60e90;
    static final int GREY_COLOR = 0x505050;
    static final int INFO_COLOR = 0xa1ebfc;
    static final int POINT_COLOR = 0x3030ff;
    static final int TIMER_COLOR = 0xff00ff;
    static final int RED_COLOR = 0xff0000;
    static final int BUTTON_COLOR = 0x47b2e8;

//    private final ButtonImage backImage = new ButtonImage(0xbcbcbc);
    static final int TIME_OUT_SECONDS = 25;
    private final String playerId;
    private String playerName;
    private final Form mainForm;
    private final TuoLaJi main;

    private UITimer gameTimer;
    private Runnable notifyPlayer = new Runnable() {
        @Override
        public void run() {
            Display.getInstance().vibrate(1000);  // this works
            gameTimer = null;
        }
    };

    public Player(String playerId, TuoLaJi main) {
        this.playerId = playerId;
        this.mainForm = main.formMain;
        this.main = main;
    }

    public Player(String playerId, String playerName, TuoLaJi main) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.mainForm = main.formMain;
        this.main = main;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    private MySocket mySocket = null;

    static final String actionJoinTable = "join";
    static final String actionExit = "out";
    static final String actionBid = "bid";
    static final String actionSetTrump = "trump";
    static final String actionBuryCards = "bury";
    static final String actionPlayCards = "play";
    static final String actionPartner = "partner";

    public void connectServer(boolean rejoin) {
        if (!Socket.isSupported()) {
            Dialog.show("Alert", "Socket is not supported", "OK", "");
            return;
        }

        main.disableButtons();
        this.mySocket = new MySocket();
        Socket.connect(Card.TLJ_HOST, Card.TLJ_PORT, mySocket);
        if (rejoin) {
            joinTable();
        }
    }

    public void startPlay(String playerName) {
        Log.p(playerName);
        this.playerName = playerName;
        joinTable();
    }

    public void joinTable() {
        if (this.mySocket == null) {
            return;
        }
        this.tableOn = true;
        mySocket.checkConnection = true;
        mySocket.addRequest(actionJoinTable, "\"id\":\"" + this.playerId
                + "\",\"name\":\"" + this.playerName
                + "\",\"lang\":\"" + main.lang
                + "\",\"ver\":\"" + main.version
                + "\"");
    }

    public void disconnect() {
        if (this.mySocket != null) {
            if (this.mySocket.isConnected()) {
                this.mySocket.closeConnection();
                this.mySocket = null;
            }
        }
    }

    private List<Character> candidateTrumps = new ArrayList<>();
    private void addCardsToHand(char suite, List<Object> lst) {
        if (lst == null || lst.isEmpty()) {
            return;
        }
        for (Object d : lst) {
            int rank = parseInteger(d);
            if (rank > 0) {
                hand.addCard(new Card(suite, rank));
                if (rank == this.playerRank || suite == Card.JOKER) {
                    if (!candidateTrumps.contains(suite)) candidateTrumps.add(suite);
                }
            }
        }
    }

    synchronized private void addCards(Map<String, Object> data) {
        addCardsToHand(Card.SPADE, (List<Object>) data.get("S"));
        addCardsToHand(Card.HEART, (List<Object>) data.get("H"));
        addCardsToHand(Card.DIAMOND, (List<Object>) data.get("D"));
        addCardsToHand(Card.CLUB, (List<Object>) data.get("C"));
        addCardsToHand(Card.JOKER, (List<Object>) data.get("T"));
    }

    synchronized private void addRemains(Map<String, Object> data) {
        if (this.hand.isEmpty()) return;
        String cards = trimmedString(data.get("cards"));
        if (cards.isEmpty()) return;
        int x = cards.indexOf(',');
        while (x > 0) {
            String s = cards.substring(0, x);
            hand.addCard(Card.create(s));
            cards = cards.substring(x + 1);
            x = cards.indexOf(',');
        }

        if (!cards.isEmpty()) {
            hand.addCard(Card.create(cards));
        }

        hand.sortCards(currentTrump, playerRank, true);
        int actTime = parseInteger(data.get("acttime"));
        infoLst.get(0).showTimer(actTime, 100, "bury");
    }

    private void buryCards(Map<String, Object> data) {
        if (this.hand.isEmpty()) return;
        String strCards = trimmedString(data.get("cards"));
        if (strCards.isEmpty()) return;
        hand.removeCards(strCards);

        infoLst.get(0).showTimer(this.timeout, 100, "partner");
    }

    private String partnerDef(String def) {
        if(def.isEmpty()) return " ";
        String part = Dict.get(main.lang, "1 vs 5");
        if(def.length() >= 3){
            char seq = def.charAt(2);
            part = Card.suiteSign(def.charAt(0)) + def.charAt(1);
            switch(seq) {
                case '0':
                    part = Dict.get(main.lang, " 1st ") + part;
                    break;
                case '1':
                    part = Dict.get(main.lang, " 2nd ") + part;
                    break;
                case '2':
                    part = Dict.get(main.lang, " 3rd ") + part;
                    break;
                case '3':
                    part = Dict.get(main.lang, " 4th ") + part;
                    break;
            }
            part = Dict.get(main.lang, "Partner") + ":" + part;
        }
        return part;
    }

    private void definePartner(Map<String, Object> data) {
        int seat = parseInteger(data.get("seat"));
        PlayerInfo pp = this.playerMap.get(seat);
        if (pp != null) pp.showTimer(timeout, 0, "play");
        String def = trimmedString(data.get("def"));
        this.partnerInfo.setText(partnerDef(def));
    }

    private boolean isValid(List<Card> cards, UserHelp uh) {
        if (cards.isEmpty()) {
            uh.showHelp(uh.NO_CARD_SELECTED);
            return false;
        }
        if (!hand.validSelection()) {
            uh.showHelp(uh.INVALID_PLAY);
            return false;
        }
        return true;
    }

    public static int parseInteger(Object obj) {
        if (obj == null) return -1;
        try {
            return (int) Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean parseBoolean(Object obj) {
        if (obj == null) return false;
        String r = obj.toString();
        return r.equalsIgnoreCase("yes") || r.equalsIgnoreCase("true");
    }

    public final List<PlayerInfo> infoLst = new ArrayList<>();
    public Map<Integer, PlayerInfo> playerMap = new HashMap<>();
    private boolean tableOn = false;
    private boolean tableEnded = false;
    private int timeout = 30;   // 30 seconds
    public boolean isPlaying = false;
    private int contractPoint = -1;
    private int playerRank;
    private int currentSeat;
    private Hand hand;
    private Label lbGeneral;
    private Label gameInfo;
    private Label partnerInfo;
    private Label pointsInfo;
    private Button bExit;

    private void resetTable() {
        this.tableEnded = false;
        this.hand.setIsReady(false);
        this.hand.clearCards();
        candidateTrumps.clear();
        mainForm.setGlassPane(null);

        isPlaying = false;
        timeout = 30;
        contractPoint = -1;
        playerRank = 0;
        currentSeat = 0;
        gameInfo.setText(" ");
        partnerInfo.setText(" ");
        pointsInfo.setText(" ");
        for (PlayerInfo pp : this.infoLst) {
            pp.reset();
        }
    }

    private void refreshTable(Map<String, Object> data) {
        this.resetTable();

        String stage = trimmedString(data.get("stage"));
        this.isPlaying = stage.equalsIgnoreCase(PLAYING_STAGE);
        currentSeat = parseInteger(data.get("seat"));
        int actionSeat = parseInteger(data.get("next"));
        this.playerRank = parseInteger(data.get("rank"));
        int game = parseInteger(data.get("game"));
        int defaultTimeout = parseInteger(data.get("timeout"));
        if (defaultTimeout > 0) this.timeout = defaultTimeout;

        this.addCards(data);

        PlayerInfo p0 = this.infoLst.get(0);
        p0.setMainInfo(currentSeat, playerName, this.playerRank);
        this.playerMap.put(currentSeat, p0);
        char trumpSuite = Card.JOKER;

        String info = trimmedString(data.get("info"));
        if (info.isEmpty()) {
            this.gameRank = parseInteger(data.get("gameRank"));
            this.contractPoint = parseInteger(data.get("contract"));

            String trump = data.get("trump").toString();
            if (!trump.isEmpty()) trumpSuite = trump.charAt(0);

            if (gameRank > 0) {
                hand.sortCards(trumpSuite, gameRank, true);
            } else {
                hand.sortCards(trumpSuite, playerRank, true);
            }

            if (!this.isPlaying) {
                int minBid = parseInteger(data.get("minBid"));
                if (minBid > 0) p0.addMinBid(minBid);
                displayBidInfo(p0, trimmedString(data.get("bid")));
            } else {
                List<Card> lst = Card.fromString(trimmedString(data.get("cards")), this.currentTrump, this.gameRank);
                if (lst != null) {
                    this.hand.addPlayCards(p0, lst);
                }
                int point1 = parseInteger(data.get("pt1")); // points earned by player itself
                if (point1 != -1) {
                    if (point1 == 0) {
                        p0.contractor.setText("");
                    } else {
                        p0.contractor.setText(point1 + "");
                    }
                }
            }
        } else {
            p0.userHelp.showInfo(info);
            startNotifyTimer(data);
        }

        List<Object> players = (List<Object>) data.get("players");
        for (int i = 0, j = 1; j < Card.TOTAL_SEATS; i++, j++) {
            parsePlayerInfo(this.infoLst.get(j), (Map<String, Object>) players.get(i));
        }

        lbGeneral.setText(main.lang.equalsIgnoreCase("zh") ? "第" + game + "局" : "Game " + game);

        String gmInfo = " ";
        String ptInfo = " ";
        String pointInfo = " ";
        int actTime = parseInteger(data.get("acttime"));
        String act = trimmedString(data.get("act"));
        if (this.isPlaying) {
            p0.needChangeActions = true;
            gmInfo = this.contractPoint + " ";
            if (!act.equals("dim")) {
                if (trumpSuite == Card.JOKER) {
                    gmInfo += "NT ";
                } else {
                    gmInfo += Card.suiteSign(trumpSuite);
                }
                gmInfo += Card.rankToString(gameRank);
            }
            ptInfo = this.partnerDef(trimmedString(data.get("def")));
            int points = parseInteger(data.get("pt0"));
            pointInfo = points + Dict.get(main.lang, " points");
        }
        this.gameInfo.setText(gmInfo);
        this.partnerInfo.setText(ptInfo);
        this.pointsInfo.setText(pointInfo);

        PlayerInfo pp = this.playerMap.get(actionSeat);
        if (pp != null) {
            if (act.equals("bury")) {
                pp.showTimer(actTime, this.contractPoint, "bury");
            } else if (act.isEmpty()) {
                pp.showTimer(this.timeout, this.contractPoint, "bid");
            } else {
                pp.showTimer(this.timeout, this.contractPoint, act);
            }
        }

        if (this.isPlaying) {
            int seatContractor = parseInteger(data.get("seatContractor"));
            pp = this.playerMap.get(seatContractor);
            if (pp != null) pp.setContractor(CONTRACTOR);
            int seatPartner = parseInteger(data.get("seatPartner"));
            pp = this.playerMap.get(seatPartner);
            if (pp != null) {
                if (pp.isContractSide) {
                    pp.setContractor(CONTRACTOR + "," + PARTNER);
                } else {
                    pp.setContractor(PARTNER);
                }
            }

            this.currentTrump = trumpSuite;
        }
        hand.setIsReady(true);
        this.tableOn = true;
        main.enableButtons();
        if (Card.DEBUG_MODE) Log.p("refresh table: done");
    }

    private void startNotifyTimer(Map<String, Object> data) {
        if (gameTimer == null) {
            int pauseSeconds = parseInteger(data.get("pause"));
            if (pauseSeconds - 5 > 0) {
                gameTimer = new UITimer(this.notifyPlayer);
                gameTimer.schedule((pauseSeconds - 5) * 1000, false, mainForm);
            }
        }
    }

    private Command holdCommand(int holdMinutes) {
        String txt = Dict.get(main.lang, "No");
        if (holdMinutes > 0) {
            txt = holdMinutes + Dict.get(main.lang, " minutes");
        }
        return new Command(txt) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                if (mySocket != null) {
                    mySocket.addRequest(actionExit, "\"hold\":" + holdMinutes);
                }
            }
        };
    }

    public void createTable(Container table) {
        this.hand = new Hand(this);

        PlayerInfo p0 = new PlayerInfo("bottom");
        this.infoLst.add(p0);
        this.infoLst.add(new PlayerInfo("right down"));
        this.infoLst.add(new PlayerInfo("right up"));
        this.infoLst.add(new PlayerInfo("top"));
        this.infoLst.add(new PlayerInfo("left up"));
        this.infoLst.add(new PlayerInfo("left down"));

        this.bExit = new Button(Dict.get(main.lang, "Exit"));
        FontImage.setMaterialIcon(bExit, FontImage.MATERIAL_EXIT_TO_APP);

        bExit.setUIID("myExit");
        bExit.addActionListener((e) -> {
            tableOn = false;
            cancelTimers();
            if (!tableEnded) Dialog.show("", Dict.get(main.lang, "Hold Seat") + "?", holdCommand(15), holdCommand(5), holdCommand(0));
            main.switchScene("entry");
        });

        this.lbGeneral = new Label("Game ");
        this.lbGeneral.getStyle().setFont(Hand.fontGeneral);

        String gmInfo = "gmInfo";
        String ptInfo = "ptInfo";
        String pointInfo = "pointInfo";

        this.gameInfo = new Label(gmInfo);
        this.gameInfo.getStyle().setFgColor(0xebef07);
        this.gameInfo.getStyle().setFont(Hand.fontRank);
        this.partnerInfo = new Label(ptInfo);
        this.partnerInfo.getStyle().setFgColor(INFO_COLOR);
        this.partnerInfo.getStyle().setFont(Hand.fontGeneral);

        this.pointsInfo = new Label(pointInfo);
        this.pointsInfo.getStyle().setFgColor(POINT_COLOR);
        this.pointsInfo.getStyle().setFont(Hand.fontRank);

        table.add(hand);
        table.add(bExit).add(this.lbGeneral).add(this.gameInfo).add(this.partnerInfo).add(this.pointsInfo);
        LayeredLayout ll = (LayeredLayout) table.getLayout();
        ll.setInsets(bExit, "0 0 auto auto");   //top right bottom left
        ll.setInsets(this.lbGeneral, "0 auto auto 0")
                .setInsets(this.partnerInfo, "0 0 auto auto")
                .setInsets(this.pointsInfo, "0 auto auto 20%")
                .setInsets(this.gameInfo, "0 auto auto 0");
        ll.setReferenceComponentTop(this.gameInfo, lbGeneral, 1f);
        ll.setReferenceComponentTop(this.partnerInfo, bExit, 1f);

        for (PlayerInfo pp : infoLst) {
            pp.addItems(table);
        }
    }

    public void refreshLang() {
        this.bExit.setText(Dict.get(main.lang, "Exit"));
    }

    private void cancelTimers() {
        for (PlayerInfo pp : infoLst) {
            pp.cancelTimer();
        }
    }

    private void parsePlayerInfo(PlayerInfo pp, Map<String, Object> rawData) {
        int seat = parseInteger(rawData.get("seat"));
        String name = trimmedString(rawData.get("name"));
        if (name.isEmpty()) name = "#" + seat;
        pp.setMainInfo(seat, name, parseInteger(rawData.get("rank")));
        this.playerMap.put(seat, pp);
        if (!this.isPlaying) {
            displayBidInfo(pp, trimmedString(rawData.get("bid")));
        } else {
            List<Card> lst = Card.fromString(trimmedString(rawData.get("cards")), this.currentTrump, this.gameRank);
            if (lst != null) {
                this.hand.addPlayCards(pp, lst);
            }
            int point1 = parseInteger(rawData.get("pt1")); // points earned by player itself
            if (point1 != -1) {
                if (point1 == 0) {
                    pp.contractor.setText("");
                } else {
                    pp.contractor.setText(point1 + "");
                }
            }

        }
    }

    private String bidToString(String bid) {
        if (bid == null || bid.isEmpty() || bid.equals("-")) return "";
        if (bid.equalsIgnoreCase("pass")) {
            return "Pass";
        }

        return "" + parseInteger(bid);
    }

    public PlayerInfo getLeadingPlayer() {
        for (int x = 1; x < this.infoLst.size(); x++) {
            PlayerInfo pp = this.infoLst.get(x);
            if (!pp.cards.isEmpty()) {
                return pp;
            }
        }

        return null;
    }

    private void displayBidInfo(PlayerInfo pp, String bid) {
        pp.showPoints(bidToString(bid));
    }

    private void displayBid(Map<String, Object> data) {
        int seat = parseInteger(data.get("seat"));
        int actionSeat = parseInteger(data.get("next"));
        this.contractPoint = parseInteger(data.get("contract"));   // send contract point every time to avoid error
        String bid = data.get("bid").toString();
        PlayerInfo pp = this.playerMap.get(seat);
        if (pp != null) displayBidInfo(pp, bid);

        pp = this.playerMap.get(actionSeat);
        if (pp != null) {
            boolean bidOver = parseBoolean(data.get("bidOver"));
            int actTime = parseInteger(data.get("acttime"));
            pp.showTimer(actTime > 1 ? actTime : this.timeout, this.contractPoint, bidOver ? "dim" : "bid");
        }
    }

    private void displayCards(PlayerInfo pp, String cards) {
        List<Card> lst = Card.fromString(cards, this.currentTrump, this.gameRank);
        if (lst != null) {
            this.hand.addPlayCards(pp, lst);
            if (pp.location.equals("bottom")) {
                if (!this.hand.isEmpty()) hand.removeCards(cards);
                if (pp.actionButtons != null) {
                    pp.actionButtons.setVisible(false);
                    pp.actionButtons.setEnabled(false);
                    pp.btnPlay.setVisible(false);
                    pp.btnPlay.setEnabled(false);
                }
                pp.userHelp.clear();
            }
        }
        pp.cancelTimer();
    }

    public static String trimmedString(Object obj) {
        if(obj == null) return "";
        return obj.toString().trim();
    }

    private void gameSummary(Map<String, Object> data) {
        if (!this.tableOn) {
            return;
        }

        int points = parseInteger(data.get("pt0"));
        if (points != -1) {
            this.pointsInfo.setText(points + Dict.get(main.lang, " points"));
        } else {
            this.tableEnded = true;
        }
        final String summary = trimmedString(data.get("summary"));
        int seat = parseInteger(data.get("seat"));  // the contractor
        for (PlayerInfo pp : this.infoLst) {
            this.hand.clearPlayCards(pp);
            if (pp.seat == seat) {
                String cards = trimmedString(data.get("hole"));
                List<Card> lst = Card.fromString(cards, this.currentTrump, this.gameRank);
                this.hand.addPlayCards(pp, lst);
            }
        }
        hand.repaint();
        this.infoLst.get(0).needChangeActions = true;

        int x = hand.displayWidth(6) + 15;
        if (!summary.isEmpty()) {
            mainForm.setGlassPane((g, rect) -> {
                g.setColor(INFO_COLOR);
                g.setFont(Hand.fontGeneral);
                int idx = -1;
                int y = this.infoLst.get(2).posY();    // right top player position Y
                String str = summary;
                while (!str.isEmpty()) {
                    idx = str.indexOf("\n");
                    if (idx >= 0) {
                        g.drawString(str.substring(0, idx), x, y);
                    } else {
                        g.drawString(str, x, y);
                        break;
                    }
                    y += Hand.fontGeneral.getHeight();
                    str = str.substring(idx + 1);
                }
            });
        }
    }

    private void showInfo(Map<String, Object> data) {
        final String info = trimmedString(data.get("info"));
        int x = hand.displayWidth(6) + 15;
        if (!info.isEmpty()) {
            mainForm.setGlassPane((g, rect) -> {
                g.setColor(INFO_COLOR);
                g.setFont(Hand.fontGeneral);
                int idx = -1;
                int y = this.infoLst.get(2).posY();
                String str = info;
                while (!str.isEmpty()) {
                    idx = str.indexOf("\n");
                    if (idx >= 0) {
                        g.drawString(str.substring(0, idx), x, y);
                    } else {
                        g.drawString(str, x, y);
                        break;
                    }
                    y += Hand.fontGeneral.getHeight();
                    str = str.substring(idx + 1);
                }
            });
        }
    }

    private void playCards(Map<String, Object> data) {
        int seat = parseInteger(data.get("seat"));
        int actionSeat = parseInteger(data.get("next"));
        int points = parseInteger(data.get("pt0")); // total points by non-contract players
        if (points != -1) {
            this.pointsInfo.setText(points + Dict.get(main.lang, " points"));
        }

        int pointSeat = parseInteger(data.get("pseat"));
        if (pointSeat > 0) {
            PlayerInfo pp = this.playerMap.get(pointSeat);
            if (pp != null && !pp.isContractSide) {
                int point = parseInteger(data.get("pt")); // points earned by player itself
                if (point != -1) {
                    if (point == 0) {
                        pp.contractor.setText("");
                    } else {
                        pp.contractor.setText(point + "");
                    }
                }
            }
        }

        if(seat > 0) {
            String cards = trimmedString(data.get("cards"));
            PlayerInfo pp = this.playerMap.get(seat);
            if (pp != null) {
                displayCards(pp, cards);

                boolean isPartner = parseBoolean(data.get("isPartner"));
                if (isPartner) {
                    if (pp.isContractSide) {
                        pp.setContractor(CONTRACTOR + "," + PARTNER);
                    } else {
                        pp.setContractor(PARTNER);
                    }
                }
                if (!pp.isContractSide) {
                    int point1 = parseInteger(data.get("pt1")); // points earned by player itself
                    if (point1 != -1) {
                        if (point1 == 0) {
                            pp.contractor.setText("");
                        } else {
                            pp.contractor.setText(point1 + "");
                        }
                    }
                }
            }
        } else {
            for (PlayerInfo pp : this.infoLst) {
                this.hand.clearPlayCards(pp);
            }
        }

        if (actionSeat > 0) {
            PlayerInfo pp = this.playerMap.get(actionSeat);
            if (pp != null) {
                int actTime = parseInteger(data.get("acttime"));
                pp.showTimer(actTime > 1 ? actTime : this.timeout, this.contractPoint, "play");
            }
        }
    }

    private char currentTrump;
    private int gameRank;
    synchronized private void setTrump(Map<String, Object> data) {
        if (this.hand.isEmpty()) return;
        String trump = data.get("trump").toString();
        if (trump.isEmpty()) return;
        this.currentTrump = trump.charAt(0);
        int seat = parseInteger(data.get("seat"));
        this.gameRank = parseInteger(data.get("gameRank"));
        int actTime = parseInteger(data.get("acttime"));
        int contractPoint = parseInteger(data.get("contract"));
        this.hand.sortCards(currentTrump, this.gameRank, true);
//        this.hand.repaint();

        for (int st : this.playerMap.keySet()) {
            PlayerInfo pp = this.playerMap.get(st);
            if (st == seat) {
                pp.setContractor(CONTRACTOR);
                if (seat != this.currentSeat) {
                    pp.showTimer(actTime, contractPoint, "bury");
                }
            } else {
                pp.points.setText("");
                pp.needChangeActions = true;
            }
        }

        String gmInfo = contractPoint + " ";
        if (currentTrump == Card.JOKER) {
            gmInfo += "NT ";
        } else {
            gmInfo += Card.suiteSign(currentTrump);
        }
        gmInfo += Card.rankToString(gameRank);
        this.gameInfo.setText(gmInfo);
        this.isPlaying = true;
    }

    static int serverWaitCycle = 10; // 10 times

    class MySocket extends SocketConnection {

        private boolean closeRequested = false;
        private boolean checkConnection = false;
        private List<String> pendingRequests = new ArrayList<>();

        public void closeConnection() {
            this.closeRequested = true;
            if (Card.DEBUG_MODE) Log.p("this.closeRequested: " + this.closeRequested);
        }

        public void setCheckConnection() {
            this.checkConnection = true;
        }

        public void addRequest(String action, String data) {
            String json = "\"action\":\"" + action + "\"";
            if (data != null && !data.isEmpty()) {
                json += "," + data;
            }
            pendingRequests.add("{" + json + "}");
//            Log.p(json);
        }

        private void processReceived(String msg) throws IOException {
//            Log.p("Raw data: " + msg);
//            if (!msg.startsWith("{")) {
//                msg = Card.confusedData(msg);
//                msg = new String(Base64.decode(msg.getBytes()));
//            }

            JSONParser parser = new JSONParser();
            int idx = msg.indexOf("\n");
            while (idx > 0) {
                String subMsg = msg.substring(0, idx);
                msg = msg.substring(idx + 1);
                idx = msg.indexOf("\n");
                if (subMsg.trim().isEmpty()) continue;

                if (!subMsg.startsWith("{")) {
                    subMsg = Card.confusedData(subMsg);
                    subMsg = new String(Base64.decode(subMsg.getBytes()));
                }
//                Log.p("Received: " + subMsg);
                Map<String, Object> data = parser.parseJSON(new StringReader(subMsg));
                final String action = trimmedString(data.get("action"));

                if (!tableOn && !action.equals("info")) {
                    continue;
                }

                switch (action) {
                    case "init":
                        main.switchScene("table");
                        refreshTable(data);
                        break;
                    case "bid":
                        displayBid(data);
                        break;
                    case "set_trump":
                        setTrump(data);
                        break;
                    case "add_remains":
                        addRemains(data);
                        break;
                    case "bury":
                        buryCards(data);
                        break;
                    case "partner":
                        definePartner(data);
                        break;
                    case "play":
                        playCards(data);
                        break;
                    case "gameover":
                        startNotifyTimer(data);
                        cancelTimers();
                        gameSummary(data);
                        break;
                    case "info":
                        showInfo(data);
                        break;
                }
            }
        }

        @Override
        public void connectionError(int errorCode, String message) {
//            if (isConnected()) closeRequested = true;
//            main.enableButtons();
            main.onConnectionError();
            mySocket = null;    // reset connection
//            Dialog.show("Error", message, "OK", "");
            if (tableOn) {
                cancelTimers();
                try {
                    //                tablePane.removeAll();
//                mainForm.setGlassPane(null);
////                mainForm.getContentPane().setVisible(true);
//                mainForm.repaint();
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {

                }
                connectServer(true);
            }
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            main.enableButtons();
            byte[] buffer = new byte[4096];
            int count = 0;
            try {
                if (Card.DEBUG_MODE) Log.p("connected!");
                while (isConnected() && !closeRequested) {
                    if (!pendingRequests.isEmpty()) {
                        String request = pendingRequests.remove(0);
                        if (Card.DEBUG_MODE) {
                            os.write(request.getBytes());
                            Log.p("send request: " + request);
                        } else {
                            request = Base64.encode(request.getBytes());
                            os.write(Card.confusedData(request).getBytes());
                        }
                        if (!checkConnection) {
                            checkConnection = tableOn;
                        }
                    }
                    int n = is.available();
                    if (n > 0) {
                        checkConnection = false;
                        count = 0;
                        String msg = "";
                        while ((n = is.read(buffer, 0, 4096)) > 0) {
                            msg += new String(buffer, 0, n);
                            if (n < 4096) break;
                        }
                        processReceived(msg);
                    } else {
                        if (checkConnection) count++;
                        if (count > serverWaitCycle) {
                            Log.p("lost conncetion!");
                            break;
                        }
                        Thread.sleep(500);
                    }
                }
                os.close();
                is.close();
            } catch (Exception err) {
                Log.p("exception conncetion!");
                err.printStackTrace();
//                Dialog.show("Exception", "Error: " + err.getMessage(), "OK", "");
            }

            if (!closeRequested) {
                // not expected, connect again
                Log.p("re-connect");
                connectServer(true);
            }
        }
    }

    class CountDown implements Runnable {

        PlayerInfo pInfo;
        Label timer;
        int timeout;
        CountDown(PlayerInfo pInfo, int timeout) {
            this.pInfo = pInfo;
            this.timer = pInfo.timer;
            this.timeout = timeout;
        }
        public void run() {
            this.timeout--;
            if (this.timeout > 0) {
                this.timer.setText(this.timeout + "");
            } else {
                this.timer.setText("");
                FontImage.setMaterialIcon(timer, FontImage.MATERIAL_TIMER_OFF);
                if (pInfo.actionButtons != null) {
                    pInfo.actionButtons.setVisible(false);
                    pInfo.actionButtons.setEnabled(false);
                }
                pInfo.countDownTimer.cancel();
                if (mySocket != null) {
                    mySocket.setCheckConnection();
                }
            }
        }
    }

    class PlayerInfo {

        final String location;    // top, bottom, left up, left down, right up, right down
        Label mainInfo;
        UserHelp userHelp;
        Label points;
        Label contractor;   // for contractor and partner
        Label timer;   // count down timer
        List<Card> cards = new ArrayList<>();   // cards played
        String playerName;
        UITimer countDownTimer;
        Component actionButtons;
        Container bidButtons;
        Container passButton;
//        Container playButton;

        Container central;

        Button btnBid;
        Button btnPlus;
        Button btnMinus;
        Button btnPass;
        Button btnPassSingle;
        Button btnPlay;

//        Container parent;

        int seat;
        int rank;

        boolean isContractSide = false;
        boolean isInitial = true;

        PlayerInfo(String loc) {
            this.location = loc;

            mainInfo = new Label(loc);

            points = new Label("        ");
            points.getAllStyles().setFont(Hand.fontRank);

            contractor = new Label("     ");
            contractor.getAllStyles().setFgColor(POINT_COLOR);
            contractor.getAllStyles().setFont(Hand.fontRank);

            timer = new Label("    ");
            timer.getAllStyles().setFgColor(TIMER_COLOR);
            timer.getAllStyles().setFont(Hand.fontRank);
//            timer.setHidden(true, true);    // setHidden Does not work

            if (loc.equals("bottom")) {
                Command commonCmd = new Command("Play") {   // for pass, bury and play
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        final String action = ev.getActualComponent().getName();
                        List<Card> cards;
                        switch (action) {
                            case "pass":
                                mySocket.addRequest(actionBid, "\"bid\":\"pass\"");
                                actionButtons.setVisible(false);
                                actionButtons.setEnabled(false);
                                break;
                            case "bury":
                                cards = hand.getSelectedCards();
                                if (cards.size() != 6) {
                                    btnPlay.setEnabled(false);
                                    return;
                                }
                                userHelp.clear();
                                mySocket.addRequest(action, "\"cards\":\"" + Card.cardsToString(cards) + "\"");
                                btnPlay.setVisible(false);
                                btnPlay.setEnabled(false);
                                break;
                            case "play":
                                cards = hand.getSelectedCards();
                                if (!isValid(cards, userHelp)) {
                                    btnPlay.setEnabled(false);
                                    return;
                                }
                                userHelp.clear();
                                mySocket.addRequest(action, "\"cards\":\"" + Card.cardsToString(cards) + "\"");
                                btnPlay.setVisible(false);
                                btnPlay.setEnabled(false);
                                break;
                        }

                        cancelTimer();
                    }
                };

                btnBid = new Button("200");
                btnPlus = new Button("");
                btnMinus = new Button("");
                btnPass = new Button(commonCmd);
                btnPassSingle = new Button(commonCmd);
                btnPass.setText(Dict.get(main.lang, "Pass"));
                btnPassSingle.setText(Dict.get(main.lang, "Pass"));
                btnPass.setName("pass");
                btnPassSingle.setName("pass");

                FontImage.setMaterialIcon(btnPlus, FontImage.MATERIAL_ARROW_UPWARD);
                FontImage.setMaterialIcon(btnMinus, FontImage.MATERIAL_ARROW_DOWNWARD);

//                btnBid.getAllStyles().setFgColor(BUTTON_COLOR);
                btnBid.getAllStyles().setFont(Hand.fontRank);
//                btnBid.getAllStyles().setBgImage(backImage);
//                btnPass.getAllStyles().setBgImage(backImage);
//                btnPassSingle.getAllStyles().setBgImage(backImage);
                btnBid.getAllStyles().setBgImage(main.back);
                btnPass.getAllStyles().setBgImage(main.back);
                btnPassSingle.getAllStyles().setBgImage(main.back);

                btnBid.addActionListener((e) -> {
                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);

                    cancelTimer();
                    mySocket.addRequest(actionBid, "\"bid\":" + btnBid.getText().trim());
                });

                btnPlus.addActionListener((e) -> {
                    int point = parseInteger(btnBid.getText());
                    if (point < this.maxBid) {
                        point += 5;
                        btnBid.setText("" + point);
                    }
                });
                btnMinus.addActionListener((e) -> {
                    int point = parseInteger(btnBid.getText());
                    if (point > 0) {
                        point -= 5;
                        btnBid.setText("" + point);
                    }
                });

                btnPlay = new Button(commonCmd);
//                btnPlay.setUIID("play");
//                btnPlay.setSize(new Dimension(100, 40)); // does not work
//                btnPlay.getAllStyles().setBgImage(backImage);
                btnPlay.getAllStyles().setBgImage(main.back);
                btnPlay.getAllStyles().setAlignment(Component.CENTER);

                bidButtons = BoxLayout.encloseXNoGrow(btnPlus, btnBid, btnMinus, btnPass);
                bidButtons.getAllStyles().setAlignment(Component.CENTER);
                passButton = BoxLayout.encloseXNoGrow(btnPassSingle);
                passButton.getAllStyles().setAlignment(Component.CENTER);
//                playButton = BoxLayout.encloseXNoGrow(btnPlay);
//                playButton = BoxLayout.encloseX(btnPlay);
//                playButton.getAllStyles().setAlignment(Component.CENTER);
                actionButtons = bidButtons;

                userHelp = new UserHelp(main.lang);
                central = new Container(new BoxLayout(BoxLayout.Y_AXIS));
                central.getAllStyles().setAlignment(Component.CENTER);
                timer.getAllStyles().setAlignment(Component.CENTER);
                userHelp.getAllStyles().setAlignment(Component.CENTER);
                central.add(userHelp).add(timer).add(actionButtons);
            }
        }

        int posX() {
            return mainInfo.getAbsoluteX();
        }

        int posY() {
            return mainInfo.getAbsoluteY() + mainInfo.getHeight();
        }

        int posY0() {
            return mainInfo.getAbsoluteY();
        }

        synchronized void reset() {
            cancelTimer();
            contractor.getAllStyles().setFgColor(POINT_COLOR);
            contractor.setText("");
            points.setText("");
            hand.clearPlayCards(this);
            this.isContractSide = false;
            if (location.equals("bottom")) {
                if (!isInitial) {
                    needChangeActions = true;
                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);
                } else {
                    isInitial = false;
                }
                userHelp.clear();
                userHelp.setLanguage(main.lang);
                btnPass.setText(Dict.get(main.lang, "Pass"));
            }
        }

        synchronized void addItems(Container pane) {
//            parent = pane;
            if (this.location.equals("bottom")) {
                pane.add(mainInfo).add(points).add(contractor);
            } else {
                pane.add(mainInfo).add(points).add(timer).add(contractor);
            }

            LayeredLayout ll = (LayeredLayout) pane.getLayout();

            switch (this.location) {
                case "left up":
                    ll.setInsets(mainInfo, "15% auto auto 0");  //top right bottom left
                    ll.setInsets(points, "0 auto auto 20")
                            .setInsets(timer, "0 auto auto 20")
                            .setInsets(contractor, "14% auto auto 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "left down":
                    ll.setInsets(mainInfo, "35% auto auto 0");
                    ll.setInsets(points, "0 auto auto 20")
                            .setInsets(timer, "0 auto auto 20")
                            .setInsets(contractor, "34% auto auto 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "right up":
                    ll.setInsets(mainInfo, "15% 0 auto auto");
                    ll.setInsets(points, "0 20 auto auto")
                            .setInsets(timer, "0 20 auto auto")
                            .setInsets(contractor, "14% 20 auto auto");
                    ll.setReferenceComponentRight(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "right down":
                    ll.setInsets(mainInfo, "35% 0 auto auto");
                    ll.setInsets(points, "0 20 auto auto")
                            .setInsets(timer, "0 20 auto auto")
                            .setInsets(contractor, "34% 20 auto auto");
                    ll.setReferenceComponentRight(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "top":
                    ll.setInsets(mainInfo, "0 auto auto auto");
                    ll.setInsets(points, "0 auto auto auto")
                            .setInsets(contractor, "0 auto auto 20")
                            .setInsets(timer, "0 auto auto auto");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f);
                    break;
                case "bottom":
//                    pane.add(actionButtons).add(userHelp);
//                    ll.setInsets(actionButtons, "auto auto 33% auto");
                    pane.add(central);
                    ll.setInsets(central, "auto auto 33% auto");
                    pane.add(btnPlay);
                    ll.setInsets(btnPlay, "auto auto 33% auto");
                    btnPlay.setVisible(false);

//                    ll.setInsets(userHelp, "auto auto 0 auto");
                    ll.setInsets(mainInfo, "auto auto 0 auto");
                    ll.setInsets(points, "auto auto 35% auto")
                            //                            .setInsets(timer, "auto auto 0 auto")
                            .setInsets(contractor, "auto auto 0 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f);
//                            .setReferenceComponentBottom(timer, actionButtons, 1f)
//                            .setReferenceComponentBottom(userHelp, timer, 1f);

                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);
                    break;
            }

        }

        void setMainInfo(int seat, String playerName, int rank) {
            this.seat = seat;
            this.rank = rank;
            this.playerName = playerName;
            String info = playerName + "," + Card.rankToString(rank, "R");
            this.mainInfo.setText(info);
        }

        void addMinBid(int minBid) {
            String info = this.mainInfo.getText();
            this.mainInfo.setText( info + ", " + minBid);
        }

        void cancelTimer() {
            if (countDownTimer != null) countDownTimer.cancel();
//            this.timer.setHidden(true, true); // setHidden does not work well
//            this.timer.setText("");
//            FontImage.setMaterialIcon(timer, '0');  // hide it
            this.timer.setVisible(false);
        }

        synchronized void showPoints(String point) {
            cancelTimer();
            this.points.setText(point);
            if (point.equalsIgnoreCase("pass")) {
                this.points.getStyle().setFgColor(GREY_COLOR);
            } else {
                this.points.getStyle().setFgColor(POINT_COLOR);
            }

            if (this.actionButtons != null) {
                this.actionButtons.setVisible(false);
                this.actionButtons.setEnabled(false);
            }
        }

        int maxBid = -1;
        boolean needChangeActions=false;
        synchronized void showTimer(int timeout, int contractPoint, String act) {
            cancelTimer();  // cancel the running timer if any
            if (act.equals("dim")) {
                this.setContractor(CONTRACTOR);
                needChangeActions = true;
            }

            this.points.setText("");
            this.timer.setText(timeout + "");
            FontImage.setMaterialIcon(timer, FontImage.MATERIAL_TIMER);
            this.timer.setVisible(true);
            countDownTimer = new UITimer(new CountDown(this, timeout));
            countDownTimer.schedule(950, true, mainForm);   // slightly less to 1 sec

            if (this.location.equals("bottom")) {
                userHelp.clear();
//                if (Display.getInstance().isBuiltinSoundAvailable(Display.SOUND_TYPE_ALARM)) {
//                    Display.getInstance().playBuiltinSound(Display.SOUND_TYPE_ALARM);
//                }

                if (act.equals("dim")) {
                    actionButtons.setVisible(true);
                    actionButtons.setEnabled(true);
                    userHelp.showHelp(userHelp.SET_TRUMP);
                    Container buttons = new Container(new BoxLayout(BoxLayout.X_AXIS_NO_GROW));
                    for (char c : candidateTrumps) {
                        Button btn = new Button();
                        if (c == Card.JOKER) {
                            btn.setText(Dict.get(main.lang, "NT"));
                        } else {
                            btn.setText(Card.suiteSign(c));
                        }
                        buttons.add(btn);
                        btn.addActionListener((e) -> {
                            actionButtons.setVisible(false);
                            actionButtons.setEnabled(false);

                            cancelTimer();
                            mySocket.addRequest(actionSetTrump, "\"trump\":\"" + c + "\"");
                        });
                    }

                    central.replaceAndWait(actionButtons, buttons, null);
                    actionButtons = buttons;
                    needChangeActions = true;
                } else if (act.equals("bid")) {
                    actionButtons.setVisible(true);
                    actionButtons.setEnabled(true);
//                    if (needChangeActions) {
                        if (candidateTrumps.isEmpty()) {
//                            if (actionButtons != passButton) {
                                central.replaceAndWait(actionButtons, passButton, null);
                                actionButtons = passButton;
//                            }
                        } else {
//                            if (actionButtons != bidButtons) {
                                central.replaceAndWait(actionButtons, bidButtons, null);
                                actionButtons = bidButtons;
//                            }
                        }

                        needChangeActions = false;
//                    }
                    this.maxBid = contractPoint - 5;
                    btnBid.setText("" + this.maxBid);
                } else if (act.equals("bury")) {
                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);
                    userHelp.showHelp(userHelp.BURY_CARDS);
//                    central.replaceAndWait(actionButtons, playButton, null);
//                    actionButtons = playButton;
                    btnPlay.setName("bury");
                    btnPlay.setText(Dict.get(main.lang, "Bury"));
                    needChangeActions = true;
                    btnPlay.setVisible(true);
                    btnPlay.setEnabled(true);
                } else if (act.equals("partner")) {
                    actionButtons.setVisible(true);
                    actionButtons.setEnabled(true);
                    userHelp.showHelp(userHelp.SET_PARTNER);

                    Container buttons = new Container(new BoxLayout(BoxLayout.X_AXIS_NO_GROW));
                    RadioButton rb1 = new RadioButton(Dict.get(main.lang, "1st"));
                    RadioButton rb2 = new RadioButton(Dict.get(main.lang, "2nd"));
                    RadioButton rb3 = new RadioButton(Dict.get(main.lang, "3rd"));
                    RadioButton rb4 = new RadioButton(Dict.get(main.lang, "4th"));
                    ButtonGroup btnGroup = new ButtonGroup(rb1,rb2,rb3,rb4);
                    buttons.addAll(rb1, rb2, rb3, rb4);
                    String rnk = Card.rankToString(playerRank);
                    rnk = rnk.equals("A") ? "K" : "A";
                    addCardButton(buttons, Card.SPADE, rnk, btnGroup);
                    addCardButton(buttons, Card.HEART, rnk, btnGroup);
                    addCardButton(buttons, Card.DIAMOND, rnk, btnGroup);
                    addCardButton(buttons, Card.CLUB, rnk, btnGroup);
                    Button btn = new Button(Dict.get(main.lang, "1vs5"));
                    btn.setCapsText(false);
                    btn.addActionListener((e)->{
                        actionButtons.setVisible(false);
                        actionButtons.setEnabled(false);
                        cancelTimer();
                        mySocket.addRequest(actionPartner, "\"def\":\"0\"");
                    });
                    buttons.add(new Label("   ")).add(btn);
                    central.replaceAndWait(actionButtons, buttons, null);
                    actionButtons = buttons;
                    needChangeActions = true;
                } else if(act.equals("play")){
                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);
//                    if(needChangeActions) {
                        btnPlay.setName("play");
                        btnPlay.setText(Dict.get(main.lang, Dict.PLAY));
//                    if (actionButtons != playButton) {
//                        central.replaceAndWait(actionButtons, playButton, null);
//                        actionButtons = playButton;
//                    }
                    needChangeActions = false;
//                    }
                    btnPlay.setVisible(true);
                    btnPlay.setEnabled(true);
//                    btnPlay.setSize(new Dimension(50, 10)); // not work
//                    }
                } else {
                    // not supported
                    Log.p("Unknown act: " + act);
                }
            }
        }

        private void addCardButton(Container buttons, char suite, String rnk, ButtonGroup btnGroup) {
            if(suite != currentTrump) {
                Button btn = new Button(Card.suiteSign(suite) + rnk, "suite" + suite);
                buttons.add(new Label("   "));
                buttons.add(btn);
                btn.addActionListener((e)->{
                    if(!btnGroup.isSelected()) {
                        Dialog.show("Alert", "请指定第几个");
                    } else {
                        actionButtons.setVisible(false);
                        actionButtons.setEnabled(false);
                        cancelTimer();
                        mySocket.addRequest(actionPartner,
                                "\"def\":\"" + suite+rnk+btnGroup.getSelectedIndex() + "\"");
                    }
                });
            }
        }

        void setContractor(String txt) {
            // txt could be Contractor or Partner
            this.points.setText("");
            this.contractor.getAllStyles().setFgColor(RED_COLOR);
            this.contractor.setText(txt);
            this.isContractSide = true;
        }
    }

    class UserHelp extends Container {
        Label engLabel = new Label();
        Label chnLabel = new Label();

        final int SET_TRUMP = 10;
        final int BURY_CARDS = 20;
        final int SET_PARTNER = 25;
        final int PLAY_PAIR = 31;
        final int PLAY_TRIPS = 32;
        final int PLAY_TRACTOR = 33;
        final int PLAY_SAME_SUIT = 35;
        final int NO_CARD_SELECTED = 30;
        final int INVALID_PLAY = 99;

        String curLang;
        UserHelp(String lang) {
            chnLabel.getAllStyles().setAlignment(Component.CENTER);
            engLabel.getAllStyles().setAlignment(Component.CENTER);
            this.setLayout(new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST));
            if (lang.equalsIgnoreCase("zh")) {
                this.add(chnLabel);
            } else {
                this.add(engLabel);
            }
            curLang = lang;
        }

        void setLanguage(String lang) {
            if (curLang.equalsIgnoreCase(lang)) {
                return;
            }
            this.removeAll();
            if (lang.equalsIgnoreCase("zh")) {
                this.add(chnLabel);
            } else {
                this.add(engLabel);
            }
            curLang = lang;
        }

        void clear() {
            engLabel.setText("");
            chnLabel.setText("");
        }

        void showInfo(String info) {
            if (curLang.equalsIgnoreCase("zh")) {
                chnLabel.setText(info);
            } else {
                engLabel.setText(info);
            }
            Log.p(info);
            mainForm.repaint();
        }

        void showHelp(int category) {
            switch (category) {
                case SET_TRUMP:
                    engLabel.setText("Set Trump");
                    chnLabel.setText("请选将牌");
                    break;
                case BURY_CARDS:
                    engLabel.setText("Please select exactly six cards");
                    chnLabel.setText("请选择6张底牌");
                    break;
                case SET_PARTNER:
                    engLabel.setText("Who plays this card will be your partner");
                    chnLabel.setText("找朋友(需指定第几张，包括自己)");
                    break;
                case NO_CARD_SELECTED:
                    engLabel.setText("Please select card(s) to play");
                    chnLabel.setText("请先选定要出的牌");
                    break;
                case INVALID_PLAY:
                    engLabel.setText("Invalid play");
                    chnLabel.setText("非法出牌");
                    break;
                case PLAY_SAME_SUIT:
                    engLabel.setText("Must play same suite");
                    chnLabel.setText("必须出相同花色");
                    break;
                case PLAY_PAIR:
                    engLabel.setText("Must play pair");
                    chnLabel.setText("必须出对");
                    break;
                case PLAY_TRIPS:
                    engLabel.setText("Must play trips");
                    chnLabel.setText("必须出三张");
                    break;
                case PLAY_TRACTOR:
                    engLabel.setText("Must play connected pairs");
                    chnLabel.setText("必须出拖拉机");
                    break;
            }
        }
    }

    class ButtonImage extends DynamicImage {
        int bgColor = 0x00ffff;

        ButtonImage(int bgColor) {
            this.bgColor = bgColor;
        }

        @Override
        protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
//            Log.p("x,y,w,h: " + x + "," + y + "," + w + "," + h);
            g.setColor(this.bgColor);
            g.fillRoundRect(x, y, w, h, 60, 60);
        }
    }
}
