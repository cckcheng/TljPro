package com.ccd.tljpro;

import com.codename1.ui.Form;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/**
 *
 * @author ccheng
 */
public class TableView extends Form {

    private Container tableList = new Container();
    private final Toolbar topTool;
    private final TuoLaJiPro main;

    TableView(TuoLaJiPro tljMain) {
        this.main = tljMain;
        this.setSafeArea(true);
        this.setLayout(new BorderLayout());
        this.topTool = this.getToolbar();
        this.topTool.getAllStyles().setBgTransparency(0);
        this.topTool.setHeight(Hand.fontGeneral.getHeight());
        this.topTool.addMaterialCommandToLeftBar("", FontImage.MATERIAL_ARROW_BACK, (e) -> {
            main.switchScene("entry");
        });
        this.topTool.setUIID("myTableTool");
        this.add(BorderLayout.CENTER, this.tableList);
//                .add(BorderLayout.NORTH, this.topTool);
    }

    @Override
    protected boolean shouldPaintStatusBar() {
        return false;
    }

    public void addContent() {
        this.tableList.add(new Label("This is a test"));
        this.tableList.add(new Label("This is a test"));
        this.tableList.add("This is last test");
        this.revalidate();
    }
}
