package com.ccd.tlj;

import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.DynamicImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ccheng
 */
public class Tutor extends Container {

    private final TuoLaJi main;

    Tutor(TuoLaJi main) {
        this.main = main;
        this.setLayout(new LayeredLayout());
    }

    int currentIndex = 0;
    Container index;

    public void showTopic() {
        if (index == null) {
            int idx = 0;
            Object sObj = Storage.getInstance().readObject("tutor");
            if (sObj != null) idx = Player.parseInteger(sObj);
            if (idx < 0) idx = 0;
            currentIndex = idx;
            index = new Container(BoxLayout.y());
            index.setScrollableY(true);

            List<Topic> topics = allTopics();
            index.add("Concepts");

            for (int x = 0; x < topics.size(); x++) {
                if (x == conceptTopicNum) {
                    index.add("Basic Play");
                }
                Topic topic = topics.get(x);
                if (x > currentIndex) topic.enableButton(false);
                index.add(topic.button);
            }
            this.add(index);

            Button bExit = new Button(Dict.get(main.lang, "Exit"));
            FontImage.setMaterialIcon(bExit, FontImage.MATERIAL_EXIT_TO_APP);
            bExit.setUIID("myExit");
            bExit.addActionListener((e) -> {
                main.switchScene("entry");
            });
            this.add(bExit);
            LayeredLayout ll = (LayeredLayout) this.getLayout();
            ll.setInsets(bExit, "auto 0 0 auto");   //top right bottom left
        }
    }

    int conceptTopicNum = 5;

    private List<Topic> allTopics() {
        int idx = 0;
        List<Topic> lst = new ArrayList<>();
        lst.add(new Topic(idx++, "point_cards", "Point Cards"));
        lst.add(new Topic(idx++, "card_rank", "Card Rank"));
        lst.add(new Topic(idx++, "combination", "Card Combinations"));
        lst.add(new Topic(idx++, "trump", "Trump"));
        lst.add(new Topic(idx++, "table", "Table Layout"));

        conceptTopicNum = lst.size();

        lst.add(new Topic(idx++, "lead", "Leading"));
        lst.add(new Topic(idx++, "follow", "Following"));
        lst.add(new Topic(idx++, "exchange", "Declarer: exchange hole cards"));
        lst.add(new Topic(idx++, "pass_declarer", "Pass to partner: declarer side"));
        lst.add(new Topic(idx++, "pass_defender", "Pass to partner: defenders"));
        lst.add(new Topic(idx++, "choose_side", "Choose the correct side"));
        return lst;
    }

    class Topic {
        final String title;
        final String id;
        final Button button;
        final int idx;

        Topic(int idx, String id, String title) {
            this.idx = idx;
            this.id = id;
            this.title = title;
            this.button = new Button(command());
        }

        void enableButton(boolean enabled) {
            this.button.setEnabled(enabled);
            if (enabled && this.idx > currentIndex) {
                Storage.getInstance().writeObject("tutor", this.idx);
                currentIndex = this.idx;
            }
        }

        Command command() {
            return new Command(title) {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    Dialog dlg = new Dialog(title, new BorderLayout());
                    Component content = null;
                    switch (id) {
                        case "point_cards":
                            content = topicPointCards();
                            break;
                        case "card_rank":
                            break;
                        case "game_rank":
                            break;
                        case "combination":
                            break;
                        case "trump":
                            break;
                        case "contract":
                            break;

                        case "lead":
                            break;
                        case "follow":
                            break;
                        case "exchange":
                            break;
                        case "pass_declarer":
                            break;
                        case "pass_defender":
                            break;
                        case "choose_side":
                            break;

                        default:
                            break;
                    }

                    Command okCmd = new Command("Done") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            dlg.dispose();
                        }
                    };
                    if (content != null) dlg.add(BorderLayout.CENTER, content);
                    dlg.add(BorderLayout.SOUTH, new Button(okCmd));
                    dlg.show(0, 0, 0, 0);
                }
            };
        }

        Component topicPointCards() {
            return BoxLayout.encloseY(new Label("Tutor Content"));
        }
    }

    class CardList extends DynamicImage {

        int bgColor = 0x00ffff;

        CardList(List<Card> cards) {
        }

        @Override
        protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
            g.setColor(this.bgColor);
            g.fillRoundRect(x, y, w, h, 60, 60);
        }
    }
}
