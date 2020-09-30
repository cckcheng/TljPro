package com.ccd.tljpro;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
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

        this.body = new Container(BoxLayout.y());
        this.add(BorderLayout.NORTH, this.menu);
        this.add(BorderLayout.CENTER, this.body);

        if (playerNames.isEmpty()) {
            player.sendRequest(Request.create(Request.TOUR, "user", 1));
        }
    }

    public void fetchPlayers(Map<String, Object> data) {
        String ids = Func.trimmedString(data.get("ids"));
        this.playerNames.clear();
        this.accountIds = Func.toStringList(ids, ',');
        if (this.accountIds.isEmpty()) return;
        for (String id : this.accountIds) {
            this.playerNames.add(Func.trimmedString(data.get(id)));
        }
    }
}
