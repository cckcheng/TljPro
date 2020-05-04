package com.ccd.tljpro;

import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Tabs;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.TableLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ccheng
 */
public class TableView extends Form {

    private final Tabs listTabs = new Tabs(Component.BOTTOM);
    private final TuoLaJiPro main;
    private final Player player;

    private final List<TableContainer> tableList = new ArrayList<>();
    private final Map<String, Integer> categoryIndex = new HashMap<>();

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
            int idx = this.listTabs.getSelectedIndex();
            TableContainer t = this.tableList.get(idx);
            player.sendRequest(player.initRequest().append("opt", t.category).setReSend(true));
            Storage.getInstance().writeObject("category", t.category);
        });
        cmdPrivateTable = Command.createMaterial(Dict.get(main.lang, Dict.PRIVATE_TABLE), FontImage.MATERIAL_LOCK, (e) -> {
            Log.p("private clicked");
        });
//        cmdNewTable = Command.createMaterial(Dict.get(main.lang, Dict.NEW_TABLE), FontImage.MATERIAL_GROUP_ADD, (e) -> {
        cmdNewTable = Command.createMaterial(Dict.get(main.lang, Dict.NEW_TABLE), (char) 57669, (e) -> {
            int idx = this.listTabs.getSelectedIndex();
            TableContainer t = this.tableList.get(idx);
            TableLayout tl = new TableLayout(2, 1);
            Container props = new Container(tl);
            String lang = main.lang;
            RadioButton rb1 = new RadioButton("2->A");
            RadioButton rb2 = new RadioButton("8->A");
            RadioButton rb3 = new RadioButton("10->A");
            RadioButton rb4 = new RadioButton("5 10 K A");
            new ButtonGroup(rb1, rb2, rb3, rb4);
            rb1.setSelected(true);
            props.add(BoxLayout.encloseX(rb1, rb2, rb3, rb4));
            props.add(new CheckBox(Dict.get(lang, Dict.PRIVATE_TABLE)));

            Player p = this.player;
            Command okCmd = new Command(Dict.get(lang, "OK")) {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    String type = "FULL";
                    if (rb2.isSelected()) type = "HALF";
                    else if (rb3.isSelected()) type = "EXPRESS";
                    else if (rb4.isSelected()) type = "POINTS";
                    p.sendRequest(p.initRequest(Request.CREATE).setReSend(true)
                            .append("category", t.category)
                            .append("private", 0)
                            .append("tableType", type)
                    );
                    Storage.getInstance().writeObject("category", t.category);
                }
            };
//            Dialog.show("", props, okCmd);
            Dialog dlg = new Dialog();
            dlg.setLayout(BoxLayout.y());
            dlg.add(props);
            dlg.add(new Button(okCmd));
            dlg.setBackCommand("", null, (ev) -> {
                dlg.dispose();
            });
            dlg.show();
        });

        Toolbar topTool = this.getToolbar();
        topTool.getAllStyles().setBgTransparency(0);
//        topTool.setHeight(Hand.fontGeneral.getHeight());
        topTool.setUIID("myTableTool");
        topTool.setBackCommand("", (e) -> {
            main.switchScene("entry");
        });

        topTool.addCommandToLeftBar(cmdQuickJoin);
//        topTool.addCommandToLeftBar(cmdNewTable);
        topTool.addCommandToRightBar(cmdPrivateTable);

        this.listTabs.setTabTextPosition(Component.RIGHT);
//        this.listTabs.setSelectedStyle(UIManager.getInstance().getComponentStyle("RaisedButton"));
        this.listTabs.getStyle().setFont(Hand.fontRank);

        /*
        int idx = 0;
        String category = "Practice";
        this.tableList.add(new TableContainer(category));
        this.listTabs.addTab(category, this.tableList.get(idx));
        idx++;
        category = "Novice";
        this.tableList.add(new TableContainer(category));
        this.listTabs.addTab(category, this.tableList.get(idx));

        idx++;
        category = "Experienced";
        this.tableList.add(new TableContainer("Experienced"));
        this.listTabs.addTab(category, this.tableList.get(idx));
        idx++;
        category = "Expert";
        this.tableList.add(new TableContainer("Expert"));
        this.listTabs.addTab(category, this.tableList.get(idx));

        this.tableList.get(1).addContent();

        this.listTabs.setTabSelectedIcon(0, FontImage.createMaterial((char) 57669, s));
        this.listTabs.setTabSelectedIcon(1, FontImage.createMaterial(FontImage.MATERIAL_CASINO, s));
         */
        this.add(BorderLayout.CENTER, this.listTabs);

        this.listTabs.addSelectionListener((oldIdx, newIdx) -> {
            if (newIdx == 0) {
                topTool.removeCommand(cmdNewTable);
            } else if (oldIdx == 0) {
                topTool.addCommandToLeftBar(cmdNewTable);
            }
        });
    }

    @Override
    protected boolean shouldPaintStatusBar() {
        return false;
    }

    public void refreshTableList(Map<String, Object> data) {
        if (this.listTabs.getTabCount() < 1) {
            String category = Player.trimmedString(data.get("category"));
            if (category.isEmpty()) return; // should not happen
            while (category.length() > 0) {
                int idx = category.indexOf(',');
                if (idx > 0) {
                    addCategoryTab(category.substring(0, idx));
                } else {
                    addCategoryTab(category);
                    break;
                }
                category = category.substring(idx + 1);
            }

            Object sObj = Storage.getInstance().readObject("category");
            if (sObj != null) {
                String defaultCategory = sObj.toString();
                if (this.categoryIndex.containsKey(defaultCategory)) {
                    this.listTabs.setSelectedIndex(this.categoryIndex.get(defaultCategory));
                }
            }

            this.revalidate();
        }

        for (TableContainer c : this.tableList) {
            c.addContent(Player.trimmedString(data.get(c.category)), data);
        }
    }

    private void addCategoryTab(String cStr) {
        int idx = cStr.indexOf('|');
        if (idx <= 0) return;
        String category = cStr.substring(0, idx);
        this.tableList.add(new TableContainer(category));

        String tabName = cStr.substring(idx + 1);
        idx = tabName.indexOf('|');
        char icon = FontImage.MATERIAL_CASINO;
        if (idx > 0) {
            icon = (char) Player.parseInteger(tabName.substring(idx + 1));
            tabName = tabName.substring(0, idx);
        }

        int idxTab = this.listTabs.getTabCount();
        this.categoryIndex.put(category, idxTab);
        this.listTabs.addTab(tabName, this.tableList.get(idxTab));
        Style s = UIManager.getInstance().getComponentStyle("Tab");
        this.listTabs.setTabSelectedIcon(idxTab, FontImage.createMaterial(icon, s));
    }

    int idx = 0;
    public void addContent() {
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

    class TableContainer extends Container {

        String category;
        String dispName;

        TableContainer(String category) {
            this.category = category;
        }

        void addContent(String tableIds, Map<String, Object> data) {
            if (this.getComponentCount() < 1) {
                this.setLayout(BoxLayout.yCenter());
                this.setScrollableX(true);
                this.setScrollableY(true);
            } else {
                this.removeAll();
            }

            if (tableIds.isEmpty()) return;

            while (true) {
                int idx = tableIds.indexOf(',');
                if (idx > 0) {
                    this.add(new Button(Player.trimmedString(data.get(tableIds.subSequence(0, idx)))));
                } else {
                    this.add(new Button(Player.trimmedString(data.get(tableIds))));
                    break;
                }
                tableIds = tableIds.substring(idx + 1);
            }

            this.revalidate();
        }

    }
}
