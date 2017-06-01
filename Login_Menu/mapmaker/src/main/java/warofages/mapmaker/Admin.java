package warofages.mapmaker;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import coms309.mike.clientcomm.ClientComm;
import coms309.mike.clientcomm.VolleyCallback;
/**
 * Created by Bakes on 11/17/16.
 */

public class Admin extends AppCompatActivity{
    private int countLength=0;
    private boolean p1Capital = false;
    private boolean p2Capital = false;
    private String[] terrainMap;
    private int mapSize;
    private Context context;
    public Admin(Context context, int rootOfMapSize){
        this.context=context;
        initMap(rootOfMapSize);
    }
    public void initMap(int rootOfMapSize){
        String s = "Map of size "+rootOfMapSize+" created.";
        if(rootOfMapSize < 2) {
            rootOfMapSize = 2;
            s = "Too small. Map size increased to 2.";
        }
        else if(rootOfMapSize > 99){
            rootOfMapSize = 99;
            s = "Too large. Map size reduced to 99.";
        }
        mapSize = rootOfMapSize * rootOfMapSize;
        terrainMap = new String[mapSize];
    }
    public int getMapSize(){
        return mapSize;
    }

    public void addTile(String terrain, int mapID){
        //length of array is constant, but I only want to sell a full array
        if(terrainMap[mapID] == null)
            countLength++;
        //make sure a start location is correctly flagged if another tile is placed over it
        else if(terrainMap[mapID].equals("town_friendly_start"))
            p1Capital = false;
        else if(terrainMap[mapID].equals("town_hostile_start"))
            p2Capital = false;
        // maps must have starting locations for both players.
        // I check here so I won't have to check in linear time on sendMap
        if(terrain.equals("town_friendly_start"))
            p1Capital = true;
        else if(terrain.equals("town_hostile_start"))
            p2Capital = true;
        terrainMap[mapID] = terrain;
    }

    public void sendMap(){
        if(countLength < mapSize){
            Toast.makeText(context, "Map is not complete.", Toast.LENGTH_SHORT).show();
            return;
        }
        else if(!(p1Capital && p2Capital)){
            Toast.makeText(context, "Maps must have starting locations for both players", Toast.LENGTH_SHORT).show();
            return;
        }
        ClientComm comm = new ClientComm(context);
        JSONArray map = new JSONArray();
        JSONObject terrainObj = new JSONObject();
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(terrainMap));
        try{
            terrainObj.put("TerrainID", arrayList);
        }
        catch (JSONException e){
            System.out.println(e.toString());  //printing e itself can lead to errors.
        }

        map.put(terrainObj);
        comm.serverPostRequest("makeMap.php", map, new VolleyCallback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray result) {
                //Toasts display a quick popup message at the bottom of the screen
                Toast toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
                try {
                    if (result.getJSONObject(0).getString("code").equals("update_success")) {
                        toast.setText("Map created. You may press back now.");
                    }
                    else{
                        toast.setText("There was a server error.");
                    }
                }
                catch(JSONException e){
                    toast.setText("There was a client error. Sorry");
                }
                toast.show();
            }
        });
    }
}