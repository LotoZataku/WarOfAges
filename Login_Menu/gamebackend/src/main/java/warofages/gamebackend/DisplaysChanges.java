package warofages.gamebackend;

import org.json.JSONArray;

/**
 * Created by mike on 6/5/2017.
 * Basically a callback to return the response to whoever needs it
 */

public interface DisplaysChanges {

    void displayPollResult(JSONArray result);

    void continueAfterTerrainLoaded();

    void displayTownMenu();
    void dismissTownMenu();

    /**
     * used for showing information to the player. Lasts until changed, so is much more permanent than a toast
     * @param text the test to be displayed
     */
    void setInfoBar(String text);

    void makeToast(String text);

    /**
     * displays a unit and/or selection highlight as a foreground over terrain
     * @param mapID position in map to be changed
     * @param unitID ID of unit to be displayed: 0 if no unit, 1 if archer, 2 if cavalry,
     *               3 if sword, 4 if spear, 5 if general
     * @param friendly indicate if a friendly or hostile unit is to be displayed.
     *                 if unitID = 0, this value is ignored
     * @param selected indicate if the foreground is to be highlighted.
     *                 If false it will remove the foreground from the terrain
     */
    void displayForeground(int mapID, int unitID, boolean friendly, boolean selected);

}
