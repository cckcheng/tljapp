package com.ccd.tlj;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author ccheng
 */
public class Hand extends Component {

    static final int TOTAL_SUITES = 4;
    static final int MAX_CARDS = 28; // maximum cards per line to display

    private List<Card> trumps = new ArrayList<>();
    private List<Card> spades = new ArrayList<>();
    private List<Card> hearts = new ArrayList<>();
    private List<Card> diamonds = new ArrayList<>();
    private List<Card> clubs = new ArrayList<>();

    private List<List> suites = new ArrayList<>();
    private List<Character> suiteIndex = new ArrayList<>();

    private List<Card> upperList = new ArrayList<>();
    private List<Card> lowerList = new ArrayList<>();
    private List<Card> selected = new ArrayList<>();
    
    private final Player player;

    int maxWidth = 1900;
    int xPitch = 60;
    int yPitch = 30;
    int cardWidth = 100;
    int cardHeight = 160;

    int popHeight = 20;
    int hReserved = 0;

    Hand(Player player) {
        suites.add(this.spades);
        suites.add(this.hearts);
        suites.add(this.clubs);
        suites.add(this.diamonds);

        suiteIndex.add(Card.SPADE);
        suiteIndex.add(Card.HEART);
        suiteIndex.add(Card.CLUB);
        suiteIndex.add(Card.DIAMOND);
        
        this.player = player;
    }

//    int xPR1 = 0;   // right player 1 coodinate
//    int yPR1 = 0;   // right player 1 coodinate
//    int xPR2 = 0;   // right player 2 coodinate
//    int yPR2 = 0;   // right player 2 coodinate
//    int xPL1 = 0;   // left player 1 coodinate
//    int yPL1 = 0;   // left player 1 coodinate
//    int xPL2 = 0;   // left player 2 coodinate
//    int yPL2 = 0;   // left player 2 coodinate
//    int xPopp = 0;   // oppisite player coodinate
//    int yPopp = 0;   // opposite player coodinate

    private void init() {
        int w = getWidth();
        int h = getHeight();
        if (h > w) w = h;
        this.cardWidth = w / 19;
        this.cardHeight = this.cardWidth * 3 / 2;
        this.xPitch = w / MAX_CARDS;
        this.yPitch = (this.cardHeight - 10) / 5;

        this.hReserved = this.yPitch * 2 + 10;
        this.maxWidth = w;

//        this.xPL1 = this.xPL2 = getX() + 5;
//        this.xPR1 = this.xPR2 = getX() + w - 50;
//        this.yPL1 = this.yPR1 = getY() + h - h / 4 - 20;
//        this.yPL2 = this.yPR2 = getY() + h / 2 - 20;
//        this.xPopp = getX() + w / 4;
//        this.yPopp = getY() + h / 4 - 20;
    }

    public void addCard(Card c) {
        switch (c.suite) {
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
            default:
                this.trumps.add(c);
                break;
        }
    }

    private void makeLists1_3(int idx, boolean singleOnTop) {
        // list0 - 1 suite ( and trumps if singleOnTop = true)
        // list1 - (trumps if singleOnTop = true) + 3 suites
        List list0 = this.lowerList;
        List list1 = this.upperList;
        if (singleOnTop) {
            list0 = this.upperList;
            list1 = this.lowerList;
        }

        list0.addAll(this.suites.get(idx));
        int count = TOTAL_SUITES - 1;
        int nextIdx = idx + 1;
        while (count > 0) {
            if (nextIdx == TOTAL_SUITES) nextIdx = 0;
            list1.addAll(this.suites.get(nextIdx));
            nextIdx++;
            count--;
        }
    }

