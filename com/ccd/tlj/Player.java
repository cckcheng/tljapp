package com.ccd.tlj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author ccheng
 */
public class Player {

    private List<Card> hand = new ArrayList<>();
    private List<Card> jokers = new ArrayList<>();
    private List<Card> spades = new ArrayList<>();
    private List<Card> hearts = new ArrayList<>();
    private List<Card> diamonds = new ArrayList<>();
    private List<Card> clubs = new ArrayList<>();
    private List<Card> trumps = new ArrayList<>();

    public Player() {
    }

    public void addCards(List<Card> cards) {
        this.hand.addAll(cards);
        Collections.sort(hand, Collections.reverseOrder());

        for (Card c : cards) {
            switch (c.getSuite()) {
                case Card.BIG_JOKER:
                case Card.SMALL_JOKER:
                    this.jokers.add(c);
                    break;
                case Card.SPADE:
                    this.spades.add(c);
                    break;
                case Card.HEART:
                    this.hearts.add(c);
                    break;
                case Card.DIAMOND:
                    this.diamonds.add(c);
                    break;
                case Card.CLUB:
                    this.clubs.add(c);
                    break;
            }
        }

        Collections.sort(this.jokers);
        Collections.sort(this.spades);
        Collections.sort(this.hearts);
        Collections.sort(this.diamonds);
        Collections.sort(this.clubs);
    }

    public String showCards() {
        StringBuilder sb = new StringBuilder();
        for (Card c : this.hand) {
            sb.append(c.toString());
        }

        return sb.toString();
    }

    public String showSplitCards() {
        StringBuilder sb = new StringBuilder();
        for (Card c : this.jokers) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Card c : this.spades) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Card c : this.hearts) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Card c : this.diamonds) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Card c : this.clubs) {
            sb.append(c.toString());
        }
        sb.append("\n");

        return sb.toString();
    }
}
