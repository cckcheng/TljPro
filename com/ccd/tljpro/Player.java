package com.ccd.tljpro;

import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;
import com.codename1.io.Storage;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import static com.codename1.ui.CN.getCurrentForm;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.DynamicImage;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.Base64;
import com.codename1.util.regex.StringReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ccheng
 */
public class Player {

    static final String BIDDING_STAGE = "bid";
    static final String PLAYING_STAGE = "play";

    static final String CONTRACTOR = "庄";
    static final String PARTNER = "帮";

//    static final int POINT_COLOR = 0xd60e90;
    static final int BLACK_COLOR = 0x00000;
    static final int GREY_COLOR = 0x505050;
    static final int INFO_COLOR = 0xa1ebfc;
    static final int POINT_COLOR = 0x3030ff;
    static final int TIMER_COLOR = 0xff00ff;
    static final int RED_COLOR = 0xff0000;
    static final int BUTTON_COLOR = 0x47b2e8;

//    private final ButtonImage backImage = new ButtonImage(0xbcbcbc);
    static final int TIME_OUT_SECONDS = 25;
    private final TuoLaJiPro main;
    public int coins = 0;

    private String option;

    private UITimer gameTimer;
    private Runnable notifyPlayer = new Runnable() {
        @Override
        public void run() {
            Display.getInstance().callSerially(() -> {
                Display.getInstance().playBuiltinSound(Display.SOUND_TYPE_ALARM);
                Display.getInstance().vibrate(1000);  // this works
            });
            gameTimer = null;
        }
    };

    public Player(TuoLaJiPro main) {
        this.main = main;
    }

    public void sendRequest(Request req) {
        if (mySocket == null || !mySocket.isConnected()) {
            this.connectServer(req);
            return;
        }
        mySocket.addRequest(req);
    }

    public boolean isConnected() {
        if (mySocket == null) return false;
        return mySocket.isConnected();
    }

    private MySocket mySocket = null;

    static final String OPTION_RESUME = "resume";    // legacy
    static final String OPTION_PRACTICE = "practice";    // legacy
    static final String OPTION_CHECK = "check";  // check table status, resume play if table alive, otherwise no action
    static final String OPTION_VIEW = "view";  // get table list

    static boolean checkOnce = true;
    static String tljHost = Card.TLJ_HOST;

    private boolean initConnect = true;

    public void connectServer(boolean init) {
        if (!Socket.isSupported()) {
            Dialog.show("Alert", "Socket is not supported", "OK", "");
            return;
        }

        if (this.mySocket == null) {
            this.mySocket = new MySocket();
        }
        if (!this.mySocket.isConnected()) {
            initConnect = init;
            Socket.connect(tljHost, Card.TLJ_PORT, mySocket);
        } else {
            main.showLogin();
        }
    }

    public void connectServer(String option) {
        if (!Socket.isSupported()) {
            Dialog.show("Alert", "Socket is not supported", "OK", "");
            return;
        }

        if (this.mySocket == null) {
            this.mySocket = new MySocket();
        }
        if (!this.mySocket.isConnected()) {
            main.disableButtons();
            Socket.connect(tljHost, Card.TLJ_PORT, mySocket);
        }

        if (option.equals("IGNORE")) return; // done here

        if (option == null || option.isEmpty()) {
            option = OPTION_CHECK;
        }
        initCheckin(option);
    }

    public void connectServer(Request lastRequest) {
        if (lastRequest != null) {
            connectServer("IGNORE");
            mySocket.addRequest(lastRequest.appendPlayerInfo(main));
        } else {
            connectServer("");
        }
    }

    public void startPlay() {
        this.startPlay(null);
    }

    public void startPlay(String option) {
        this.option = option;
        initCheckin(option);
    }

    public Request initRequest() {
        if (this.tableOn) return initRequest(Request.JOIN);
        return initRequest(Request.LIST);
    }

    public Request initRequest(String action) {
        if (action == null || action.isEmpty()) {
            action = Request.JOIN;
        }
        Request req = new Request(action, true);
        return req.appendPlayerInfo(main);
    }

    public void initCheckin(String option) {
//        if (this.mySocket == null) {
//            this.connectServer(false);
//        }
//        this.tableOn = true;
//        mySocket.checkConnection = true;
        Request req = initRequest();
        if (option != null && !option.isEmpty()) {
            this.option = option;
            req.append("opt", option);
        }
        mySocket.clearRequest();  // clear pending requests
        mySocket.addRequest(req);
    }

    public void disconnect() {
        if (this.mySocket != null) {
            if (this.mySocket.isConnected()) {
                this.mySocket.closeConnection();
                this.mySocket = null;
            }
        }
    }

    private List<Character> candidateTrumps = new ArrayList<>();
    private void addCardsToHand(char suite, List<Object> lst) {
        if (lst == null || lst.isEmpty()) {
            return;
        }
        for (Object d : lst) {
            int rank = Func.parseInteger(d);
            if (rank > 0) {
                hand.addCard(new Card(suite, rank));
            }
        }
    }

    synchronized private void addCards(Map<String, Object> data) {
        addCardsToHand(Card.SPADE, (List<Object>) data.get("S"));
        addCardsToHand(Card.HEART, (List<Object>) data.get("H"));
        addCardsToHand(Card.DIAMOND, (List<Object>) data.get("D"));
        addCardsToHand(Card.CLUB, (List<Object>) data.get("C"));
        addCardsToHand(Card.JOKER, (List<Object>) data.get("T"));
    }

    synchronized private void addRemains(Map<String, Object> data) {
        if (this.hand.isEmpty()) return;
        String cards = Func.trimmedString(data.get("cards"));
        if (cards.isEmpty()) return;
        int x = cards.indexOf(',');
        while (x > 0) {
            String s = cards.substring(0, x);
            hand.addCard(Card.create(s));
            cards = cards.substring(x + 1);
            x = cards.indexOf(',');
        }

        if (!cards.isEmpty()) {
            hand.addCard(Card.create(cards));
        }

        hand.sortCards(currentTrump, playerRank, true);
        int actTime = Func.parseInteger(data.get("acttime"));
        infoLst.get(0).showTimer(actTime, 100, "bury");
    }

    private String defRecommend = "";
    private void buryCards(Map<String, Object> data) {
        if (this.hand.isEmpty()) return;
        String strCards = Func.trimmedString(data.get("cards"));
        if (strCards.isEmpty()) return;
        hand.removeCards(strCards);

        this.defRecommend = Func.trimmedString(data.get("def"));
        infoLst.get(0).showTimer(this.timeout, 100, "partner");
    }

    private String partnerDef(String def) {
        if(def.isEmpty()) return " ";
        String part = Dict.get(main.lang, "1 vs 5");
        if(def.length() >= 3){
            char seq = def.charAt(2);
            part = Card.suiteSign(def.charAt(0)) + def.charAt(1);
            switch(seq) {
                case '0':
                    part = Dict.get(main.lang, " 1st ") + part;
                    break;
                case '1':
                    part = Dict.get(main.lang, " 2nd ") + part;
                    break;
                case '2':
                    part = Dict.get(main.lang, " 3rd ") + part;
                    break;
                case '3':
                    part = Dict.get(main.lang, " 4th ") + part;
                    break;
            }
//            part = Dict.get(main.lang, "Partner") + ":" + part;
        }
        return part;
    }

    private void displayPartnerDef(String def) {
        if (def.isEmpty()) {
            return;
        }
        String part = Dict.get(main.lang, "1 vs 5");
        if (def.length() >= 3) {
            char seq = def.charAt(2);
            part = Card.suiteSign(def.charAt(0)) + def.charAt(1);
            this.partnerCard.setText(part);
            if (def.charAt(0) == Card.HEART || def.charAt(0) == Card.DIAMOND) {
                this.partnerCard.getStyle().setFgColor(RED_COLOR);
            } else {
                this.partnerCard.getStyle().setFgColor(BLACK_COLOR);
            }
            switch (seq) {
                case '0':
                    part = Dict.get(main.lang, " 1st");
                    break;
                case '1':
                    part = Dict.get(main.lang, " 2nd");
                    break;
                case '2':
                    part = Dict.get(main.lang, " 3rd");
                    break;
                case '3':
                    part = Dict.get(main.lang, " 4th");
                    break;
            }
            this.partnerCardSeq.setText(Dict.get(main.lang, "Partner") + ":" + part);
        } else {
            this.partnerCardSeq.setText(part);
        }
        this.widget.revalidate();
//        this.partnerInfo.revalidate();
    }

    private void definePartner(Map<String, Object> data) {
        int seat = Func.parseInteger(data.get("seat"));
        PlayerInfo pp = this.playerMap.get(seat);
        if (pp != null) pp.showTimer(timeout, 0, "play");
        String def = Func.trimmedString(data.get("def"));
//        this.partnerCard.setText(partnerDef(def));
        this.displayPartnerDef(def);
    }

    private boolean isValid(List<Card> cards, UserHelp uh) {
        if (cards.isEmpty()) {
            uh.showHelp(uh.NO_CARD_SELECTED);
            return false;
        }
        if (!hand.validSelection()) {
            uh.showHelp(uh.INVALID_PLAY);
            return false;
        }
        return true;
    }

