package com.ccd.tlj;

import com.codename1.ui.Component;
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

    private List<Card> cards = new ArrayList<>();
    private List<Card> selected = new ArrayList<>();
    Hand() {
    }

    public void addCard(Card c) {
        this.cards.add(c);
    }

    public void sortCards() {
        Collections.sort(cards, Collections.reverseOrder());
    }

    int blackColor = 0x000000;
    int redColor = 0xff0000;
    int whiteColor = 0xffffff;
    Font fontRank = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    Font fontSymbol = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    int xPitch = 40;
    int yPitch = 30;
    int cardWidth = 100;
    int cardHeight = 160;

    private void drawCard(Graphics g, Card c) {
        int x0 = 5;
        int y0 = -cardHeight;
        if (this.selected.contains(c)) {
            y0 -= 10;
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
            String s = c.rankToString();
            if (s.length() < 2) {
                g.setFont(fontRank);
                g.drawString(s, x0 + 2, y0);
            } else {
                g.setFont(fontRank);
                g.drawString("" + s.charAt(0), x0 - 2, y0);
                g.drawString("" + s.charAt(1), x0 + 16, y0);
            }
            g.setFont(fontSymbol);
            g.drawString(c.suiteSign(), x0, y0 + 50);
        } else {
            g.setFont(fontSymbol);
            g.drawString("J", x0 + 1, y0);
            g.drawString("O", x0 + 1, y0 + yPitch);
            g.drawString("K", x0 + 1, y0 + yPitch * 2);
            g.drawString("E", x0 + 1, y0 + yPitch * 3);
            g.drawString("R", x0 + 1, y0 + yPitch * 4);
        }
    }

    @Override
    public void paint(Graphics g) {
//        float dx = (float) getWidth() / 1000;
//        float dy = (float) getHeight() / 1000;
//        g.scale(dx, dy);
        int x = getX();
//        int y = getY() + getHeight() - 100;
        int y = getY() + getHeight() - 50;
        g.translate(x, y);
        for (Card c : this.cards) {
            drawCard(g, c);
            g.translate(xPitch, 0);
        }
        g.translate(-g.getTranslateX(), -g.getTranslateY());
    }

    @Override
    public void pointerPressed(int x, int y) {
        int y0 = getY() + getHeight() + 50;
        int y1 = y0 - cardHeight;
        if (y > y0 || y < y1) return;
        int x0 = getX() + 5;
        int x1 = x0 + (this.cards.size() - 1) * xPitch + cardWidth;
        if (x < x0 || x > x1) return;
        int idx = (x - x0) / xPitch;
        if (idx >= this.cards.size()) idx = this.cards.size() - 1;
        Card c = this.cards.get(idx);
        if (this.selected.contains(c)) {
            this.selected.remove(c);
        } else {
            this.selected.add(c);
        }
    }
}
