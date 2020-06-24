package com.ccd.tljpro;

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
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.UITimer;
import java.util.ArrayList;
import java.util.Date;
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
    private Command cmdRefresh;

    private long lastRefreshTime = 0;
    private final static long MIN_REFRESH_TIME = 10000; // 10 seconds

    public void init() {
        this.setSafeArea(true);
        this.setLayout(new BorderLayout());
        cmdQuickJoin = Command.createMaterial(Dict.get(main.lang, Dict.QUICK_JOIN), FontImage.MATERIAL_PLAY_ARROW, (e) -> {
            int idx = this.listTabs.getSelectedIndex();
            if (idx > 0 && !main.registered) {
                main.infoRegisterRequired();
                return;
            }
            TableContainer t = this.tableList.get(idx);
            player.sendRequest(Request.create(Request.JOIN, "opt", t.category).setReSend(true));
            Storage.getInstance().writeObject("category", t.category);
        });
        cmdRefresh = Command.createMaterial(Dict.get(main.lang, "Refresh"), FontImage.MATERIAL_REFRESH, (e) -> {
            long tm = new Date().getTime();
            if (tm > MIN_REFRESH_TIME + lastRefreshTime) {
                player.sendRequest(new Request(Request.LIST, true));
                lastRefreshTime = tm;
                this.setGlassPane(null);
            }
        });
        cmdPrivateTable = Command.createMaterial(Dict.get(main.lang, Dict.PRIVATE_TABLE), FontImage.MATERIAL_LOCK, (e) -> {
            if (main.registered) {
                inputPassword(this.player);
            } else {
                main.infoRegisterRequired();
            }
        });
        cmdNewTable = Command.createMaterial(Dict.get(main.lang, Dict.NEW_TABLE), FontImage.MATERIAL_ADD_CIRCLE, (e) -> {
//        cmdNewTable = Command.createMaterial(Dict.get(main.lang, Dict.NEW_TABLE), (char) 57669, (e) -> {
            if (!main.registered) {
                main.infoRegisterRequired();
                return;
            }
            int idx = this.listTabs.getSelectedIndex();
            TableContainer t = this.tableList.get(idx);
            TableLayout tl = new TableLayout(2, 1);
            Container props = new Container(tl);
            String lang = main.lang;
            RadioButton rb1 = new RadioButton("2->A");
            RadioButton rb2 = new RadioButton("8->A");
            RadioButton rb3 = new RadioButton("10->A");
            RadioButton rb4 = new RadioButton("5 10 K");
            new ButtonGroup(rb1, rb2, rb3, rb4);
            rb1.setSelected(true);
            props.add(BoxLayout.encloseX(rb1, rb2, rb3, rb4));
            CheckBox cPrivate = new CheckBox(Dict.get(lang, Dict.PRIVATE_TABLE));
            props.add(cPrivate);

            Player p = this.player;
            Command okCmd = new Command(Dict.get(lang, "OK")) {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    String type = "FULL";
                    if (rb2.isSelected()) type = "HALF";
                    else if (rb3.isSelected()) type = "EXPRESS";
                    else if (rb4.isSelected()) type = "POINTS";
                    p.sendRequest(Request.create(Request.CREATE, "category", t.category)
                            .append("private", cPrivate.isSelected() ? 1 : 0)
                            .append("tableType", type)
                            .setReSend(true)
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
        topTool.addCommandToRightBar(cmdRefresh);
        topTool.addCommandToRightBar(cmdPrivateTable);

        this.listTabs.setTabTextPosition(Component.RIGHT);
//        this.listTabs.setSelectedStyle(UIManager.getInstance().getComponentStyle("RaisedButton"));
        this.listTabs.getStyle().setFont(Hand.fontRank);

        this.add(BorderLayout.CENTER, this.listTabs);

        this.listTabs.addSelectionListener((oldIdx, newIdx) -> {
            if (newIdx == 0) {
                topTool.removeCommand(cmdNewTable);
            } else if (oldIdx == 0) {
                topTool.addCommandToLeftBar(cmdNewTable);
            }
        });

        new UITimer(new Runnable() {
            @Override
            public void run() {
                long tm = new Date().getTime();
                if (tm > MIN_REFRESH_TIME + lastRefreshTime) {
                    player.sendRequest(new Request(Request.LIST, true));
                    lastRefreshTime = tm;
                }
            }
        }).schedule(TuoLaJiPro.DEBUG ? 15000 : 60000, true, this);
    }

    @Override
    protected boolean shouldPaintStatusBar() {
        return false;
    }

    public void inputPassword(Player p) {
        Dialog dlg = new Dialog();
        TextField tf = new TextField(6);
        tf.setMaxSize(8);
        dlg.add(Dict.get(main.lang, "Password")).add(tf);
        dlg.add(new Button(Command.create(Dict.get(main.lang, "OK"), null, (ev) -> {
            String pass = tf.getText().trim();
            if (pass.isEmpty()) return;
            p.sendRequest(Request.create(Request.SIT, "pass", pass).setReSend(true));
        })));
        dlg.setBackCommand("", null, (ev) -> {
            dlg.dispose();
        });
        dlg.setDialogPosition(BorderLayout.NORTH);
        dlg.showModeless();
        Display.getInstance().callSerially(() -> {
            Display.getInstance().editString(tf, tf.getMaxSize(), TextArea.NUMERIC, "");
        });
    }

    public void pullTableList() {
        // only when the tabs is empty
        if (this.listTabs.getTabCount() < 1) {
            player.sendRequest(new Request(Request.LIST, true));
        }
        this.setGlassPane(null);
    }

    public void resetTableList() {
        for (int i = this.listTabs.getTabCount() - 1; i >= 0; i--) {
            this.listTabs.removeTabAt(i);
        }

        Toolbar topTool = this.getToolbar();
        topTool.removeCommand(cmdNewTable);
        topTool.removeCommand(cmdQuickJoin);
        topTool.removeCommand(cmdRefresh);
        topTool.removeCommand(cmdPrivateTable);
        this.cmdNewTable.setCommandName(Dict.get(main.lang, Dict.NEW_TABLE));
        this.cmdQuickJoin.setCommandName(Dict.get(main.lang, Dict.QUICK_JOIN));
        this.cmdRefresh.setCommandName(Dict.get(main.lang, "Refresh"));
        this.cmdPrivateTable.setCommandName(Dict.get(main.lang, Dict.PRIVATE_TABLE));

        topTool.addCommandToLeftBar(cmdQuickJoin);
        topTool.addCommandToRightBar(cmdRefresh);
        topTool.addCommandToRightBar(cmdPrivateTable);

        this.getToolbar().revalidate();
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

            this.revalidate();
            this.listTabs.revalidate();
            Object sObj = Storage.getInstance().readObject("category");
            if (sObj != null) {
                String defaultCategory = sObj.toString();
                if (this.categoryIndex.containsKey(defaultCategory)) {
                    final Tabs tabs = this.listTabs;
                    final int cIdx = this.categoryIndex.get(defaultCategory);

                    tabs.setSelectedIndex(cIdx);
                    tabs.revalidate();
                }
            }
        }

        for (TableContainer c : this.tableList) {
            c.addContent(Player.trimmedString(data.get(c.category)), data);
        }
        this.revalidate();
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
                    this.add(toTableButton(tableIds.substring(0, idx), data));
                } else {
                    this.add(toTableButton(tableIds, data));
                    break;
                }
                tableIds = tableIds.substring(idx + 1);
            }

            this.revalidate();
        }

        private Button toTableButton(String tableId, Map<String, Object> data) {
            Button btn = new Button(Player.trimmedString(data.get(tableId)));
            btn.addActionListener((ev) -> {
                player.sendRequest(Request.create(Request.WATCH, "tid", tableId.substring(1)).setReSend(true));
                Storage.getInstance().writeObject("category", category);
            });
            if (tableId.startsWith("L")) {
                btn.setMaterialIcon(FontImage.MATERIAL_LOCK);
            }
            return btn;
        }

    }
}