    public void sortCards(char trumpSuite, int gameRank, boolean doPreSort) {
        this.setOpaque(false);
        this.upperList.clear();
        this.lowerList.clear();
        if (doPreSort) {
            int idx = -1;
            for (int x = 0; x < this.trumps.size(); x++) {
                Card c = this.trumps.get(x);
                if (c.suite != Card.JOKER) {
                    idx = x;
                    break;
                }
            }
            if (idx >= 0) {
                List<Card> subLst = this.trumps.subList(idx, this.trumps.size());
                this.trumps = this.trumps.subList(0, idx);
                for (Card c : subLst) {
                    this.suites.get(this.suiteIndex.indexOf(c.suite)).add(c);
                }
            }

            Collections.sort(this.trumps, Collections.reverseOrder());
            Collections.sort(this.spades, Collections.reverseOrder());
            Collections.sort(this.hearts, Collections.reverseOrder());
            Collections.sort(this.diamonds, Collections.reverseOrder());
            Collections.sort(this.clubs, Collections.reverseOrder());
        }

        if (gameRank == 0) {
            splitSuites(trumpSuite);
            return;
        }

        int idx0 = 0;
        if (trumpSuite != Card.JOKER) {
            idx0 = this.suiteIndex.indexOf(trumpSuite);
        }

        for (int count = TOTAL_SUITES, idx = idx0; count > 0; count--, idx++) {
            if (idx == TOTAL_SUITES) idx = 0;
            moveTrumpCards(this.suites.get(idx), gameRank);
        }

        if (trumpSuite != Card.JOKER) {
            List<Card> tmpCards = this.suites.get(idx0);
            this.trumps.addAll(tmpCards);
            tmpCards.clear();
        }
        splitSuites(trumpSuite);

        this.setOpaque(true);
    }

    private void splitSuites(char trumpSuite) {
        this.upperList.addAll(this.trumps);
        int lenT = this.trumps.size();
        int lenS = this.spades.size();
        int lenH = this.hearts.size();
        int lenC = this.clubs.size();
        int lenD = this.diamonds.size();
        int total = lenT + lenS + lenH + lenC + lenD;
        int halfLen = total / 2 + 1;
        int tolerance = 3;
        int threshold = halfLen - tolerance;

        if (lenT >= threshold) {
            for (int i = 0; i < TOTAL_SUITES; i++) {
                List<Card> cc = this.suites.get(i);
                if (!cc.isEmpty()) {
                    this.lowerList.addAll(cc);
                    continue;
                }

                if (i == 1) {
                    this.lowerList.addAll(this.suites.get(3));
                    this.lowerList.addAll(this.suites.get(2));
                    break;
                } else if (i == 2) {
                    this.lowerList.addAll(0, this.suites.get(3));
                    break;
                }
            }
            return;
        }

        List<Integer> suiteLens = new ArrayList<>();
        suiteLens.add(lenS);
        suiteLens.add(lenH);
        suiteLens.add(lenC);
        suiteLens.add(lenD);

        int maxLen = Collections.max(suiteLens);
        int idx0 = suiteLens.indexOf(maxLen);
        if (maxLen >= threshold) {
            makeLists1_3(idx0, false);
            return;
        }

        boolean redTrump = (trumpSuite == Card.HEART || trumpSuite == Card.DIAMOND);
        boolean blackTrump = (trumpSuite == Card.SPADE || trumpSuite == Card.CLUB);

        if (redTrump) {
            int biasS = Math.abs(lenT + lenS - halfLen);
            int biasC = Math.abs(lenT + lenC - halfLen);
            if (biasC < biasS && biasC <= tolerance) {
                makeLists1_3(suiteIndex.indexOf(Card.CLUB), true);
                return;
            } else if (biasS <= tolerance) {
                makeLists1_3(suiteIndex.indexOf(Card.SPADE), true);
                return;
            }
        } else if (blackTrump) {
            int biasH = Math.abs(lenT + lenH - halfLen);
            int biasD = Math.abs(lenT + lenD - halfLen);
            if (biasD < biasH && biasD <= tolerance) {
                makeLists1_3(suiteIndex.indexOf(Card.DIAMOND), true);
                return;
            } else if (biasH <= tolerance) {
                makeLists1_3(suiteIndex.indexOf(Card.HEART), true);
                return;
            }
        } else {
            idx0 = 0;
            int bias = Math.abs(lenT + suiteLens.get(idx0) - halfLen);
            int idx = 1;
            while (bias > 0) {
                int newBias = Math.abs(lenT + suiteLens.get(idx) - halfLen);
                if (newBias < bias) {
                    bias = newBias;
                    idx0 = idx;
                }
                if (++idx == TOTAL_SUITES) break;
            }
            if (bias <= tolerance) {
                makeLists1_3(idx0, true);
                return;
            }
        }

        int seq = 1;
        int biasSH = Math.abs(lenS + lenH - halfLen);
        int minBias = biasSH;
        int biasSD = Math.abs(lenS + lenD - halfLen);
        if (biasSD < minBias) {
            seq = 2;
            minBias = biasSD;
        }
        int biasCH = Math.abs(lenC + lenH - halfLen);
        if (biasCH < minBias) {
            seq = 3;
            minBias = biasCH;
        }
        int biasCD = Math.abs(lenC + lenD - halfLen);
        if (biasCD < minBias) {
            seq = 4;
            minBias = biasCD;
        }

        switch (seq) {
            case 1:  //S+H
                this.lowerList.addAll(this.spades);
                this.lowerList.addAll(this.hearts);
                this.upperList.addAll(this.diamonds);
                this.upperList.addAll(this.clubs);
                break;
            case 2:  //S+D
                this.lowerList.addAll(this.spades);
                this.lowerList.addAll(this.diamonds);
                this.upperList.addAll(this.hearts);
                this.upperList.addAll(this.clubs);
                break;
            case 3:  //C+H
                this.lowerList.addAll(this.clubs);
                this.lowerList.addAll(this.hearts);
                this.upperList.addAll(this.diamonds);
                this.upperList.addAll(this.spades);
                break;
            case 4:  //C+D
                this.lowerList.addAll(this.clubs);
                this.lowerList.addAll(this.diamonds);
                this.upperList.addAll(this.hearts);
                this.upperList.addAll(this.spades);
                break;
        }
    }

