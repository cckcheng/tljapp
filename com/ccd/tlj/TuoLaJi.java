package com.ccd.tlj;


import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UITimer;
import com.codename1.util.StringUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose
 * of building native mobile applications using Java.
 */
public class TuoLaJi {

    static public int BACKGROUND_COLOR = Card.DEBUG_MODE ? 0xffffff : 0x008000;
    static public final boolean DEBUG = false;
    private Form current;
    public Resources theme;

    private String uniqueID = null;

    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });

    }

    public Form formMain = null;
    public Form formTable = null;
    public Form formHelp = null;
    public Form formTutor = null;

    private Label lbTitle;
    private Button btnTutor = null;
    private Button btnPlay = null;
    private Button btnHelp = null;
//    private Button btnExit = null;
    private Button btnSetting = null;

    public void enableButtons() {
        if (this.btnPlay != null) {
            this.btnPlay.setEnabled(true);
            this.btnPlay.setText(Dict.get(lang, "Play"));
        }
        if (this.btnHelp != null) {
            this.btnHelp.setEnabled(true);
        }
    }

    public void disableButtons() {
        if (this.btnPlay != null) {
            this.btnPlay.setEnabled(false);
            this.btnPlay.setText(Dict.get(lang, "Connecting") + "...");
        }
    }

    public void showPlayButton() {
        if (this.entry.getComponentIndex(this.btnPlay) < 0) {
            this.entry.addComponent(this.entry.getComponentIndex(this.btnHelp), this.btnPlay);
        }
    }

    public void refreshButtons() {
        this.lbTitle.setText(Dict.get(lang, title));

        if (this.btnPlay.isEnabled()) {
            this.btnPlay.setText(Dict.get(lang, "Play"));
        }
        this.btnHelp.setText(Dict.get(lang, "Help"));
//        this.btnExit.setText(Dict.get(lang, "Exit"));
        this.btnTutor.setText(Dict.get(lang, "Tutorial"));
        this.btnSetting.setText(Dict.get(lang, "Settings"));
        if (this.player != null) {
            this.player.refreshLang();
        }
    }

    public void validateTable() {
        this.table.forceRevalidate();
    }

    public Form getCurForm() {
        return this.isMainForm ? this.formMain : this.formTable;
    }

    private Player player = null;

    public void onConnectionError() {
        Player p = this.player;
        if (this.btnPlay != null) {
            this.btnPlay.setEnabled(false);
            this.btnPlay.setText(Dict.get(lang, "Network Error"));
            Button btn = this.btnPlay;
            if (this.currentComp == this.entry) {
                new UITimer(new Runnable() {
                    @Override
                    public void run() {
                        btn.setText(Dict.get(lang, "Connecting") + "...");
                        p.connectServer(false);
                    }
                }).schedule(3000, false, this.formMain);
            }
        }
    }

    public String version = "3.09";
    public String OS = "";
    public final static String title = "Langley TuoLaJi";

    public String lang = "en";
    private Container entry;
    private Container table;
    private Container help;
    private Tutor tutor;

    public Image back;
    public void start() {
        if(current != null){
            current.show();
            return;
        }

        Display disp = Display.getInstance();
        Object sObj = Storage.getInstance().readObject("lang");
        if (sObj != null) {
            this.lang = sObj.toString();
        } else {
            L10NManager l10n = disp.getLocalizationManager();
            this.lang = l10n.getLanguage();
            Storage.getInstance().writeObject("lang", this.lang);
        }

        back = theme.getImage("btn.png");
//        back = back.scaledHeight(Hand.fontRank.getHeight());
//        String onlineHelp = getHelp();
        disp.lockOrientation(false);
//        disp.requestFullScreen();
        disp.setNoSleep(true);
        disp.setScreenSaverEnabled(false);
        disp.setBuiltinSoundsEnabled(true);
        this.version = disp.getProperty("AppVersion", this.version);

        if (DEBUG) {
            System.out.println("Platform=" + disp.getProperty("Platform", ""));
            System.out.println("User-Agent=" + disp.getProperty("User-Agent", ""));
            this.OS = disp.getProperty("OS", "");
            System.out.println("OS=" + this.OS);
        }

        String playerId = getPlayerID(disp);
        if (playerId == null) {
            Dialog.show(Dict.get(lang, "Error"), "Failed to generate Player ID", Dict.get(lang, "OK"), "");
            disp.exitApplication();;
        }

        Form mainForm = new Form(title, new BorderLayout());
        mainForm.setSafeArea(true);
        if (DEBUG) {
            Rectangle safeRect = mainForm.getSafeArea();
            System.out.print(" x=" + safeRect.getX());
            System.out.print(" y=" + safeRect.getY());
            System.out.print(" w=" + safeRect.getWidth());
            System.out.print(" h=" + safeRect.getHeight());
        }
        this.formMain = mainForm;
        mainForm.getStyle().setBgColor(BACKGROUND_COLOR);
        mainForm.getToolbar().hideToolbar();

        this.player = new Player(playerId, this);

        this.entry = new Container(BoxLayout.yLast());
        this.entry.setSafeArea(true);
        this.table = new Container(new LayeredLayout());
        this.table.setSafeArea(true);
        this.tutor = new Tutor(this);
        this.player.createTable(this.table);

//        mainForm.getToolbar().addCommandToLeftSideMenu("New Game", null, (e) -> {
//            Log.p("Start New Game");
//        });

//        Style sTitle = mainForm.getToolbar().getTitleComponent().getUnselectedStyle();
//        sTitle.setFont(Hand.fontSymbol);
        lbTitle = new Label(Dict.get(lang, title));
//        lbTitle.getStyle().setAlignment(CENTER);
        lbTitle.getStyle().setFont(Hand.fontRank);
        lbTitle.getAllStyles().setFgColor(0);
        entry.add(lbTitle);

        int menuColor = Player.BUTTON_COLOR;
        Button bPlay = new Button(Dict.get(lang, "Connecting") + "...");
        bPlay.getStyle().setFgColor(menuColor);
        bPlay.getAllStyles().setFont(Hand.fontRank);
        bPlay.setEnabled(false);
        this.btnPlay = bPlay;

        FontImage.setMaterialIcon(bPlay, FontImage.MATERIAL_PEOPLE);
        bPlay.addActionListener((e) -> {
            Object sgObj = Storage.getInstance().readObject("playerName");
            if (sgObj == null) {
                showPlayOption();
            } else {
                bPlay.setEnabled(false);
                this.player.startPlay(sgObj.toString(), "resume");
            }
        });

//        BrowserComponent browser = new BrowserComponent();    // not work
//        WebBrowser browser = new WebBrowser();    // not work
//        browser.setURL(Card.HELP_URL);
//        browser.setPage(onlineHelp, null);
//        helpDlg.add(theme.getImage("h2.png").scaledWidth(disp.getDisplayWidth() - 400));

        btnHelp = new Button(Dict.get(lang, "Help"));
        btnHelp.getStyle().setFgColor(menuColor);
        btnHelp.getAllStyles().setFont(Hand.fontRank);
        FontImage.setMaterialIcon(btnHelp, FontImage.MATERIAL_HELP);
//        btnHelp.setEnabled(false);
        btnHelp.addActionListener((e) -> {
            showHelp(lang);
        });

        btnTutor = new Button(Dict.get(lang, "Tutorial"));
        btnTutor.getStyle().setFgColor(menuColor);
        btnTutor.getAllStyles().setFont(Hand.fontRank);
        FontImage.setMaterialIcon(btnTutor, FontImage.MATERIAL_TOUCH_APP);
        btnTutor.addActionListener((e) -> {
            this.switchScene("tutor");
        });

//        btnExit = new Button(Dict.get(lang, "Exit"));
//        btnExit.getStyle().setFgColor(menuColor);
//        btnExit.getStyle().setFont(Hand.fontRank);
//        FontImage.setMaterialIcon(btnExit, FontImage.MATERIAL_EXIT_TO_APP);
//        btnExit.addActionListener((e) -> {
//            disp.playBuiltinSound(Display.SOUND_TYPE_ALARM);
//            if (this.player != null) {
//                player.disconnect();
//            }
//            Display.getInstance().exitApplication();
//        });

        btnSetting = new Button(Dict.get(lang, "Settings"));
        btnSetting.getStyle().setFgColor(menuColor);
        btnSetting.getAllStyles().setFont(Hand.fontRank);
        FontImage.setMaterialIcon(btnSetting, FontImage.MATERIAL_SETTINGS);
        btnSetting.addActionListener((e) -> {
            disp.playBuiltinSound(Display.SOUND_TYPE_WARNING);
            TextField pName = new TextField("", Dict.get(lang, "Your Name"), 16, TextArea.ANY);
            pName.setMaxSize(16);
            Object sgObj = Storage.getInstance().readObject("playerName");
            if (sgObj != null) {
                pName.setText(sgObj.toString());
            }
//            Dialog settingDlg = new Dialog(Dict.get(lang, "Settings"), new BorderLayout());
            TableLayout tl = new TableLayout(2, 2);
            Container props = new Container(tl);
//            settingDlg.add(BorderLayout.CENTER, TableLayout.encloseIn(2, true,
//                    new Label(Dict.get(lang, "Player Name")), pName,
//                    new Label(Dict.get(lang, "Version")), new Label(this.version)
//            ));

            props.add(tl.createConstraint().widthPercentage(30).horizontalAlign(Component.RIGHT), new Label(Dict.get(lang, "Player Name"))).add(pName)
                    .add(tl.createConstraint().widthPercentage(30).horizontalAlign(Component.RIGHT), new Label(Dict.get(lang, "Version"))).add(new Label(this.version));
            Command okCmd = new Command(Dict.get(lang, "OK")) {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    String playerName = savePlayerName(pName);
                    if (playerName == null) return;
                    player.setPlayerName(playerName);
                }
            };
//            settingDlg.add(BorderLayout.CENTER, props);
//            settingDlg.add(BorderLayout.SOUTH, new Button(okCmd));
//            settingDlg.setDisposeWhenPointerOutOfBounds(true);
//            settingDlg.show(0, 0, 200, 200);
            Dialog.show(Dict.get(lang, "Settings"), props, okCmd);
        });

        RadioButton rbEn = new RadioButton("English");
        RadioButton rbZh = new RadioButton("中文");
        ButtonGroup btnGroup = new ButtonGroup(rbEn, rbZh);
        btnGroup.addActionListener((e) -> {
            if (rbEn.isSelected()) {
                this.lang = "en";
            } else if (rbZh.isSelected()) {
                this.lang = "zh";
            }
            Storage.getInstance().writeObject("lang", this.lang);
            refreshButtons();
        });
        if (lang.equalsIgnoreCase("zh")) {
            rbZh.setSelected(true);
        } else {
            rbEn.setSelected(true);
        }

        entry.add(this.btnTutor);
        entry.add(this.btnPlay);
        entry.add(this.btnHelp)
                .add(this.btnSetting);