    public final List<PlayerInfo> infoLst = new ArrayList<>();
    public Map<Integer, PlayerInfo> playerMap = new HashMap<>();
    public boolean tableOn = false;
    private boolean robotOn = false;
    private boolean watching = false;
    public boolean tableEnded = false;
    private int timeout = 30;   // 30 seconds
    public boolean isPlaying = false;
    private int contractPoint = -1;
    private int playerRank;
    private int currentSeat;
    private Hand hand;
    private Label lbGeneral;
    private Label lbPass;
//    private Container gameInfo;
//    private Container partnerInfo;
    private Label contractInfo;
    private Label trumpInfo;
    private Label partnerCardSeq;
    private Label partnerCard;
    private Label pointsInfo;
    private Button bExit;
    private CheckBox bRobot;
    private Button bSit;

    private String currentTableId = "";
    private String currentPass = "";

    private void resetTable() {
        this.tableEnded = false;
        this.hand.setIsReady(false);
        this.hand.clearCards();
        candidateTrumps.clear();
        main.formTable.setGlassPane(null);

        isPlaying = false;
        timeout = 30;
        contractPoint = -1;
        playerRank = 0;
        currentSeat = 0;
        contractInfo.setText(" ");
        trumpInfo.setText(" ");
        partnerCardSeq.setText(" ");
        partnerCard.setText(" ");
        pointsInfo.setText(" ");
        for (PlayerInfo pp : this.infoLst) {
            pp.reset();
        }
        this.leadingPlayer = null;
        this.currentPass = "";

        this.robotOn = false;
        this.bRobot.setSelected(false);
    }

    public int numCardsLeft = 0;

    private void refreshTable(Map<String, Object> data) {
        this.tableOn = true;
        this.resetTable();

        this.currentTableId = Func.trimmedString(data.get("tid"));
        String stage = Func.trimmedString(data.get("stage"));
        this.isPlaying = stage.equalsIgnoreCase(PLAYING_STAGE);
        currentSeat = Func.parseInteger(data.get("seat"));

        String visit = Func.trimmedString(data.get("visit"));
        this.watching = visit.equals("Y");
        this.bRobot.setVisible(!this.watching);
        this.bSit.setVisible(this.watching);
        if (this.watching) {
            this.numCardsLeft = Func.parseInteger(data.get("cnum"));
        } else {
            this.numCardsLeft = 0;
            if (this.currentTableId.startsWith("L")) {
                this.currentPass = Func.trimmedString(data.get("pass"));
            }
            String pTrumps = Func.trimmedString(data.get("ptrumps"));
            if (!pTrumps.isEmpty()) {
                for (int i = 0, n = pTrumps.length(); i < n; i++) {
                    this.candidateTrumps.add(pTrumps.charAt(i));
                }
            }
        }

        int actionSeat = Func.parseInteger(data.get("next"));
        this.playerRank = Func.parseInteger(data.get("rank"));
        int game = Func.parseInteger(data.get("game"));
        int defaultTimeout = Func.parseInteger(data.get("timeout"));
        if (defaultTimeout > 0) this.timeout = defaultTimeout;

        if (!this.watching) this.addCards(data);

        PlayerInfo p0 = this.infoLst.get(0);
        String myName = Func.trimmedString(data.get("name"));
        p0.setMainInfo(currentSeat, myName, this.playerRank);
        this.playerMap.put(currentSeat, p0);
        char trumpSuite = Card.JOKER;

        String info = Func.trimmedString(data.get("info"));
        if (info.isEmpty()) {
            this.gameRank = Func.parseInteger(data.get("gameRank"));
            this.contractPoint = Func.parseInteger(data.get("contract"));

            String trump = Func.trimmedString(data.get("trump"));
            if (!trump.isEmpty()) trumpSuite = trump.charAt(0);

            if (gameRank > 0) {
                hand.sortCards(trumpSuite, gameRank, true);
            } else {
                hand.sortCards(trumpSuite, playerRank, true);
            }

            if (!this.isPlaying) {
                int minBid = Func.parseInteger(data.get("minBid"));
                if (minBid > 0) p0.addMinBid(minBid);
                displayBidInfo(p0, Func.trimmedString(data.get("bid")));
            } else {
                List<Card> lst = Card.fromString(Func.trimmedString(data.get("cards")), this.currentTrump, this.gameRank);
                if (lst != null) {
                    this.hand.addPlayCards(p0, lst);
                }
                int point1 = Func.parseInteger(data.get("pt1")); // points earned by player itself
                if (point1 != -1) {
                    if (point1 == 0) {
                        p0.contractor.setText("");
                    } else {
                        p0.contractor.setText(point1 + "");
                    }
                }
                int lead = Func.parseInteger(data.get("lead"));
                if (lead > 0) {
                    p0.setLeadSign(true);
                }
            }
        } else {
            p0.userHelp.showInfo(info);
            startNotifyTimer(data);
        }

        List<Object> players = (List<Object>) data.get("players");
        for (int i = 0, j = 1; j < Card.TOTAL_SEATS; i++, j++) {
            parsePlayerInfo(this.infoLst.get(j), (Map<String, Object>) players.get(i));
        }

        lbGeneral.setText(main.lang.equalsIgnoreCase("zh") ? "第" + game + "局" : "Game " + game);
        if (!this.currentPass.isEmpty()) {
            lbPass.setText(this.currentPass);
            lbPass.setVisible(true);
            String passInfo = Dict.get(main.lang, Dict.TABLE_CODE) + ": " + this.currentPass;
            p0.userHelp.showInfo(passInfo);
//            showInfo(passInfo);
//            ToastBar.showInfoMessage(Dict.get(main.lang, Dict.TABLE_CODE) + ": " + this.currentPass); //not work
        } else {
            lbPass.setVisible(false);
//            FontImage.setMaterialIcon(lbPass, '\0');
        }

        String strTrump = "";
//        String ptInfo = " ";
        String pointInfo = " ";
        int actTime = Func.parseInteger(data.get("acttime"));
        String act = Func.trimmedString(data.get("act"));
        if (this.isPlaying) {
            p0.needChangeActions = true;
            if (!act.equals("dim")) {
                if (trumpSuite == Card.JOKER) {
                    strTrump += "NT ";
                } else {
                    strTrump += Card.suiteSign(trumpSuite);
                }
                strTrump += Card.rankToString(gameRank);
            }
//            ptInfo = this.partnerDef(Func.trimmedString(data.get("def")));
            this.displayPartnerDef(Func.trimmedString(data.get("def")));
            int points = Func.parseInteger(data.get("pt0"));
            pointInfo = points + Dict.get(main.lang, " points");

            this.contractInfo.setText(this.contractPoint + "");
            this.trumpInfo.setText(strTrump);
            if (trumpSuite == Card.HEART || trumpSuite == Card.DIAMOND) {
                this.trumpInfo.getStyle().setFgColor(RED_COLOR);
            } else {
                this.trumpInfo.getStyle().setFgColor(BLACK_COLOR);
            }
        }
//        this.partnerCard.setText(ptInfo);
        this.pointsInfo.setText(pointInfo);

        PlayerInfo pp = this.playerMap.get(actionSeat);
        if (pp != null) {
            if (act.equals("bury")) {
                pp.showTimer(actTime, this.contractPoint, "bury");
            } else if (act.isEmpty()) {
                pp.showTimer(this.timeout, this.contractPoint, "bid");
            } else {
                pp.showTimer(this.timeout, this.contractPoint, act);
            }
        }

        if (this.isPlaying) {
            int seatContractor = Func.parseInteger(data.get("seatContractor"));
            pp = this.playerMap.get(seatContractor);
            if (pp != null) pp.setContractor(CONTRACTOR);
            int seatPartner = Func.parseInteger(data.get("seatPartner"));
            pp = this.playerMap.get(seatPartner);
            if (pp != null) {
                if (pp.isContractSide) {
                    pp.setContractor(CONTRACTOR + "," + PARTNER);
                } else {
                    pp.setContractor(PARTNER);
                }
            }

            this.currentTrump = trumpSuite;
        }
        hand.setIsReady(true);
        this.tableOn = true;
        main.enableButtons();
        if (this.robotOn) {
            mySocket.addRequest(Request.create(Request.ROBOT, "on", 1));
        }

//        hand.repaint();
//        this.widget.revalidate();
        main.validateTable();
        if (Card.DEBUG_MODE) Log.p("refresh table: done");
    }

    private void startNotifyTimer(Map<String, Object> data) {
        if (gameTimer == null) {
            int pauseSeconds = Func.parseInteger(data.get("pause"));
            if (pauseSeconds - 5 > 0) {
                gameTimer = new UITimer(this.notifyPlayer);
                gameTimer.schedule((pauseSeconds - 5) * 1000, false, main.formTable);
            }
            this.infoLst.get(0).showTimer(pauseSeconds, 0, "wait");
        }
    }

