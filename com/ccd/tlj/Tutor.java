package com.ccd.tlj;

import com.codename1.components.SpanLabel;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.DynamicImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
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
    List<Topic> topics;

    public void showTopic() {
        if (index == null) {
            int idx = 0;
            Object sObj = Storage.getInstance().readObject("tutor");
            if (sObj != null) idx = Player.parseInteger(sObj);
            if (idx < 0) idx = 0;
            currentIndex = idx;

            topics = allTopics();

            index = new Container(BoxLayout.y());
            index.setScrollableY(true);

            GridLayout layout1 = new GridLayout(2);
            layout1.setAutoFit(true);
            Container index1 = new Container(layout1);
            for (int x = 0; x < basicTopicNum; x++) {
                Topic topic = topics.get(x);
                if (x > currentIndex) topic.enableButton(false);
                index1.add(topic.button);
            }

            GridLayout layout2 = new GridLayout(2);
            layout2.setAutoFit(true);
            Container index2 = new Container(layout2);
            for (int x = basicTopicNum; x < topics.size(); x++) {
                Topic topic = topics.get(x);
                if (x > currentIndex) topic.enableButton(false);
                index2.add(topic.button);
            }

            index.add(index1);
            index.add(index2);
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

    int basicTopicNum = 5;

    private List<Topic> allTopics() {
        int idx = 0;
        List<Topic> lst = new ArrayList<>();
        lst.add(new Topic(idx++, "point_cards", "Point Cards"));
        lst.add(new Topic(idx++, "card_rank", "Card Rank"));
        lst.add(new Topic(idx++, "combination", "Card Combinations"));
        lst.add(new Topic(idx++, "trump", "Trump"));
        lst.add(new Topic(idx++, "table", "Table Layout"));

        lst.add(new Topic(idx++, "lead", "Leading Play"));
        lst.add(new Topic(idx++, "follow", "Following Play"));
        lst.add(new Topic(idx++, "ruff", "Ruff & Over-ruff"));

        basicTopicNum = lst.size();

        lst.add(new Topic(idx++, "flop", "Flop Play"));
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
                    showContent();
                }
            };
        }

        void showContent() {
            Dialog dlg = new Dialog(new BorderLayout());
            Command okCmd = new Command("Done") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dlg.dispose();
                }
            };
            Button btnNext = new Button("Next");
            btnNext.setEnabled(idx < currentIndex);
            btnNext.addActionListener((e) -> {
                dlg.dispose();
                Topic nextTopic = topics.get(idx + 1);
                nextTopic.enableButton(true);
                nextTopic.showContent();
            });
            dlg.add(BorderLayout.SOUTH, BoxLayout.encloseXNoGrow(btnNext, new Button(okCmd)));
            Component content = null;
            switch (id) {
                case "point_cards":
                    content = topicPointCards(btnNext);
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

            if (content != null) {
                dlg.add(BorderLayout.CENTER, content);
            }
            dlg.show(0, 0, 0, 0);
        }

        Component topicPointCards(Button btnNext) {
            SpanLabel lb0 = new SpanLabel("Point cards are value cards. To win a game, defenders need to earn enough points(money) to beat the contract"
                    + "(final points, bid by declarer). Here are the whole point cards in a single deck:");
            Container content = BoxLayout.encloseY(lb0);

            Hand hand = main.getPlayer().getHand();
            List<Card> cards = new ArrayList<>();
            cards.add(new Card(Card.SPADE, 5));
            cards.add(new Card(Card.HEART, 5));
            cards.add(new Card(Card.CLUB, 5));
            cards.add(new Card(Card.DIAMOND, 5));
            cards.add(new Card(Card.SPADE, 10));
            cards.add(new Card(Card.HEART, 10));
            cards.add(new Card(Card.CLUB, 10));
            cards.add(new Card(Card.DIAMOND, 10));
            cards.add(new Card(Card.SPADE, 13));
            cards.add(new Card(Card.HEART, 13));
            cards.add(new Card(Card.CLUB, 13));
            cards.add(new Card(Card.DIAMOND, 13));
            CardList img = new CardList(cards);
            img.scale(hand.displayWidthNormal(cards.size()), hand.cardHeight + 10);
            content.add(img);

            content.add("Card 5: 5 points; Card 10: 10 points; Card K: 10 points.");
            content.add(" ");
            content.add("Quiz: What is the total points in a single deck?");

            RadioButton rb1 = new RadioButton("40");
            RadioButton rb2 = new RadioButton("80");
            RadioButton rb3 = new RadioButton("100");
            RadioButton rb4 = new RadioButton("150");
            RadioButton rb5 = new RadioButton("200");
            ButtonGroup btnGroup = new ButtonGroup(rb1, rb2, rb3, rb4, rb5);
            content.add(BoxLayout.encloseXNoGrow(rb1, rb2, rb3, rb4, rb5));

            btnGroup.addActionListener((e) -> {
                btnNext.setEnabled(rb3.isSelected());
            });
            return content;
        }
    }

    class CardList extends DynamicImage {

        int bgColor = 0x00ffff;
        final List<Card> cards;

        CardList(List<Card> cards) {
            this.cards = cards;
        }

        @Override
        protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
            if (cards == null || cards.isEmpty()) return;
            Hand hand = main.getPlayer().getHand();
            for (Card c : cards) {
                drawCard(g, c, x, y, hand.cardWidth, hand.cardHeight);
                x += hand.xPitch;
            }
        }

        private void drawCard(Graphics g, Card c, int x0, int y0, int cardW, int cardH) {
            Hand hand = main.getPlayer().getHand();
            g.setColor(hand.blackColor);
            g.drawRoundRect(x0 - 1, y0 - 1, cardW + 2, cardH + 2, 10, 10);
            g.setColor(hand.whiteColor);
            g.fillRoundRect(x0, y0, cardW, cardH, 10, 10);
            if (c.suite == Card.SPADE || c.suite == Card.CLUB || c.rank == Card.SmallJokerRank) {
                g.setColor(hand.blackColor);
            } else {
                g.setColor(hand.redColor);
            }

            if (c.rank < Card.SmallJokerRank) {
                g.setFont(hand.fontRank);
                String s = c.rankToString();
                if (s.length() < 2) {
                    g.drawString(s, x0 + 2, y0);
                } else {
                    g.drawString("" + s.charAt(0), x0 - 5, y0);
                    g.drawString("" + s.charAt(1), x0 + 20, y0);
                }
                g.setFont(hand.fontSymbol);
                g.drawString(Card.suiteSign(c.suite), x0 + 2, y0 + hand.fontRank.getHeight() - 5);
            } else {
                g.setFont(hand.fontSymbol);
                x0 += 5;
                int py = cardH / 5 - 2;
                g.drawString("J", x0, y0);
                g.drawString("O", x0, y0 + py);
                g.drawString("K", x0, y0 + py * 2);
                g.drawString("E", x0, y0 + py * 3);
                g.drawString("R", x0, y0 + py * 4);
            }
        }
    }
}
