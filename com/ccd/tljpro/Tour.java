package com.ccd.tljpro;

import com.codename1.components.CheckBoxList;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.spinner.Picker;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ccheng
 */
public class Tour extends Form {
    private final TuoLaJiPro main;
    private final Player player;

    private Container menu;
    private Container body;
    private List<String> playerNames = new ArrayList<>();
    private List<String> accountIds = new ArrayList<>();

    public Tour(TuoLaJiPro main, String title, Layout contentPaneLayout) {
        super(title, contentPaneLayout);
        this.main = main;
        this.player = main.getPlayer();
    }

    public void init() {
        Button btnNew = new Button("New");
        Button btnRefresh = new Button("Refresh");
        this.menu = BoxLayout.encloseX(btnNew, btnRefresh);
        btnNew.addActionListener((ev) -> {
            editGroup(null);
        });

        this.body = new Container(BoxLayout.y());
        this.add(BorderLayout.NORTH, this.menu);
        this.add(BorderLayout.CENTER, this.body);

        if (playerNames.isEmpty()) {
            player.sendRequest(Request.create(Request.GROUP, "type", "list").append("user", 1));
        }
    }

    private void editGroup(Group grp) {
        Form thisForm = this;
        Form frm = new Form("Group", BoxLayout.y());
        DefaultListModel model = new DefaultListModel(this.playerNames);
        model.setMultiSelectionMode(true);
        CheckBoxList lst = new CheckBoxList(model);
        String gName = grp == null ? "" : grp.name;
        TextField grpName = new TextField(gName, "Group Name");
        Picker dateTimePicker = new Picker();
        dateTimePicker.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        if (grp != null) {
            dateTimePicker.setDate(grp.startTime);
            int n = grp.players.size();
            if (n > 0) {
                int[] ind = new int[n];
                for (int x = 0; x < n; x++) {
                    int idx = this.accountIds.indexOf(grp.players.get(x));
                    if (idx < 0) continue;
                    ind[x] = idx;
                }
                model.setSelectedIndices(ind);
            }
        }

        frm.add(grpName);
        frm.add(dateTimePicker);
        frm.add(lst);

        frm.getToolbar().addCommandToRightBar(Command.createMaterial("Save", FontImage.MATERIAL_SAVE, (e) -> {
            String name = grpName.getText().trim();
            if (name.isEmpty()) {
                Dialog.show(Dict.get(main.lang, "Error"), "Group Name is required", Dict.get(main.lang, "OK"), "");
                return;
            }
            Date dt = (Date) dateTimePicker.getValue();
//            System.out.println(dt.toString());
            int[] indices = model.getSelectedIndices();
            String pids = "";
            for (int n : indices) {
                pids += "," + this.accountIds.get(n);
            }
            if (!pids.isEmpty()) pids = pids.substring(1);
            Request req = Request.create(Request.GROUP, "type", "save")
                    .append("gname", name)
                    .append("ids", pids)
                    .append("time", "" + dt.getTime() / 1000);
            if (grp != null) {
                req.append("gid", grp.id);
            }
            player.sendRequest(req);
            thisForm.showBack();
        }));

        frm.setBackCommand("", null, (ev) -> {
            thisForm.showBack();
        });
        frm.show();
    }

    public void loadGroups(Map<String, Object> data) {
        String ids = Func.trimmedString(data.get("gids"));
        List<String> grpIds = Func.toStringList(ids, ',');
        if (grpIds.isEmpty()) return;
        this.body.removeAll();
        for (String id : grpIds) {
            String s = Func.trimmedString(data.get(id));
            Group grp = new Group(id, s);
            Button btn = new Button(grp.brief());
            btn.addActionListener(evt -> {
                editGroup(grp);
            });
            this.body.add(btn);
        }
        this.body.revalidate();
        this.fetchPlayers(data);
    }

    public void fetchPlayers(Map<String, Object> data) {
        String ids = Func.trimmedString(data.get("ids"));
        List<String> idLst = Func.toStringList(ids, ',');
        if (idLst.isEmpty()) return;
        this.accountIds = idLst;
        this.playerNames.clear();
        for (String id : this.accountIds) {
            this.playerNames.add(Func.trimmedString(data.get(id)));
        }
    }

    class Group {

        String id;
        String name;
        Date startTime;
        String playerIds;
        List<String> players;

        Group(String id, String info) {
            this.id = id;
            List<String> grpInfo = Func.toStringList(info, '|');
            if (grpInfo.size() < 3) return;
            this.name = grpInfo.get(0);
            String s = grpInfo.get(1);
            if (!s.isEmpty()) {
                this.startTime = new Date();
                long tm = Func.parseLong(s);
                if (tm > 0) this.startTime.setTime(tm);
            }
            this.playerIds = grpInfo.get(2);
            this.players = Func.toStringList(this.playerIds, ',');
        }

        String brief() {
            String s = this.name;
            if (this.startTime != null) s += ", " + this.startTime.toString();
            return s;
        }
    }
}
