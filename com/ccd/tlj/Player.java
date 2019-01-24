package com.ccd.tlj;

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.LayeredLayout;
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
    static final int TLJ_PORT = 6688;
    static final int POINT_COLOR = 0xd60e90;
    static final int CONTRACT_COLOR = 0xff0000;

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

    public void connectServer() {
        if (!Socket.isSupported()) {
            Dialog.show("Alert", "Socket is not supported", "OK", "");
            return;
        }
//        if (this.mySocket != null) {
//            if (!mySocket.isConnected()) {
//                mySocket.addRequest(actionJoinTable, "\"id\":\"" + this.playerId + "\"");
//                Socket.connect(TLJ_HOST, TLJ_PORT, mySocket);
//            }
//        } else {
            this.mySocket = new MySocket();
            mySocket.addRequest(actionJoinTable, "\"id\":\"" + this.playerId + "\"");
            Socket.connect(TLJ_HOST, TLJ_PORT, mySocket);
//        }
    }

    public void disconnect() {
        if (this.mySocket != null) {
            if (this.mySocket.isConnected()) {
                this.mySocket.closeConnection();
                this.mySocket = null;
            }
        }
    }

    private void addCardsToHand(Hand hand, char suite, List<Object> lst) {
        if (lst.isEmpty()) return;
        for (Object d : lst) {
            int rank = parseInteger(d);
            if (rank > 0) hand.addCard(new Card(suite, rank));
        }
    }

    public static int parseInteger(Object obj) {
        try {
            return (int) Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    private List<PlayerInfo> infoLst = new ArrayList<>();
    private Map<Integer, PlayerInfo> playerMap = new HashMap<>();
    private boolean tableOn = false;
    
    private void showTable(Map<String, Object> data) {
        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 01");
        Container pane = mainForm.getFormLayeredPane(mainForm.getClass(), true);
        pane.setLayout(new LayeredLayout());
        if (this.tableOn) {
            pane.removeAll();
        }

        infoLst.clear();

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 02");
        int seat = parseInteger(data.get("seat"));
        int rank = parseInteger(data.get("rank"));
        int game = parseInteger(data.get("game"));
        int gameRank = parseInteger(data.get("game_rank"));

        Hand hand = new Hand();
        addCardsToHand(hand, Card.SPADE, (List<Object>) data.get("S"));
        addCardsToHand(hand, Card.HEART, (List<Object>) data.get("H"));
        addCardsToHand(hand, Card.DIAMOND, (List<Object>) data.get("D"));
        addCardsToHand(hand, Card.CLUB, (List<Object>) data.get("C"));
        addCardsToHand(hand, Card.JOKER, (List<Object>) data.get("T"));

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 03");
        char trumpSuite = Card.JOKER;
        String trump = data.get("trump").toString();
        if (!trump.isEmpty()) trumpSuite = trump.charAt(0);
        
        if(gameRank>0) {
            hand.sortCards(trumpSuite, gameRank, true);
        } else {
            hand.sortCards(trumpSuite, rank, true);
        }

        PlayerInfo p0 = new PlayerInfo("bottom", seat, rank);
        this.infoLst.add(p0);
        this.playerMap.put(seat, p0);
        p0.setPlayerName(playerName);
        int minBid = parseInteger(data.get("minBid"));
        if (minBid > 0) p0.minBid = minBid;

        Button bExit = new Button("Exit");
        FontImage.setMaterialIcon(bExit, FontImage.MATERIAL_EXIT_TO_APP);
        bExit.setUIID("myExit");
        bExit.addActionListener((e) -> {
            this.tableOn = false;
            pane.removeAll();
            mainForm.setGlassPane(null);
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

        seat = parseInteger(pR1.get("seat"));
        PlayerInfo pp = new PlayerInfo("right down", seat, parseInteger(pR1.get("rank")));
        this.infoLst.add(pp);
        this.playerMap.put(seat, pp);
        
        seat = parseInteger(pR2.get("seat"));
        pp = new PlayerInfo("right up", seat, parseInteger(pR2.get("rank")));
        this.infoLst.add(pp);
        this.playerMap.put(seat, pp);
        
        seat = parseInteger(pL2.get("seat"));
        pp = new PlayerInfo("left up", seat, parseInteger(pL2.get("rank")));
        this.infoLst.add(pp);
        this.playerMap.put(seat, pp);
        
        seat = parseInteger(pL1.get("seat"));
        pp = new PlayerInfo("left down", seat, parseInteger(pL1.get("rank")));
        this.infoLst.add(pp);
        this.playerMap.put(seat, pp);
        
        seat = parseInteger(pTop.get("seat"));
        pp = new PlayerInfo("top", seat, parseInteger(pTop.get("rank")));
        this.infoLst.add(pp);
        this.playerMap.put(seat, pp);

        Label lbGeneral = new Label("Game " + game);
        lbGeneral.getStyle().setFont(Hand.fontRank);

        pane.add(hand);
        pane.add(bExit).add(lbGeneral);
        LayeredLayout ll = (LayeredLayout) pane.getLayout();
        ll.setInsets(bExit, "0 0 auto auto");   //top right bottom left
        ll.setInsets(lbGeneral, "0 auto auto 0");

        if (TuoLaJi.DEBUG_MODE) Log.p("Show table: 05");
        for (PlayerInfo info : infoLst) {
            info.addItems(pane);
//            break;
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

    private void displayBid(Map<String, Object> data) {
        int seat = parseInteger(data.get("seat"));
        String bid = data.get("bid").toString();
        PlayerInfo pp = this.playerMap.get(seat);
        if(pp == null) return;
        if(bid.equalsIgnoreCase("pass")) {
            pp.points.setText("Pass");
        } else {
            pp.points.setText("" + parseInteger(bid));
        }
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
                String action = data.get("action").toString();
                if (action.equals("init")) {
                    if (TuoLaJi.DEBUG_MODE) Log.p("init table");
                    showTable(data);
                    continue;
                }
                if (action.equals("bid")) {
                    if (TuoLaJi.DEBUG_MODE) Log.p("Please bid");
                    displayBid(data);
                    continue;
                }
            }
        }

        @Override
        public void connectionError(int errorCode, String message) {
//            if (isConnected()) closeRequested = true;
            main.enableButtons();
            Dialog.show("Error", message, "OK", "");
//            mySocket = null;    // reset connection
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
//            closeRequested = false;
            byte[] buffer = new byte[4096];
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
                    }
                    int n = is.available();
                    if (n > 0) {
                        n = is.read(buffer, 0, 4096);
                        if (n < 0) break;
                        processReceived(new String(buffer, 0, n));
                    } else {
                        Thread.sleep(500);
                    }
                }
                os.close();
                is.close();
            } catch (Exception err) {
                err.printStackTrace();
                Dialog.show("Exception", "Error: " + err.getMessage(), "OK", "");
            }
//            Dialog.show("Alert", "Closed.", "OK", "");
        }
    }

    class PlayerInfo {

        final String location;    // top, bottom, left up, left down, right up, right down
        Label mainInfo;
        Label points;
        Label contractor;   // for contractor and partner
        List<Card> cards;   // cards played
        String playerName;

        int minBid = -1;
        int seat;
        int rank;
        PlayerInfo(String loc, int seat, int rank) {
            this.location = loc;
            this.seat = seat;
            this.rank = rank;
        }

        void addItems(Container pane) {
            String info = "#" + seat + "," + rankToString(rank);
            if (playerName != null) info = playerName + " " + info;
            if (minBid > 0) info += ", " + minBid;
            mainInfo = new Label(info);
            points = new Label("        ");
            points.getStyle().setFgColor(POINT_COLOR);
//            contractor = new Label("庄");
            contractor = new Label("        ");
            contractor.getStyle().setFgColor(CONTRACT_COLOR);
            
            pane.add(mainInfo).add(points).add(contractor);
            LayeredLayout ll = (LayeredLayout) pane.getLayout();
            
            switch (this.location) {
                case "left up":
                    ll.setInsets(mainInfo, "20% auto auto 0");  //top right bottom left
                    ll.setInsets(points, "20% auto auto 0")
                            .setInsets(contractor, "20% auto auto 0");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1.1f)
                            .setReferenceComponentLeft(points, mainInfo, 1.5f);
                    break;
                case "left down":
                    ll.setInsets(mainInfo, "40% auto auto 0");
                    ll.setInsets(points, "40% auto auto 0")
                            .setInsets(contractor, "40% auto auto 0");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1.1f)
                            .setReferenceComponentLeft(points, mainInfo, 1.5f);
                    break;
                case "right up":
                    ll.setInsets(mainInfo, "20% 0 auto auto");
                    ll.setInsets(points, "20% 20 auto auto")
                            .setInsets(contractor, "20% 20 auto auto");
                    ll.setReferenceComponentRight(contractor, mainInfo, 1.1f)
                            .setReferenceComponentRight(points, mainInfo, 1.5f);
                    break;
                case "right down":
                    ll.setInsets(mainInfo, "40% 0 auto auto");
                    ll.setInsets(points, "40% 20 auto auto")
                            .setInsets(contractor, "40% 20 auto auto");
                    ll.setReferenceComponentRight(contractor, mainInfo, 1.1f)
                            .setReferenceComponentRight(points, mainInfo, 1.5f);
                    break;
                case "top":
                    ll.setInsets(mainInfo, "0 auto auto auto");
                    ll.setInsets(points, "0 auto auto 0")
                            .setInsets(contractor, "0 auto auto 0");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentLeft(points, mainInfo, 1.2f);
                    break;
                case "bottom":
                    ll.setInsets(mainInfo, "auto auto 0 auto");
                    ll.setInsets(points, "auto auto 0 0")
                            .setInsets(contractor, "auto auto 0 0");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentLeft(points, mainInfo, 1.2f);
                    break;
            }

        }

        private void setPlayerName(String playerName) {
            this.playerName = playerName;
        }
    }
}
