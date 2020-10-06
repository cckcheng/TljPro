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
            editGroup();
        });

        this.body = new Container(BoxLayout.y());
        this.add(BorderLayout.NORTH, this.menu);
        this.add(BorderLayout.CENTER, this.body);

        if (playerNames.isEmpty()) {
            player.sendRequest(Request.create(Request.GROUP, "type", "list").append("user", 1));
        }
    }

    private void editGroup() {
        Form thisForm = this;
        Form frm = new Form("Group", BoxLayout.y());
        DefaultListModel model = new DefaultListModel(this.playerNames);
        CheckBoxList lst = new CheckBoxList(model);
        TextField grpName = new TextField("", "Group Name");
        Picker dateTimePicker = new Picker();
        dateTimePicker.setType(Display.PICKER_TYPE_DATE_AND_TIME);

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
            System.out.println(dt.toString());
            int[] indices = model.getSelectedIndices();
            List<Integer> ll = new ArrayList<Integer>();
            for (int n : indices) {
                ll.add(n);
            }
            System.out.println(ll.toString());
        }));

        frm.setBackCommand("", null, (ev) -> {
            thisForm.showBack();
        });
        frm.show();
    }

    public void fetchPlayers(Map<String, Object> data) {
        String ids = Func.trimmedString(data.get("ids"));
        this.playerNames.clear();
        this.accountIds = Func.toStringList(ids, ',');
        if (this.accountIds.isEmpty()) return;
        for (String id : this.accountIds) {
            this.playerNames.add(Func.trimmedString(data.get(id)));
        }
//        System.out.println("total player: " + this.playerNames.size());
    }
}
