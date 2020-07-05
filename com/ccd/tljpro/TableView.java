package com.ccd.tljpro;

import com.codename1.components.ToastBar;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import static com.codename1.ui.CN.getCurrentForm;
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
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
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

    static String TABLE_OPTIONS = "ALMWB";

    private Tabs listTabs;
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

    private Label lblAccount;

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
            if (t.coins > 0 && player.coins < t.coins) {
                Func.noEnoughCoin(main.lang);
                return;
            }
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
            int idx = this.listTabs.getSelectedIndex();
            if (idx == 0) {
                ToastBar.showInfoMessage(Dict.get(main.lang, Dict.NOT_AVAILABLE));
                new UITimer(() -> {
                    main.startupShow();
                }).schedule(5000, false, getCurrentForm());
                return;
            }
            if (!main.registered) {
                main.infoRegisterRequired();
                return;
            }
            TableContainer t = this.tableList.get(idx);
            if (t.coins > 0 && player.coins < t.coins) {
                Func.noEnoughCoin(main.lang);
                return;
            }

            TableLayout tl = new TableLayout(5, 1);
            Container props = new Container(tl);
            String lang = main.lang;
            RadioButton rb1 = new RadioButton("2->A");
            RadioButton rb2 = new RadioButton("8->A");
            RadioButton rb3 = new RadioButton("10->A");
            RadioButton rb4 = new RadioButton("5 10 K");
            ButtonGroup rbGrp = new ButtonGroup(rb1, rb2, rb3, rb4);
            rb1.setSelected(true);
            props.add(BoxLayout.encloseX(rb1, rb2, rb3, rb4));
            CheckBox cPrivate = new CheckBox(Dict.get(lang, Dict.PRIVATE_TABLE));
            props.add(cPrivate);

            for (int x = 0, n = TABLE_OPTIONS.length(); x < n; x++) {
                char code = TABLE_OPTIONS.charAt(x);
                boolean available = t.optionSupported(code);
                if (!available && idx <= 1) continue;
                props.add(optionComponent(code, available));
            }

            Player p = this.player;
            Command okCmd = new Command(Dict.get(lang, "OK")) {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    String type = rbGrp.getRadioButton(rbGrp.getSelectedIndex()).getText();
                    p.sendRequest(Request.create(Request.CREATE, "category", t.category)
                            .append("private", cPrivate.isSelected() ? 1 : 0)
                            .append("tableType", type)
                            .append("option", selectedOptions(t))
                            .setReSend(true)
                    );
                    Storage.getInstance().writeObject("category", t.category);
                }
            };
//            Dialog.show("", props, okCmd);
            Dialog dlg = new Dialog();
            dlg.setLayout(BoxLayout.y());
            dlg.add(props);
            dlg.add(BoxLayout.encloseXCenter(new Button(okCmd),
                    new Button(Command.create(Dict.get(lang, "Cancel"), null, (ev) -> {
                    })))
            );
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
        topTool.addCommandToLeftBar(cmdNewTable);
        topTool.addCommandToRightBar(cmdRefresh);
        topTool.addCommandToRightBar(cmdPrivateTable);

        this.lblAccount = new Label("");
        this.lblAccount.getAllStyles().setFont(Hand.fontGeneral);
        topTool.add(CENTER, BoxLayout.encloseXCenter(this.lblAccount));
        if (player.coins != 0) this.updateBalance(player.coins);

        this.listTabs = new Tabs(Component.BOTTOM);
        this.listTabs.setTabTextPosition(Component.RIGHT);
//        this.listTabs.setSelectedStyle(UIManager.getInstance().getComponentStyle("RaisedButton"));
        this.listTabs.getStyle().setFont(Hand.fontRank);

        this.add(BorderLayout.CENTER, this.listTabs);

