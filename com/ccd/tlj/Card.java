package com.ccd.tlj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author ccheng
 */
public class Card implements Comparable {
    static final char SPADE = 'S';
    static final char HEART = 'H';
    static final char DIAMOND = 'D';
    static final char CLUB = 'C';
    static final char JOKER = 'V';
//    static final char BIG_JOKER = '\u265B';
//    static final char SMALL_JOKER = '\u2657';

    static final int SmallJokerRank = 97;
    static final int BigJokerRank = 98;

    public final char suite;
    public final int rank;

    Card(char suite, int rank) {
        this.suite = suite;
        this.rank = rank;
    }

    public static Card create(String s) {
        s = s.trim();
        if (s.length() < 2) return null;
        char suite = s.charAt(0);
        int rank = Integer.parseInt(s.substring(1));
        return new Card(suite, rank);
    }

    public int trumpRank(char trumpSuite, int gameRank) {
        if (this.rank == gameRank) {
            return this.suite == trumpSuite ? 15 : 14;
        }

        if (this.rank == SmallJokerRank) {
            return trumpSuite == Card.JOKER ? 16 : 17;
        }
        if (this.rank == BigJokerRank) {
            return trumpSuite == Card.JOKER ? 15 : 16;
        }

        return this.rank < gameRank ? this.rank : this.rank - 1;
    }

    public String rankToString() {
        if (this.rank <= 10) return "" + this.rank;
        switch (this.rank) {
//            case 10:
//                return "\u2491";
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            case 14:
                return "A";
        }

        return "V";
    }

    public static String cardsToString(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return "";
        String ret = "";
        for (Card c : cards) {
            ret += "," + c.suite + c.rank;
        }

        return ret.substring(1);
    }

    public static List<Card> fromString(String cards, char trump, int gameRank) {
        if (cards == null || cards.isEmpty()) return null;

        List<Card> lst = new ArrayList<>();
        while (true) {
            int x = cards.indexOf(',');
            if (x < 0) {
                Card c = Card.create(cards);
                if (c != null) lst.add(c);
                break;
            } else {
                Card c = Card.create(cards.substring(0, x));
                if (c != null) lst.add(c);
                cards = cards.substring(x + 1);
            }
        }
        Collections.sort(lst, (Card a, Card b) -> {
            boolean aTrump = a.suite == trump || a.rank == gameRank || a.suite == Card.JOKER;
            boolean bTrump = b.suite == trump || b.rank == gameRank || b.suite == Card.JOKER;

            if (aTrump && bTrump) {
                return b.trumpRank(trump, gameRank) - a.trumpRank(trump, gameRank);
            } else if (aTrump) {
                return -1;
            } else if (bTrump) {
                return 1;
            }

            return b.compareTo(a);
        });
        return lst;
    }

    public static String suiteSign(char suit) {
        switch (suit) {
            case SPADE:
                return "\u2660";
            case HEART:
                return "\u2665";
            case DIAMOND:
                return "\u2666";
            case CLUB:
//                return "\u2663";
                return "\u2667";
        }

        return "" + JOKER;
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) return false;
//        Card otherCard = (Card) obj;
//        return this.suite == otherCard.suite && this.rank == otherCard.rank;
//    }

    @Override
    public int compareTo(Object o) {
        if (o == null) return 1;
        Card otherCard = (Card) o;
        if (this.suite == otherCard.suite) {
            return this.rank - otherCard.rank;
        }

        if (this.suite == DIAMOND) return -1;
        if (otherCard.suite == DIAMOND) return 1;
        return this.suite - otherCard.suite;
    }
}
