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

    static final int BigJoker = 99;
    static final int SmallJoker = 33;
    private char suite;
    private int rank;

    public Card(char suite, int rank) {
        this.suite = suite;
        this.rank = rank;
    }

    public char getSuite() {
        return suite;
    }

    public int getRank() {
        return rank;
    }

    public static int suiteRank(char suite) {
        int n = 0;
        switch (suite) {
            case CLUB:
                n = 10;
                break;
            case DIAMOND:
                n = 20;
                break;
            case HEART:
                n = 30;
                break;
            case SPADE:
                n = 40;
                break;
            case SMALL_JOKER:
                n = 50;
                break;
            case BIG_JOKER:
                n = 60;
                break;
            default:
                n = 100;
                break;
        }
        return n;
    }

    @Override
    public String toString() {
        String s = "";
        switch (rank) {
            case 11:
                s += 'J';
                break;
            case 12:
                s += 'Q';
                break;
            case 13:
                s += 'K';
                break;
            case 14:
                s += 'A';
                break;
            case BigJoker:
            case SmallJoker:
                break;
            default:
                s += rank;
        }
        return suite + s;
    }

    @Override
    public int compareTo(Object o) {
        Card other = (Card) o;
        if (this.suite == other.suite) return this.rank > other.rank ? 1 : (this.rank == other.rank ? 0 : -1);

        int thisSuiteRank = suiteRank(this.suite);
        int otherSuiteRank = suiteRank(other.suite);
        return thisSuiteRank > otherSuiteRank ? 1 : (thisSuiteRank == otherSuiteRank ? 0 : -1);
    }
}
