/*
 * JOGRE (Java Online Gaming Real-time Engine) - API
 * Copyright (C) 2004  Bob Marks (marksie531@yahoo.com)
 * http://jogre.sourceforge.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.if3games.chessonline.utils;

import java.util.HashMap;
import java.util.StringTokenizer;

import com.if3games.chessonline.data.ConstantsData;

/**
 * JOGRE's implementation of the ELO rating system.  The following is an
 * example of how to use the Elo Rating System.
 * <code>
 * 		EloRatingSystem elo = new EloRatingSystem();
 * 		int userRating = 1600;
 * 		int opponentRating = 1650; 
 * 		int newUserRating = elo.getNewRating(userRating, opponentRating, WIN);
 * 		int newOpponentRating = elo.getNewRating(opponentRating, userRating, LOSS);
 * </code>
 * 
 * @author Garrett Lehman (gman)
 */
public class EloRatingSystem {
	
	public final static int SUPPORTED_PLAYERS = 2;
	
	// Score constants
	public final static double WIN = 1.0;
	public final static double DRAW = 0.5;
	public final static double LOSS = 0.0;

	// Attributes
	private String game;
		
	// List of singletons are stored in this HashMap
	private static HashMap ratingSystems = null;
		
	/**
	 * Constructor to the JOGRE ELO rating system.
	 * 
	 * @param game   Game to do the rating on as games may vary
	 *               in their implementation of ELO.
	 */
	private EloRatingSystem (String game) {
		this.game = game;			// Set game.
	}
	
	/**
	 * Return instance of an ELO rating system.
	 * 
	 * @param game   Game to key of.
	 * @return       ELO rating system for specified game.
	 */
	public static synchronized EloRatingSystem getInstance (String game) {
		if (ratingSystems == null)
			ratingSystems = new HashMap ();
		
		// Retrieve rating system
		Object ratingSystem = ratingSystems.get (game);
		
		// If null then create new one and add to hash keying off the game
		if (ratingSystem == null) {
			ratingSystem = new EloRatingSystem (game);
			ratingSystems.put (game, ratingSystem);
			
			return (EloRatingSystem)ratingSystem;
		}
		else
			return (EloRatingSystem)ratingSystem;
	}
	
	/**
	 * Convience overloaded version of getNewRating (int, int, double)
	 * which takes a result type and 
	 * 
	 * @param rating
	 * @param opponentRating
	 * @param resultType
	 * @param gameMode
	 * 				This param for K Factor: gameMode=1 - Blitz mode
	 * 										 gameMode=0 - Standart game mode
	 * @param isNewbie
	 * 				for a player new to the rating list until he has completed events with a total of at least 30 games.
	 * 				true if <=30, false if > 30
	 * @return
	 */
	public int getNewRating (int rating, int opponentRating, int resultType, int gameMode, boolean isNewbie) {
		switch (resultType) {
			case ConstantsData.GAME_WON:
				return getNewRating (rating, opponentRating, WIN, gameMode, isNewbie);
			case ConstantsData.GAME_LOSS:
				return getNewRating (rating, opponentRating, LOSS, gameMode, isNewbie);
			case ConstantsData.GAME_DRAW:
				return getNewRating (rating, opponentRating, DRAW, gameMode, isNewbie);				
		}
		return -1;		// no score this time.
	}
	
	/**
	 * Get new rating.
	 * 
	 * @param rating
	 *            Rating of either the current player or the average of the
	 *            current team.
	 * @param opponentRating
	 *            Rating of either the opponent player or the average of the
	 *            opponent team or teams.
	 * @param score
	 *            Score: 0=Loss 0.5=Draw 1.0=Win
	 * @return the new rating
	 */
	public int getNewRating(int rating, int opponentRating, double score, int gameMode, boolean isNewbie) {
		double kFactor       = getKFactor(rating, gameMode, isNewbie);
		double expectedScore = getExpectedScore(rating, opponentRating);
		int    newRating     = calculateNewRating(rating, score, expectedScore, kFactor);
		
		return newRating;
	}	
	
	/**
	 * Calculate the new rating based on the ELO standard formula.
	 * newRating = oldRating + constant * (score - expectedScore)
	 * 
	 * @param oldRating 	Old Rating
	 * @param score			Score
	 * @param expectedScore	Expected Score
	 * @param constant		Constant
	 * @return				the new rating of the player
	 */
	private int calculateNewRating(int oldRating, double score, double expectedScore, double kFactor) {
		return oldRating + (int) (kFactor * (score - expectedScore));
	}
	
	/**
	 * This is the standard chess constant.  This constant can differ
	 * based on different games.  The higher the constant the faster
	 * the rating will grow.  That is why for this standard chess method,
	 * the constant is higher for weaker players and lower for stronger
	 * players.
	 *  
	 * @param rating		Rating
	 * @return				Constant
	 */
	private double getKFactor (int rating, int gameMode, boolean isNewbie) {
		
		if (isNewbie) {
			return ConstantsData.ELO_K_FACTOR_NEWBIE; 
		} else if(gameMode == 1) {
			return ConstantsData.ELO_K_FACTOR_BLITZ;
		} else if(rating < 2400) {
			return ConstantsData.ELO_K_FACTOR_DOWN2400;
		} else if(rating >= 2400) {
			return ConstantsData.ELO_K_FACTOR_UP2400;
		}	
		return ConstantsData.DEFAULT_ELO_K_FACTOR;
	}
	
	/**
	 * Get expected score based on two players.  If more than two players
	 * are competing, then opponentRating will be the average of all other
	 * opponent's ratings.  If there is two teams against each other, rating
	 * and opponentRating will be the average of those players.
	 * 
	 * @param rating			Rating
	 * @param opponentRating	Opponent(s) rating
	 * @return					the expected score
	 */
	private double getExpectedScore (int rating, int opponentRating) {
		return 1.0 / (1.0 + Math.pow(10.0, ((double) (opponentRating - rating) / 400.0)));
	}
}
