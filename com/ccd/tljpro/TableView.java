package com.ccd.tljpro;

import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
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
    private final TuoLaJiPro main;
    private final Player player;

    TableView(TuoLaJiPro tljMain) {
        this.main = tljMain;
        this.player = main.getPlayer();
    }

    private Command cmdQuickJoin;
    private Command cmdPrivateTable;
    private Command cmdNewTable;

    public void init() {
        this.setSafeArea(true);
        this.setLayout(new BorderLayout());
        cmdQuickJoin = Command.createMaterial(Dict.get(main.lang, Dict.QUICK_JOIN), FontImage.MATERIAL_PLAY_ARROW, (e) -> {
            Request req = new Request(Request.JOIN, true, true);
            player.sendRequest(req);
        });
        cmdPrivateTable = Command.createMaterial(Dict.get(main.lang, Dict.PRIVATE_TABLE), FontImage.MATERIAL_LOCK, (e) -> {
            Log.p("private clicked");
        });
//        cmdNewTable = Command.createMaterial(Dict.get(main.lang, Dict.NEW_TABLE), FontImage.MATERIAL_GROUP_ADD, (e) -> {
        cmdNewTable = Command.createMaterial(Dict.get(main.lang, Dict.NEW_TABLE), (char) 57669, (e) -> {
            Log.p("new table clicked");
        });

        Toolbar topTool = this.getToolbar();
        topTool.getAllStyles().setBgTransparency(0);
//        topTool.setHeight(Hand.fontGeneral.getHeight());
        topTool.setUIID("myTableTool");
        topTool.setBackCommand("", (e) -> {
            main.switchScene("entry");
        });

        topTool.addCommandToLeftBar(cmdQuickJoin);
        topTool.addCommandToLeftBar(cmdPrivateTable);
        topTool.addCommandToLeftBar(cmdNewTable);
        this.add(BorderLayout.CENTER, this.tableList);

        Container bbar = new Container();
//        this.setToolbar(bbar);
        this.add(BorderLayout.SOUTH, bbar);
        Button b1 = new Button("Practice", "RaisedButton");
        b1.getStyle().setBgColor(TuoLaJiPro.BACKGROUND_COLOR);
        bbar.add(b1);
    }

    @Override
    protected boolean shouldPaintStatusBar() {
        return false;
    }

    int idx = 0;
    public void addContent() {
        this.tableList.add(new Label("This is a test"));
        this.tableList.add(new Label("This is a test"));
        this.tableList.add("This is last test");

        Toolbar topTool = this.getToolbar();
        switch (idx++ % 3) {
            case 0:
                topTool.removeCommand(cmdPrivateTable);
                topTool.removeCommand(cmdNewTable);
                break;
            case 1:
                topTool.addCommandToLeftBar(cmdPrivateTable);
                topTool.addCommandToLeftBar(cmdNewTable);
                break;
            case 2:
//                topTool.addCommandToLeftBar(cmdPrivateTable);
//                topTool.addCommandToLeftBar(cmdNewTable);
                break;
        }

        this.revalidate();
    }
}