    private void moveTrumpCards(List<Card> cards, int gameRank) {
        if (cards.isEmpty()) return;
        List<Card> tmpTrumps = new ArrayList<>();
        for (Card c : cards) {
            if (c.rank == gameRank) tmpTrumps.add(c);
        }
        this.trumps.addAll(tmpTrumps);
        cards.removeAll(tmpTrumps);
    }

    int blackColor = 0x000000;
    int redColor = 0xff0000;
    int whiteColor = 0xffffff;
    static public Font fontRank = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    static public Font fontGeneral = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    static public Font fontSymbol = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

    private void drawCard(Graphics g, Card c) {
        int x0 = 5;
        int y0 = -cardHeight;
        if (this.selected.contains(c)) {
            y0 -= popHeight;
        }
        g.setColor(blackColor);
        g.drawRoundRect(x0 - 1, y0 - 1, cardWidth + 2, cardHeight + 2, 10, 10);
        g.setColor(whiteColor);
        g.fillRoundRect(x0, y0, cardWidth, cardHeight, 10, 10);
        if (c.suite == Card.SPADE || c.suite == Card.CLUB || c.rank == Card.SmallJokerRank) {
            g.setColor(blackColor);
        } else {
            g.setColor(redColor);
        }

        if (c.rank < Card.SmallJokerRank) {
            g.setFont(fontRank);
            String s = c.rankToString();
            if (s.length() < 2) {
                g.drawString(s, x0 + 2, y0);
            } else {
                g.drawString("" + s.charAt(0), x0 - 5, y0);
                g.drawString("" + s.charAt(1), x0 + 20, y0);
            }
            g.setFont(fontSymbol);
            g.drawString(Card.suiteSign(c.suite), x0 + 2, y0 + fontRank.getHeight() - 5);
        } else {
            g.setFont(fontSymbol);
            x0 += 5;
            g.drawString("J", x0, y0);
            g.drawString("O", x0, y0 + yPitch);
            g.drawString("K", x0, y0 + yPitch * 2);
            g.drawString("E", x0, y0 + yPitch * 3);
            g.drawString("R", x0, y0 + yPitch * 4);
        }
    }
    /*
    @Override
    public void paintBackgrounds(Graphics g) {
        this.getStyle().setBgTransparency(0);
//        this.getStyle().setBgColor(TuoLaJi.BACKGROUND_COLOR);
//        g.setColor(blackColor);
//        g.drawLine(getX(), getY() + getHeight(), getX() + 100, getY());
//        g.drawLine(getX() + 100, 0, getX() + getWidth() - 50, getY() + Display.getInstance().getDisplayHeight());
    }
*/