//        this.listTabs.addSelectionListener((oldIdx, newIdx) -> {
//            Display.getInstance().callSerially(() -> {
//                if (newIdx == 0) {
//                    topTool.removeCommand(cmdNewTable);
//                } else if (oldIdx == 0) {
//                    topTool.addCommandToLeftBar(cmdNewTable);
//                }
//            });
//        });

        this.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 1000));
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

    public void updateBalance(int coins) {
        this.lblAccount.setText(Card.suiteSign(Card.DIAMOND) + coins);
        this.lblAccount.getParent().animateLayout(500);
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
            player.sendRequest(new Request(Request.LIST, true).appendPlayerInfo(main));
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
            String category = Func.trimmedString(data.get("category"));
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

        String stat = Func.trimmedString(data.get("stat"));
        main.updateStatInfo(stat);
        for (TableContainer c : this.tableList) {
            c.addContent(Func.trimmedString(data.get(c.category)), data);
        }
        this.revalidate();
    }

    private void addCategoryTab(String cStr) {
        List<String> lst = Func.toStringList(cStr, '|');
        if (lst.isEmpty()) return;

        int idx = 0, n = lst.size();
        String category = lst.get(idx++);
        TableContainer tc = new TableContainer(category);
        this.tableList.add(tc);

        if (idx >= n) return;
        String tabName = lst.get(idx++);

        char icon = FontImage.MATERIAL_CASINO;
        if (idx < n) {
            icon = (char) Func.parseInteger(lst.get(idx++));
        }

        int coins = 0;
        if (n > 3) {
            coins = Func.parseInteger(lst.get(3));
        }

        if (coins > 0) {
            tabName += " " + Card.suiteSign(Card.DIAMOND) + coins;
            tc.coins = coins;
//            tabName += " " + FontImage.MATERIAL_MONETIZATION_ON + coins;
        }

        if (n > 4) {
            tc.avlOptions = lst.get(4);
        }

        int idxTab = this.listTabs.getTabCount();
        this.categoryIndex.put(category, idxTab);
        this.listTabs.addTab(tabName, this.tableList.get(idxTab));
        Style s = UIManager.getInstance().getComponentStyle("Tab");
        this.listTabs.setTabSelectedIcon(idxTab, FontImage.createMaterial(icon, s));
    }

    static public String getTableOption(char code, String lang) {
        String str = "";
        switch (code) {
            case 'A':
                if (lang.equalsIgnoreCase("zh")) str = "任意定主";
                else str = "No trump restriction";
                break;
            case 'L':
                if (lang.equalsIgnoreCase("zh")) str = "起底后定主";
                else str = "Late define trump";
                break;
            case 'M':
                if (lang.equalsIgnoreCase("zh")) str = "抠底倍数";
                else str = "Hole point multiple";
                break;
            case 'W':
                if (lang.equalsIgnoreCase("zh")) str = "等人入场";
                else str = "Minimum players";
                break;
            case 'B':
                if (lang.equalsIgnoreCase("zh")) str = "中场休息";
                else str = "Long break";
                break;
        }

        return str;
    }

    private Map<Character, Object> optComponent = new HashMap<>();
    private Component optionComponent(char code, boolean enabled) {
        String str = getTableOption(code, main.lang);
        if (!enabled) str += "(" + Dict.get(main.lang, "TBA") + ")";
        Component cmp = null;
        switch (code) {
            case 'A':
            case 'L':
                cmp = new CheckBox(str);
                optComponent.put(code, cmp);
                break;
            case 'M':
                RadioButton rb1 = new RadioButton("4n");
                rb1.putClientProperty("val", "4n");
                rb1.setSelected(true);
                RadioButton rb2 = new RadioButton("2n");
                rb2.putClientProperty("val", "2n");
                RadioButton rb3 = new RadioButton("2^n");
                rb3.putClientProperty("val", "2^n");
                optComponent.put(code, new ButtonGroup(rb1, rb2, rb3));
                cmp = FlowLayout.encloseIn(new Label(str), rb1, rb2, rb3);
                break;
            case 'W':
                RadioButton p0 = new RadioButton(Dict.get(main.lang, "No"));
                p0.putClientProperty("val", "0");
                p0.setSelected(true);
                RadioButton p2 = new RadioButton("2");
                p2.putClientProperty("val", "2");
                RadioButton p3 = new RadioButton("3");
                p3.putClientProperty("val", "3");
                RadioButton p4 = new RadioButton("4");
                p4.putClientProperty("val", "4");
                RadioButton p5 = new RadioButton("5");
                p5.putClientProperty("val", "5");
                RadioButton p6 = new RadioButton("6");
                p6.putClientProperty("val", "6");
                optComponent.put(code, new ButtonGroup(p0, p2, p3, p4, p5, p6));
                cmp = FlowLayout.encloseIn(new Label(str), p0, p2, p3, p4, p5, p6);
                break;
            case 'B':
                RadioButton b0 = new RadioButton(Dict.get(main.lang, "No"));
                b0.putClientProperty("val", "0");
                b0.setSelected(true);
                RadioButton b5 = new RadioButton("5" + Dict.get(main.lang, "minutes"));
                b5.putClientProperty("val", "5");
                RadioButton b10 = new RadioButton("10" + Dict.get(main.lang, "minutes"));
                b10.putClientProperty("val", "10");
                optComponent.put(code, new ButtonGroup(b0, b5, b10));
                cmp = FlowLayout.encloseIn(new Label(str), b0, b5, b10);
                break;
            default:
                cmp = new Label(str);
                break;
        }

        cmp.setEnabled(enabled);
        return cmp;
    }

    private String selectedOptions(TableContainer t) {
        String opt = "";
        for (int x = 0, n = TABLE_OPTIONS.length(); x < n; x++) {
            char code = TABLE_OPTIONS.charAt(x);
            boolean available = t.optionSupported(code);
            if (!available) continue;
            switch (code) {
                case 'A':
                case 'L':
                    CheckBox cb = (CheckBox) optComponent.get(code);
                    if (cb.isSelected()) {
                        opt += "," + code;
                    }
                    break;
                case 'M':
                case 'W':
                case 'B':
                    ButtonGroup bg = (ButtonGroup) optComponent.get(code);
                    int sIdx = bg.getSelectedIndex();
//                    if (sIdx > 0) opt += "," + code + sIdx;
                    if (sIdx > 0) {
                        RadioButton rb = bg.getRadioButton(sIdx);
                        opt += "," + code + rb.getClientProperty("val");
                    }
                    break;
            }
        }

        if (!opt.isEmpty()) opt = opt.substring(1);
        return opt;
    }

    class TableContainer extends Container {

        String category;
        String dispName;
        String avlOptions;
        int coins = 0;

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
            Button btn = new Button(Func.trimmedString(data.get(tableId)));
            btn.addActionListener((ev) -> {
                player.sendRequest(Request.create(Request.WATCH, "tid", tableId.substring(1)).setReSend(true));
                Storage.getInstance().writeObject("category", category);
            });
            if (tableId.startsWith("L")) {
                btn.setMaterialIcon(FontImage.MATERIAL_LOCK);
            }
            return btn;
        }

        boolean optionSupported(char code) {
            return avlOptions != null && avlOptions.indexOf(code) >= 0;
        }
    }
}
