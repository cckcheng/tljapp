package com.ccd.tlj;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ccheng
 */
public class Deck {
    private final int deckNumber;
    private final int playerNumber;
    private final int cardNumPerDeck = 54;
    private final int totalCards;

    public Deck(int deckNum, int playerNum) {
        this.deckNumber = deckNum;
        this.playerNumber = playerNum;
        this.totalCards = deckNum * cardNumPerDeck;
        this.init();
    }

    private List<Hand> wholeDeck = new ArrayList<>();

    public void init() {
    }

    private void deal(Player player) {
        int reserved = this.totalCards % this.playerNumber;
        if (reserved < this.playerNumber / 2) reserved += this.playerNumber;
        int num = (this.totalCards - reserved) / this.playerNumber;
        List<Hand> cards = wholeDeck.subList(0, num);
        player.addCards(cards);
        wholeDeck.removeAll(cards);
    }

    public static void main(String[] args) {
        Deck deck = new Deck(4, 6);

        Player[] players = new Player[6];

        for (Player p : players) {
            p = new Player();
            deck.deal(p);
//            System.out.println(p.showCards());
            System.out.println(p.showSplitCards());
        }
        System.out.println("Done");
    }
}
