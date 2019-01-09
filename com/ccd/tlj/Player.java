package com.ccd.tlj;

import com.codename1.io.Log;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.sf.json.JSONObject;
/**
 *
 * @author ccheng
 */
public class Player {
//    static final String TLJ_HOST = "tlj.webhop.me";
    static final String TLJ_HOST = "172.16.107.204";
    static final int TLJ_PORT = 6688;
    static final int TIME_OUT_SECONDS = 10;
    private final String playerName;
    private final Form mainForm;

    public Player(String playerName, Form mainForm) {
        this.playerName = playerName;
        this.mainForm = mainForm;
    }

    private MySocket mySocket = null;

    public void connectServer() {
        if (!Socket.isSupported()) {
            Dialog.show("Alert", "Socket is not supported", "OK", "");
            return;
        }
        if (this.mySocket != null) {
            if (!mySocket.isConnected()) {
                Socket.connect(TLJ_HOST, TLJ_PORT, mySocket);
            }
        } else {
            this.mySocket = new MySocket();
            Socket.connect(TLJ_HOST, TLJ_PORT, mySocket);
        }
        /*
        new UITimer(new Runnable() {
            @Override
            public void run() {
                if (mySocket.isConnected()) {
                    Dialog.show("Good", "Start New Game Now.", "OK", "Cancel");
                }
            }
        }).schedule(TIME_OUT_SECONDS * 1000, false, mainForm);
         */
    }

    public void disconnect() {
        if (this.mySocket != null) {
            if (this.mySocket.isConnected()) {
                mySocket.closeConnection();
                return;
            }
        }
        Display.getInstance().exitApplication();
    }

    static final String actionJoinTable = "join_table";

    class MySocket extends SocketConnection {

        private boolean closeRequested = false;

        public void closeConnection() {
            this.closeRequested = true;
        }

        private void sendRequest(OutputStream os, String action) throws IOException {
//            JsonObject json = Json.createObjectBuilder()
//                    .add(action, action)
//                    .build();
            JSONObject json = new JSONObject();
            json.put("action", action);
            os.write(json.toString().getBytes());
        }

        private void processReceived(String msg) {
            Log.p("Received: " + msg);
//            JsonObject json = Json.createReader(new StringReader(msg)).readObject();
//            Log.p("Received: " + json.toString());
        }

        @Override
        public void connectionError(int errorCode, String message) {
            Dialog.show("Error", "Connection Error. Please check your network.", "OK", "");
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
//            Dialog.show("Success", "Connected.", "OK", "");
            byte[] buffer = new byte[4096];
            try {
                sendRequest(os, actionJoinTable);
                while (isConnected() && !closeRequested) {
                    int n = is.available();
                    if (n > 0) {
                        n = is.read(buffer, 0, 4096);
                        if (n < 0) break;
                        processReceived(new String(buffer, 0, n));
                    } else {
                        Thread.sleep(1000);
                    }
                }
                is.close();
                os.close();
            } catch (Exception err) {
                Dialog.show("Error", "Connection Error: " + err.getMessage(), "OK", "");
            }
//            Dialog.show("Alert", "Closed.", "OK", "");
            Display.getInstance().exitApplication();
        }
    }
}
