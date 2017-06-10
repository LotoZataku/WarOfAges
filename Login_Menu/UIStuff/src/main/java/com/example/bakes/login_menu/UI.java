package com.example.bakes.login_menu;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import coms309.mike.clientcomm.ClientComm;
import coms309.mike.clientcomm.VolleyCallback;
import coms309.mike.units.Archer;
import coms309.mike.units.Cavalry;
import coms309.mike.units.General;
import coms309.mike.units.Spearman;
import coms309.mike.units.Swordsman;
import coms309.mike.units.Unit;
import warofages.gamebackend.ActivePlayer;
import warofages.gamebackend.DisplaysChanges;
import warofages.gamebackend.InactivePlayer;
import warofages.gamebackend.Player;
import warofages.gamebackend.UIbackend;

public class UI extends AppCompatActivity implements DisplaysChanges {
    boolean movedToOtherIntent = false;

    private UIbackend uiBackend;
    private final int MOVE_ICON_ID = 10000;

    //The number of squares in the map. Used so often that I save the value in here
    private int mapSize;
    //size of the map tiles image
    int tileSize = 100;
    //username
    String username = "";
    //current player
//    Player player;
//    //list of my units.     Using until active player has move checking implimmented
//    ArrayList<Unit> myArmy;
//    //list of enemy units
//    ArrayList<Unit> enemyArmy;
    //list of terrain locations
//    int terrainMap[];
    int cash;
    //if click on town with friendly unit and 0, open town menu.
    // if 1, currently using popup. if 2, moving unit.
    int unitVtown;
    //town menu
    PopupWindow townMenu;
    //popul showing whose turn it is
    PopupWindow endMenu;
    //text in endMenu
    TextView endText;
    //scrollview. class wide so inactive player can use it.
    ScrollView scroller;
    boolean gameOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        username = getIntent().getStringExtra("username");

        //initialize gameOn
        gameOn = true;

        //initialize scroller
        scroller = (ScrollView) findViewById(R.id.scroll);

        unitVtown = -1;

        makeTownMenu();
        makeEndMenu();

