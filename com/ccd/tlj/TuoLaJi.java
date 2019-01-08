package com.ccd.tlj;


import com.codename1.io.Log;
import com.codename1.ui.Button;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class TuoLaJi {

    static public int BACKGROUND_COLOR = 0x008000;
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
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }

//        String s = "Hi World: " + Character.toChars(0x1F0B4);
//        String s = "Player 1: " + "\uD83C\uDCC1";
        Display disp = Display.getInstance();
        disp.lockOrientation(false);

        String title = "Bid Tractor";
//        String title = "";
        BoxLayout layout = BoxLayout.y();
        Form mainForm = new Form(title, layout);
        mainForm.getStyle().setBgColor(BACKGROUND_COLOR);
        mainForm.getToolbar().hideToolbar();
//        mainForm.getToolbar().addCommandToLeftSideMenu("New Game", null, (e) -> {
//            Log.p("Start New Game");
//        });

//        Style sTitle = mainForm.getToolbar().getTitleComponent().getUnselectedStyle();
//        sTitle.setFont(Hand.fontSymbol);
        Label lbTitle = new Label(title);
//        lbTitle.getStyle().setAlignment(CENTER);
        mainForm.add(lbTitle);

        TextField pName = new TextField("", "Your Name", 20, TextArea.ANY);
//        Button bPlay = new Button("Play", "PlayButton");
        Button bPlay = new Button("Play");
        bPlay.addActionListener((e) -> {
            String playerName = pName.getText().trim();
            if (playerName.isEmpty()) {
                Dialog.show("Name Required", "Please input your name", "OK", "");
            } else {
                startGame(mainForm, playerName);
            }
        });
        Button bExit = new Button("Exit");
        bExit.addActionListener((e) -> {
            if (this.player != null) {
                player.disconnect();
            } else {
                Display.getInstance().exitApplication();
            }
        });
        mainForm.add(pName)
                .add(bPlay)
                .add(bExit);

        mainForm.show();
    }

    private Player player = null;

    private void startGame(Form mainForm, String playerName) {
        if (this.player == null) {
            player = new Player(playerName, mainForm);
        }

        player.connectServer();
        if (true) {
            return;
        }

        Hand hand = new Hand();
        hand.addCard(new Card(Card.SPADE, 8));
        hand.addCard(new Card(Card.SPADE, 8));
        hand.addCard(new Card(Card.CLUB, 9));
        hand.addCard(new Card(Card.DIAMOND, 12));
        hand.addCard(new Card(Card.CLUB, 11));
        hand.addCard(new Card(Card.DIAMOND, 14));
        hand.addCard(new Card(Card.CLUB, 10));
        hand.addCard(new Card(Card.DIAMOND, 14));
        hand.addCard(new Card(Card.CLUB, 10));
        hand.addCard(new Card(Card.SPADE, 5));

        hand.addCard(new Card(Card.DIAMOND, 9));
        hand.addCard(new Card(Card.DIAMOND, 3));
        hand.addCard(new Card(Card.CLUB, 11));
        hand.addCard(new Card(Card.DIAMOND, 12));
        hand.addCard(new Card(Card.DIAMOND, 10));
        hand.addCard(new Card(Card.DIAMOND, 10));
        hand.addCard(new Card(Card.SPADE, 5));
        hand.addCard(new Card(Card.SPADE, 6));
        hand.addCard(new Card(Card.SPADE, 8));
        hand.addCard(new Card(Card.DIAMOND, 9));

        hand.addCard(new Card(Card.SPADE, 13));
        hand.addCard(new Card(Card.CLUB, 11));
        hand.addCard(new Card(Card.DIAMOND, 13));
        hand.addCard(new Card(Card.SPADE, 10));
        hand.addCard(new Card(Card.DIAMOND, 12));
        hand.addCard(new Card(Card.CLUB, 10));
        hand.addCard(new Card(Card.SPADE, 5));
        hand.addCard(new Card(Card.SPADE, 6));
        hand.addCard(new Card(Card.JOKER, Card.BigJokerRank));
        hand.addCard(new Card(Card.JOKER, Card.SmallJokerRank));

        hand.addCard(new Card(Card.CLUB, 9));
        hand.addCard(new Card(Card.CLUB, 9));
        hand.addCard(new Card(Card.CLUB, 9));
        hand.addCard(new Card(Card.CLUB, 9));
        hand.addCard(new Card(Card.CLUB, 9));

        hand.sortCards('#', 0, true);
//        hand.addCard(new Card(Card.HEART, 9));
//        hand.addCard(new Card(Card.DIAMOND, 9));
//        hand.addCard(new Card(Card.CLUB, 11));
//        hand.addCard(new Card(Card.DIAMOND, 12));
//        hand.addCard(new Card(Card.CLUB, 10));
//        hand.addCard(new Card(Card.DIAMOND, 12));
//        hand.sortCards('#', 0, true);

        mainForm.add(BorderLayout.CENTER, hand);
//        hand.sortCards(Card.CLUB, 10, false);
//        hand.sortCards(Card.JOKER, 10, false);
        hand.sortCards(Card.DIAMOND, 7, false);

        String playerInfo = playerName + ", Rank 2, Current Contractor";
        Label lbInfo = new Label(playerInfo);
        lbInfo.getStyle().setAlignment(CENTER);

        mainForm.add(BorderLayout.SOUTH, lbInfo);
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
