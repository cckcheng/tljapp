package com.ccd.tlj;

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.DynamicImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
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
    static final int POINT_COLOR = 0x3030ff;
    static final int TIMER_COLOR = 0xff00ff;
    static final int RED_COLOR = 0xff0000;
    static final int BUTTON_COLOR = 0x47b2e8;

    private final ButtonImage backImage = new ButtonImage(80, 30, 0xbcbcbc);
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

    static final String actionJoinTable = "join_table";
    static final String actionBid = "bid";

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

    private void addCards(Map<String, Object> data) {
        addCardsToHand(Card.SPADE, (List<Object>) data.get("S"));
        addCardsToHand(Card.HEART, (List<Object>) data.get("H"));
        addCardsToHand(Card.DIAMOND, (List<Object>) data.get("D"));
        addCardsToHand(Card.CLUB, (List<Object>) data.get("C"));
        addCardsToHand(Card.JOKER, (List<Object>) data.get("T"));
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

    private List<PlayerInfo> infoLst = new ArrayList<>();
    private Map<Integer, PlayerInfo> playerMap = new HashMap<>();
    private boolean tableOn = false;
    private int timeout = 30;   // 30 seconds
    public boolean isPlaying = false;
    private int contractPoint = -1;
    private int playerRank;
    private Hand hand;
    private Label gameInfo;
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

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 02");
        String stage = data.get("stage").toString();
        this.isPlaying = stage.equalsIgnoreCase(PLAYING_STAGE);
        int seat = parseInteger(data.get("seat"));
        int actionSeat = parseInteger(data.get("actionSeat"));
        this.playerRank = parseInteger(data.get("rank"));
        int game = parseInteger(data.get("game"));
        int gameRank = parseInteger(data.get("gameRank"));
        this.contractPoint = parseInteger(data.get("contractPoint"));
        int defaultTimeout = parseInteger(data.get("timeout"));
        if (defaultTimeout > 0) this.timeout = defaultTimeout;

        this.hand = new Hand(this);
        this.addCards(data);

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 03");
        char trumpSuite = Card.JOKER;
        String trump = data.get("trump").toString();
        if (!trump.isEmpty()) trumpSuite = trump.charAt(0);
        
        if(gameRank>0) {
            hand.sortCards(trumpSuite, gameRank, true);
        } else {
            hand.sortCards(trumpSuite, playerRank, true);
        }

        PlayerInfo p0 = new PlayerInfo("bottom", seat, playerRank);
        this.infoLst.add(p0);
        this.playerMap.put(seat, p0);
        p0.setPlayerName(playerName);
        int minBid = parseInteger(data.get("minBid"));
        if (minBid > 0) p0.addMidBid(minBid);
        if (!this.isPlaying) displayBidInfo(p0, data.get("bid").toString());

        Button bExit = new Button("Exit");
        FontImage.setMaterialIcon(bExit, FontImage.MATERIAL_EXIT_TO_APP);
        bExit.setUIID("myExit");
        bExit.addActionListener((e) -> {
            this.tableOn = false;
            tablePane.removeAll();
            mainForm.setGlassPane(null);
            mainForm.getContentPane().setVisible(true);
            mainForm.repaint();
            disconnect();
        });

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 04");
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
        int buryTime = parseInteger(data.get("burytime"));
        if (this.isPlaying) {
            gmInfo = this.contractPoint + " ";
            Object act = data.get("act");
            if (act != null && act.toString().equalsIgnoreCase("dim")) {
            } else {
                if (trumpSuite == Card.JOKER) {
                    gmInfo += "NT ";
                } else {
                    gmInfo += Card.suiteSign(trumpSuite);
                }
                gmInfo += gameRank;
            }
        }
        this.gameInfo = new Label(gmInfo);
        this.gameInfo.getStyle().setFgColor(0xebef07);
        this.gameInfo.getStyle().setFont(Hand.fontRank);

        tablePane.add(hand);
        tablePane.add(bExit).add(lbGeneral).add(this.gameInfo);
        LayeredLayout ll = (LayeredLayout) tablePane.getLayout();
        ll.setInsets(bExit, "0 0 auto auto");   //top right bottom left
        ll.setInsets(lbGeneral, "0 auto auto 0")
                .setInsets(this.gameInfo, "0 auto auto 0");
        ll.setReferenceComponentTop(this.gameInfo, lbGeneral, 1f);

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 05");
        for (PlayerInfo info : infoLst) {
            info.addItems(tablePane);
        }

        PlayerInfo pp = this.playerMap.get(actionSeat);
        if (pp != null) {
            if (buryTime > 1) {
                pp.showTimer(buryTime, this.contractPoint, false);
            } else {
                pp.showTimer(this.timeout, this.contractPoint, false);
            }
        }

        if (this.isPlaying) {
            int seatContractor = parseInteger(data.get("seatContractor"));
            pp = this.playerMap.get(seatContractor);
            if (pp != null) pp.setContractor(CONTRACTOR);
            int seatPartner = parseInteger(data.get("seatPartner"));
            pp = this.playerMap.get(seatPartner);
            if (pp != null) pp.setContractor(PARTNER);
        }
//        mainForm.setGlassPane((g, rect) -> {
//            int x0 = lbL1Player.getAbsoluteX() + 5;
//            int y0 = lbL1Player.getAbsoluteY() + 50;
//            g.setColor(0);
//            g.drawRoundRect(x0 - 1, y0 - 1, 50, 80, 10, 10);
//        });
        mainForm.repaint();
        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 06");
        this.tableOn = true;
        main.enableButtons();

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: done");
    }

    private void parsePlayerInfo(Map<String, Object> rawData, String location) {
        int seat = parseInteger(rawData.get("seat"));
        PlayerInfo pp = new PlayerInfo(location, seat, parseInteger(rawData.get("rank")));
        if (!this.isPlaying) displayBidInfo(pp, rawData.get("bid").toString());
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
        int actionSeat = parseInteger(data.get("nextActionSeat"));
        this.contractPoint = parseInteger(data.get("contractPoint"));   // send contract point every time to avoid error
        String bid = data.get("bid").toString();
        PlayerInfo pp = this.playerMap.get(seat);
        if (pp != null) displayBidInfo(pp, bid);

        pp = this.playerMap.get(actionSeat);
        if (pp != null) {
            boolean bidOver = parseBoolean(data.get("bidOver"));
            pp.showTimer(this.timeout, this.contractPoint, bidOver);
        }
    }

    private char currentTrump;
    private void setTrump(Map<String, Object> data) {
        String trump = data.get("trump").toString();
        if (trump.isEmpty()) return;
        this.currentTrump = trump.charAt(0);
        int seat = parseInteger(data.get("seat"));
        int gameRank = parseInteger(data.get("gameRank"));
        int contractPoint = parseInteger(data.get("contractPoint"));
        this.hand.sortCards(currentTrump, gameRank, true);
        this.hand.repaint();

        for (int st : this.playerMap.keySet()) {
            PlayerInfo pp = this.playerMap.get(st);
            if (st == seat) {
                pp.setContractor(CONTRACTOR);
            } else {
                pp.points.setText("");
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
        private List<String> pendingRequests = new ArrayList<>();

        public void closeConnection() {
            this.closeRequested = true;
            if (TuoLaJi.DEBUG_MODE) Log.p("this.closeRequested: " + this.closeRequested);
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
            if (TuoLaJi.DEBUG_MODE) Log.p("Received: " + msg);
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
                        if (TuoLaJi.DEBUG_MODE) Log.p("init table");
                        showTable(data);
                        break;
                    case "bid":
                        if (TuoLaJi.DEBUG_MODE) Log.p("bidding");
                        displayBid(data);
                        break;
                    case "set_trump":
                        if (TuoLaJi.DEBUG_MODE) Log.p("set trump");
                        setTrump(data);
                        break;
                    case "add_remains":
                        if (TuoLaJi.DEBUG_MODE) Log.p("add_remains");
                        addCards(data);
                        hand.sortCards(currentTrump, playerRank, true);
                        hand.repaint();
                        int buryTime = parseInteger(data.get("burytime"));
                        infoLst.get(0).showTimer(buryTime, 100, true);
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
                tablePane.removeAll();
                mainForm.setGlassPane(null);
                mainForm.getContentPane().setVisible(true);
                mainForm.repaint();
            }
//            mySocket = null;    // reset connection
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
//            closeRequested = false;
            byte[] buffer = new byte[4096];
            int count = 0;
            boolean startCount = false;
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
                        startCount = true;
                    }
                    int n = is.available();
                    if (n > 0) {
                        startCount = false;
                        count = 0;
                        n = is.read(buffer, 0, 4096);
                        if (n < 0) break;
                        processReceived(new String(buffer, 0, n));
                    } else {
                        if (startCount) count++;
                        if (count > serverWaitCycle) break;
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
//            Dialog.show("Alert", "Closed.", "OK", "");
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
                }
                pInfo.countDownTimer.cancel();
            }
        }
    }

    class PlayerInfo {

        final String location;    // top, bottom, left up, left down, right up, right down
        Label mainInfo;
        Label points;
        Label contractor;   // for contractor and partner
        Label timer;   // count down timer
        List<Card> cards;   // cards played
        String playerName;
        UITimer countDownTimer;
        Container actionButtons;
        Button btnBid;
        Button btnPlus;
        Button btnMinus;
        Button btnPass;

        int seat;
        int rank;
        PlayerInfo(String loc, int seat, int rank) {
            this.location = loc;
            this.seat = seat;
            this.rank = rank;
            String info = "#" + seat + "," + rankToString(rank);
            mainInfo = new Label(info);

            points = new Label("        ");
            points.getAllStyles().setFont(Hand.fontRank);

//            contractor = new Label("庄");
            contractor = new Label("     ");
            contractor.getStyle().setFgColor(RED_COLOR);
            contractor.getStyle().setFont(Hand.fontRank);

            timer = new Label("    ");
            timer.getAllStyles().setFgColor(TIMER_COLOR);
            timer.getAllStyles().setFont(Hand.fontRank);
//            timer.setHidden(true, true);    // setHidden Does not work

            if (loc.equals("bottom")) {
                btnBid = new Button("200");
                btnPlus = new Button("");
                btnMinus = new Button("");
                btnPass = new Button("Pass");

//                btnBid = new Button(" 200 ", "bid");
//                btnPlus = new Button("", "plus");
//                btnMinus = new Button("", "minus");
//                btnPass = new Button("Pass", "pass");
                FontImage.setMaterialIcon(btnPlus, FontImage.MATERIAL_ARROW_UPWARD);
                FontImage.setMaterialIcon(btnMinus, FontImage.MATERIAL_ARROW_DOWNWARD);

//                btnPlus.setUIID("plus");
//                btnMinus.setUIID("minus");

//                btnPlus.getStyle().setFgColor(BUTTON_COLOR);
//                btnPlus.getStyle().setFont(Hand.fontRank);
//                btnMinus.getStyle().setFgColor(BUTTON_COLOR);
//                btnMinus.getStyle().setFont(Hand.fontRank);
////
//                btnBid.getStyle().setFgColor(BUTTON_COLOR);
//                btnBid.getStyle().setFont(Hand.fontRank);
//
//                btnPass.getStyle().setFgColor(BUTTON_COLOR);
//                btnPass.getStyle().setFont(Hand.fontRank);

                btnBid.getAllStyles().setFont(Hand.fontRank);
                btnBid.getAllStyles().setBgImage(backImage);
                btnPass.getAllStyles().setBgImage(backImage);

                btnBid.addActionListener((e) -> {
                    actionButtons.setVisible(false);
                    if(countDownTimer != null) countDownTimer.cancel();
                    mySocket.addRequest(actionBid, "\"bid\":" + btnBid.getText().trim());
                });
                btnPass.addActionListener((e) -> {
                    actionButtons.setVisible(false);
                    if(countDownTimer != null)countDownTimer.cancel();
                    mySocket.addRequest(actionBid, "\"bid\":\"pass\"");
                });
                btnPlus.addActionListener((e) -> {
                    int point = parseInteger(btnBid.getText());
                    if (point < this.maxBid) {
                        point += 5;
                        btnBid.setText("" + point);
//                        btnBid.setText(" " + point + " ");
                    }
                });
                btnMinus.addActionListener((e) -> {
                    int point = parseInteger(btnBid.getText());
                    if (point > 0) {
                        point -= 5;
                        btnBid.setText("" + point);
//                        btnBid.setText(" " + point + " ");
                    }
                });

                if (candidateTrumps.isEmpty()) {
                    actionButtons = BoxLayout.encloseXNoGrow(btnPass);
                } else {
                    actionButtons = BoxLayout.encloseXNoGrow(btnPlus, btnBid, btnMinus, btnPass);
                }
//                actionButtons = BoxLayout.encloseXNoGrow(btnPlus, new Label("   "), btnBid,
//                        new Label("   "), btnMinus, new Label("   "), new Label("   "), btnPass, test);
            }
        }

        void addItems(Container pane) {
            pane.add(mainInfo).add(points).add(timer).add(contractor);
            LayeredLayout ll = (LayeredLayout) pane.getLayout();
            
            switch (this.location) {
                case "left up":
                    ll.setInsets(mainInfo, "20% auto auto 0");  //top right bottom left
                    ll.setInsets(points, "0 auto auto 20")
                            .setInsets(timer, "0 auto auto 20")
                            .setInsets(contractor, "19% auto auto 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "left down":
                    ll.setInsets(mainInfo, "40% auto auto 0");
                    ll.setInsets(points, "0 auto auto 20")
                            .setInsets(timer, "0 auto auto 20")
                            .setInsets(contractor, "39% auto auto 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "right up":
                    ll.setInsets(mainInfo, "20% 0 auto auto");
                    ll.setInsets(points, "0 20 auto auto")
                            .setInsets(timer, "0 20 auto auto")
                            .setInsets(contractor, "19% 20 auto auto");
                    ll.setReferenceComponentRight(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "right down":
                    ll.setInsets(mainInfo, "40% 0 auto auto");
                    ll.setInsets(points, "0 20 auto auto")
                            .setInsets(timer, "0 20 auto auto")
                            .setInsets(contractor, "39% 20 auto auto");
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
                    pane.add(actionButtons);
//                    ll.setInsets(actionButtons, "auto auto 35% 40%");
                    ll.setInsets(actionButtons, "auto auto 35% auto");

                    ll.setInsets(mainInfo, "auto auto 0 auto");
                    ll.setInsets(points, "auto auto 35% auto")
                            .setInsets(timer, "auto auto 0 45%")
                            .setInsets(contractor, "auto auto 0 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentBottom(timer, actionButtons, 1f);

                    actionButtons.setVisible(false);
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

        void showPoints(String point) {
            if (countDownTimer != null) countDownTimer.cancel();
//            this.timer.setHidden(true, true); // setHidden does not work well
//            this.points.setHidden(false, true);
            this.timer.setText("");
            FontImage.setMaterialIcon(timer, '0');  // hide it
            this.points.setText(point);
            if (point.equalsIgnoreCase("pass")) {
                this.points.getStyle().setFgColor(GREY_COLOR);
            } else {
                this.points.getStyle().setFgColor(POINT_COLOR);
            }

            if (this.actionButtons != null) {
//                this.actionButtons.removeAll();
                this.actionButtons.setVisible(false);
            }
        }

        int maxBid = -1;
        void showTimer(int timeout, int contractPoint, boolean bidOver) {
            if(bidOver) {
                this.contractor.setText(CONTRACTOR);
//                return;
            }
//            this.timer.setHidden(false, true);
//            this.points.setHidden(true, true);
            this.points.setText("");
            this.timer.setText(timeout + "");
            FontImage.setMaterialIcon(timer, FontImage.MATERIAL_TIMER);
            countDownTimer = new UITimer(new CountDown(this, timeout));
            countDownTimer.schedule(1000, true, mainForm);

            if (this.location.equals("bottom")) {
                if (bidOver) {
                    actionButtons.removeAll();
                    actionButtons.add(new Label("请选将"));
                    for (char c : candidateTrumps) {
                        if (c == Card.JOKER) {
                            actionButtons.add(new Button("NT"));
                        } else {
                            actionButtons.add(new Button(Card.suiteSign(c)));
                        }
                    }
                } else {
                    this.maxBid = contractPoint - 5;
                    btnBid.setText("" + this.maxBid);
                }
                actionButtons.setVisible(true);
            }
        }

        void setContractor(String txt) {
            // txt could be Contractor or Partner
            this.points.setText("");
            this.contractor.setText(txt);
        }
    }

    class ButtonImage extends DynamicImage {
        int bgColor = 0x00ffff;

        ButtonImage(int w, int h, int bgColor) {
            super(w, h);
            this.bgColor = bgColor;
        }

        @Override
        protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
            g.setColor(this.bgColor);
            g.fillRoundRect(x, y, w, h, 35, 35);
        }

    }
}