        uiBackend = new UIbackend(getApplicationContext(), username, this);
        uiBackend.getMapFromServer();
    }

    //shows whose turn it is
    private void makeEndMenu(){
        endMenu = new PopupWindow(this);
        LinearLayout popLayout = new LinearLayout(this);
        endText = new TextView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        endText.setText("Turn over");
        popLayout.addView(endText, layoutParams);
        endMenu.setContentView(popLayout);
        endMenu.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        endMenu.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        //I didn't like how ugly the black box is
        endMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    //initialize townMenu window
    private void makeTownMenu(){
        townMenu = new PopupWindow(this);
        LinearLayout popLayout = new LinearLayout(this);
        ImageView image;
        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(150, 150);
        ArrayList<Integer> icons = findTownMenuIDs();

        for(int i = 0; i < icons.size(); i++){
            image = new ImageView(this);
            image.setImageResource(icons.get(i));
            image.setImageResource(MOVE_ICON_ID + i);
            image.setOnClickListener(townMenuListener);
            popLayout.addView(image, layoutParams);
        }
        townMenu.setContentView(popLayout);
        //The menu didn't show up on mobile devices (worked on emulator though). fixed by http://stackoverflow.com/a/39363218
        townMenu.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        townMenu.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        townMenu.getBackground().setAlpha(102); //40% opaque
    }

    private ArrayList<Integer> findTownMenuIDs(){
        ArrayList<Integer> unitIDs = new ArrayList<>();
        unitIDs.add(R.drawable.move_icon);
        Field[] fields = R.drawable.class.getFields();
        for(Field field : fields){
            if(field.getName().endsWith("friendly")){
                try {
                    unitIDs.add(field.getInt(null));
                }
                catch(IllegalAccessException e){

                }
            }
        }
        return unitIDs;
    }

    private void finishSettingUp(){
        //get passed in username
        Intent intent = getIntent();

        //players start game with 1000 cash
//        player.setCash(1000);
        cash = 1000;

        //display cash
        setInfoBar("Cash: " + cash);

        //Only players call this. Spectators do not need to get players.
        if(!intent.hasExtra("spectator")){
            Log.d("game start", "calling getPlayers");
            getPlayers();
        }

        uiBackend.waitForTurn();
    }

    //Create table of any size(must be square)
    private void createTerrainButtons(){
        //Creates an initial row
        LinearLayout mapLayout = (LinearLayout) findViewById(R.id.gameLayout);

        //Creates an initial column
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        mapLayout.addView(column);

        //set images to all columns. create new columns as needed
        for(int id = 0, rowLength = 0; id < mapSize; id++, rowLength++){
            //if column is filled, begin new column and set parameters for it
            if(rowLength == Math.sqrt(mapSize)){
                //Creates a new row for both grids
                column = new LinearLayout(this);
                //we move every second column down a bit
                if((id / rowLength) % 2 == 1)
                    adjustColumnParams(column, rowLength, false);
                else
                    adjustColumnParams(column, rowLength, true);

                //Adds column to the matrix
                mapLayout.addView(column);
                rowLength = 0;
            }

            //creates image and adds it to terrain. HexagonMaskView has default size of 100x100
            HexagonMaskView image = new HexagonMaskView(this);
            image.setId(id);
            column.addView(image);

            //he had a separate method just for this, which is odd since it always adds the same image
            image.setOnClickListener(gameClickListener);
        }
    }

    /* Should not be called on first column
     * first column doesn't need special parameters besides being set to vertical.
     * evenIndex: true if index 0,2,4... false if its odd
     */
    private void adjustColumnParams(LinearLayout column, int rowLength, boolean evenIndex){
        //set orientation just in case it hasn't been done at creation
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params;
        if(evenIndex) {
            params = new LinearLayout.LayoutParams(tileSize, rowLength * tileSize);
            params.setMargins(-tileSize/4, 0,0,0);
        }
        else {
            params = new LinearLayout.LayoutParams(tileSize, rowLength * tileSize + tileSize / 2);
            params.setMargins(-tileSize/4, (int)(Math.sqrt(3.0)/2 * tileSize / 2), 0, -(int)(Math.sqrt(3.0)/2 * tileSize / 2));
        }
        column.setLayoutParams(params);
    }

    //gets players from server
    private void getPlayers(){
        ClientComm comm = new ClientComm(getApplicationContext());
        JSONArray nameArray = new JSONArray();
        JSONObject nameObject = new JSONObject();
        try {
            nameObject.put("userID", username);
        }
        catch(JSONException e){
            //TODO
        }
        nameArray.put(nameObject);
        comm.serverPostRequest("getPlayers.php", nameArray, new VolleyCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                //don't need to do anything here. But I check code for testing purposes
                Log.d("getPlayers result", result.toString());
            }
        });
    }

    //load given terrain at given id
    public void loadTerrainToButtons(){
        for(int id = 0; id < uiBackend.getMapSize(); id++) {
            int terrainTypeID = uiBackend.getTerrainAtLocation(id);
            //gets imageview object at given id
            String picName = "";
            switch (terrainTypeID) {
                case 1:
                    picName = "tile_desert";
                    break;
                case 2:
                    picName = "tile_forest";
                    break;
                case 3:
                    picName = "tile_meadow";
                    break;
                case 4:
                    picName = "tile_mountain";
                    break;
                case 5:
                    picName = "tile_town_friendly";
                    break;
                case 6:
                    picName = "tile_town_hostile";
                    break;
                case 7:
                    picName = "tile_town_neutral";
                    break;
                case 8:
                    picName = "tile_water";
                    break;
            }
            //gets and sets reference for picture ID. "p12" would indicate something went wrong
            int resID = getResources().getIdentifier(picName.equals("") ? "p12" : picName, "drawable", getPackageName());
            HexagonMaskView image = getImage(id);

            image.setImageResource(resID);
        }
    }

    HexagonMaskView getImage(int mapID){
        int mapRoot = (int)Math.sqrt(mapSize);
        int x = mapID / mapRoot;
        int y = mapID % mapRoot;
        LinearLayout layout = (LinearLayout)findViewById(R.id.gameLayout);
        layout = (LinearLayout)layout.getChildAt(x);
        return (HexagonMaskView)layout.getChildAt(y);
    }

    public void clearMap(){
        for(int x = 0; x < mapSize; x++){
            clearImage(x);
        }
    }
    private void clearImage(int mapID){
        if(getImage(mapID).getForeground() != null)
            getImage(mapID).setForeground(null);
    }

    public void updateUnits(boolean friendly){
        SparseArray<Unit> units;
        if(friendly) {
            units = uiBackend.getPlayer().getMyUnits();
        }
        else{
            units = uiBackend.getPlayer().getEnemyUnits();
        }
        for (int i = 0; i < units.size(); i++) {
            displaySingleUnit(units.valueAt(i), false);
        }
    }

    public void endTurn(){
        if(uiBackend.playerIsActive()){
            ClientComm comm = new ClientComm(getApplicationContext());
            comm.serverPostRequest("checkActivePlayer.php", new JSONArray(), new VolleyCallback<JSONArray>() {
                @Override
                public void onSuccess(JSONArray result) {
                    endTurnHelper();
                }
            });

        }
    }
    //because I can't access this UI's instance from the onSuccess inner Class
    private void endTurnHelper(){
        String end = uiBackend.checkIfGameOver();
        if(end.equals("Game in Progress")){
            setInfoBar("Cash: " + cash);
//            player = new InactivePlayer(player);
//            ((InactivePlayer)player).waitForTurn();
        }
        else{
            gameOn = false;
            setInfoBar(end);
        }
    }

    public void bottomBarListener(View v){
        boolean resized = false;
        switch (v.getId()){
            case R.id.endTurn:
                if(gameOn){
                    endTurn();
                    return;
                }
                else{
                    String end = uiBackend.checkIfGameOver();
                    setInfoBar(end);
                }
                break;
            case R.id.zoomIn:
                if(tileSize < 500) {
                    tileSize += 100;
                    resized = true;
                }
                break;
            case R.id.zoomOut:
                if(tileSize > 100) {
                    tileSize -= 100;
                    resized = true;
                }
                break;
        }
        if(resized){
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tileSize, tileSize);
            for (int id = 0; id < mapSize; id++) {
                HexagonMaskView image = getImage(id);
                image.setLayoutParams(params);
            }
            //loop through all columns after the first and adjust their parameters
            LinearLayout rootLayout = (LinearLayout) findViewById(R.id.mapMakerLayout);
            LinearLayout column;
            //number of columns = number of rows, since I only make square maps, so I can use count as rowLength
            int count = rootLayout.getChildCount();

            for (int i = 1; i < count; i++) {
                column = (LinearLayout)rootLayout.getChildAt(i);
                if(i % 2 == 1)
                    adjustColumnParams(column, count, false);
                else
                    adjustColumnParams(column, count, true);
            }
        }
    }

    //processes clicks when the town menu is open
    View.OnClickListener townMenuListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            uiBackend.helpWithTownMenuClicks();
            Unit movingUnit = uiBackend.getUnitFromMap(unitVtown, true);

            if (v.getId() == MOVE_ICON_ID) {//clicked on move button
                // TODO: I have this little bit in a few places. feels sloppy, but all of UI feels slopy
                Integer moves[] = getMoves(movingUnit);
                Integer attacks[] = getAttackRange(movingUnit);
                Integer largestArea[];
                if (moves.length > attacks.length && movingUnit != null && !movingUnit.checkIfMoved()) {
                    largestArea = moves;
                } else {
                    largestArea = attacks;
                }
                //highlight surrounding area
                for (int move : largestArea) {
                    int moveCheck = ((ActivePlayer)uiBackend.getPlayer()).spaceAvaliableMove(move);
                    if (moveCheck == 0) {
                        displaySingleUnit(uiBackend.getUnitFromMap(move, true), true);
                    } else if (moveCheck == 1) {
                        ImageView image = (ImageView) findViewById(move + mapSize);
                        image.setImageResource(R.drawable.selected_tile);
                    } else if (moveCheck == 2) {
                        displaySingleUnit(uiBackend.getUnitFromMap(move, false), true);
                    }
                }
                //display unit stats
                double[] stats = ((ActivePlayer)uiBackend.getPlayer()).getMyStats(unitVtown);
                setInfoBar("Health: " + (int) stats[0] + ", Attack: " + (int) stats[1] + ", Defense: " + stats[2]);
                //current location of unit in the terrain map
                ((ActivePlayer)uiBackend.getPlayer()).moving = unitVtown;
                //close menu
                unitVtown = -2;
                townMenu.dismiss();
            }
            else if (v.getId() > MOVE_ICON_ID && movingUnit != null && !movingUnit.checkIfMoved()) {//clicked on one of the add unit buttons
                //take all surrounding tiles and add unit to first empty one
                int unitIDtoAdd = v.getId() - MOVE_ICON_ID;
                //surrounding tiles
                Integer[] moves = ((ActivePlayer)uiBackend.getPlayer()).checkArea(unitVtown, 1, unitIDtoAdd, uiBackend.getMap(), false);
                for (int move : moves) {
                    if ((move > -1) && (move < mapSize) && ((ActivePlayer)uiBackend.getPlayer()).spaceAvaliableMove(move) == 1
                            && uiBackend.getTerrainAtLocation(move) != 6
                            && !(unitIDtoAdd == 5 && uiBackend.getTerrainAtLocation(move) == 4)
                            && !(unitIDtoAdd == 2 && uiBackend.getTerrainAtLocation(move) == 4)) {
                        //creates unit and sends it to server
                        createUnit(move, unitIDtoAdd);
                        // don't actually move the unit, but dont let it move anymore
                        movingUnit.moveUnit(movingUnit.getMapID());
                        //close popup
                        unitVtown = -1;
                        townMenu.dismiss();
                        break;
                    }
                    //if not empty space, close without adding unit
                    unitVtown = -1;
                    townMenu.dismiss();
                }
            } else {
                //if anything other than popup clicked on, close popup
                unitVtown = -1;
                townMenu.dismiss();
            }
        }
    };

    //processes clicks
    View.OnClickListener gameClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v){
            if(gameOn) {
                //increases size of map when clicked button is increase size button
                 if (uiBackend.playerIsActive()) {
                    //end turn button
                    if (R.id.endTurn == v.getId()) {
                        endTurn();
                        return;
                    }
                    int currentMapClicked = v.getId() - mapSize;
                    //Get the unit thats moving (only from myArmy), so I can get its movespeed and stuff
                    Unit movingUnit;
                     Unit enemyUnit = null;
                    if (((ActivePlayer)uiBackend.getPlayer()).moving == -1) {
                        movingUnit = uiBackend.getUnitFromMap(currentMapClicked, true);
                    } else {
                        movingUnit = uiBackend.getUnitFromMap(((ActivePlayer)uiBackend.getPlayer()).moving, true);
                    }
                    if (movingUnit != null && movingUnit.checkIfMoved() && movingUnit.checkIfAttacked()) {
                        movingUnit = null;
                    }
                    if (movingUnit == null) {
                        enemyUnit = uiBackend.getUnitFromMap(currentMapClicked, false);
                    }
                    //if click on enemy unit, display its stats
                    if (enemyUnit != null) {
                        //display unit stats
                        double[] stats = ((ActivePlayer)uiBackend.getPlayer()).getEnemyStats(currentMapClicked);
                        setInfoBar("Enemy Health: " + (int) stats[0] + ", Attack: " + (int) stats[1] + ", Defense: " + stats[2]);
                    }//if click on empty space, display cash
                    else if ((movingUnit == null) && (v.getId() < MOVE_ICON_ID)) {
                        //display cash
                        setInfoBar("Cash: " + cash);
                    }
                    //if friendly unit on a town and you click it, open town menu
                    else if (currentMapClicked >= 0 && currentMapClicked <= mapSize - 1 &&
                            uiBackend.getTerrainAtLocation(currentMapClicked) == 5 && (unitVtown == -1) &&
                            (movingUnit != null) && (((ActivePlayer)uiBackend.getPlayer()).moving == -1)) {

                        ScrollView scroll = (ScrollView) findViewById(R.id.scroll);
                        townMenu.showAtLocation(scroll, Gravity.TOP, 0, 500);
                        unitVtown = currentMapClicked;
                    }//town menu interactions
                    else if (unitVtown > -1) {

                    }
                    /*
                        The difference between the mapIDs and buttonIDs are mapSize (mapID1 = buttonID1 + mapSize)
                        so there will be a lot adding and subtracting mapSize below, as needed to perform the actions.
                    */
                    else if (unitVtown < 0) {
                        //must be careful not to get nullPointerExceptions if I click on an empty space.
                        if (movingUnit == null) {
                            unitVtown = -1;
                            return;
                        }
                        Integer moves[] = getMoves(movingUnit);
                        Integer attacks[] = getAttackRange(movingUnit);
                        Integer largestArea[];
                        //even if moves are larger than attacks, doesnt matter if unit already moved
                        if (moves.length > attacks.length && !movingUnit.checkIfMoved()) {
                            largestArea = moves;
                        } else {
                            largestArea = attacks;
                        }
                        //Click on a unit (initiate a move)
                        if (((ActivePlayer)uiBackend.getPlayer()).moving == -1) {
                            for (int move : largestArea) {
                                int moveCheck = ((ActivePlayer)uiBackend.getPlayer()).spaceAvaliableMove(move);
                                if (moveCheck == 0) {
                                    displaySingleUnit(uiBackend.getUnitFromMap(move, true), true);
                                } else if (moveCheck == 1) {
                                    ImageView image = (ImageView) findViewById(move + mapSize);
                                    image.setImageResource(R.drawable.selected_tile);
                                } else if (moveCheck == 2) {
                                    displaySingleUnit(uiBackend.getUnitFromMap(move, false), true);
                                }
                            }
                            //current location of unit in the terrain map
                            ((ActivePlayer)uiBackend.getPlayer()).moving = currentMapClicked;

                            //display unit stats
                            double[] stats = ((ActivePlayer)uiBackend.getPlayer()).getMyStats(currentMapClicked);
                            setInfoBar("Health: " + (int) stats[0] + ", Attack: " + (int) stats[1] + ", Defense: " + stats[2]);
                        }
                        //Click on a space after clicking on a unit (complete the move)
                        else if (((ActivePlayer)uiBackend.getPlayer()).moving != -1) {
                            int moving = ((ActivePlayer)uiBackend.getPlayer()).moving;
                            for (int move : largestArea) {
                                int moveCheck = ((ActivePlayer)uiBackend.getPlayer()).spaceAvaliableMove(move);
                                //Clears the images for spaces around where unit moves from
                                if (moveCheck == 1) {
                                    clearImage(move);
                                }
                                //clears the image for current space and moves unit to new space
                                if (move == currentMapClicked && moveCheck == 1 && !movingUnit.checkIfMoved()) {
                                    clearImage(moving);
                                    ((ActivePlayer)uiBackend.getPlayer()).sendMove(currentMapClicked, moving);
                                    displaySingleUnit(movingUnit, false);
                                    //display cash
                                    setInfoBar("Cash: " + cash);
                                } else if (moveCheck == 2) {
                                    //I need to un-highlight the unit
                                    displaySingleUnit(uiBackend.getUnitFromMap(move, false), false);
                                    //after its un-highlighted, do combat
                                    // (I un-highlight first in case the attack is out of range)
                                    if (move == currentMapClicked && !movingUnit.checkIfAttacked()) {
                                        UIAttack(movingUnit, moving, move);
                                    }
                                } else if (moveCheck == 0) {
                                    displaySingleUnit(uiBackend.getUnitFromMap(move, true), false);
                                }
                            }
                            unitVtown = -1;
                            ((ActivePlayer)uiBackend.getPlayer()).moving = -1;
                        }
                    }
                }
            }
            else{
                String end = uiBackend.checkIfGameOver();
                setInfoBar(end);
            }
        }
    };

    private Integer[] getMoves(Unit movingUnit){
        int moveSpeed = movingUnit.getMoveSpeed();
        int gridID = movingUnit.getMapID();
        return ((ActivePlayer)uiBackend.getPlayer()).checkArea(gridID, moveSpeed, movingUnit.getUnitID(), uiBackend.getMap(), false);
    }

    private Integer[] getAttackRange(Unit movingUnit){
        int attackRange = 1;
        if(movingUnit instanceof Archer){
            attackRange = 3;
        }
        return ((ActivePlayer)uiBackend.getPlayer()).checkArea(movingUnit.getMapID(), attackRange, movingUnit.getUnitID(), uiBackend.getMap(), true);
    }

    private void UIAttack(Unit movingUnit, int attackerGridID, int defenderGridID){
        Integer possibleAttacks[] = getAttackRange(movingUnit);
        //if enemy if outside of attack range, it will return without attempting an attack
        for(int index : possibleAttacks){
            if(defenderGridID == index){
                String attackResults = ((ActivePlayer)uiBackend.getPlayer()).attack(defenderGridID, attackerGridID, uiBackend.getMap());
                if (attackResults.equals("Fail")) {
                    clearImage(attackerGridID);
                } else if (attackResults.equals("Success")) {
                    clearImage(defenderGridID);
                }
                setInfoBar(attackResults);
                break;
            }
        }
    }

    private void displaySingleUnit(Unit unit, boolean selected){
        boolean friendly = false;
        if(unit.getOwner().equals(username)){
            friendly = true;
        }
        int unitDrawableID = -1;
        switch(unit.getUnitID()){
            case 1: //archer
                if(friendly && selected)
                    unitDrawableID = R.drawable.unit_archer_friendly_selected;
                else if(friendly)
                    unitDrawableID = R.drawable.unit_archer_friendly;
                else if(selected)
                    unitDrawableID = R.drawable.unit_archer_hostile_selected;
                else
                    unitDrawableID = R.drawable.unit_archer_hostile;
                break;
            case 2: //cavalry
                if(friendly && selected)
                    unitDrawableID = R.drawable.unit_cavalry_friendly_selected;
                else if(friendly)
                    unitDrawableID = R.drawable.unit_cavalry_friendly;
                else if(selected)
                    unitDrawableID = R.drawable.unit_cavalry_hostile_selected;
                else
                    unitDrawableID = R.drawable.unit_cavalry_hostile;
                break;
            case 3: //swordsman
                if(friendly && selected)
                    unitDrawableID = R.drawable.unit_sword_friendly_selected;
                else if(friendly)
                    unitDrawableID = R.drawable.unit_sword_friendly;
                else if(selected)
                    unitDrawableID = R.drawable.unit_sword_hostile_selected;
                else
                    unitDrawableID = R.drawable.unit_sword_hostile;
                break;
            case 4: //spearman
                if(friendly && selected)
                    unitDrawableID = R.drawable.unit_spear_friendly_selected;
                else if(friendly)
                    unitDrawableID = R.drawable.unit_spear_friendly;
                else if(selected)
                    unitDrawableID = R.drawable.unit_spear_hostile_selected;
                else
                    unitDrawableID = R.drawable.unit_spear_hostile;
                break;
            case 5: //general
                if(friendly && selected)
                    unitDrawableID = R.drawable.unit_general_friendly_selected;
                else if(friendly)
                    unitDrawableID = R.drawable.unit_general_friendly;
                else if(selected)
                    unitDrawableID = R.drawable.unit_general_hostile_selected;
                else
                    unitDrawableID = R.drawable.unit_general_hostile;
                break;
        }

        HexagonMaskView image = getImage(unit.getMapID());
        image.setForeground(getDrawable(unitDrawableID));
    }

    private void createUnit(int mapID, int unitID){
        ClientComm comm = new ClientComm(getApplicationContext());
        Unit newUnit;
        String message;
        int cost;
        switch(unitID){
            case 1:
                cost = 100;
                    newUnit = new Archer(mapID, unitID, username,300.0);
                    message = "Archer";
                break;
            case 2:
                cost = 250;
                    newUnit = new Cavalry(mapID, unitID, username,900.0);
                    message = "Cavalry";
                break;
            case 3:
                cost = 150;
                    newUnit = new Swordsman(mapID, unitID, username,600.0);
                    message = "Swordsman";
                break;
            case 4:
                cost = 200;
                    newUnit = new Spearman(mapID, unitID, username,450.0);
                    message = "spearman";
                break;
            case 5:
                cost = 100000;
                    newUnit = new General(mapID, unitID, username,2000.0);
                    message = "General";
                break;
            default:
                return;
        }
        if(cash <= cost) {
            cash -= cost;
            message = message + " has been recruited.";
            //Didn't actually move, but sets its moved boolean because new units cant move
            newUnit.moveUnit(mapID);
            newUnit.setHasAttacked(); //ensure the new unit doesn't attack
            uiBackend.getPlayer().getMyUnits().put(mapID, newUnit);
            //set unit image
            displaySingleUnit(newUnit, false);

            JSONArray requestArray = new JSONArray();
            JSONObject nameObject = new JSONObject();
            JSONObject gridObject = new JSONObject();
            JSONObject unitObject = new JSONObject();
            JSONObject unitHealth = new JSONObject();
            try {
                nameObject.put("userID", username);
                gridObject.put("GridID", mapID);
                unitObject.put("UnitID", unitID);
                unitHealth.put("health", newUnit.getHealth());
            } catch (JSONException e) {
                //TODO
            }
            requestArray.put(nameObject);
            requestArray.put(gridObject);
            requestArray.put(unitObject);
            requestArray.put(unitHealth);

            comm.serverPostRequest("createUnit.php", requestArray, new VolleyCallback<JSONArray>() {
                @Override
                public void onSuccess(JSONArray result) {
                    Log.d("createUnit", result.toString());
                    //TODO check results
                }
            });
        }
        else{
            message = "You do not have enough cash.";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed(){
        if(!uiBackend.playerIsActive()){
            ((InactivePlayer)uiBackend.getPlayer()).killPoll();
        }
        Intent intent = new Intent(getApplicationContext(), com.example.bakes.login_menu.Menu.class);
        intent.putExtra("username", username);
        intent.putExtra("message", "leftGame");
        startActivity(intent);
        finish();
    }

    @Override
    public void onDestroy(){
        Log.d("UI Destroy", "destroy called");
        if(isFinishing() && !movedToOtherIntent) {
            endMenu.dismiss();
            Intent forceLogout = new Intent(this, com.example.bakes.login_menu.LogoutBackgroundService.class);
            forceLogout.putExtra("username", username);
            startService(forceLogout);
        }
        else{
            Log.d("destroying", "was not finishing");
        }
        super.onDestroy();
    }
    private void setInfoBar(String text){
        TextView info = (TextView) findViewById(R.id.infoBar);
        info.setText(text);
    }
    private void beginTurnMakeMoney(){
//        player.setCash(oldCashAmount + 50);
//        player.cash = oldCashAmount + player.incrementCash(terrainMap);
//        player.setCash(oldCashAmount + player.incrementCash(terrainMap));
        //TODO crashed when incrementCash was called - haven't updated that stuff yet
//        cash += player.incrementCash(terrainMap);
        setInfoBar("Cash: " + cash);
    }

    @Override
    public void displayPollResult(JSONArray result){
        String activePlayerName;
        try {
            activePlayerName = result.getJSONObject(0).getString("userID");
        }
        catch(JSONException e){
            activePlayerName = "null";
        }
        //if server says active player is this player, begin turn
        if(activePlayerName.equals(uiBackend.getPlayer().getName())) {
            if (endMenu.isShowing()) {
                endMenu.dismiss();
            }
            String end = uiBackend.checkIfGameOver();
            if (!end.equals("Game in Progress")) {
                gameOn = false;
            }
            beginTurnMakeMoney();
        }
        //if active player is not this player, set text of ui popup to show whose turn it is
        else{
            if(activePlayerName.equals("null")){
                endText.setText("Need additional player to start game");
            }
            else if(((InactivePlayer)uiBackend.getPlayer()).isSpectator()){
                endText.setText(activePlayerName + " is currently playing");
            }
            else {
                endText.setText("It is " + activePlayerName + "'s turn.");
            }
            endMenu.showAtLocation(scroller, Gravity.BOTTOM, 0, 400);
        }
        clearMap();
        updateUnits(true);
        updateUnits(false);
    }

    @Override
    public void continueAfterTerrainLoaded(){
        mapSize = uiBackend.getMapSize();
        createTerrainButtons();
        loadTerrainToButtons();
        finishSettingUp();
    }
}