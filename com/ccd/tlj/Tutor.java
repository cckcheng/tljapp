package com.ccd.tlj;

import com.codename1.components.SpanLabel;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.DynamicImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
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
            index.add("Before playing games, please finish this tutorial first:");

            GridLayout layout1 = new GridLayout(2);
            layout1.setAutoFit(true);
            Container index1 = new Container(layout1);
            for (int x = 0; x < basicTopicNum; x++) {
                Topic topic = topics.get(x);
                if (x > currentIndex) topic.enableButton(false);
                index1.add(topic.button);
            }

//            GridLayout layout2 = new GridLayout(2);
//            layout2.setAutoFit(true);
//            Container index2 = new Container(layout2);
            for (int x = basicTopicNum; x < topics.size(); x++) {
                Topic topic = topics.get(x);
                if (x > currentIndex) topic.enableButton(false);
                index1.add(topic.button);
            }

            index.add(index1);
//            index.add(index2);
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
        lst.add(new Topic(idx++, "trump", "Trump"));
        lst.add(new Topic(idx++, "combination", "Card Combinations"));
        lst.add(new Topic(idx++, "table", "Table Layout"));

        lst.add(new Topic(idx++, "bid", "Bidding"));
        lst.add(new Topic(idx++, "exchange", "Exchange Cards"));
        lst.add(new Topic(idx++, "basic", "Basic Play"));
        basicTopicNum = lst.size();
        lst.add(new Topic(idx++, "flop", "Flop Play"));