//                .add(this.btnExit);
        entry.add(BoxLayout.encloseX(rbEn, rbZh));

        mainForm.add(BorderLayout.CENTER, entry);
        this.currentComp = entry;
        mainForm.show();

        this.formTable = new Form(new LayeredLayout());
        this.formTable.getStyle().setBgColor(BACKGROUND_COLOR);
        this.formTable.getToolbar().hideToolbar();
        this.formTable.addComponent(this.table);

        this.player.connectServer(false);
    }

    void showPlayOption() {
        Object sgObj = Storage.getInstance().readObject("playerName");
        final TextField pName = new TextField("", Dict.get(lang, "Your Name")
                + "(" + Dict.get(lang, Dict.PNAME) + ")", 16, TextArea.ANY);
        pName.setMaxSize(16);
        if (sgObj != null) {
            pName.setText(sgObj.toString());
        }
        Command practiceCmd = new Command(Dict.get(lang, "Practice")) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                String playerName = savePlayerName(pName);
                if (playerName == null) {
                    btnPlay.setEnabled(true);
                    return;
                }
                btnPlay.setEnabled(false);
                player.startPlay(playerName, "practice");
            }
        };
        FontImage.setMaterialIcon(practiceCmd, FontImage.MATERIAL_DIRECTIONS_WALK, "Button");
        Command matchCmd = new Command(Dict.get(lang, "Match")) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                String playerName = savePlayerName(pName);
                if (playerName == null) {
                    btnPlay.setEnabled(true);
                    return;
                }
                btnPlay.setEnabled(false);
                player.startPlay(playerName);
            }
        };
        FontImage.setMaterialIcon(matchCmd, FontImage.MATERIAL_DIRECTIONS_RUN, "Button");
        int finPrac = Player.parseInteger(Storage.getInstance().readObject("finprac"));
        if (finPrac < 1) {
            int totalScore = Player.parseInteger(Storage.getInstance().readObject("tutor_score"));
            if (totalScore >= 80) {
                finPrac = 1;
                Storage.getInstance().writeObject("finprac", 1);
            }
        }
        matchCmd.setEnabled(finPrac > 0);

        Dialog.show("", pName, practiceCmd, matchCmd);
    }

    public Player getPlayer() {
        return player;
    }

    private String savePlayerName(TextField pName) {
        String playerName = pName.getText().trim();
        if (playerName.isEmpty()) return null;
        pName.stopEditing();
        playerName = StringUtil.replaceAll(playerName, "\"", "");
        playerName = StringUtil.replaceAll(playerName, "\\", "");
        playerName = StringUtil.replaceAll(playerName, "'", "");
        playerName = StringUtil.replaceAll(playerName, ":", " ");
        if (playerName.isEmpty()) return null;
        Storage.getInstance().writeObject("playerName", playerName);
        return playerName;
    }

    private Component currentComp;
    public boolean isMainForm = true;

    public void switchScene(final String scene) {
        Display.getInstance().lockOrientation(false);
        isMainForm = false;
        switch (scene) {
            case "entry":
                this.formMain.showBack();
                isMainForm = true;
                break;
            case "table":
                this.formTable.show();
                this.formTable.repaint();
                break;
            case "help":
                this.formHelp.show();
                break;
            case "tutor":
                if (this.formTutor == null) {
                    this.formTutor = new Form("Tutor", new BorderLayout());
                    this.formTutor.getStyle().setBgColor(BACKGROUND_COLOR);
                    this.formTutor.getToolbar().hideToolbar();
                    this.formTutor.addComponent(BorderLayout.CENTER, this.tutor);
                }

                this.tutor.showTopic();
                this.formTutor.show();
                break;
        }

//        this.formMain.setGlassPane(null);
//        this.formMain.repaint();
    }

    public void switchSceneDeprecated(final String scene) {
//        this.formMain.removeAll();
        switch (scene) {
            case "entry":
                if (this.currentComp != this.entry) {
                    this.formMain.replaceAndWait(currentComp, this.entry, null);
//                this.formMain.add(BorderLayout.CENTER, this.entry);
                    this.currentComp = this.entry;
                }
                break;
            case "table":
                if (this.currentComp != this.table) {
                    this.formMain.replaceAndWait(currentComp, this.table, null);
//                this.formMain.add(BorderLayout.CENTER, this.table);
                    this.currentComp = this.table;
                }
                break;
            case "help":
                if (this.currentComp != this.help) {
                    this.formMain.replaceAndWait(currentComp, this.help, null);
                    this.currentComp = this.help;
                }
                break;
            case "tutor":
                if (this.currentComp != this.tutor) {
                    this.formMain.replaceAndWait(currentComp, this.tutor, null);
                    this.currentComp = this.tutor;
                    this.tutor.showTopic();
                }
                break;
        }

        this.formMain.setGlassPane(null);
        this.formMain.repaint();
    }

    private String getPlayerID(Display disp) {
        String playerId = null;
        try {
//        List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
// WifiManager wimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//    String macAddress = wimanager.getConnectionInfo().getMacAddress();

            // skip this for now, may implement in future
//            String udid = disp.getUdid();
//            if (udid != null && !udid.trim().isEmpty()) {
//                return udid.trim();
//            }
//            String msisdn = disp.getMsisdn();
//            if (msisdn != null && !msisdn.trim().isEmpty()) {
//                return msisdn.trim();
//            }

            Storage storage = Storage.getInstance();
            Object pId = storage.readObject("playerId");
            if (pId == null || !pId.toString().startsWith("TLJ")) {
                pId = "TLJ" + Long.toString(System.currentTimeMillis(), 16);
                storage.writeObject("playerId", pId);
            }
            playerId = pId.toString();
        } catch (Exception e) {
            Dialog.show("Fail to get player ID", e.getMessage(), "OK", "");
        }
        return playerId;
    }
    /*
    private String getHelp1() {
        try {
            ConnectionRequest r = new ConnectionRequest();
            r.setPost(false);
            r.setUrl(Card.HELP_URL);
            NetworkManager.getInstance().addToQueueAndWait(r);
            Map<String, Object> result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            Map<String, Object> response = (Map<String, Object>) result.get("response");
            return response.get("listings").toString();
        } catch (Exception err) {
            Log.e(err);
            return null;
        }
    }

    private String getHelp2() {
        try {
            URL u = new URL(Card.HELP_URL);
            URL.HttpURLConnection conn = (URL.HttpURLConnection) u.openConnection();
            conn.connect();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            StringBuilder s = new StringBuilder();
            char[] buf = new char[2048];
            int num = 0;
            while ((num = in.read(buf)) != -1) {
                s.append(buf, 0, num);
            }
            return s.toString();
        } catch (Exception err) {
            Log.e(err);
            return "Not Available";
        }
    }
*/
    private String getHelp() {
        String s = "TBA\n";
        for (int i = 0; i < 10; i++) {
            s += "Test " + i + "\n";
        }
        return s;
    }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }

        if (this.isMainForm || this.player == null || this.player.tableEnded || !this.player.tableOn) {
            if (this.player != null) {
                player.disconnect();
            }
            Display.getInstance().exitApplication();
        }
    }

    public void destroy() {
        if (this.player != null) {
            player.disconnect();
        }
        Display.getInstance().exitApplication();
    }

    private String currentLang;
    private Map<String, Container> helpMap = new HashMap<>();
    private void showHelp(final String lang) {
        if (this.help == null) {
            this.help = new Container(new LayeredLayout());
            this.help.setSafeArea(true);
            this.formHelp = new Form(new BorderLayout());
//            this.formHelp.getStyle().setBgColor(BACKGROUND_COLOR);
            this.formHelp.getToolbar().hideToolbar();
            this.formHelp.addComponent(BorderLayout.CENTER, this.help);
        } else {
            if (lang.equals(currentLang)) {
                this.switchScene("help");
                return;
            }
            this.help.removeAll();
        }

        Container currentHelp = helpMap.get(lang);

        if (currentHelp == null) {
            switch (lang) {
                case "zh":
                    currentHelp = showHelpZH();
                    break;
                case "en":
                default:
                    currentHelp = showHelpEN();
            }

            helpMap.put(lang, currentHelp);
            currentLang = lang;
        }

        Button bReturn = new Button(Dict.get(lang, "Exit"));
        FontImage.setMaterialIcon(bReturn, FontImage.MATERIAL_EXIT_TO_APP);
        bReturn.setUIID("return");
        bReturn.addActionListener((e) -> {
            this.switchScene("entry");
        });

        this.help.add(currentHelp).add(bReturn);
        LayeredLayout ll = (LayeredLayout) help.getLayout();
        ll.setInsets(bReturn, "auto 0 0 auto");  //top right bottom left
        this.switchScene("help");
    }

    private Container showHelpZH() {
        Container content = new Container(BoxLayout.y());
        content.setScrollableY(true);
        content.setScrollableX(true);
        content.getAllStyles().setFgColor(0);

        SpanLabel lb = new SpanLabel("本游戏为六人四付找朋友打法，采用竞叫上庄，庄家叫主后拿底牌并扣底，\n然后找一个朋友（也可以不找，一个打五个，打成后升级翻倍）");
        content.add(lb);
        lb = new SpanLabel("如果被闲家抠底，则底牌分翻4倍（对子抠底则再乘以2,三张则再乘3, ...）");
        content.add(lb);
        lb = new SpanLabel("四张相同牌为炸弹，可以炸2对的拖拉机");
        content.add(lb);
        lb = new SpanLabel("拖拉机只能是连对，或者相连的三张/四张，对子和连接的三张不算拖拉机");
        content.add(lb);
        lb = new SpanLabel("甩牌失败扣分，每收回一张扣10分");
        content.add(lb);
        lb = new SpanLabel("当需要跟对子（或三张）时必须跟对，\n如果没有其它对子，但有三张（或四张）则必须拆了跟出");
        content.add(lb);
        return content;
    }

    private Container showHelpEN() {
        Container content = new Container(BoxLayout.y());
        content.setScrollableY(true);
        content.setScrollableX(false);

        SpanLabel lb = new SpanLabel("This game is played by six players,"
                + " using four full decks. Each player is dealt with 35 cards,"
                + " while the remaining 6 cards will be added to the declarer later."
                + " The bidding procedure is similar to Bridge game, each player can bid a specific point or pass."
                + " The declarer is the player who made the final point bid (contract point)."
                + " That means, after this point bid, other players all pass."
                + " Then the declarer choose which suit is trump, or NT means no-trump."
                + " After that, the remaining 6 cards is added to the declarer’s hand."
                + " The declarer then select any 6 cards to throw out, as the hole cards."
                + " At this point, the declarer can define the partner condition:"
                + " e.g. 2nd ♠A, it means who plays the second ♠A will be the declarer’s partner,"
                + " then all other 4 players will be the defenders."
                + " The defenders need collect enough points (equals or greater than the contract point) to beat the contract.");
        content.add(lb);
        Container p = new Container();
        content.add(p);
        p.add(boldText("Playing Stage:"));
        addLongText(p, "The declarer plays the first hand, then each player plays in a counter-clockwise order."
                + " The player who wins this round collects all the points (sum all the point cards played, if any),"
                + " and will be the next leading player, and so on."
                + " If a defender wins the final round and there are point cards in the hole cards,"
                + " the total points in the hole cards will be times by a multiple (4 or more, depends on the winning hand strength) and added the defenders’ collected points."
                + " If the contract is made, the declarer and partner is promoted to next rank,"
                + " otherwise the defenders are promoted to next rank.");

        content.add(theme.getImage("h2.png").scaledWidth(Display.getInstance().getDisplayWidth()));

        content.add(p = new Container());
        p.add(boldText("Point Cards:"));
        addLongText(p, "5 (5 points), 10 and K (10 points). 100 points per deck, total points is 400.");

        content.add(p = new Container());
        p.add(boldText("Card Rank"));
        addLongText(p, "(from low to high): 2, 3, 4 … 10, J, Q, K, A, game rank (not in trump suit), game rank (in trump suit), Black Joker, Red Joker.");

        content.add(p = new Container());
        p.add(boldText("Game Rank:"));
        addLongText(p, "The declarer’s current rank (In general, every player begins from Rank 2)");

        content.add(p = new Container());
        p.add(boldText("Trump:"));
        addLongText(p, "Red Joker, Black Joker and the game rank cards are always trumps (even in a NT game).");
        content.add(p = new Container());
        p.add(boldText("Flop Play:"));
        addLongText(p, "The leading player plays multiple combinations together. To make it a valid play, all the combinations must not be beaten by other players.");
        content.add(p = new Container());
        addLongText(p, "   e.g. the leading player try play ♥A♥K♥K, but another player has ♥A♥A, then the leading player is forced to play ♥K♥K (♥A will be returned to his/her hand), and get a 10 point penalty (each card returned get a 10 point penalty).");
        content.add(p = new Container());
        p.add(boldText("Ruff:"));
        addLongText(p, "A player can ruff by using his/her trump if the leading suit is empty in his/her hand.\n");
        content.add(p = new Container());
        p.add(boldText("Overruff:"));
        addLongText(p, "If the leading hand is a Flop, and two players can ruff, then only the strongest(longest) combination is compared to determine which is the winning hand.");
        content.add(p = new Container());
        addLongText(p, "    e.g. suppose ♥ is trump, game rank is 10");
        content.add(p = new Container());
        addLongText(p, "    a. the leading hand is ♠AQQJJ, one player ruffs with ♥A6655, then another player can overruff with ♥59988");
        content.add(p = new Container());
        addLongText(p, "    b. the leading hand is ♠AAJJ, one player ruffs with ♥JJ99, then another player can overruff with ♥QQ22");

        content.add(p = new Container());
        p.add(boldText("DaGuang:"));
        addLongText(p, "Defenders collected no point, declarer and partner is promoted by 3 ranks.");
        content.add(p = new Container());
        p.add(boldText("XiaoGuang:"));
        addLongText(p, "The total collected points are less than half of the contract point, declarer and partner is promoted by 2 ranks.");
        content.add(p = new Container());
        p.add(boldText("Bounce:"));
        addLongText(p, "The total collected points minus the contract point, for each additional 80 points, the defenders are promoted by 1 more rank.");
        content.add(p = new Container());
        p.add(boldText("1 vs 5:"));
        addLongText(p, "At the beginning of playing stage, the declarer can choose “1 vs 5” (no partner). If the contract is made, the declarer will get double promotion.");
        content.add(p = new Container());
        p.add(boldText("Match:"));
        addLongText(p, "The player whose rank passes Rank A wins the match. A full match (2 -> A) usually takes 3.5 to 4.5 hours.");

        content.add(p = new Container());
        p.add(boldText("Card Combinations\n"));
        content.add(p = new Container());
        p.add(boldText("Single:"));
        addLongText(p, "a single card");
        content.add(p = new Container());
        p.add(boldText("Pair:"));
        addLongText(p, "2 same cards");
        content.add(p = new Container());
        p.add(boldText("Tractor:"));
        addLongText(p, "connected pairs: e.g. 2233, 667788, 5577(while 6 is the game rank), ♠K♠K♠A♠A♥5♥5♠5♠5BBRR (while game rank is 5 and ♠ is trump, B is Black Joker, R is Red Joker)");
        content.add(p = new Container());
        p.add(boldText("Trips:"));
        addLongText(p, "3 same cards (if leaded, other player has to follow a pair if he has no trips to play)");
        content.add(p = new Container());
        p.add(boldText("Quads:"));
        addLongText(p, "4 same cards (bomb, can beat 2-pair tractor; if leaded, the follow play preference is: quads, trips + single, 2-pair tractor, 2 pairs, 1 pair + 2 singles, 4 singles)");
        content.add(p = new Container());
        p.add(boldText("Bulldozer:"));
        addLongText(p, "connected trips (or quads): e.g. 444555, JJJQQQKKK (if leaded, the follow play preference is (for 2-trips bulldozer): 2 trips, 1 trips + 1 pair + 1 single, 2-pair tractor + 2 singles, 2 pairs + 2 singles, 1 pair + 4 singles, 6 singles)");

        return content;
    }

    public static Label boldText(String txt) {
        Label boldLabel = new Label(txt);
        boldLabel.getAllStyles().setFont(Hand.fontGeneral);
        boldLabel.getAllStyles().setFgColor(0);
        return boldLabel;
    }

    private void addLongText(Container p, String txt) {
        while (txt.length() > 0) {
            int idx = txt.indexOf(" ");
            if (idx < 0) {
                p.add(new Label(txt));
                return;
            }
            p.add(new Label(txt.substring(0, idx)));
            txt = txt.substring(idx + 1);
        }
    }
}