    boolean flip = false;
    @Override
    public void paintBackground(Graphics g) {
        g.translate(-g.getTranslateX(), -g.getTranslateY());
//        g.clearRect(0, 0, getWidth(), getY() + getHeight());
        if (flip) return;
        init();

        g.setColor(TuoLaJi.BACKGROUND_COLOR);
        int x = getX();
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight * 5 / 6;

        g.fillRect(0, 0, getX() + getWidth(), getY() + getHeight());

        g.translate(x, y1);
        int px = this.xPitch;
        if (this.upperList.size() > MAX_CARDS) px = this.maxWidth / this.upperList.size();
        for (Card c : this.upperList) {
            drawCard(g, c);
            g.translate(px, 0);
        }
        g.translate(-g.getTranslateX(), -g.getTranslateY());

        g.translate(x, y0);
        px = this.xPitch;
        if (this.lowerList.size() > MAX_CARDS) px = this.maxWidth / this.lowerList.size();
        for (Card c : this.lowerList) {
            drawCard(g, c);
            g.translate(px, 0);
        }
        g.translate(-g.getTranslateX(), -g.getTranslateY());
    }

    @Override
    public void pointerPressed(int x, int y) {
        if(!this.player.isPlaying) return;
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight;
        int y2 = y1 - cardHeight * 5 / 6;
        int x0 = getX() + 5;
        int x1 = x0 + (this.upperList.size() - 1) * xPitch + cardWidth;
        int x2 = x0 + (this.lowerList.size() - 1) * xPitch + cardWidth;
        Card c = selectCard(x, y, x0, x1, x2, y0, y1, y2);
        if (c != null) dragged.add(c);
//        if (this.selected.contains(c)) {
//            this.selected.remove(c);
//        } else {
//            this.selected.add(c);
//        }
    }

    Set<Card> dragged = new HashSet<>();

    @Override
    public void pointerDragged(int x[], int y[]) {
        if(!this.player.isPlaying) return;
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight;
        int y2 = y1 - cardHeight * 4 / 5;
        int x0 = getX() + 5;
        int x1 = x0 + (this.upperList.size() - 1) * xPitch + cardWidth;
        int x2 = x0 + (this.lowerList.size() - 1) * xPitch + cardWidth;

//        for (int i = 0; i < x.length; i++) {
//            dragged.add(selectCard(x[i], y[i], x0, x1, x2, y0, y1, y2));
//        }

        Card c1 = selectCard(x[0], y[0], x0, x1, x2, y0, y1, y2);
        if (c1 == null) return;
        int lastIdx = x.length - 1;
        Card c2 = selectCard(x[lastIdx], y[lastIdx], x0, x1, x2, y0, y1, y2);
        if (c2 == null || c2 == c1) {
            dragged.add(c1);
            return;
        }

        if (this.upperList.contains(c1)) {
            if (this.lowerList.contains(c2)) return;
            int i = this.upperList.indexOf(c1);
            int j = this.upperList.indexOf(c2);
            if (i < j) {
                dragged.addAll(upperList.subList(i, j + 1));
            } else {
                dragged.addAll(upperList.subList(j, i + 1));
            }
        } else if (this.lowerList.contains(c2)) {
            int i = this.lowerList.indexOf(c1);
            int j = this.lowerList.indexOf(c2);
            if (i < j) {
                dragged.addAll(lowerList.subList(i, j + 1));
            } else {
                dragged.addAll(lowerList.subList(j, i + 1));
            }
        }
    }

