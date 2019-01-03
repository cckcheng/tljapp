package com.ccd.tlj;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author ccheng
 */
public class Hand extends Component {

    private List<Card> trumps = new ArrayList<>();
    private List<Card> spades = new ArrayList<>();
    private List<Card> hearts = new ArrayList<>();
    private List<Card> diamonds = new ArrayList<>();
    private List<Card> clubs = new ArrayList<>();

    private List<Card> upperList = new ArrayList<>();
    private List<Card> lowerList = new ArrayList<>();
    private List<Card> selected = new ArrayList<>();

    int xPitch = 60;
    int yPitch = 30;
    int cardWidth = 100;
    int cardHeight = 160;

    int hReserved = 30;

    Hand() {
    }

    private void init() {
        int w = getWidth();
        int h = getHeight();
        if (h > w) w = h;
        this.cardWidth = w / 19;
        this.cardHeight = this.cardWidth * 3 / 2;
        this.xPitch = w / 33;
        this.yPitch = (this.cardHeight - 10) / 5;

//        this.hReserved = this.yPitch + 2;
        this.hReserved = 0;
    }

    public void addCard(Card c) {
        switch (c.suite) {
            case 'S':
                this.spades.add(c);
                break;
            case 'H':
                this.hearts.add(c);
                break;
            case 'D':
                this.diamonds.add(c);
                break;
            case 'C':
                this.clubs.add(c);
                break;
            default:
                this.trumps.add(c);
                break;
        }
    }

    public void sortCards() {
        Collections.sort(this.trumps, Collections.reverseOrder());
        Collections.sort(this.spades, Collections.reverseOrder());
        Collections.sort(this.hearts, Collections.reverseOrder());
        Collections.sort(this.diamonds, Collections.reverseOrder());
        Collections.sort(this.clubs, Collections.reverseOrder());

        this.upperList.addAll(this.trumps);
        this.upperList.addAll(this.spades);
        int upperLen = this.upperList.size();
        if (upperLen >= 15) {
            this.lowerList.addAll(this.hearts);
            this.lowerList.addAll(this.clubs);
            this.lowerList.addAll(this.diamonds);
        } else {
            int lenSH = upperLen + this.hearts.size();
            int lenSD = upperLen + this.diamonds.size();
            if (lenSH <= lenSD) {
                this.upperList.addAll(this.hearts);
                this.lowerList.addAll(this.clubs);
                this.lowerList.addAll(this.diamonds);
            } else {
                this.upperList.addAll(this.diamonds);
                this.lowerList.addAll(this.hearts);
                this.lowerList.addAll(this.clubs);
            }
        }
    }

    int blackColor = 0x000000;
    int redColor = 0xff0000;
    int whiteColor = 0xffffff;
    Font fontRank = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    Font fontSymbol = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

    private void drawCard(Graphics g, Card c) {
        int x0 = 5;
        int y0 = -cardHeight;
        if (this.selected.contains(c)) {
            y0 -= 20;
        }
        g.setColor(blackColor);
        g.drawRoundRect(x0 - 1, y0 - 1, cardWidth + 2, cardHeight + 2, 10, 10);
        g.setColor(whiteColor);
        g.fillRoundRect(x0, y0, cardWidth, cardHeight, 10, 10);
        if (c.suite == 'S' || c.suite == 'C' || c.rank == Card.SmallJokerRank) {
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
            g.drawString(c.suiteSign(), x0 + 2, y0 + 70);
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

    @Override
    public void paint(Graphics g) {
        init();

        g.translate(-g.getTranslateX(), -g.getTranslateY());
        g.setColor(blackColor);
        g.drawLine(getX(), getY() + getHeight(), getX() + 100, getY());
        g.drawLine(getX() + 100, 0, getX() + getWidth() - 50, getY() + Display.getInstance().getDisplayHeight());
        int x = getX();
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight * 5 / 6;
        g.translate(x, y1);
        for (Card c : this.upperList) {
            drawCard(g, c);
            g.translate(xPitch, 0);
        }
        g.translate(-g.getTranslateX(), -g.getTranslateY());

        g.translate(x, y0);
        for (Card c : this.lowerList) {
            drawCard(g, c);
            g.translate(xPitch, 0);
        }
        g.translate(-g.getTranslateX(), -g.getTranslateY());
    }

    @Override
    public void pointerPressed(int x, int y) {
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight;
        int y2 = y1 - cardHeight * 5 / 6;
        int x0 = getX() + 5;
        int x1 = x0 + (this.upperList.size() - 1) * xPitch + cardWidth;
        int x2 = x0 + (this.lowerList.size() - 1) * xPitch + cardWidth;
        Card c = selectCard(x, y, x0, x1, x2, y0, y1, y2);
        if (this.selected.contains(c)) {
            this.selected.remove(c);
        } else {
            this.selected.add(c);
        }
    }
    /*
    @Override
    public void pointerDragged(int x[], int y[]) {
        int y0 = getY() + getHeight() - hReserved;
        int y1 = y0 - cardHeight;
        int y2 = y1 - cardHeight * 4 / 5;
        int x0 = getX() + 5;
        int x1 = x0 + (this.upperList.size() - 1) * xPitch + cardWidth;
        int x2 = x0 + (this.lowerList.size() - 1) * xPitch + cardWidth;

        Set<Card> cc = new HashSet<>();
        for (int i = 0; i < x.length; i++) {
            cc.add(selectCard(x[i], y[i], x0, x1, x2, y0, y1, y2));
        }

        for (Card c : cc) {
            if (this.selected.contains(c)) {
                this.selected.remove(c);
            } else {
                this.selected.add(c);
            }
        }
    }
*/
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
        this.startY = x;
    }

    @Override
    public void pointerReleased(int x, int y) {
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
        this.startY = x;
    }

    Card selectCard(int x, int y, int x0, int x1, int x2, int y0, int y1, int y2) {
        if (y > y0 || y < y2) return null;
        if (x < x0) return null;
        if (y < y1 && x > x1) return null;   // upper
        if (y >= y1 && x > x2) return null;  // lower
        int idx = (x - x0) / xPitch;

        Card c = null;
        if (y < y1) {
            if (idx >= this.upperList.size()) idx = this.upperList.size() - 1;
            c = this.upperList.get(idx);
        } else {
            if (idx >= this.lowerList.size()) idx = this.lowerList.size() - 1;
            c = this.lowerList.get(idx);
        }

        return c;
    }
}
