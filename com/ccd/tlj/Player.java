package com.ccd.tlj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author ccheng
 */
public class Player {

    private List<Hand> hand = new ArrayList<>();
    private List<Hand> jokers = new ArrayList<>();
    private List<Hand> spades = new ArrayList<>();
    private List<Hand> hearts = new ArrayList<>();
    private List<Hand> diamonds = new ArrayList<>();
    private List<Hand> clubs = new ArrayList<>();
    private List<Hand> trumps = new ArrayList<>();

    public Player() {
    }

    public void addCards(List<Hand> cards) {
        this.hand.addAll(cards);
        Collections.sort(hand, Collections.reverseOrder());

    }

    public String showCards() {
        StringBuilder sb = new StringBuilder();
        for (Hand c : this.hand) {
            sb.append(c.toString());
        }

        return sb.toString();
    }

    public String showSplitCards() {
        StringBuilder sb = new StringBuilder();
        for (Hand c : this.jokers) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Hand c : this.spades) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Hand c : this.hearts) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Hand c : this.diamonds) {
            sb.append(c.toString());
        }
        sb.append("\n");
        for (Hand c : this.clubs) {
            sb.append(c.toString());
        }
        sb.append("\n");

        return sb.toString();
    }
}
