/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.if3games.chessonline.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.games.Games;
import com.if3games.chessonline.utils.EloRatingSystem;

import android.content.SharedPreferences;

/**
 * Represents the player's progress in the game. The player's progress is how many stars
 * they got on each level.
 *
 * @author Bruno Oliveira
 */
public class SaveGame {
    // serialization format version
    private static final String SERIAL_VERSION = "9.0";
    private String username;

    // Maps a level name (like "2-8") to the number of stars the user has in that level.
    // Any key that doesn't exist in this map is considered to be associated to the value 0.
    Map<String,Integer> mStats = new HashMap<String,Integer>(ConstantsData.initMap);

    /** Constructs an empty SaveGame object. No stars on no levels. */
    public SaveGame() {
    }

    /** Constructs a SaveGame object from serialized data. */
    public SaveGame(byte[] data) {
        if (data == null) return; // default progress
        loadFromJson(new String(data));
    }

    /** Constructs a SaveGame object from a JSON string. */
    public SaveGame(String json) {
        if (json == null) return; // default progress
        loadFromJson(json);
    }

    /** Constructs a SaveGame object by reading from a SharedPreferences. */
    public SaveGame(SharedPreferences sp, String key) {
        loadFromJson(sp.getString(key, ""));
    }
    
    /** Replaces this SaveGame's content with the content loaded from the given JSON string. */
    public void loadFromJson(String json) {
        //zero();
        if (json == null || json.trim().equals("")) return;

        try {
            JSONObject obj = new JSONObject(json);
            String format = obj.getString("version");
            if (!format.equals(SERIAL_VERSION)) {
                throw new RuntimeException("Unexpected loot format " + format);
            }
            JSONObject stats = obj.getJSONObject("stats");
            Iterator<?> iter = stats.keys();

            while (iter.hasNext()) {
                String statName = (String)iter.next();
                mStats.put(statName, stats.getInt(statName));
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Save data has a syntax error: " + json, ex);
        }
        catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Save data has an invalid number in it: " + json, ex);
        }
    }

    /** Serializes this SaveGame to an array of bytes. */
    public byte[] toBytes() {
        return toString().getBytes();
    }

    /** Serializes this SaveGame to a JSON string. */
    @Override
    public String toString() {
        try {
            JSONObject stats = new JSONObject();
            for (String statName : mStats.keySet()) {
            	stats.put(statName, mStats.get(statName));
            }

            JSONObject obj = new JSONObject();
            obj.put("version", SERIAL_VERSION);
            obj.put("username", username);
            obj.put("stats", stats);
            return obj.toString();
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error converting save data to JSON.", ex);
        }
    }

    /**
     * Computes the union of this SaveGame with the given SaveGame. The union will have any
     * levels present in either operand. If the same level is present in both operands,
     * then the number of stars will be the greatest of the two.
     *
     * @param other The other operand with which to compute the union.
     * @return The result of the union.
     */
    public SaveGame unionWith(SaveGame other) {
        SaveGame result = clone();
        int tmpNewRating = 0;
        boolean isChangeStats = false;
        for (String statName : other.mStats.keySet()) {
            int existingStats = result.getStatsFromName(statName);
            int newStats = other.getStatsFromName(statName);

            if(statName.equals(ConstantsData.CH_KEY_RATIND)) {
            	tmpNewRating = newStats;
            	continue;
            }
            // only overwrite if number of stars is greater
            if (newStats > existingStats) {
                result.setStatsFromName(statName, newStats);
                isChangeStats = true;
            }
            // note that this code doesn't preserve mappings from a level to the value 0,
            // but that is not a problem because, in our semantics, the absence of a mapping
            // is equivalent to mapping to 0 stars.
        }
        if(isChangeStats)
        	result.setStatsFromName(ConstantsData.CH_KEY_RATIND, tmpNewRating);
        return result;
    }

    /** Returns a clone of this SaveGame object. */
    public SaveGame clone() {
        SaveGame result = new SaveGame();
        for (String statName : mStats.keySet()) {
            result.setStatsFromName(statName, getStatsFromName(statName));
        }
        return result;
    }

    /** Resets this SaveGame object to be empty. Empty means no stars on no levels. */
    public void zero() {
        mStats.clear();
    }

    /** Returns whether or not this SaveGame is empty. Empty means no stars on no levels. */
    public boolean isZero() {
        return mStats.keySet().size() == 0;
    }

    /** Save this SaveGame object to a SharedPreferences. */
    public void save(SharedPreferences sp, String key) {
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(key, toString());
        spe.commit();
    }

    public int getStatsFromName(String statName) {
        Integer r = mStats.get(statName);
        return r == null ? 0 : r.intValue();
    }

    public void setStatsFromName(String statName, int stars) {
        mStats.put(statName, stars);
    }
    
	/**
	 * Set username from current getApiClient (Games.Players.getCurrentPlayer(getApiClient()).getDisplayName()).
	 * 
	 * @param username GMS player name
	 *            
	 */
    public void setUserName(String username) {
        this.username = username;
    }
    
	/**
	 * Set stats info from result of match.
	 * 
	 * @param result
	 *            1=WON, 0=DRAW, -1=LOSS
	 * @param opponentRating
	 *            Rating of either the opponent player or the average of the
	 *            opponent team or teams.
	 * @param gameMode
	 * 				This param for K Factor: gameMode=1 - Blitz mode
	 * 										 gameMode=0 - Standart game mode    
	 */
    public void setStatsFromResult(int result, int opponentRating, int gameMode) {
    	boolean isNewbie = mStats.get(ConstantsData.CH_KEY_PLAYED) > 30 ? false : true;
    	EloRatingSystem rs = EloRatingSystem.getInstance("ChessOnline");
    	
		mStats.put(ConstantsData.CH_KEY_PLAYED, mStats.get(ConstantsData.CH_KEY_PLAYED) + 1);
    	switch (result) {
		case ConstantsData.GAME_WON: // won
			int rating = rs.getNewRating(mStats.get(ConstantsData.CH_KEY_RATIND), opponentRating, ConstantsData.GAME_WON, gameMode, isNewbie);
			mStats.put(ConstantsData.CH_KEY_RATIND, rating);
			mStats.put(ConstantsData.CH_KEY_WON, mStats.get(ConstantsData.CH_KEY_WON) + 1);
			break;
		case ConstantsData.GAME_DRAW: // draw
			int rating1 = rs.getNewRating(mStats.get(ConstantsData.CH_KEY_RATIND), opponentRating, ConstantsData.GAME_DRAW, gameMode, isNewbie);
			mStats.put(ConstantsData.CH_KEY_RATIND, rating1);
			mStats.put(ConstantsData.CH_KEY_DRAW, mStats.get(ConstantsData.CH_KEY_DRAW) + 1);
			break;
		case ConstantsData.GAME_LOSS: // lost
			int rating2 = rs.getNewRating(mStats.get(ConstantsData.CH_KEY_RATIND), opponentRating, ConstantsData.GAME_LOSS, gameMode, isNewbie);
			mStats.put(ConstantsData.CH_KEY_RATIND, rating2);
			mStats.put(ConstantsData.CH_KEY_LOST, mStats.get(ConstantsData.CH_KEY_LOST) + 1);
			break;
		default:
			break;
		}
    }
}