 /*
    int startX = -1;
    int startY = -1;

    @Override
    public void pointerDragged(int x, int y) {
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight;
        int y2 = y1 - cardHeight * 5 / 6;
        int x0 = getX() + 5;
        int x1 = x0 + (this.upperList.size() - 1) * xPitch + cardWidth;
        int x2 = x0 + (this.lowerList.size() - 1) * xPitch + cardWidth;
        if (y > y0 || y < y2) return;
        if (x < x0) return;
        if (y < y1 && x > x1) return;   // upper
        if (y >= y1 && x > x2) return;  // lower

        this.startX = x;
        this.startY = y;
    }
    
    @Override
    public void pointerReleased(int x, int y) {
        if (this.startX < 0) return;
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight;
        int y2 = y1 - cardHeight * 5 / 6;
        int x0 = getX() + 5;
        int x1 = x0 + (this.upperList.size() - 1) * xPitch + cardWidth;
        int x2 = x0 + (this.lowerList.size() - 1) * xPitch + cardWidth;
        if (y > y0 || y < y2 || x < x0 || (y < y1 && x > x1) || (y >= y1 && x > x2)) {
            this.startX = -1;
            return;
        }

        Card c1 = selectCard(this.startX, this.startY, x0, x1, x2, y0, y1, y2);
        Card c2 = selectCard(x, y, x0, x1, x2, y0, y1, y2);
        this.startX = this.startY = -1;
        if (c1 == c2) {
            if (this.selected.contains(c1)) {
                this.selected.remove(c1);
            } else {
                this.selected.add(c1);
            }
            return;
        }

        List<Card> dragged = new ArrayList<>();
        if (this.upperList.contains(c1)) {
            if (this.lowerList.contains(c2)) return;
            int i = this.upperList.indexOf(c1);
            int j = this.upperList.indexOf(c2);
            if (i < j) {
                dragged.addAll(upperList.subList(i, j));
            } else {
                dragged.addAll(upperList.subList(j, i));
            }
        } else if (this.lowerList.contains(c2)) {
            int i = this.lowerList.indexOf(c1);
            int j = this.lowerList.indexOf(c2);
            if (i < j) {
                dragged.addAll(lowerList.subList(i, j));
            } else {
                dragged.addAll(lowerList.subList(j, i));
            }
        } else {
            return;
        }

        for (Card c : dragged) {
            if (this.selected.contains(c)) {
                this.selected.remove(c);
            } else {
                this.selected.add(c);
            }
        }
    }
*/

    @Override
    public void pointerReleased(int x, int y) {
        if(!this.player.isPlaying) return;
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight;
        int y2 = y1 - cardHeight * 4 / 5;
        int x0 = getX() + 5;
        int x1 = x0 + (this.upperList.size() - 1) * xPitch + cardWidth;
        int x2 = x0 + (this.lowerList.size() - 1) * xPitch + cardWidth;
        Card c1 = selectCard(x, y, x0, x1, x2, y0, y1, y2);
        if (c1 == null) {
            dragged.clear();
            return;
        }

        dragged.add(c1);
        for (Card c : dragged) {
            if (this.selected.contains(c)) {
                this.selected.remove(c);
            } else {
                this.selected.add(c);
            }
        }
        dragged.clear();
    }

    Card selectCard(int x, int y, int x0, int x1, int x2, int y0, int y1, int y2) {
        if (y > y0 || y < y2) return null;
        if (x < x0) return null;
        if (y < y1 && x > x1) return null;   // upper
        if (y >= y1 && x > x2) return null;  // lower

        Card c = null;
        if (y < y1) {
            int px = xPitch;
            if (this.upperList.size() > MAX_CARDS) px = this.maxWidth / this.upperList.size();
            int idx = (x - x0) / px;
            if (idx >= this.upperList.size()) idx = this.upperList.size() - 1;
            c = this.upperList.get(idx);
        } else {
            int px = xPitch;
            if (this.lowerList.size() > MAX_CARDS) px = this.maxWidth / this.lowerList.size();
            int idx = (x - x0) / px;
            if (idx >= this.lowerList.size()) idx = this.lowerList.size() - 1;
            c = this.lowerList.get(idx);
        }

        return c;
    }
}
