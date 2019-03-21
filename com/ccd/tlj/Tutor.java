package com.ccd.tlj;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;

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

    Container topic;

    public void showTopic() {
        if (topic == null) {
            topic = new Container(BoxLayout.y());
            topic.add("Point Cards");
            topic.add("Card Rank");
            topic.add("Card Combinations");
            topic.add("Trump");
            this.add(topic);

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
}