    private Command holdCommand(int holdMinutes) {
        String txt = Dict.get(main.lang, "No");
        if (holdMinutes > 0) {
            txt = holdMinutes + Dict.get(main.lang, " minutes");
        }

        final Player p = this;
        return new Command(txt) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                p.tableOn = false;
                p.cancelTimers();
                if (p.mySocket != null) {
                    p.mySocket.clearRequest();
                    Request req = new Request(Request.EXIT, false);
                    p.mySocket.addRequest(req.append("hold", holdMinutes));
                }
                Display.getInstance().callSerially(() -> {
                    p.main.switchScene("view");
                });
            }
        };
    }

    public Hand getHand() {
        return hand;
    }

    private Container widget;
    public void createTable(Container table) {
        this.hand = new Hand(this);

        PlayerInfo p0 = new PlayerInfo("bottom");
        this.infoLst.add(p0);
        this.infoLst.add(new PlayerInfo("right down"));
        this.infoLst.add(new PlayerInfo("right up"));
        this.infoLst.add(new PlayerInfo("top"));
        this.infoLst.add(new PlayerInfo("left up"));
        this.infoLst.add(new PlayerInfo("left down"));

        this.bExit = new Button(Dict.get(main.lang, "Exit"));
        this.bExit.getAllStyles().setFont(Hand.fontGeneral);
        FontImage.setMaterialIcon(bExit, FontImage.MATERIAL_EXIT_TO_APP);
        if (!Card.FOR_IOS) this.bExit.setUIID("myExit");
        bExit.addActionListener((e) -> {
            if (!watching && !tableEnded) {
                boolean orgRobotOn = robotOn;
                if (!orgRobotOn) mySocket.addRequest(Request.create(Request.ROBOT, "on", 1));

                Dialog dlg = new Dialog(Dict.get(main.lang, "Hold Seat") + "?");
                dlg.add(new Button(holdCommand(15)));
                dlg.add(new Button(holdCommand(5)));
                dlg.add(new Button(holdCommand(0)));
                dlg.setBackCommand("", null, (ev) -> {
                    if (!orgRobotOn) mySocket.addRequest(Request.create(Request.ROBOT, "on", 0));
                    dlg.dispose();
                });
                dlg.show();
            } else {
                mySocket.addRequest(new Request(Request.EXIT, false));
                tableOn = false;
                cancelTimers();
                main.switchScene("view");
            }
        });

        this.bRobot = new CheckBox(Dict.get(main.lang, "Robot"));
        this.bRobot.getAllStyles().setFont(Hand.fontGeneral);
        FontImage.setMaterialIcon(bRobot, FontImage.MATERIAL_ANDROID);
        bRobot.getAllStyles().setFgColor(INFO_COLOR);
        bRobot.addActionListener((e) -> {
            robotOn = bRobot.isSelected();
            if (mySocket != null) {
                mySocket.addRequest(Request.create(Request.ROBOT, "on", robotOn ? 1 : 0));
            }
            if (robotOn) {
                infoLst.get(0).dismissActions();
            }
        });

        this.bSit = new Button(Dict.get(main.lang, "Join"));
        this.bSit.getAllStyles().setFont(Hand.fontRank);
        this.bSit.getAllStyles().setBgImage(main.back);
        bSit.addActionListener((e) -> {
            if (currentTableId.startsWith("L")) {
                main.formView.inputPassword(Player.this);
                return;
            }
            mySocket.addRequest(Request.create(Request.SIT, "tid", currentTableId.substring(1)).setReSend(true));
        });

        this.lbGeneral = new Label("Game ");
        this.lbGeneral.getStyle().setFont(Hand.fontGeneral);
        this.lbGeneral.getStyle().setFgColor(main.currentColor.generalColor);

        this.lbPass = new Label(" ");
        this.lbPass.getStyle().setFont(Hand.fontGeneral);
        this.lbPass.getStyle().setFgColor(TIMER_COLOR);
        FontImage.setMaterialIcon(lbPass, FontImage.MATERIAL_LOCK_OPEN);

        String gmInfo = "gmInfo";
        String ptInfo = "ptInfo";
        String pointInfo = "pointInfo";

//        this.gameInfo = new Container();
        this.contractInfo = new Label(gmInfo);
        this.trumpInfo = new Label(gmInfo);
        this.contractInfo.getStyle().setFgColor(0xebef07);
        this.contractInfo.getStyle().setFont(Hand.fontRank);
        this.trumpInfo.getStyle().setFont(Hand.fontRank);
//        this.gameInfo.add(this.contractInfo).add(this.trumpInfo);

//        this.partnerInfo = new Container();
        this.partnerCardSeq = new Label(ptInfo);
        this.partnerCard = new Label(ptInfo);
        this.partnerCardSeq.getStyle().setFgColor(INFO_COLOR);
        this.partnerCardSeq.getStyle().setFont(Hand.fontGeneral);
        this.partnerCard.getStyle().setFont(Hand.fontRank);
//        this.partnerInfo.add(this.partnerCardSeq).add(this.partnerCard);

        this.pointsInfo = new Label(pointInfo);
        this.pointsInfo.getStyle().setFgColor(main.currentColor.pointColor);
        this.pointsInfo.getStyle().setFont(Hand.fontRank);

        this.widget = new Container(new LayeredLayout());

//        this.widget.add(bExit).add(this.lbGeneral).add(this.gameInfo).add(this.partnerInfo).add(this.pointsInfo);
        this.widget.add(bExit).add(this.lbGeneral).add(this.lbPass).add(this.trumpInfo).add(this.contractInfo)
                .add(this.partnerCardSeq).add(this.partnerCard).add(this.pointsInfo);
        this.widget.add(bRobot);
        if (main.registered) this.widget.add(bSit);
        this.widget.revalidate();

        table.add(hand);
        table.revalidate();
        table.add(this.widget);
        table.revalidate();

        LayeredLayout ll = (LayeredLayout) table.getLayout();
        ll.setInsets(bExit, "0 0 auto auto");   //top right bottom left
        ll.setInsets(bRobot, "auto 0 0 auto");   //top right bottom left
        if (main.registered) ll.setInsets(bSit, "auto 0 0 auto");   //top right bottom left
        ll.setInsets(this.lbGeneral, "-" + Hand.deltaGeneral + " auto auto 0")
                .setInsets(this.lbPass, "-" + Hand.deltaGeneral + " auto auto 0")
                .setInsets(this.partnerCardSeq, "auto 0 " + Hand.deltaGeneral + " auto")
                .setInsets(this.partnerCard, "-" + Hand.deltaRank + " 0 auto auto")
                .setInsets(this.pointsInfo, "0 auto auto 20%")
                //                .setInsets(this.gameInfo, "-" + Hand.deltaRank + " auto auto 0");
                .setInsets(this.contractInfo, "-" + Hand.deltaRank + " auto auto 0")
                .setInsets(this.trumpInfo, "-" + Hand.deltaRank + " auto auto 0");
//        ll.setReferenceComponentTop(this.gameInfo, lbGeneral, 1f);
//        ll.setReferenceComponentTop(this.partnerInfo, bExit, 1f);
        ll.setReferenceComponentTop(this.contractInfo, lbGeneral, 1f);
        ll.setReferenceComponentTop(this.trumpInfo, lbGeneral, 1f);
        ll.setReferenceComponentLeft(this.trumpInfo, this.contractInfo, 1f);
        ll.setReferenceComponentTop(this.partnerCard, lbGeneral, 1f);
        ll.setReferenceComponentLeft(this.lbPass, lbGeneral, 1f);
        ll.setReferenceComponentBottom(this.partnerCardSeq, this.partnerCard, 0f);
        ll.setReferenceComponentRight(this.partnerCardSeq, this.partnerCard, 1f);

        for (PlayerInfo pp : infoLst) {
            pp.addItems(this.widget);
        }

        table.forceRevalidate();
    }

    public void refreshLang() {
        this.bExit.setText(Dict.get(main.lang, "Exit"));
        this.bRobot.setText(Dict.get(main.lang, "Robot"));
        this.bSit.setText(Dict.get(main.lang, "Join"));

        this.lbGeneral.getStyle().setFgColor(main.currentColor.generalColor);
        this.pointsInfo.getStyle().setFgColor(main.currentColor.pointColor);
    }

    private void cancelTimers() {
        for (PlayerInfo pp : infoLst) {
            pp.cancelTimer();
        }
    }

    private void parsePlayerInfo(PlayerInfo pp, Map<String, Object> rawData) {
        int seat = Func.parseInteger(rawData.get("seat"));
        String name = Func.trimmedString(rawData.get("name"));
        if (name.isEmpty()) name = "#" + seat;
        pp.setMainInfo(seat, name, Func.parseInteger(rawData.get("rank")));
        this.playerMap.put(seat, pp);
        if (!this.isPlaying) {
            displayBidInfo(pp, Func.trimmedString(rawData.get("bid")));
        } else {
            List<Card> lst = Card.fromString(Func.trimmedString(rawData.get("cards")), this.currentTrump, this.gameRank);
            if (lst != null) {
                this.hand.addPlayCards(pp, lst);
            }
            int point1 = Func.parseInteger(rawData.get("pt1")); // points earned by player itself
            if (point1 != -1) {
                if (point1 == 0) {
                    pp.contractor.setText("");
                } else {
                    pp.contractor.setText(point1 + "");
                }
            }
            int lead = Func.parseInteger(rawData.get("lead"));
            if (lead > 0) {
                pp.setLeadSign(true);
            }
        }
    }

    private String bidToString(String bid) {
        if (bid == null || bid.isEmpty() || bid.equals("-")) return "";
        if (bid.equalsIgnoreCase("pass")) {
            return "Pass";
        }

        return "" + Func.parseInteger(bid);
    }

    private PlayerInfo leadingPlayer;

    public PlayerInfo getLeadingPlayer() {
        if (this.leadingPlayer != null) {
            return this.leadingPlayer;
        }
        for (int x = 1; x < this.infoLst.size(); x++) {
            PlayerInfo pp = this.infoLst.get(x);
            if (!pp.cards.isEmpty()) {
                return pp;
            }
        }

        return null;
    }

    private void displayBidInfo(PlayerInfo pp, String bid) {
        pp.showPoints(bidToString(bid));
    }

    private String trumpRecommend = "";
    private void displayBid(Map<String, Object> data) {
        int seat = Func.parseInteger(data.get("seat"));
        int actionSeat = Func.parseInteger(data.get("next"));
        this.contractPoint = Func.parseInteger(data.get("contract"));   // send contract point every time to avoid error
        String bid = Func.trimmedString(data.get("bid"));
        PlayerInfo pp = this.playerMap.get(seat);
        if (pp != null) displayBidInfo(pp, bid);

        pp = this.playerMap.get(actionSeat);
        if (pp != null) {
            boolean bidOver = Func.parseBoolean(data.get("bidOver"));
            trumpRecommend = Func.trimmedString(data.get("itrump"));
            int actTime = Func.parseInteger(data.get("acttime"));
            pp.showTimer(actTime > 1 ? actTime : this.timeout, this.contractPoint, bidOver ? "dim" : "bid");
        }
    }

    private void displayCards(PlayerInfo pp, String cards) {
        List<Card> lst = Card.fromString(cards, this.currentTrump, this.gameRank);
        if (lst != null) {
            this.hand.addPlayCards(pp, lst);
            if (pp.location.equals("bottom")) {
                if (!this.hand.isEmpty()) {
                    hand.removeCards(cards);
                } else if (this.watching) {
                    this.numCardsLeft -= lst.size();
                }

                pp.userHelp.clear();
            }
        }
        pp.cancelTimer();
    }

    private void gameSummary(Map<String, Object> data) {
        if (!this.tableOn) {
            return;
        }

        bRobot.setSelected(false);
        this.robotOn = false;

        int points = Func.parseInteger(data.get("pt0"));
        if (points != -1) {
            this.pointsInfo.setText(points + Dict.get(main.lang, " points"));
        } else {
            this.tableEnded = true;
            int finPrac = Func.parseInteger(Storage.getInstance().readObject("finprac"));
            if (finPrac < 1) {
                Storage.getInstance().writeObject("finprac", 1);
            }
        }
        final String summary = Func.trimmedString(data.get("summary"));
        int seat = Func.parseInteger(data.get("seat"));  // the contractor
        for (PlayerInfo pp : this.infoLst) {
            this.hand.clearPlayCards(pp);
            if (pp.seat == seat) {
                String cards = Func.trimmedString(data.get("hole"));
                List<Card> lst = Card.fromString(cards, this.currentTrump, this.gameRank);
                this.hand.addPlayCards(pp, lst);
            }
        }
        hand.repaint();
        this.infoLst.get(0).needChangeActions = true;

        int x = hand.displayWidth(6) + Hand.fontRank.getHeight();
        if (!summary.isEmpty()) {
            Rectangle safeRect = this.main.formTable.getSafeArea();
            this.main.formTable.setGlassPane((g, rect) -> {
                g.translate(safeRect.getX(), safeRect.getY());
                g.setColor(INFO_COLOR);
                g.setFont(Hand.fontGeneral);
                int idx = -1;
                int y = this.infoLst.get(2).posY();    // right top player position Y
                String str = summary;
                while (!str.isEmpty()) {
                    idx = str.indexOf("\n");
                    if (idx >= 0) {
                        g.drawString(str.substring(0, idx), x, y);
                    } else {
                        g.drawString(str, x, y);
                        break;
                    }
                    y += Hand.fontGeneral.getHeight();
                    str = str.substring(idx + 1);
                }
                g.translate(-g.getTranslateX(), -g.getTranslateY());
            });
        }
    }

    private void playerIn(Map<String, Object> data) {
        int seat = Func.parseInteger(data.get("seat"));
        PlayerInfo pp = this.playerMap.get(seat);
        if (pp != null) {
            pp.updateName(Func.trimmedString(data.get("name")), false);
        }
    }

    private void playerOut(Map<String, Object> data) {
        int seat = Func.parseInteger(data.get("seat"));
        PlayerInfo pp = this.playerMap.get(seat);
        if (pp != null) {
            pp.updateName(pp.playerName, true);
        }
    }

    private void drawBackShade(Graphics g, int x, int y, String str, Font f) {
        if (str.isEmpty() || str.equals(".")) return;
        int idx = -1;
        int w = 0, h = 0;
        int wStr = 0;
        String line = "";
        while (!str.isEmpty()) {
            idx = str.indexOf("\n");
            if (idx >= 0) {
                line = str.substring(0, idx);
            } else {
                line = str;
            }
            wStr = f.charsWidth(line.toCharArray(), 0, line.length());
            if (wStr > w) w = wStr;
            str = str.substring(idx + 1);
            h += f.getHeight();
            if (idx < 0) break;
        }
        g.setColor(TuoLaJiPro.BACKGROUND_COLOR);
        g.fillRoundRect(x - 10, y - 10, w + 20, h + 20, 20, 20);
    }

    private void showInfo(Map<String, Object> data) {
        showInfo(Func.trimmedString(data.get("info")));
    }

    private void showInfo(String info) {
//        final Form curForm = main.getCurForm();
        final Player thisPlayer = this;
        final Form curForm = getCurrentForm();
        Rectangle safeRect = curForm.getSafeArea();
        if (!info.isEmpty() && !info.equals(".")) {
            int fontHeight = Hand.fontGeneral.getHeight();
            int x = main.isMainForm ? fontHeight : (hand.displayWidth(6) + fontHeight);
            curForm.setGlassPane((g, rect) -> {
                g.translate(safeRect.getX(), safeRect.getY());

                int idx = -1;
                int y = main.isMainForm ? fontHeight * 2 : thisPlayer.infoLst.get(2).posY();
                drawBackShade(g, x, y, info, Hand.fontGeneral);

                int y0 = y;
                String str = info;
                g.setColor(INFO_COLOR);
                g.setFont(Hand.fontGeneral);
                while (!str.isEmpty()) {
                    idx = str.indexOf("\n");
                    if (idx >= 0) {
                        g.drawString(str.substring(0, idx), x, y);
                    } else {
                        g.drawString(str, x, y);
                        break;
                    }
                    y += fontHeight;
                    str = str.substring(idx + 1);
                }

                g.translate(-g.getTranslateX(), -g.getTranslateY());
            });
        } else {
            curForm.setGlassPane(null);
        }

    }

    void setLeadingIcon(PlayerInfo pp) {
        for (PlayerInfo p : infoLst) {
            p.setLeadSign(p == pp);
        }
    }

    private void playCards(Map<String, Object> data) {
        int seat = Func.parseInteger(data.get("seat"));
        int actionSeat = Func.parseInteger(data.get("next"));
        int points = Func.parseInteger(data.get("pt0")); // total points by non-contract players
        if (points != -1) {
            this.pointsInfo.setText(points + Dict.get(main.lang, " points"));
            this.widget.revalidate();
        }

        int pointSeat = Func.parseInteger(data.get("pseat"));
        if (pointSeat > 0) {
            PlayerInfo pp = this.playerMap.get(pointSeat);
            if (pp != null && !pp.isContractSide) {
                int point = Func.parseInteger(data.get("pt")); // points earned by player itself
                if (point != -1) {
                    if (point == 0) {
                        pp.contractor.setText("");
                    } else {
                        pp.contractor.setText(point + "");
                    }
                }
            }
        }

        if(seat > 0) {
            String cards = Func.trimmedString(data.get("cards"));
            PlayerInfo pp = this.playerMap.get(seat);
            if (pp != null) {
                displayCards(pp, cards);
                int lead = Func.parseInteger(data.get("lead"));
                if (lead > 0) {
                    setLeadingIcon(pp);
                }

                boolean isPartner = Func.parseBoolean(data.get("isPartner"));
                if (isPartner) {
                    if (pp.isContractSide) {
                        pp.setContractor(CONTRACTOR + "," + PARTNER);
                    } else {
                        pp.setContractor(PARTNER);
                    }
                }
                if (!pp.isContractSide) {
                    int point1 = Func.parseInteger(data.get("pt1")); // points earned by player itself
                    if (point1 != -1) {
                        if (point1 == 0) {
                            pp.contractor.setText("");
                        } else {
                            pp.contractor.setText(point1 + "");
                        }
                    }
                }
            }
        } else {
            for (PlayerInfo pp : this.infoLst) {
                this.hand.clearPlayCards(pp);
            }
            this.leadingPlayer = null;
        }

        if (actionSeat > 0) {
            PlayerInfo pp = this.playerMap.get(actionSeat);
            if (pp != null) {
                int actTime = Func.parseInteger(data.get("acttime"));
                pp.showTimer(actTime > 1 ? actTime : this.timeout, this.contractPoint, "play");
                if (this.leadingPlayer == null) {
                    this.leadingPlayer = pp;
                }

                if (pp.location.equals("bottom")) {
                    String sugCards = Func.trimmedString(data.get("sug"));   // suggested cards by AI
                    if (!sugCards.isEmpty()) {
                        hand.autoSelectCards(sugCards);
                    } else {
                        hand.autoSelectCards();
                    }
                }
            }
        }

        this.widget.revalidate();
    }

    protected char currentTrump;
    protected int gameRank;
    synchronized private void setTrump(Map<String, Object> data) {
        if (!this.watching && this.hand.isEmpty()) return;
        String trump = Func.trimmedString(data.get("trump"));
        if (trump.isEmpty()) return;
        this.currentTrump = trump.charAt(0);
        int seat = Func.parseInteger(data.get("seat"));
        this.gameRank = Func.parseInteger(data.get("gameRank"));
        int actTime = Func.parseInteger(data.get("acttime"));
        int contractPoint = Func.parseInteger(data.get("contract"));
        this.hand.sortCards(currentTrump, this.gameRank, true);
//        this.hand.repaint();

        for (int st : this.playerMap.keySet()) {
            PlayerInfo pp = this.playerMap.get(st);
            if (st == seat) {
                pp.setContractor(CONTRACTOR);
                if (seat != this.currentSeat) {
                    pp.showTimer(actTime, contractPoint, "bury");
                }
            } else {
                pp.points.setText("");
                pp.needChangeActions = true;
            }
        }

        String trumpInfo = "";
        if (currentTrump == Card.JOKER) {
            trumpInfo += "NT ";
        } else {
            trumpInfo += Card.suiteSign(currentTrump);
        }
        trumpInfo += Card.rankToString(gameRank);
        this.contractInfo.setText(this.contractPoint + "");
        this.trumpInfo.setText(trumpInfo);
        if (currentTrump == Card.HEART || currentTrump == Card.DIAMOND) {
            this.trumpInfo.getStyle().setFgColor(RED_COLOR);
        } else {
            this.trumpInfo.getStyle().setFgColor(BLACK_COLOR);
        }
        this.isPlaying = true;
//        this.gameInfo.revalidate();
        this.widget.revalidate();
    }

    static int serverWaitCycle = 10; // 10 times
    static int idleWaitCycle = 20; // 20 times

    class MySocket extends SocketConnection {

        private boolean closeRequested = false;
        private boolean checkConnection = false;
//        private List<String> pendingRequests0 = new ArrayList<>();
        private List<Request> pendingRequests = new ArrayList<>();
        private Request currentRequest;
        private Request lastRequest;

        public void closeConnection() {
            this.closeRequested = true;
            if (Card.DEBUG_MODE) Log.p("this.closeRequested: " + this.closeRequested);
        }

        public void clearRequest() {
            pendingRequests.clear();
        }

        public void addRequest(Request req) {
            pendingRequests.add(req);
        }

        private void processReceived(String msg) throws IOException {
            try {
                JSONParser parser = new JSONParser();
                int idx = msg.indexOf("\n");
                while (idx > 0) {
                    String subMsg = msg.substring(0, idx);
                    msg = msg.substring(idx + 1);
                    idx = msg.indexOf("\n");
                    if (subMsg.trim().isEmpty()) continue;

                    if (!subMsg.startsWith("{")) {
                        subMsg = Card.confusedData(subMsg);
                        subMsg = new String(Base64.decode(subMsg.getBytes()));
                    }

                    if (!subMsg.startsWith("{") || !subMsg.endsWith("}")) continue;

                    if (TuoLaJiPro.DEBUG) Log.p("Received: " + subMsg);
                    Map<String, Object> data = parser.parseJSON(new StringReader(subMsg));
                    final String action = Func.trimmedString(data.get("action"));

                    Display.getInstance().callSeriallyAndWait(() -> {
                        switch (action) {
                            case "reg": // registered
                                main.finishRegistration();
                                break;
                            case "auth": // need verify auth code
                                main.startRegistration();
                                break;

                            case "acc": // account info
                                coins = Func.parseInteger(data.get("coin"));
                                break;
                            case "coin":
                                Func.noEnoughCoin(main.lang);
                                break;
                            case "list":
                                // list current tables
                                main.formView.refreshTableList(data);
                                break;

                            case "init":
                                main.switchScene("table");
                                refreshTable(data);
                                break;
                            case "bid":
                                if (tableOn) displayBid(data);
                                break;
                            case "set_trump":
                                if (tableOn) setTrump(data);
                                break;
                            case "add_remains":
                                if (tableOn) addRemains(data);
                                break;
                            case "bury":
                                if (tableOn) buryCards(data);
                                break;
                            case "partner":
                                if (tableOn) definePartner(data);
                                break;
                            case "play":
                                if (tableOn) playCards(data);
                                break;
                            case "gameover":
                                if (tableOn) {
                                    hand.clearCards();
                                    cancelTimers();
                                    startNotifyTimer(data);
                                    gameSummary(data);
                                }
                                break;
                            case "info":
                                showInfo(data);
                                break;
                            case "in":
                                if (tableOn) playerIn(data);
                                break;
                            case "out":
                                if (tableOn) playerOut(data);
                                break;
                            case "robot":
                                bRobot.setSelected(true);
                                robotOn = true;
                                break;
                            case "opt":
                                tableOn = false;
                                main.showPlayOption();
                                break;

                            case "priv":
                                main.showPrivacy(Func.trimmedString(data.get("msg")));
                                break;
                        }
                    });
                }
            } catch (Exception err) {
                // to prevent break the connection by accident
                if (TuoLaJiPro.DEBUG) {
                    err.printStackTrace();
                } else {
                    Log.p(err.getMessage());
                }
            }
        }

        @Override
        public void connectionError(int errorCode, String message) {
            closeRequested = true;
//            main.enableButtons();
            if (checkOnce) {
                if (tljHost.equals(Card.TLJ_HOST)) {
                    tljHost = Card.TLJ_HOST_IP;
                } else {
                    tljHost = Card.TLJ_HOST;
                }
                if (initConnect) checkOnce = false;
            } else {
                if (initConnect) {
                    Dialog.show(Dict.get(main.lang, "Error"), Dict.get(main.lang, Dict.FAIL_CONNECT_SERVER), Dict.get(main.lang, "OK"), "");
                    Display.getInstance().exitApplication();
                    return;
                }
            }

            //mySocket = null;    // reset connection
            if (tableOn || initConnect) {
                cancelTimers();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {

                }

                if (initConnect) {
                    connectServer(initConnect);
                } else {
                    connectServer("");
                }
            } else {
                main.onConnectionError();
            }
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            closeRequested = false;
            checkOnce = false;

            if (initConnect) {
                initConnect = false;
                main.showLogin();
            } else {
                main.enableButtons();
            }

            byte[] buffer = new byte[4096];
            int count = 0;
            try {
                if (Card.DEBUG_MODE) Log.p("connected!");
                while (isConnected() && !closeRequested) {
                    if (!checkConnection && !pendingRequests.isEmpty()) {
                        this.currentRequest = pendingRequests.remove(0);
                        String request = this.currentRequest.getMsg();
                        if (Card.DEBUG_MODE) {
                            os.write(request.getBytes());
                            Log.p("send request: " + request);
                        } else {
                            if (TuoLaJiPro.DEBUG) Log.p("Send: " + request);
                            request = Base64.encode(request.getBytes());
                            os.write(Card.confusedData(request).getBytes());
                        }

                        checkConnection = this.currentRequest.isCheckReply();
                        count = 0;
                    }
                    int n = is.available();
                    if (n > 0) {
                        checkConnection = false;
                        count = 0;
//                        count1 = 0;
                        String msg = "";
                        while ((n = is.read(buffer, 0, 4096)) > 0) {
                            msg += new String(buffer, 0, n);
                            if (n < 4096) break;
                        }
                        processReceived(msg);
                    } else {
                        count++;
                        if (count > serverWaitCycle) {
                            if (checkConnection) {
                                this.lastRequest = this.currentRequest.isReSend() ? this.currentRequest : null;
                                if (TuoLaJiPro.DEBUG) Log.p("lost conncetion!");
                                break;
                            } else if (tableOn) {
//                                if (robotOn || tableEnded || watching) {
                                if (robotOn || tableEnded) {
                                    count = 0;
                                } else if (count > idleWaitCycle) {
                                    addRequest(new Request(Request.RE, true));
                                    count = 0;
                                }
                            } else {
                                count = 0;
                            }
                        }

                        Thread.sleep(500);
                    }
                }
            } catch (Exception err) {
                if (TuoLaJiPro.DEBUG) Log.p("exception conncetion!");
                err.printStackTrace();
            }

            try {
                os.close();
                is.close();
            } catch (Exception err) {

            }

            if (!closeRequested) {
                // not expected, connect again
               if (TuoLaJiPro.DEBUG) Log.p("re-connect");
                mySocket = null;
                connectServer(this.lastRequest);
            }
        }
    }

    class CountDown implements Runnable {

        PlayerInfo pInfo;
        Label timer;
        int timeout;
        CountDown(PlayerInfo pInfo, int timeout) {
            this.pInfo = pInfo;
            this.timer = pInfo.timer;
            this.timeout = timeout;
        }
        public void run() {
            this.timeout--;
            if (this.timeout > 0) {
                this.timer.setText(this.timeout + "");
            } else {
                this.timer.setText("");
                FontImage.setMaterialIcon(timer, FontImage.MATERIAL_TIMER_OFF);
                pInfo.dismissActions();
                pInfo.countDownTimer.cancel();
                pInfo.countDownTimer = null;
//                if (mySocket != null) {
//                    mySocket.setCheckConnection();
//                }
            }
        }
    }

    class PlayerInfo {

        final String location;    // top, bottom, left up, left down, right up, right down
        Label mainInfo;
        UserHelp userHelp;
        Label points;
        Label contractor;   // for contractor and partner
        Label timer;   // count down timer
        List<Card> cards = new ArrayList<>();   // cards played
        String playerName;
        UITimer countDownTimer;
        Component actionButtons;
        Container bidButtons;
//        Container passButton;
//        Container playButton;

        Container central;
        Container buttonContainer;
        FontImage leadIcon;

        Button btnBid;
        Button btnPlus;
        Button btnMinus;
        Button btnPass;
//        Button btnPassSingle;
        Button btnPlay;

        Container parent;

        int seat;
        int rank;

        boolean isContractSide = false;
        boolean isInitial = true;

        PlayerInfo(String loc) {
            this.location = loc;

            mainInfo = new Label(loc);
            mainInfo.getAllStyles().setFgColor(main.currentColor.generalColor);
            mainInfo.getAllStyles().setFont(Hand.fontPlain);

            points = new Label("        ");
            points.getAllStyles().setFont(Hand.fontRank);

            contractor = new Label("     ");
            contractor.getAllStyles().setFgColor(main.currentColor.pointColor);
            contractor.getAllStyles().setFont(Hand.fontRank);

            timer = new Label("    ");
            timer.getAllStyles().setFgColor(TIMER_COLOR);
            timer.getAllStyles().setFont(Hand.fontRank);
//            timer.setHidden(true, true);    // setHidden Does not work

            leadIcon = FontImage.createMaterial(FontImage.MATERIAL_MOOD, timer.getUnselectedStyle());

            if (loc.equals("bottom")) {
                Command commonCmd = new Command("Play") {   // for pass, bury and play
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        final String action = ev.getActualComponent().getName();
                        List<Card> cards;
                        switch (action) {
                            case "pass":
                                mySocket.addRequest(Request.create(Request.BID, "bid", "pass"));
                                break;
                            case "bury":
                                cards = hand.getSelectedCards();
                                if (cards.size() != 6) {
                                    btnPlay.setEnabled(false);
                                    return;
                                }
                                userHelp.clear();
                                mySocket.addRequest(Request.create(action, "cards", Card.cardsToString(cards)));
                                break;
                            case "play":
                                cards = hand.getSelectedCards();
                                if (!isValid(cards, userHelp)) {
                                    btnPlay.setEnabled(false);
                                    return;
                                }
                                userHelp.clear();
                                mySocket.addRequest(Request.create(action, "cards", Card.cardsToString(cards)));
                                break;
                        }

                        cancelTimer();
                    }
                };

                btnBid = new Button("200");
                btnPlus = new Button("");
                btnMinus = new Button("");
                btnPass = new Button(commonCmd);
//                btnPassSingle = new Button(commonCmd);
                btnPass.setText(Dict.get(main.lang, "Pass"));
//                btnPassSingle.setText(Dict.get(main.lang, "Pass"));
                btnPass.setName("pass");
//                btnPassSingle.setName("pass");

                FontImage.setMaterialIcon(btnPlus, FontImage.MATERIAL_ARROW_UPWARD);
                FontImage.setMaterialIcon(btnMinus, FontImage.MATERIAL_ARROW_DOWNWARD);

//                btnBid.getAllStyles().setFgColor(BUTTON_COLOR);
                btnBid.getAllStyles().setFont(Hand.fontRank);
//                btnBid.getAllStyles().setBgImage(backImage);
//                btnPass.getAllStyles().setBgImage(backImage);
//                btnPassSingle.getAllStyles().setBgImage(backImage);
                btnBid.getAllStyles().setBgImage(main.back);
                btnPass.getAllStyles().setBgImage(main.back);
                btnPass.getAllStyles().setFont(Hand.fontRank);
//                btnPassSingle.getAllStyles().setBgImage(main.back);

                btnBid.addActionListener((e) -> {
                    cancelTimer();
//                    mySocket.addRequest(actionBid, "\"bid\":" + btnBid.getText().trim());
                    mySocket.addRequest(Request.create(Request.BID, "bid", Func.parseInteger(btnBid.getText())));
                });

                btnPlus.addActionListener((e) -> {
                    int point = Func.parseInteger(btnBid.getText());
                    if (point < this.maxBid) {
                        point += 5;
                        btnBid.setText("" + point);
                    }
                });
                btnMinus.addActionListener((e) -> {
                    int point = Func.parseInteger(btnBid.getText());
                    if (point > 0) {
                        point -= 5;
                        btnBid.setText("" + point);
                    }
                });

                btnPlay = new Button(commonCmd);
//                btnPlay.setUIID("myLabel");
//                btnPlay.setSize(new Dimension(100, 40)); // does not work
//                btnPlay.getAllStyles().setBgImage(backImage);
                btnPlay.getAllStyles().setBgImage(main.back);
                btnPlay.getAllStyles().setFont(Hand.fontRank);
                btnPlay.getAllStyles().setAlignment(Component.CENTER);

                bidButtons = BoxLayout.encloseXNoGrow(btnPlus, btnBid, btnMinus, btnPass);
                bidButtons.getAllStyles().setAlignment(Component.CENTER);
//                passButton = BoxLayout.encloseXNoGrow(btnPassSingle);
//                passButton.getAllStyles().setAlignment(Component.CENTER);
//                playButton = BoxLayout.encloseXNoGrow(btnPlay);
//                playButton.getAllStyles().setAlignment(Component.CENTER);

                userHelp = new UserHelp(main.lang);
                central = new Container(new BoxLayout(BoxLayout.Y_AXIS));
                central.getAllStyles().setAlignment(Component.CENTER);
//                buttonContainer = new Container(new BoxLayout(BoxLayout.X_AXIS_NO_GROW));
//                buttonContainer = new Container(BorderLayout.center());
                buttonContainer = new Container(BorderLayout.absolute());
//                buttonContainer.getAllStyles().setAlignment(Component.CENTER);
                buttonContainer.add(BorderLayout.CENTER, bidButtons);
                actionButtons = bidButtons;

                timer.getAllStyles().setAlignment(Component.CENTER);
                userHelp.getAllStyles().setAlignment(Component.CENTER);
                central.add(timer).add(buttonContainer);
            }
        }

        int posX() {
            return mainInfo.getAbsoluteX();
        }

        int posY() {
            return Card.FOR_IOS
                    ? mainInfo.getAbsoluteY() + mainInfo.getHeight() - Hand.deltaGeneral
                    : mainInfo.getAbsoluteY() + mainInfo.getHeight() - Hand.deltaSymbol;
        }

        int posY0() {
            return mainInfo.getAbsoluteY();
        }

        void setLeadSign(boolean isLead) {
            if (isLead) {
                mainInfo.setIcon(leadIcon);
            } else {
                mainInfo.setIcon(null);
            }
        }

        void dismissActions() {
            if (actionButtons == null) {
                return;
            }
            actionButtons.setEnabled(false);
            actionButtons.setVisible(false);
        }

        synchronized void reset() {
            cancelTimer();
            mainInfo.getAllStyles().setFgColor(main.currentColor.generalColor);
            contractor.getAllStyles().setFgColor(main.currentColor.pointColor);
            contractor.setText("");
            points.setText("");
            hand.clearPlayCards(this);
            this.isContractSide = false;
            mainInfo.setIcon(null);
            if (location.equals("bottom")) {
                userHelp.clear();
                userHelp.setLanguage(main.lang);
                btnPass.setText(Dict.get(main.lang, "Pass"));

//                buttonContainer.removeAll();
//                central.removeComponent(buttonContainer);
//                buttonContainer = new Container(BorderLayout.absolute());
//                central.add(buttonContainer);
//                if (actionButtons != bidButtons) {
                    bidButtons.setEnabled(true);
                    bidButtons.setVisible(true);
                    buttonContainer.removeAll();
                    buttonContainer.add(BorderLayout.CENTER, bidButtons);
                    actionButtons = bidButtons;
                bidButtons.setEnabled(false);
                bidButtons.setVisible(false);
//                }
            }
        }

        synchronized void addItems(Container pane) {
            parent = pane;
            if (this.location.equals("bottom")) {
                pane.add(mainInfo).add(points).add(contractor);
            } else {
                pane.add(mainInfo).add(points).add(timer).add(contractor);
            }

            LayeredLayout ll = (LayeredLayout) pane.getLayout();
            int deltaY = -Hand.deltaRank;

            switch (this.location) {
                case "left up":
                    ll.setInsets(mainInfo, "15% auto auto 0");  //top right bottom left
                    ll.setInsets(points, deltaY + " auto auto 20")
                            .setInsets(timer, deltaY + " auto auto 20")
                            .setInsets(contractor, "14% auto auto 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "left down":
                    ll.setInsets(mainInfo, "35% auto auto 0");
                    ll.setInsets(points, deltaY + " auto auto 20")
                            .setInsets(timer, deltaY + " auto auto 20")
                            .setInsets(contractor, "34% auto auto 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "right up":
                    ll.setInsets(mainInfo, "15% 0 auto auto");
                    ll.setInsets(points, deltaY + " 20 auto auto")
                            .setInsets(timer, deltaY + " 20 auto auto")
                            .setInsets(contractor, "14% 20 auto auto");
                    ll.setReferenceComponentRight(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "right down":
                    ll.setInsets(mainInfo, "35% 0 auto auto");
                    ll.setInsets(points, deltaY + " 20 auto auto")
                            .setInsets(timer, deltaY + " 20 auto auto")
                            .setInsets(contractor, "34% 20 auto auto");
                    ll.setReferenceComponentRight(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f);
                    break;
                case "top":
                    ll.setInsets(mainInfo, "0 auto auto auto");
                    ll.setInsets(points, "0 auto auto auto")
                            .setInsets(contractor, "0 auto auto 20")
                            .setInsets(timer, "0 auto auto auto");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f)
                            .setReferenceComponentTop(points, mainInfo, 1f)
                            .setReferenceComponentTop(timer, mainInfo, 1f);
                    break;
                case "bottom":
//                    pane.add(actionButtons).add(userHelp);
//                    ll.setInsets(actionButtons, "auto auto 33% auto");
                    pane.add(central);
                    pane.add(userHelp);
                    ll.setInsets(central, "auto auto 35% auto");
//                    pane.add(btnPlay);
//                    ll.setInsets(btnPlay, "auto auto 33% auto");
//                    btnPlay.setVisible(false);

                    ll.setInsets(userHelp, "auto auto 0 auto");
                    ll.setInsets(mainInfo, "auto auto 0 auto");
                    ll.setInsets(points, "auto auto 35% auto")
                            //                            .setInsets(timer, "auto auto 0 auto")
                            .setInsets(contractor, "auto auto 0 20");
                    ll.setReferenceComponentLeft(contractor, mainInfo, 1f);
                    ll.setReferenceComponentBottom(userHelp, central, 1f);
//                            .setReferenceComponentBottom(timer, actionButtons, 1f)
//                            .setReferenceComponentBottom(userHelp, timer, 1f);

                    break;
            }

            parent.revalidate();
        }

        void setMainInfo(int seat, String playerName, int rank) {
            this.seat = seat;
            this.rank = rank;
            this.playerName = playerName;
            String info = playerName + ":" + Card.rankToString(rank, "");
            this.mainInfo.setText(info);
        }

        void updateName(String playerName, boolean out) {
            if (!out) this.playerName = playerName;
            String name = this.playerName;
            if (out) {
                name += "(" + Dict.get(main.lang, "away") + ")";
            }
            String info = name + ":" + Card.rankToString(rank, "");
            this.mainInfo.setText(info);
            parent.revalidate();
        }

        void addMinBid(int minBid) {
            String info = this.mainInfo.getText();
            this.mainInfo.setText( info + ", " + minBid);
        }

        void cancelTimer() {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            countDownTimer = null;
//            this.timer.setHidden(true, true); // setHidden does not work well
//            this.timer.setText("");
//            FontImage.setMaterialIcon(timer, '0');  // hide it
            this.timer.setVisible(false);
            if (this.location.equals("bottom")) {
                dismissActions();
            }
        }

        synchronized void showPoints(String point) {
            cancelTimer();
            this.points.setText(point);
            if (point.equalsIgnoreCase("pass")) {
                this.points.getStyle().setFgColor(GREY_COLOR);
            } else {
                this.points.getStyle().setFgColor(main.currentColor.pointColor);
            }
            parent.revalidate();
//            this.points.repaint();
        }

        int maxBid = -1;
        boolean needChangeActions=false;
        synchronized void showTimer(int timeout, int contractPoint, String act) {
            cancelTimer();  // cancel the running timer if any
            if (act.equals("dim")) {
                this.setContractor(CONTRACTOR);
                needChangeActions = true;
            }

            this.points.setText("");
            this.timer.setText(timeout + "");
            FontImage.setMaterialIcon(timer, FontImage.MATERIAL_TIMER);
            this.timer.setVisible(true);
            countDownTimer = new UITimer(new CountDown(this, timeout));
            countDownTimer.schedule(950, true, main.formTable);   // slightly less to 1 sec

            if (this.location.equals("bottom")) {
                userHelp.clear();
                if (robotOn || watching) {
                    parent.revalidate();
                    return;
                }
//                if (Display.getInstance().isBuiltinSoundAvailable(Display.SOUND_TYPE_ALARM)) {
//                    Display.getInstance().playBuiltinSound(Display.SOUND_TYPE_ALARM);
//                }

                if (act.equals("dim")) {
                    userHelp.showHelp(userHelp.SET_TRUMP);
                    Container buttons = new Container(new BoxLayout(BoxLayout.X_AXIS_NO_GROW));
                    for (char c : candidateTrumps) {
                        Button btn = new Button();
                        if (c == Card.JOKER) {
                            btn.setText(Dict.get(main.lang, "NT"));
                        } else {
                            btn.setText(Card.suiteSign(c));
                        }
                        btn.getAllStyles().setFont(Hand.fontRank);
                        if (c == Card.HEART || c == Card.DIAMOND) {
                            btn.getAllStyles().setFgColor(RED_COLOR);
                        } else {
                            btn.getAllStyles().setFgColor(BLACK_COLOR);
                        }
                        buttons.add(btn);
                        btn.addActionListener((e) -> {
                            if (!trumpRecommend.isEmpty() && c != trumpRecommend.charAt(0)) {
                                Dialog dlg = new Dialog(Dict.get(main.lang, Dict.SUGGEST));
                                dlg.setBackCommand("", null, (ev) -> {
                                    dlg.dispose();
                                });
                                dlg.add(BoxLayout.encloseX(
                                        new Label(Dict.get(main.lang, Dict.CHANGE_TO)),
                                        dimButton(trumpRecommend.charAt(0), dlg))
                                );
                                dlg.add(BoxLayout.encloseX(
                                        new Label(Dict.get(main.lang, Dict.NO_CHANGE)),
                                        dimButton(c, dlg))
                                );
                                Button bCancel = new Button(Dict.get(main.lang, "Cancel"));
                                bCancel.addActionListener((ev) -> {
                                    dlg.dispose();
                                });
                                dlg.add(bCancel);
                                dlg.setDialogPosition(BorderLayout.NORTH);
                                dlg.showModeless();
                            } else {
                                cancelTimer();
                                mySocket.addRequest(Request.create(Request.TRUMP, "trump", c));
                            }
                        });
                    }

                    buttonContainer.removeAll();
                    buttonContainer.add(BorderLayout.CENTER, buttons);
                    actionButtons = buttons;
                } else if (act.equals("bid")) {
                    if (candidateTrumps.isEmpty()) {
                        btnPlay.setName("pass");
                        btnPlay.setText(Dict.get(main.lang, "Pass"));
                        btnPlay.setEnabled(true);
                        if (actionButtons != btnPlay) {
                            buttonContainer.removeAll();
                            buttonContainer.add(BorderLayout.CENTER, btnPlay);
                            actionButtons = btnPlay;
                            buttonContainer.revalidate();
                        }
                    } else {
                        bidButtons.setEnabled(true);
                        if (actionButtons != bidButtons) {
                            buttonContainer.removeAll();
                            buttonContainer.add(BorderLayout.CENTER, bidButtons);
                            actionButtons = bidButtons;
                            buttonContainer.revalidate();
                        }
                        this.maxBid = contractPoint - 5;
                        btnBid.setText("" + this.maxBid);
                    }
                } else if (act.equals("partner")) {
                    userHelp.showHelp(userHelp.SET_PARTNER);

                    Container buttons = new Container(new BoxLayout(BoxLayout.X_AXIS_NO_GROW));

//                    ButtonGroup btnGroup = new ButtonGroup();
//                    RadioButton rb1 = RadioButton.createToggle(Dict.get(main.lang, "1st"), btnGroup);
//                    RadioButton rb2 = RadioButton.createToggle(Dict.get(main.lang, "2nd"), btnGroup);
//                    RadioButton rb3 = RadioButton.createToggle(Dict.get(main.lang, "3rd"), btnGroup);
//                    RadioButton rb4 = RadioButton.createToggle(Dict.get(main.lang, "4th"), btnGroup);
                    RadioButton rb1 = new RadioButton(Dict.get(main.lang, "1st"));
                    RadioButton rb2 = new RadioButton(Dict.get(main.lang, "2nd"));
                    RadioButton rb3 = new RadioButton(Dict.get(main.lang, "3rd"));
                    RadioButton rb4 = new RadioButton(Dict.get(main.lang, "4th"));
                    rb1.getAllStyles().setFont(Hand.fontGeneral);
                    rb2.getAllStyles().setFont(Hand.fontGeneral);
                    rb3.getAllStyles().setFont(Hand.fontGeneral);
                    rb4.getAllStyles().setFont(Hand.fontGeneral);
                    ButtonGroup btnGroup = new ButtonGroup(rb1, rb2, rb3, rb4);
                    buttons.addAll(rb1, rb2, rb3, rb4);
                    String rnk = Card.rankToString(playerRank);
                    rnk = rnk.equals("A") ? "K" : "A";
                    addCardButton(buttons, Card.SPADE, rnk, btnGroup);
                    addCardButton(buttons, Card.HEART, rnk, btnGroup);
                    addCardButton(buttons, Card.DIAMOND, rnk, btnGroup);
                    addCardButton(buttons, Card.CLUB, rnk, btnGroup);
                    Button btn = new Button(Dict.get(main.lang, "1vs5"));
                    btn.getAllStyles().setFont(Hand.fontGeneral);
                    btn.setCapsText(false);
                    btn.addActionListener((e)->{
                        cancelTimer();
//                        mySocket.addRequest(actionPartner, "\"def\":\"0\"");
                        mySocket.addRequest(Request.create(Request.PARTNER, "def", "0"));
                    });
                    buttons.add(btn);

                    buttonContainer.removeAll();
                    buttonContainer.add(BorderLayout.CENTER, buttons);
                    buttonContainer.revalidate();
                    actionButtons = buttons;
                } else if (act.equals("bury")) {
                    userHelp.showHelp(userHelp.BURY_CARDS);
                    btnPlay.setName("bury");
                    btnPlay.setText(Dict.get(main.lang, "Bury"));
                    btnPlay.setEnabled(true);
                    if (actionButtons != btnPlay) {
                        buttonContainer.removeAll();
                        buttonContainer.add(BorderLayout.CENTER, btnPlay);
                        actionButtons = btnPlay;
                        buttonContainer.revalidate();
                    }
                } else if (act.equals("play")) {
                    btnPlay.setName("play");
                    btnPlay.setText(Dict.get(main.lang, Dict.PLAY));
                    btnPlay.setEnabled(true);
                    if (actionButtons != btnPlay) {
                        buttonContainer.removeAll();
                        buttonContainer.add(BorderLayout.CENTER, btnPlay);
                        actionButtons = btnPlay;
                        buttonContainer.revalidate();
                    }
                } else {
                    // just wait
                    parent.revalidate();
                    return;
                }

                actionButtons.setVisible(true);
//                buttonContainer.setShouldCalcPreferredSize(true); // not work
//                central.repaint();    // no difference
                buttonContainer.revalidate();
            }

            parent.revalidate();
        }

        private Button dimButton(char c, Dialog dlg) {
            Button btn = new Button();
            if (c == Card.JOKER) {
                btn.setText(Dict.get(main.lang, "NT"));
            } else {
                btn.setText(Card.suiteSign(c));
            }
            btn.getAllStyles().setFont(Hand.fontRank);
            if (c == Card.HEART || c == Card.DIAMOND) {
                btn.getAllStyles().setFgColor(RED_COLOR);
            } else {
                btn.getAllStyles().setFgColor(BLACK_COLOR);
            }
            btn.addActionListener((ev) -> {
                dlg.dispose();
                cancelTimer();
                mySocket.addRequest(Request.create(Request.TRUMP, "trump", c));
            });

            return btn;
        }

        private Button defButton(String def, Dialog dlg) {
            String txt = partnerDef(def);
            Button btn = new Button(txt);
            char c = def.charAt(0);
            btn.getAllStyles().setFont(Hand.fontRank);
            if (c == Card.HEART || c == Card.DIAMOND) {
                btn.getAllStyles().setFgColor(RED_COLOR);
            } else {
                btn.getAllStyles().setFgColor(BLACK_COLOR);
            }
            btn.addActionListener((ev) -> {
                dlg.dispose();
                cancelTimer();
                mySocket.addRequest(Request.create(Request.PARTNER, "def", def));
            });
            return btn;
        }

        private void addCardButton(Container buttons, char suite, String rnk, ButtonGroup btnGroup) {
            if(suite != currentTrump) {
                Button btn = new Button(Card.suiteSign(suite) + rnk, "suite" + suite);
                btn.getAllStyles().setFont(Hand.fontRank);
                if (suite == Card.HEART || suite == Card.DIAMOND) {
                    btn.getAllStyles().setFgColor(RED_COLOR);
                } else {
                    btn.getAllStyles().setFgColor(BLACK_COLOR);
                }
                buttons.add(new Label("   "));
                buttons.add(btn);
                btn.addActionListener((e)->{
                    if(!btnGroup.isSelected()) {
                        userHelp.showHelp(userHelp.PARTNER_DEF);
                    } else {
                        String def = suite + rnk + btnGroup.getSelectedIndex();
                        if (!defRecommend.isEmpty() && !def.equalsIgnoreCase(defRecommend)) {
                            Dialog dlg = new Dialog(Dict.get(main.lang, Dict.SUGGEST));
                            dlg.setBackCommand("", null, (ev) -> {
                                dlg.dispose();
                            });
                            dlg.add(BoxLayout.encloseX(
                                    new Label(Dict.get(main.lang, Dict.CHANGE_TO)),
                                    defButton(defRecommend, dlg))
                            );
                            dlg.add(BoxLayout.encloseX(
                                    new Label(Dict.get(main.lang, Dict.NO_CHANGE)),
                                    defButton(def, dlg))
                            );
                            Button bCancel = new Button(Dict.get(main.lang, "Cancel"));
                            bCancel.addActionListener((ev) -> {
                                dlg.dispose();
                            });
                            dlg.add(bCancel);
                            dlg.setDialogPosition(BorderLayout.NORTH);
                            dlg.showModeless();
                        } else {
                            cancelTimer();
                            mySocket.addRequest(Request.create(Request.PARTNER, "def", def));
                        }
                    }
                });
            }
        }

        void setContractor(String txt) {
            // txt could be Contractor or Partner
            this.points.setText("");
            this.contractor.getAllStyles().setFgColor(RED_COLOR);
            this.contractor.setText(txt);
            this.isContractSide = true;
            parent.revalidate();
        }
    }

    class UserHelp extends Container {
        Label engLabel = new Label();
        Label chnLabel = new Label();

        final int SET_TRUMP = 10;
        final int BURY_CARDS = 20;
        final int SET_PARTNER = 25;
        final int PLAY_PAIR = 31;
        final int PLAY_TRIPS = 32;
        final int PLAY_TRACTOR = 33;
        final int PLAY_SAME_SUIT = 35;
        final int NO_CARD_SELECTED = 30;
        final int INVALID_PLAY = 99;
        final int PARTNER_DEF = 41;

        String curLang;
        UserHelp(String lang) {
            chnLabel.getAllStyles().setFgColor(main.currentColor.generalColor);
            engLabel.getAllStyles().setFgColor(main.currentColor.generalColor);
            chnLabel.getAllStyles().setAlignment(Component.CENTER);
            engLabel.getAllStyles().setAlignment(Component.CENTER);
            this.setLayout(new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST));
            if (lang.equalsIgnoreCase("zh")) {
                this.add(chnLabel);
            } else {
                this.add(engLabel);
            }
            curLang = lang;
        }

        void setLanguage(String lang) {
            chnLabel.getAllStyles().setFgColor(main.currentColor.generalColor);
            engLabel.getAllStyles().setFgColor(main.currentColor.generalColor);
            if (curLang.equalsIgnoreCase(lang)) {
                return;
            }
            this.removeAll();
            if (lang.equalsIgnoreCase("zh")) {
                this.add(chnLabel);
            } else {
                this.add(engLabel);
            }
            curLang = lang;
        }

        void clear() {
            engLabel.setText("");
            chnLabel.setText("");
        }

        void showInfo(String info) {
            if (curLang.equalsIgnoreCase("zh")) {
                chnLabel.setText(info);
            } else {
                engLabel.setText(info);
            }
            this.revalidate();
        }

        void showHelp(int category) {
            switch (category) {
                case PARTNER_DEF:
                    engLabel.setText("Please specify the ordinal number");
                    chnLabel.setText("请指定第几个");
                    break;
                case SET_TRUMP:
                    engLabel.setText("Set Trump");
                    chnLabel.setText("请选将牌");
                    break;
                case BURY_CARDS:
                    engLabel.setText("Please select exactly six cards");
                    chnLabel.setText("请选择6张底牌");
                    break;
                case SET_PARTNER:
                    engLabel.setText("Who plays this card will be your partner");
                    chnLabel.setText("找朋友(需指定第几张，包括自己)");
                    break;
                case NO_CARD_SELECTED:
                    engLabel.setText("Please select card(s) to play");
                    chnLabel.setText("请先选定要出的牌");
                    break;
                case INVALID_PLAY:
                    engLabel.setText("Invalid play");
                    chnLabel.setText("非法出牌");
                    break;
                case PLAY_SAME_SUIT:
                    engLabel.setText("Must play same suite");
                    chnLabel.setText("必须出相同花色");
                    break;
                case PLAY_PAIR:
                    engLabel.setText("Must play pair");
                    chnLabel.setText("必须出对");
                    break;
                case PLAY_TRIPS:
                    engLabel.setText("Must play trips");
                    chnLabel.setText("必须出三张");
                    break;
                case PLAY_TRACTOR:
                    engLabel.setText("Must play connected pairs");
                    chnLabel.setText("必须出拖拉机");
                    break;
            }
            this.revalidate();
        }
    }

    class ButtonImage extends DynamicImage {
        int bgColor = 0x00ffff;

        ButtonImage(int bgColor) {
            this.bgColor = bgColor;
        }

        @Override
        protected void drawImageImpl(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
//             if(TuoLaJiPro.DEBUG)Log.p("x,y,w,h: " + x + "," + y + "," + w + "," + h);
            g.setColor(this.bgColor);
            g.fillRoundRect(x, y, w, h, 60, 60);
        }
    }
}
