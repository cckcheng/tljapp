package com.ccd.tlj;


import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UITimer;
import com.codename1.util.StringUtil;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose
 * of building native mobile applications using Java.
 */
public class TuoLaJi {

    static public int BACKGROUND_COLOR = Card.DEBUG_MODE ? 0xffffff : 0x008000;
    private Form current;
    private Resources theme;

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
    private Button btnPlay = null;
    private UITimer connTimer = null;

    public void enableButtons() {
        if (this.btnPlay != null) {
            this.btnPlay.setEnabled(true);
            this.btnPlay.setText("Play");
        }
        this.connTimer.cancel();
    }

    public void onConnectionError() {
        if (this.btnPlay != null) {
            this.btnPlay.setText("Network Error");
            new UITimer(new Runnable() {
                @Override
                public void run() {
                    enableButtons();
                }
            }).schedule(3000, false, this.formMain);
        }
    }

    public String version = "1.02";
//        String title = "Bid Tractor";
    String title = "Langley TuoLaJi";
    public void start() {
        if(current != null){
            current.show();
            return;
        }

        Display disp = Display.getInstance();
        disp.lockOrientation(false);
        disp.requestFullScreen();
        disp.setNoSleep(true);
        disp.setScreenSaverEnabled(false);
        disp.setBuiltinSoundsEnabled(true);
        this.version = disp.getProperty("AppVersion", this.version);

        BoxLayout layout = BoxLayout.y();
        Form mainForm = new Form(title, layout);
        this.formMain = mainForm;
        mainForm.getStyle().setBgColor(BACKGROUND_COLOR);
        mainForm.getToolbar().hideToolbar();
//        mainForm.getToolbar().addCommandToLeftSideMenu("New Game", null, (e) -> {
//            Log.p("Start New Game");
//        });

//        Style sTitle = mainForm.getToolbar().getTitleComponent().getUnselectedStyle();
//        sTitle.setFont(Hand.fontSymbol);
        Label lbTitle = new Label(title);
//        lbTitle.getStyle().setAlignment(CENTER);
        lbTitle.getStyle().setFont(Hand.fontRank);
        mainForm.add(lbTitle);

        String playerId = getPlayerID(disp);
        if (playerId == null) {
            Dialog.show("Error", "Failed to generate Player ID", "OK", "");
            return;
        }

        TextField pName = new TextField("", "Your Name", 20, TextArea.ANY);
        Object sObj = Storage.getInstance().readObject("playerName");
        if (sObj != null) {
            pName.setText(sObj.toString());
        }

        Command okCmd = new Command("OK");
        Button bPlay = new Button("Play");
        this.btnPlay = bPlay;
        this.connTimer = new UITimer(new Runnable() {
            @Override
            public void run() {
                if (bPlay.isEnabled()) return;
                Dialog.show("Error", "Failed to connect, please try again later.", okCmd);
                bPlay.setEnabled(true);
                bPlay.setText("Play");
            }
        });

        FontImage.setMaterialIcon(bPlay, FontImage.MATERIAL_PEOPLE);
        bPlay.addActionListener((e) -> {
            String playerName = pName.getText().trim();
            if (playerName.isEmpty()) {
                Dialog.show("Name Required", "Please input your name", okCmd);
            } else {
                pName.stopEditing();
                playerName = StringUtil.replaceAll(playerName, "\"", "");
                playerName = StringUtil.replaceAll(playerName, "\\", "");
                playerName = StringUtil.replaceAll(playerName, "'", "");
                Storage.getInstance().writeObject("playerName", playerName);
                startGame(playerId, playerName);
                bPlay.setEnabled(false);
                bPlay.setText("Connecting...");
                connTimer.schedule(Player.TIME_OUT_SECONDS * 1000, false, mainForm);
            }
        });

//        BrowserComponent browser = new BrowserComponent();    // not work
//        WebBrowser browser = new WebBrowser();    // not work
//        browser.setURL(Card.HELP_URL);
        SpanLabel helpContent = new SpanLabel();
        helpContent.setText("TuoLaJi is a very popular Chinese card game.\nMore help\nmorehelp");
        Dialog helpDlg = new Dialog(BorderLayout.center());
        helpDlg.add(BorderLayout.CENTER, helpContent);
        helpDlg.setDisposeWhenPointerOutOfBounds(true);
        Button bHelp = new Button("Help");
        FontImage.setMaterialIcon(bHelp, FontImage.MATERIAL_HELP);
        bHelp.addActionListener((e) -> {
            helpDlg.show(0, 0, 100, 100);
        });

        Button bExit = new Button("Exit");
        FontImage.setMaterialIcon(bExit, FontImage.MATERIAL_EXIT_TO_APP);
        bExit.addActionListener((e) -> {
            if (this.player != null) {
                player.disconnect();
            }
            Display.getInstance().exitApplication();
        });

        Button bAbout = new Button("About");
        FontImage.setMaterialIcon(bAbout, FontImage.MATERIAL_INFO_OUTLINE);
        bAbout.addActionListener((e) -> {
            disp.vibrate(2);
            disp.playBuiltinSound(Display.SOUND_TYPE_WARNING);
            Dialog.show("About", this.title + "\nVersion " + this.version, okCmd);
        });

        mainForm.add(pName);
        mainForm.add(bPlay)
                .add(bHelp)
                .add(bAbout)
                .add(bExit);

        mainForm.show();
    }

    private String getPlayerID(Display disp) {
        String playerId = null;
        try {
//        List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
// WifiManager wimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//    String macAddress = wimanager.getConnectionInfo().getMacAddress();
            String msisdn = disp.getMsisdn();
            if (msisdn != null) return msisdn;
            String udid = disp.getUdid();
            if (udid != null) return udid;

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

    private Player player = null;

    private void startGame(String playerId, String playerName) {
        if (this.player == null) {
            player = new Player(playerId, playerName, this);
        } else {
            player.setPlayerName(playerName);
        }
        player.connectServer();
   }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }

    public void destroy() {
    }

}