//        lst.add(new Topic(idx++, "advanced", "Advanced Play"));

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
            btnNext.setEnabled(false);
            btnNext.addActionListener((e) -> {
                dlg.dispose();
                Topic nextTopic = topics.get(idx + 1);
                nextTopic.enableButton(true);
                nextTopic.showContent();
            });
            if (idx < topics.size() - 1) {
                dlg.add(BorderLayout.SOUTH, BoxLayout.encloseXNoGrow(btnNext, new Button(okCmd)));
            } else {
                dlg.add(BorderLayout.SOUTH, new Button(okCmd));
                Storage.getInstance().writeObject("fintutor", 1);
                main.showPlayButton();
            }
            Component content = null;
            switch (id) {
                case "point_cards":
                    content = topicPointCards(btnNext);
                    break;
                case "card_rank":
                    content = topicCardRank(btnNext);
                    break;
                case "combination":
                    content = topicCombination(btnNext);
                    break;
                case "trump":
                    content = topicTrump(btnNext);
                    break;
                case "table":
                    content = topicTable(btnNext);
                    break;

                case "bid":
                    content = topicBidding(btnNext);
                    break;
                case "basic":
                    content = topicBasicPlay(btnNext);
                    break;
                case "exchange":
                    content = topicExchange(btnNext);
                    break;

                case "flop":
                    content = topicFlop(btnNext);
                    break;
                case "advanced":
                    content = topicAdvanced(btnNext);
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

        private Component topicCardRank(Button btnNext) {
            SpanLabel lb0 = new SpanLabel("Within a suit, the highest ranked card is Ace, then K,Q,J,10,9,8,7,6,5,4,3,2.");
            Container content = BoxLayout.encloseY(lb0);
            content.setScrollableY(true);
            lb0 = new SpanLabel("The leading player can play one or more cards of a single suit,"
                    + " then other players must play same number of cards of the same suit, in a couter-clockwise order."
                    + " The player who played the highest ranked card won this round, and get all the points included."
                    + " If two more players played the highest ranked card, the first player won.");
            content.add(lb0);

            content.add(" ");
            content.add("Quiz: ♥5, ♥9, ♥Q, ♥K, ♥K, ♥10");
            content.add("For the above play sequence, which player won this round?");
            RadioButton rb2 = new RadioButton("Second");
            RadioButton rb3 = new RadioButton("Third");
            RadioButton rb4 = new RadioButton("Fouth");
            RadioButton rb5 = new RadioButton("Fifth");
            ButtonGroup btnGroup = new ButtonGroup(rb2, rb3, rb4, rb5);
            content.add(BoxLayout.encloseXNoGrow(rb2, rb3, rb4, rb5));
            content.add("How many points does the winner get?");
            RadioButton rb1_1 = new RadioButton("15");
            RadioButton rb1_2 = new RadioButton("30");
            RadioButton rb1_3 = new RadioButton("35");
            RadioButton rb1_4 = new RadioButton("40");
            rb1_1.setEnabled(false);
            rb1_2.setEnabled(false);
            rb1_3.setEnabled(false);
            rb1_4.setEnabled(false);
            ButtonGroup btnGroup1 = new ButtonGroup(rb1_1, rb1_2, rb1_3, rb1_4);
            content.add(BoxLayout.encloseXNoGrow(rb1_1, rb1_2, rb1_3, rb1_4));

            btnGroup.addActionListener((e) -> {
                if (rb4.isSelected()) {
                    rb2.setEnabled(false);
                    rb3.setEnabled(false);
                    rb4.setEnabled(false);
                    rb5.setEnabled(false);
                    rb1_1.setEnabled(true);
                    rb1_2.setEnabled(true);
                    rb1_3.setEnabled(true);
                    rb1_4.setEnabled(true);
                }
            });

            btnGroup1.addActionListener((e) -> {
                btnNext.setEnabled(rb1_3.isSelected());
            });
            return content;
        }

        private Component topicTable(Button btnNext) {
            btnNext.setEnabled(false);
            SpanLabel lb0 = new SpanLabel("Game table illustrated as below:");
            Container content = BoxLayout.encloseY(lb0);
            content.setScrollableY(true);
            content.setScrollableX(true);
            content.add(main.theme.getImage("h2.png").scaledWidth(Display.getInstance().getDisplayWidth()));

            content.add("Quiz: Based on the sample table, which player is the declarer?");
            RadioButton rb1_1 = new RadioButton("#4");
            RadioButton rb1_2 = new RadioButton("#5");
            RadioButton rb1_3 = new RadioButton("#6");
            ButtonGroup btnGroup1 = new ButtonGroup(rb1_1, rb1_2, rb1_3);
            content.add(BoxLayout.encloseXNoGrow(rb1_1, rb1_2, rb1_3));

            RadioButton rb2_1 = new RadioButton("190");
            RadioButton rb2_2 = new RadioButton("75");
            ButtonGroup btnGroup2 = new ButtonGroup(rb2_1, rb2_2);
            rb2_1.setEnabled(false);
            rb2_2.setEnabled(false);

            RadioButton rb3_1 = new RadioButton("115");
            RadioButton rb3_2 = new RadioButton("120");
            ButtonGroup btnGroup3 = new ButtonGroup(rb3_1, rb3_2);
            rb3_1.setEnabled(false);
            rb3_2.setEnabled(false);

            content.add("What is the contract point?");
            content.add(BoxLayout.encloseXNoGrow(rb2_1, rb2_2));
            content.add("To win the game, how many more points need to be collected by the defenders?");
            content.add(BoxLayout.encloseXNoGrow(rb3_1, rb3_2));

            btnGroup1.addActionListener((e) -> {
                if (rb1_2.isSelected()) {
                    rb1_1.setEnabled(false);
                    rb1_2.setEnabled(false);
                    rb1_3.setEnabled(false);
                    rb2_1.setEnabled(true);
                    rb2_2.setEnabled(true);
                }
            });
            btnGroup2.addActionListener((e) -> {
                if (rb2_1.isSelected()) {
                    rb2_1.setEnabled(false);
                    rb2_2.setEnabled(false);
//                    content.scrollComponentToVisible(addon);  // not work
                    rb3_1.setEnabled(true);
                    rb3_2.setEnabled(true);
                }
            });
            btnGroup3.addActionListener((e) -> {
                btnNext.setEnabled(rb3_1.isSelected());
            });

            return content;
        }

        private Component topicBidding(Button btnNext) {
            SpanLabel lb0 = new SpanLabel("To be a successful declarer, your hand need to be stronger than average."
                    + " Apparently, the lower the contract point, the harder the contract to be made. On the bidding stage,"
                    + " there is a recommended minimum bid point right behind your current rank."
                    + " As a beginner, you can take that advice.");
            Container content = BoxLayout.encloseY(lb0);
            content.setScrollableY(true);
            lb0 = new SpanLabel("Once you become the declarer, you need choose the trump suit."
                    + " In order to set a suit to trumps, you must have the game-rank card of that suit (or a Joker for NT)."
                    + " E.g. suppose your current rank is 6, you want specify Spade as trump,"
                    + " then you must show ♠6 to other players.");
            content.add(lb0);
            btnNext.setEnabled(true);
            return content;
        }

        private Component topicExchange(Button btnNext) {
            SpanLabel lb0 = new SpanLabel("Declarer have a chance to exchange 6 cards after setting trump.");
            Container content = BoxLayout.encloseY(lb0);
            lb0 = new SpanLabel("At this point, you have to decide how to find your partner."
                    + " The partner definition is in this way: a player who plays a specific card will be my partner."
                    + " In general, the specific card (Partner Card) is an Ace (not trump)."
                    + " E.g. the Partner Card is: 2nd ♠A, it means who plays the second ♠A will be the declarer's partner.");
            content.add(lb0);
            content.add("The following are some good examples after the exchange:");

            Container subContainer = new Container();
            content.add(subContainer);

            Hand hand = main.getPlayer().getHand();
            List<Card> cards = new ArrayList<>();
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 10));
            CardList img = new CardList(cards);
            img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);
            subContainer.add(img);

            cards = new ArrayList<>();
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 13));
            cards.add(new Card(Card.SPADE, 5));
            img = new CardList(cards);
            img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);
            subContainer.add(img);

            cards = new ArrayList<>();
            cards.add(new Card(Card.SPADE, 13));
            cards.add(new Card(Card.SPADE, 13));
            cards.add(new Card(Card.SPADE, 10));
            img = new CardList(cards);
            img.scale(hand.displayWidthNormal(cards.size()), hand.cardHeight + 10);
            subContainer.add(img);

            lb0 = new SpanLabel("For the first 2 examples, you declare the Partner Card: 2nd ♠A."
                    + " Then you play ♠A first, play ♠K next(if you have one).");
            content.add(lb0);
            lb0 = new SpanLabel("For the third examples, you declare the Partner Card: 1st ♠A."
                    + " Then you play ♠K seperately.");
            content.add(lb0);

            lb0 = new SpanLabel("Normally, at the very beginning, no player knows how strong the declarer's hand is."
                    + " So nobody is willing to take the risk to be the partner of the declarer too early.");
            content.add(lb0);

            content.setScrollableY(true);
            btnNext.setEnabled(true);
            return content;
        }

        private Component topicBasicPlay(Button btnNext) {
            Label lb0 = TuoLaJi.boldText("Leading");
            Container content = BoxLayout.encloseY(lb0);
            content.setScrollableY(true);
            SpanLabel lb = new SpanLabel("When you are at the leading position, usually you should play your strong combinations of cards."
                    + " Quads, Tractors, Trips are considered the strong combinations, especially in a higher rank."
                    + " Obviously, the longer, the stronger."
                    + " Of course, if none of the opponents showed void sign of the suit you led,"
                    + " you should cash the Ace(s) first.");
            content.add(lb);
            lb = new SpanLabel("After that, you should always try to pass to your partner."
                    + " So as to keep the leading right in your side longer and get more points.");
            content.add(lb);
            lb0 = TuoLaJi.boldText("Follow Play");
            content.add(lb0);
            lb = new SpanLabel("When your partner is winning this round, you discard point card(s)."
                    + " Otherwise, you should avoid to play point card(s).");
            content.add(lb);
            lb = new SpanLabel("If you are void in the suit led, you can ruff and take the leading right."
                    + " The valid ruff must be in the same type of card combination led.");
            content.add(lb);

            btnNext.setEnabled(true);
            return content;
        }

        private Component topicFlop(Button btnNext) {
            SpanLabel lb0 = new SpanLabel("Sometimes you can try to play multiple combinations together - Flop Play."
                    + " Following are some examples:");
            Container content = BoxLayout.encloseYBottomLast(lb0);

            Container subContainer = new Container();
            content.add(subContainer);

            Hand hand = main.getPlayer().getHand();
            List<Card> cards = new ArrayList<>();
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 13));
            cards.add(new Card(Card.SPADE, 13));
            cards.add(new Card(Card.SPADE, 13));
            CardList img = new CardList(cards);
            img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);
            subContainer.add(img);

            cards = new ArrayList<>();
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 13));
            cards.add(new Card(Card.SPADE, 13));
            img = new CardList(cards);
            img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);
            subContainer.add(img);

            cards = new ArrayList<>();
            cards.add(new Card(Card.SPADE, 14));
            cards.add(new Card(Card.SPADE, 12));
            cards.add(new Card(Card.SPADE, 12));
            cards.add(new Card(Card.SPADE, 11));
            cards.add(new Card(Card.SPADE, 11));
            cards.add(new Card(Card.SPADE, 5));
            cards.add(new Card(Card.SPADE, 5));
            cards.add(new Card(Card.SPADE, 5));
            cards.add(new Card(Card.SPADE, 5));
            img = new CardList(cards);
            img.scale(hand.displayWidthNormal(cards.size()), hand.cardHeight + 10);
            subContainer.add(img);

            lb0 = new SpanLabel("Flop Play is powerful, but has potential risks."
                    + " If the Flop Play is invalid, there will be penalty points (10 points per card took back)."
                    + " E.g. you try to play AKK, but other player has AA,"
                    + " so you are forced to play KK and take back A, then 10 points will be added to your opponents.");
            content.add(lb0);

            content.add(TuoLaJi.boldText("Congratulations. Good luck and enjoy the game!"));
            content.setScrollableY(true);
            btnNext.setEnabled(true);
            return content;
        }

        private Component topicAdvanced(Button btnNext) {
            SpanLabel lb0 = new SpanLabel("Advanced Play");
            Container content = BoxLayout.encloseY(lb0);
            content.setScrollableY(true);
            content.add("Congratulations. Good luck and enjoy the game!");
            return content;
        }

        private Component topicTrump(Button btnNext) {
            SpanLabel lb0 = new SpanLabel("Jokers and game-rank(declarer's current rank) cards are always trumps."
                    + "The declarer can choose one suit to be a trump suit, or NT means no trump suit.");
            Container content = BoxLayout.encloseY(lb0);
            content.setScrollableY(true);
            lb0 = new SpanLabel("Suppose declarer's current rank is 8, ♥ is the trump suit."
                    + " Below is the trump card rank of a single deck, from high to low:");
            content.add(lb0);

            Hand hand = main.getPlayer().getHand();
            List<Card> cards = new ArrayList<>();
            cards.add(new Card(Card.JOKER, Card.BigJokerRank));
            cards.add(new Card(Card.JOKER, Card.SmallJokerRank));
            cards.add(new Card(Card.HEART, 8));
            for (int r = 14; r >= 2; r--) {
                if (r == 8) {
                    continue;
                }
                cards.add(new Card(Card.HEART, r));
            }
            CardList img = new CardList(cards);

            List<Card> addCards = new ArrayList<>();
            addCards.add(new Card(Card.SPADE, 8));
            addCards.add(new Card(Card.DIAMOND, 8));
            addCards.add(new Card(Card.CLUB, 8));
            img.insertCards(3, addCards);
            img.scale(hand.displayWidthNormal(cards.size()), 2 * (hand.cardHeight + 10) + 50);
            content.add(img);

            content.add("Quiz: For a single deck, how many trumps are there if no trump suit specified?");
            RadioButton rb1 = new RadioButton("2");
            RadioButton rb2 = new RadioButton("4");
            RadioButton rb3 = new RadioButton("6");
            RadioButton rb4 = new RadioButton("8");
            ButtonGroup btnGroup = new ButtonGroup(rb1, rb2, rb3, rb4);
            content.add(BoxLayout.encloseXNoGrow(rb1, rb2, rb3, rb4));
            btnGroup.addActionListener((e) -> {
                btnNext.setEnabled(rb3.isSelected());
            });
            return content;
        }
    }

    private Component topicCombination(Button btnNext) {
        SpanLabel lb0 = new SpanLabel("PAIR, TRIPS, QUADS (Samples as below)");
        Container content = BoxLayout.encloseY(lb0);
        content.setScrollableY(true);

        Hand hand = main.getPlayer().getHand();
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(Card.JOKER, Card.BigJokerRank));
        cards.add(new Card(Card.JOKER, Card.BigJokerRank));
        CardList imgPair = new CardList(cards);
        imgPair.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);

        cards = new ArrayList<>();
        cards.add(new Card(Card.HEART, 8));
        cards.add(new Card(Card.HEART, 8));
        cards.add(new Card(Card.HEART, 8));
        CardList imgTrips = new CardList(cards);
        imgTrips.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);

        cards = new ArrayList<>();
        cards.add(new Card(Card.CLUB, 5));
        cards.add(new Card(Card.CLUB, 5));
        cards.add(new Card(Card.CLUB, 5));
        cards.add(new Card(Card.CLUB, 5));
        CardList imgQuads = new CardList(cards);
        imgQuads.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);

        Container subContainer = new Container();
        subContainer.add(imgPair).add(imgTrips).add(imgQuads);
        content.add(subContainer);

        lb0 = new SpanLabel("TuoLaJi(Tractor): connected pairs (or trips/quads)."
                + " Following samples are based on game-rank 10, trump suit ♦:");
        content.add(lb0);

        cards = new ArrayList<>();
        cards.add(new Card(Card.CLUB, 6));
        cards.add(new Card(Card.CLUB, 6));
        cards.add(new Card(Card.CLUB, 5));
        cards.add(new Card(Card.CLUB, 5));
        CardList img1 = new CardList(cards);
        img1.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);

        cards = new ArrayList<>();
        cards.add(new Card(Card.SPADE, 11));
        cards.add(new Card(Card.SPADE, 11));
        cards.add(new Card(Card.SPADE, 11));
        cards.add(new Card(Card.SPADE, 9));
        cards.add(new Card(Card.SPADE, 9));
        cards.add(new Card(Card.SPADE, 9));
        CardList img2 = new CardList(cards);
        img2.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);

        cards = new ArrayList<>();
        cards.add(new Card(Card.JOKER, Card.BigJokerRank));
        cards.add(new Card(Card.JOKER, Card.BigJokerRank));
        cards.add(new Card(Card.JOKER, Card.SmallJokerRank));
        cards.add(new Card(Card.JOKER, Card.SmallJokerRank));
        cards.add(new Card(Card.DIAMOND, 10));
        cards.add(new Card(Card.DIAMOND, 10));
        cards.add(new Card(Card.CLUB, 10));
        cards.add(new Card(Card.CLUB, 10));
        cards.add(new Card(Card.DIAMOND, 14));
        cards.add(new Card(Card.DIAMOND, 14));
        CardList img3 = new CardList(cards);
        img3.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight + 10);
        subContainer = new Container();
        subContainer.add(img1).add(img2).add(img3);
        content.add(subContainer);

        lb0 = new SpanLabel("When one of the combinations is led,"
                + " the following player must try to play the same type of the combination."
                + " Take quads as an example, the follow play preference is: quads, 1 trips + 1 single, 2-pair tractor,"
                + " 2 pairs, 1 pair + 2 singles, 4 singles.");
        content.add(lb0);
        content.add("Special rule: Quads can beat 2-pair tractor, so it is also called Bomb.");

        lb0 = new SpanLabel("Quiz: Please select all the correct tractors (Assumption: game-rank 10, trump suit ♦)");
        content.add(lb0);

        subContainer = new Container();
        CheckBox cb1 = new CheckBox();
        subContainer.add(cb1);
        content.add(subContainer);
        cards = new ArrayList<>();
        cards.add(new Card(Card.SPADE, 8));
        cards.add(new Card(Card.SPADE, 8));
        cards.add(new Card(Card.SPADE, 7));
        cards.add(new Card(Card.SPADE, 7));
        CardList img = new CardList(cards);
        img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight);
        subContainer.add(img);
        Label hint1 = new Label("No doubt");
        hint1.setVisible(false);
        subContainer.add(hint1);

        subContainer = new Container();
        CheckBox cb2 = new CheckBox();
        subContainer.add(cb2);
        content.add(subContainer);
        cards = new ArrayList<>();
        cards.add(new Card(Card.CLUB, 14));
        cards.add(new Card(Card.CLUB, 14));
        cards.add(new Card(Card.CLUB, 13));
        cards.add(new Card(Card.CLUB, 13));
        cards.add(new Card(Card.CLUB, 13));
        img = new CardList(cards);
        img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight);
        subContainer.add(img);
        Label hint2 = new Label("Mixed pair and trips");
        hint2.setVisible(false);
        subContainer.add(hint2);

        subContainer = new Container();
        CheckBox cb3 = new CheckBox();
        subContainer.add(cb3);
        content.add(subContainer);
        cards = new ArrayList<>();
        cards.add(new Card(Card.HEART, 11));
        cards.add(new Card(Card.HEART, 11));
        cards.add(new Card(Card.HEART, 9));
        cards.add(new Card(Card.HEART, 9));
        img = new CardList(cards);
        img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight);
        subContainer.add(img);
        Label hint3 = new Label("9 and J is connected now, since 10 is out!");
        hint3.setVisible(false);
        subContainer.add(hint3);

        subContainer = new Container();
        CheckBox cb4 = new CheckBox();
        subContainer.add(cb4);
        content.add(subContainer);
        cards = new ArrayList<>();
        cards.add(new Card(Card.DIAMOND, 7));
        cards.add(new Card(Card.DIAMOND, 7));
        cards.add(new Card(Card.DIAMOND, 7));
        cards.add(new Card(Card.DIAMOND, 5));
        cards.add(new Card(Card.DIAMOND, 5));
        cards.add(new Card(Card.DIAMOND, 5));
        img = new CardList(cards);
        img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight);
        subContainer.add(img);
        Label hint4 = new Label("Not connected");
        hint4.setVisible(false);
        subContainer.add(hint4);

        subContainer = new Container();
        CheckBox cb5 = new CheckBox();
        subContainer.add(cb5);
        content.add(subContainer);
        cards = new ArrayList<>();
        cards.add(new Card(Card.SPADE, 10));
        cards.add(new Card(Card.SPADE, 10));
        cards.add(new Card(Card.HEART, 10));
        cards.add(new Card(Card.HEART, 10));
        img = new CardList(cards);
        img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight);
        subContainer.add(img);
        Label hint5 = new Label("Two pair with same rank, not qualify!");
        hint5.setVisible(false);
        subContainer.add(hint5);

        subContainer = new Container();
        CheckBox cb6 = new CheckBox();
        subContainer.add(cb6);
        content.add(subContainer);
        cards = new ArrayList<>();
        cards.add(new Card(Card.DIAMOND, 10));
        cards.add(new Card(Card.DIAMOND, 10));
        cards.add(new Card(Card.SPADE, 10));
        cards.add(new Card(Card.SPADE, 10));
        cards.add(new Card(Card.DIAMOND, 14));
        cards.add(new Card(Card.DIAMOND, 14));
        cards.add(new Card(Card.DIAMOND, 13));
        cards.add(new Card(Card.DIAMOND, 13));
        img = new CardList(cards);
        img.scale(hand.displayWidthNormal(cards.size()) + 20, hand.cardHeight);
        subContainer.add(img);
        Label hint6 = new Label("Nice one! The longer, the better!");
        hint6.setVisible(false);
        subContainer.add(hint6);

        content.add(" ");
        CheckBox cb0 = new CheckBox("Show Hint");
        content.add(cb0);
        cb0.addActionListener((e) -> {
            hint1.setVisible(cb0.isSelected());
            hint2.setVisible(cb0.isSelected());
            hint3.setVisible(cb0.isSelected());
            hint4.setVisible(cb0.isSelected());
            hint5.setVisible(cb0.isSelected());
            hint6.setVisible(cb0.isSelected());
        });

        cb1.addActionListener((e) -> {
            btnNext.setEnabled(cb1.isSelected() && !cb2.isSelected()
                    && cb3.isSelected() && !cb4.isSelected() && !cb5.isSelected() && cb6.isSelected());
        });
        cb2.addActionListener((e) -> {
            btnNext.setEnabled(cb1.isSelected() && !cb2.isSelected()
                    && cb3.isSelected() && !cb4.isSelected() && !cb5.isSelected() && cb6.isSelected());
        });
        cb3.addActionListener((e) -> {
            btnNext.setEnabled(cb1.isSelected() && !cb2.isSelected()
                    && cb3.isSelected() && !cb4.isSelected() && !cb5.isSelected() && cb6.isSelected());
        });
        cb4.addActionListener((e) -> {
            btnNext.setEnabled(cb1.isSelected() && !cb2.isSelected()
                    && cb3.isSelected() && !cb4.isSelected() && !cb5.isSelected() && cb6.isSelected());
        });
        cb5.addActionListener((e) -> {
            btnNext.setEnabled(cb1.isSelected() && !cb2.isSelected()
                    && cb3.isSelected() && !cb4.isSelected() && !cb5.isSelected() && cb6.isSelected());
        });
        cb6.addActionListener((e) -> {
            btnNext.setEnabled(cb1.isSelected() && !cb2.isSelected()
                    && cb3.isSelected() && !cb4.isSelected() && !cb5.isSelected() && cb6.isSelected());
        });
        return content;
    }

    class CardList extends DynamicImage {

        int bgColor = 0x00ffff;
        final List<Card> cards;

        CardList(List<Card> cards) {
            this.cards = cards;
        }

        List<Card> addCards;
        int aIndex = -1;

        void insertCards(int idx, List<Card> aCards) {
            addCards = aCards;
            aIndex = idx;
        }

        @Override
        protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
            if (cards == null || cards.isEmpty()) return;
            Hand hand = main.getPlayer().getHand();
            int i = 0;
            for (Card c : cards) {
                if (addCards != null && i == aIndex) {
                    int y0 = y;
                    for (Card a : addCards) {
                        drawCard(g, a, x, y0, hand.cardWidth, hand.cardHeight);
                        y0 += hand.cardHeight * 2 / 3;
                    }
                    x += hand.xPitch;
                }

                drawCard(g, c, x, y, hand.cardWidth, hand.cardHeight);
                x += hand.xPitch;
                i++;
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
