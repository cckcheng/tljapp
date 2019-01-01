package com.ccd.tlj;

/**
 *
 * @author ccheng
 */
public class Card implements Comparable {
    static final char SPADE = '\u2660';
    static final char HEART = '\u2665';
    static final char DIAMOND = '\u2666';
    static final char CLUB = '\u2663';
    static final char BIG_JOKER = '\u265B';
    static final char SMALL_JOKER = '\u2657';

    static final int SmallJokerRank = 97;
    static final int BigJokerRank = 98;

    public final char suite;
    public final int rank;

    Card(char suite, int rank) {
        this.suite = suite;
        this.rank = rank;
    }

    public int trumpRank(char trumpSuite, int gameRank) {
        if (this.rank == gameRank) {
            return this.suite == trumpSuite ? 15 : 14;
        }

        if (this.rank == SmallJokerRank) {
            return trumpSuite == 'V' ? 16 : 17;
        }
        if (this.rank == BigJokerRank) {
            return trumpSuite == 'V' ? 15 : 16;
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

    public String suiteSign() {
        switch (this.suite) {
            case 'S':
                return "" + SPADE;
            case 'H':
                return "" + HEART;
            case 'D':
                return "" + DIAMOND;
            case 'C':
                return "" + CLUB;
        }

        return "V";
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) return 1;
        Card otherCard = (Card) o;
        if (this.suite == otherCard.suite) {
            return this.rank - otherCard.rank;
        }

        return this.suite - otherCard.suite;
    }
}
