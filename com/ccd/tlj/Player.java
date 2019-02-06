package com.ccd.tlj;

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.DynamicImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.RadioButton;
import com.codename1.ui.geom.Rectangle;
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

    static final String TLJ_HOST = TuoLaJi.DEBUG_MODE ? "172.16.107.204" : "tlj.webhop.me";
    static final String BIDDING_STAGE = "bid";
    static final String PLAYING_STAGE = "play";

    static final String CONTRACTOR = "庄";
    static final String PARTNER = "帮";

    static final int TLJ_PORT = 6688;
//    static final int POINT_COLOR = 0xd60e90;
    static final int GREY_COLOR = 0x505050;
    static final int INFO_COLOR = 0xa1ebfc;
    static final int POINT_COLOR = 0x3030ff;
    static final int TIMER_COLOR = 0xff00ff;
    static final int RED_COLOR = 0xff0000;
    static final int BUTTON_COLOR = 0x47b2e8;

    private final ButtonImage backImage = new ButtonImage(0xbcbcbc);
    static final int TIME_OUT_SECONDS = 15;
    private final String playerId;
    private String playerName;
    private final Form mainForm;
    private final TuoLaJi main;

    public Player(String playerId, String playerName, TuoLaJi main) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.mainForm = main.formMain;
        this.main = main;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public static String rankToString(int rank) {
        if (rank <= 10) return "R" + rank;
        switch (rank) {
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            case 14:
                return "A";
        }

        String s = "A";
        while (rank > 14) {
            s += "+";
            rank--;
        }
        return s;
    }

    private MySocket mySocket = null;

    static final String actionJoinTable = "join";
    static final String actionBid = "bid";
    static final String actionSetTrump = "trump";
    static final String actionBuryCards = "bury";
    static final String actionPlayCards = "play";
    static final String actionPartner = "partner";

    public void connectServer() {
        if (!Socket.isSupported()) {
            Dialog.show("Alert", "Socket is not supported", "OK", "");
            return;
        }

        this.mySocket = new MySocket();
        mySocket.addRequest(actionJoinTable, "\"id\":\"" + this.playerId + "\"");
        Socket.connect(TLJ_HOST, TLJ_PORT, mySocket);
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
        if (lst.isEmpty()) return;
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
        String strCards = trimmedString(data.get("cards"));
        if (strCards.isEmpty()) return;
        hand.removeCards(strCards);

        infoLst.get(0).showTimer(this.timeout, 100, "partner");
    }

    private String partnerDef(String def) {
        if(def.isEmpty()) return " ";
        String part = "1 vs 5";
        if(def.length() >= 3){
            char seq = def.charAt(2);
            part = Card.suiteSign(def.charAt(0)) + def.charAt(1);
            switch(seq) {
                case '0':
                    part = " 1st " + part;
                    break;
                case '1':
                    part = " 2nd " + part;
                    break;
                case '2':
                    part = " 3rd " + part;
                    break;
                case '3':
                    part = " 4th " + part;
                    break;
            }
            part = "Parter:" + part;
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
    private int timeout = 30;   // 30 seconds
    public boolean isPlaying = false;
    private int contractPoint = -1;
    private int playerRank;
    private int currentSeat;
    private Hand hand;
    private Label gameInfo;
    private Label partnerInfo;
    private Label pointsInfo;
    private Container tablePane;

    private void showTable(Map<String, Object> data) {
        mainForm.getContentPane().setVisible(false);
        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 01");
        tablePane = mainForm.getFormLayeredPane(mainForm.getClass(), true);
        tablePane.setLayout(new LayeredLayout());
        if (this.tableOn) {
            tablePane.removeAll();
        }

        infoLst.clear();

        String stage = data.get("stage").toString();
        this.isPlaying = stage.equalsIgnoreCase(PLAYING_STAGE);
        currentSeat = parseInteger(data.get("seat"));
        int actionSeat = parseInteger(data.get("next"));
        this.playerRank = parseInteger(data.get("rank"));
        int game = parseInteger(data.get("game"));
        int gameRank = parseInteger(data.get("gameRank"));
        this.contractPoint = parseInteger(data.get("contract"));
        int defaultTimeout = parseInteger(data.get("timeout"));
        if (defaultTimeout > 0) this.timeout = defaultTimeout;

        this.hand = new Hand(this);
        candidateTrumps.clear();
        this.addCards(data);

        char trumpSuite = Card.JOKER;
        String trump = data.get("trump").toString();
        if (!trump.isEmpty()) trumpSuite = trump.charAt(0);
        
        if(gameRank>0) {
            hand.sortCards(trumpSuite, gameRank, true);
        } else {
            hand.sortCards(trumpSuite, playerRank, true);
        }

        PlayerInfo p0 = new PlayerInfo("bottom", currentSeat, playerRank);
        this.infoLst.add(p0);
        this.playerMap.put(currentSeat, p0);
        p0.setPlayerName(playerName);
        if (!this.isPlaying) {
            int minBid = parseInteger(data.get("minBid"));
            if (minBid > 0) p0.addMidBid(minBid);
            displayBidInfo(p0, trimmedString(data.get("bid")));
        } else {
            List<Card> lst = Card.fromString(trimmedString(data.get("cards")));
            if (lst != null) {
                p0.cards.addAll(lst);
            }
            int point1 = parseInteger(data.get("pt1")); // points earned by player itself
            if (point1 != -1) {
                if (point1 == 0) {
                    p0.contractor.setText("");
                } else {
                    p0.contractor.setText(point1 + "分");
                }
            }
        }

        Button bExit = new Button("Exit");
        FontImage.setMaterialIcon(bExit, FontImage.MATERIAL_EXIT_TO_APP);
        bExit.setUIID("myExit");
        bExit.addActionListener((e) -> {
            this.tableOn = false;
            cancelTimers();
            tablePane.removeAll();
            mainForm.setGlassPane(null);
            mainForm.getContentPane().setVisible(true);
            mainForm.repaint();
            disconnect();
        });

        List<Object> players = (List<Object>) data.get("players");
        Map<String, Object> pR1 = (Map<String, Object>) players.get(0);
        Map<String, Object> pR2 = (Map<String, Object>) players.get(1);
        Map<String, Object> pTop = (Map<String, Object>) players.get(2);
        Map<String, Object> pL2 = (Map<String, Object>) players.get(3);
        Map<String, Object> pL1 = (Map<String, Object>) players.get(4);

        parsePlayerInfo(pR1, "right down");
        parsePlayerInfo(pR2, "right up");
        parsePlayerInfo(pL2, "left up");
        parsePlayerInfo(pL1, "left down");
        parsePlayerInfo(pTop, "top");

        Label lbGeneral = new Label("Game " + game);
        lbGeneral.getStyle().setFont(Hand.fontGeneral);
//        String gmInfo = "205 NT 2; Partner: 1st CA";  // sample
        String gmInfo = " ";
        String ptInfo = " ";
        String pointInfo = " ";
        int actTime = parseInteger(data.get("acttime"));
        String act = trimmedString(data.get("act"));
        if (this.isPlaying) {
            gmInfo = this.contractPoint + " ";
            if (!act.equals("dim")) {
                if (trumpSuite == Card.JOKER) {
                    gmInfo += "NT ";
                } else {
                    gmInfo += Card.suiteSign(trumpSuite);
                }
                gmInfo += gameRank;
            }
            ptInfo = this.partnerDef(trimmedString(data.get("def")));
            int points = parseInteger(data.get("pt0"));
            pointInfo = points + "分";
        }
        this.gameInfo = new Label(gmInfo);
        this.gameInfo.getStyle().setFgColor(0xebef07);
        this.gameInfo.getStyle().setFont(Hand.fontRank);
        this.partnerInfo = new Label(ptInfo);
        this.partnerInfo.getStyle().setFgColor(INFO_COLOR);
        this.partnerInfo.getStyle().setFont(Hand.fontGeneral);

        this.pointsInfo = new Label(pointInfo);
        this.pointsInfo.getStyle().setFgColor(POINT_COLOR);
        this.pointsInfo.getStyle().setFont(Hand.fontRank);

        tablePane.add(hand);
        tablePane.add(bExit).add(lbGeneral).add(this.gameInfo).add(this.partnerInfo).add(this.pointsInfo);
        LayeredLayout ll = (LayeredLayout) tablePane.getLayout();
        ll.setInsets(bExit, "0 0 auto auto");   //top right bottom left
        ll.setInsets(lbGeneral, "0 auto auto 0")
                .setInsets(this.partnerInfo, "0 0 auto auto")
                .setInsets(this.pointsInfo, "0 auto auto 25%")
                .setInsets(this.gameInfo, "0 auto auto 0");
        ll.setReferenceComponentTop(this.gameInfo, lbGeneral, 1f);
        ll.setReferenceComponentTop(this.partnerInfo, bExit, 1f);

        for (PlayerInfo info : infoLst) {
            info.addItems(tablePane);
        }

        PlayerInfo pp = this.playerMap.get(actionSeat);
        if (pp != null) {
            if (act.equals("bury")) {
                pp.showTimer(actTime, this.contractPoint, "bury");
            } else if (act.isEmpty()){
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
//        mainForm.setGlassPane((g, rect) -> {
//            int x0 = lbL1Player.getAbsoluteX() + 5;
//            int y0 = lbL1Player.getAbsoluteY() + 50;
//            g.setColor(0);
//            g.drawRoundRect(x0 - 1, y0 - 1, 50, 80, 10, 10);
//        });
        mainForm.repaint();
        this.tableOn = true;
        main.enableButtons();

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: done");
    }

    private void cancelTimers() {
        for (PlayerInfo pp : infoLst) {
            pp.cancelTimer();
        }
    }

    private void parsePlayerInfo(Map<String, Object> rawData, String location) {
        int seat = parseInteger(rawData.get("seat"));
        PlayerInfo pp = new PlayerInfo(location, seat, parseInteger(rawData.get("rank")));
        if (!this.isPlaying) {
            displayBidInfo(pp, trimmedString(rawData.get("bid")));
        } else {
            List<Card> lst = Card.fromString(trimmedString(rawData.get("cards")));
            if (lst != null) {
                pp.cards.addAll(lst);
            }
            int point1 = parseInteger(rawData.get("pt1")); // points earned by player itself
            if (point1 != -1) {
                if (point1 == 0) {
                    pp.contractor.setText("");
                } else {
                    pp.contractor.setText(point1 + "分");
                }
            }

        }
        this.infoLst.add(pp);
        this.playerMap.put(seat, pp);
    }

    private String bidToString(String bid) {
        if (bid == null || bid.isEmpty() || bid.equals("-")) return "";
        if (bid.equalsIgnoreCase("pass")) return "Pass";
        return "" + parseInteger(bid);
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
        List<Card> lst = Card.fromString(cards);
        if (lst != null) {
            pp.cards.addAll(lst);
            if (pp.location.equals("bottom")) {
                hand.removeCards(cards);
                if (pp.actionButtons != null) {
                    pp.actionButtons.setVisible(false);
                    pp.actionButtons.setEnabled(false);
                }
            }
        }
        pp.cancelTimer();
    }
    
    public static String trimmedString(Object obj) {
        if(obj == null) return "";
        return obj.toString().trim();
    }
    
    private void playCards(Map<String, Object> data) {
        int seat = parseInteger(data.get("seat"));
        int actionSeat = parseInteger(data.get("next"));
        
        if(seat > 0) {
            String cards = trimmedString(data.get("cards"));
            PlayerInfo pp = this.playerMap.get(seat);
            if (pp != null) {
                displayCards(pp, cards);
                int points = parseInteger(data.get("pt0")); // total points by non-contract players
                if (points != -1) {
                    this.pointsInfo.setText(points + "分");
                }

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
                        pp.contractor.setText(point1 + "分");
                    }
                }
            }
        } else {
            for (PlayerInfo pp : this.infoLst) {
                pp.cards.clear();
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
    synchronized private void setTrump(Map<String, Object> data) {
        String trump = data.get("trump").toString();
        if (trump.isEmpty()) return;
        this.currentTrump = trump.charAt(0);
        int seat = parseInteger(data.get("seat"));
        int gameRank = parseInteger(data.get("gameRank"));
        int actTime = parseInteger(data.get("acttime"));
        int contractPoint = parseInteger(data.get("contract"));
        this.hand.sortCards(currentTrump, gameRank, true);
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
        gmInfo += gameRank;
        this.gameInfo.setText(gmInfo);
        this.isPlaying = true;
    }

    Painter getPainter(String loc) {
        Painter p = (Graphics g, Rectangle rect) -> {
            String a = loc;
        };

        return p;
    }

    public static String confusedData(String data) {
        data = data.replace('j', '#');
        data = data.replace('L', 'j');
        data = data.replace('T', 'L');
        data = data.replace('#', 'T');
        return data;
    }

    static int serverWaitCycle = 10; // 10 times
    class MySocket extends SocketConnection {

        private boolean closeRequested = false;
        private boolean checkConnection = false;
        private List<String> pendingRequests = new ArrayList<>();

        public void closeConnection() {
            this.closeRequested = true;
            if (TuoLaJi.DEBUG_MODE) Log.p("this.closeRequested: " + this.closeRequested);
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
        }

        private void processReceived(String msg) throws IOException {
            if(!msg.startsWith("{")) {
                msg = confusedData(msg);
                msg = new String(Base64.decode(msg.getBytes()));
            }
//            if (TuoLaJi.DEBUG_MODE)
                Log.p("Received: " + msg);
            JSONParser parser = new JSONParser();
            int idx = msg.indexOf("\n");
            while (idx > 0) {
                String subMsg = msg.substring(0, idx);
                msg = msg.substring(idx + 1);
                idx = msg.indexOf("\n");
                Map<String, Object> data = parser.parseJSON(new StringReader(subMsg));
                final String action = data.get("action").toString();

                switch (action) {
                    case "init":
                        showTable(data);
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
                }
            }
        }

        @Override
        public void connectionError(int errorCode, String message) {
//            if (isConnected()) closeRequested = true;
            main.enableButtons();
            Dialog.show("Error", message, "OK", "");
            if (tableOn) {
                cancelTimers();
//                tablePane.removeAll();
//                mainForm.setGlassPane(null);
////                mainForm.getContentPane().setVisible(true);
//                mainForm.repaint();
            }
            mySocket = null;    // reset connection
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            byte[] buffer = new byte[4096];
            int count = 0;
            try {
                if (TuoLaJi.DEBUG_MODE) Log.p("connected!");
                while (isConnected() && !closeRequested) {
                    if (!pendingRequests.isEmpty()) {
                        String request = pendingRequests.remove(0);
                        if (TuoLaJi.DEBUG_MODE) {
                            os.write(request.getBytes());
                            Log.p("send request: " + request);
                        } else {
                            request = Base64.encode(request.getBytes());
                            os.write(confusedData(request).getBytes());
                        }
                        checkConnection = true;
                    }
                    int n = is.available();
                    if (n > 0) {
                        checkConnection = false;
                        count = 0;
                        n = is.read(buffer, 0, 4096);
                        if (n < 0) break;
                        processReceived(new String(buffer, 0, n));
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
                err.printStackTrace();
                Dialog.show("Exception", "Error: " + err.getMessage(), "OK", "");
            }

            if (!closeRequested) {
                // not expected, connect again
                connectServer();
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
                if (mySocket != null) mySocket.setCheckConnection();
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
        Container actionButtons;
        Button btnBid;
        Button btnPlus;
        Button btnMinus;
        Button btnPass;
        Button btnPlay;

        int seat;
        int rank;

        boolean isContractSide = false;

        PlayerInfo(String loc, int seat, int rank) {
//            this.cards.add(new Card(Card.JOKER, Card.BigJokerRank));
//            this.cards.add(new Card(Card.JOKER, Card.SmallJokerRank));
//            this.cards.add(new Card('H', 8));
//            this.cards.add(new Card('H', 8));
//            this.cards.add(new Card('H', 7));
//            this.cards.add(new Card('H', 7));
//            this.cards.add(new Card('H', 6));
//            this.cards.add(new Card('H', 6));
//            this.cards.add(new Card('H', 5));
//            this.cards.add(new Card('H', 5));
            this.location = loc;
            this.seat = seat;
            this.rank = rank;
            String info = "#" + seat + "," + rankToString(rank);
            mainInfo = new Label(info);
            userHelp = new UserHelp();

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
                btnBid = new Button("200");
                btnPlus = new Button("");
                btnMinus = new Button("");
                btnPass = new Button("Pass");

                FontImage.setMaterialIcon(btnPlus, FontImage.MATERIAL_ARROW_UPWARD);
                FontImage.setMaterialIcon(btnMinus, FontImage.MATERIAL_ARROW_DOWNWARD);

///
//                btnBid.getAllStyles().setFgColor(BUTTON_COLOR);
                btnBid.getAllStyles().setFont(Hand.fontRank);
                btnBid.getAllStyles().setBgImage(backImage);
                btnPass.getAllStyles().setBgImage(backImage);

                btnBid.addActionListener((e) -> {
                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);

                    cancelTimer();
                    mySocket.addRequest(actionBid, "\"bid\":" + btnBid.getText().trim());
                });
                btnPass.addActionListener((e) -> {
                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);
                    cancelTimer();
                    mySocket.addRequest(actionBid, "\"bid\":\"pass\"");
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

                btnPlay = new Button("Play");
                btnPlay.getAllStyles().setBgImage(backImage);
                btnPlay.addActionListener((e) -> {
                    String action = btnPlay.getText().toLowerCase();
                    if (action.contains("bury")) action = "bury";
                    else if (action.contains("play")) action = "play";
                    List<Card> cards = hand.getSelectedCards();
                    if (action.contains("bury") && cards.size() != 6) {
                        userHelp.showHelp(userHelp.BURY_CARDS);
                        return;
                    }
                    if (action.equals("play")) {
                        if (!isValid(cards, userHelp)) {
                            return;
                        }
                    }
                    userHelp.clear();
                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);
                    cancelTimer();
                    mySocket.addRequest(action, "\"cards\":\"" + Card.cardsToString(cards) + "\"");
                });

                if (isPlaying) {
                    actionButtons = BoxLayout.encloseXNoGrow(btnPlay);
                } else {
                    if (candidateTrumps.isEmpty()) {
                        actionButtons = BoxLayout.encloseXNoGrow(btnPass);
                    } else {
                        actionButtons = BoxLayout.encloseXNoGrow(btnPlus, btnBid, btnMinus, btnPass);
                    }
                }
            }
        }

        int posX() {
            return mainInfo.getAbsoluteX();
        }

        int posY() {
            return mainInfo.getAbsoluteY() + mainInfo.getHeight();
        }

        void addItems(Container pane) {
            pane.add(mainInfo).add(points).add(timer).add(contractor);
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
                    pane.add(actionButtons).add(userHelp);
                    ll.setInsets(actionButtons, "auto auto 33% auto");

                    ll.setInsets(userHelp, "auto auto 0 auto");
                    ll.setInsets(mainInfo, "auto auto 0 auto");
                    ll.setInsets(points, "auto auto 35% auto")
                            .setInsets(timer, "auto auto 0 auto")
                            .setInsets(contractor, "auto auto 0 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentBottom(timer, actionButtons, 1f)
                            .setReferenceComponentBottom(userHelp, timer, 1f);

                    actionButtons.setVisible(false);
                    actionButtons.setEnabled(false);
                    break;
            }

        }

        void setPlayerName(String playerName) {
            this.playerName = playerName;
            String info = this.mainInfo.getText();
            this.mainInfo.setText(playerName + " " + info);
        }

        void addMidBid(int minBid) {
            String info = this.mainInfo.getText();
            this.mainInfo.setText( info + ", " + minBid);
        }

        void cancelTimer() {
            if (countDownTimer != null) countDownTimer.cancel();
//            this.timer.setHidden(true, true); // setHidden does not work well
//            this.points.setHidden(false, true);
            this.timer.setText("");
            FontImage.setMaterialIcon(timer, '0');  // hide it
        }

        void showPoints(String point) {
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
        void showTimer(int timeout, int contractPoint, String act) {
            userHelp.clear();
            cancelTimer();  // cancel the running timer if any
            if (act.equals("dim")) {
                this.setContractor(CONTRACTOR);
                needChangeActions = true;
            }

            this.points.setText("");
            this.timer.setText(timeout + "");
            FontImage.setMaterialIcon(timer, FontImage.MATERIAL_TIMER);
            countDownTimer = new UITimer(new CountDown(this, timeout));
            countDownTimer.schedule(950, true, mainForm);   // slightly less to 1 sec

            if (this.location.equals("bottom")) {
                if (act.equals("dim")) {
                    actionButtons.removeAll();
                    userHelp.showHelp(userHelp.SET_TRUMP);
                    for (char c : candidateTrumps) {
                        Button btn = new Button();
                        if (c == Card.JOKER) {
                            btn.setText("NT");
                        } else {
                            btn.setText(Card.suiteSign(c));
                        }
                        actionButtons.add(btn);
                        btn.addActionListener((e) -> {
                            actionButtons.setVisible(false);
                            actionButtons.setEnabled(false);

                            cancelTimer();
                            mySocket.addRequest(actionSetTrump, "\"trump\":\"" + c + "\"");
                        });
                    }
                    needChangeActions = true;
                } else if (act.equals("bid")) {
                    this.maxBid = contractPoint - 5;
                    btnBid.setText("" + this.maxBid);
                } else if (act.equals("bury")) {
                    actionButtons.removeAll();
                    actionButtons.add(btnPlay);
                    btnPlay.setText("埋底Bury");
                    needChangeActions = true;                    
                } else if (act.equals("partner")) {
                    userHelp.showHelp(userHelp.SET_PARTNER);
                    actionButtons.removeAll();
                    RadioButton rb1 = new RadioButton("1st");
                    RadioButton rb2 = new RadioButton("2nd");
                    RadioButton rb3 = new RadioButton("3rd");
                    RadioButton rb4 = new RadioButton("4th");
                    ButtonGroup btnGroup = new ButtonGroup(rb1,rb2,rb3,rb4);
                    actionButtons.addAll(rb1,rb2,rb3,rb4);
                    String rnk = rankToString(playerRank);
                    rnk = rnk.equals("A") ? "K" : "A";
                    addCardButton(Card.SPADE, rnk, btnGroup);
                    addCardButton(Card.HEART, rnk, btnGroup);
                    addCardButton(Card.DIAMOND, rnk, btnGroup);
                    addCardButton(Card.CLUB, rnk, btnGroup);
//                    Button btn = new Button("1vs5", "nopartner");
                    Button btn = new Button("1vs5");
                    btn.setCapsText(false);
                    btn.addActionListener((e)->{
                        actionButtons.setVisible(false);
                        actionButtons.setEnabled(false);
                        cancelTimer();
                        mySocket.addRequest(actionPartner, "\"def\":\"0\"");
                    });
                    actionButtons.add(btn);
                    needChangeActions = true;
                } else if(act.equals("play")){
                    if(needChangeActions) {
                        actionButtons.removeAll();
                        actionButtons.add(btnPlay);
                        btnPlay.setText("Play");
                        needChangeActions = false;
                    }
                } else {
                    // not supported
                    Log.p("Unknown act: " + act);
                }
                actionButtons.setVisible(true);
                actionButtons.setEnabled(true);
            }
        }

        private void addCardButton(char suite, String rnk, ButtonGroup btnGroup) {
            if(suite != currentTrump) {
                Button btn = new Button(Card.suiteSign(suite) + rnk, "suite" + suite);
                actionButtons.add(new Label("   "));
                actionButtons.add(btn);
                btn.addActionListener((e)->{
                    if(!btnGroup.isSelected()) {
//                        Dialog.show("Alert", "请指定第几个");
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

        UserHelp() {
            this.setLayout(new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST));
            this.add(engLabel);
            this.add(chnLabel);
        }

        void clear() {
            engLabel.setText("");
            chnLabel.setText("");
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
