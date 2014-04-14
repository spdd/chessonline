package com.if3games.chessonline.data;

import java.util.HashMap;
import java.util.Map;

public class ConstantsData {
	// For Markets Rate, URL, About....
	public static String PACKAGE_NAME = "com.if3games.chessonline";
	
	public static enum MARKET { PLAY, AMAZON }	
	public static MARKET mMARKET = MARKET.PLAY;   // This set market type
	public static String MARKET_TYPE_URL_STR = mMARKET == MARKET.PLAY ? "market://details?id=" : "http://www.amazon.com/gp/mas/dl/android?p=";
	public static String MARKET_NAME_STR = mMARKET == MARKET.PLAY ? "Google Play" : "Amazon App Store"; 
	public static String MOREGAMES_URI = "https://play.google.com/store/apps/developer?id=tiny4games";
	public static String MARKET_URL_HTTP = "https://play.google.com/store/apps/details?id=" + PACKAGE_NAME;
	
	public final static String CH_KEY_RATIND = "rating";
	public final static String CH_KEY_PLAYED = "played";
	public final static String CH_KEY_WON = "won";
	public final static String CH_KEY_LOST = "lost";
	public final static String CH_KEY_DRAW = "draw";
	
	public final static int GAME_WON = 1;
	public final static int GAME_LOSS = -1;
	public final static int GAME_DRAW = 0;
	
	public final static int GAME_VARIANT_LONG = 10;
	public final static int GAME_VARIANT_1MIN = 11;
	public final static int GAME_VARIANT_2MIN = 12;
	public final static int GAME_VARIANT_3MIN = 13;
	
	public final static int GAME_DURATION_1MIN = 60;
	public final static int GAME_DURATION_2MIN = 120;
	public final static int GAME_DURATION_3MIN = 180;
	
    /** Default ELO starting rating for new users. */ 
    public static final int DEFAULT_ELO_START_RATING = 1200; 
    
    /** Default ELO, K is the development coefficient. (FIDE System) 
    K = 30 for a player new to the rating list until he has completed events with a total of at least 30 games. 
    K = 20 for RAPID and BLITZ ratings all players. 
    K = 15 as long as a player`s rating remains under 2400. 
    K = 10 once a player`s published rating has reached 2400, and he has also completed events with a total of at least 30 games. Thereafter it remains permanently at 10.
	*/
    public static final double ELO_K_FACTOR_NEWBIE = 	 30.0;
    public static final double ELO_K_FACTOR_BLITZ =  	 20.0;
    public static final double ELO_K_FACTOR_DOWN2400 =   15.0;
    public static final double ELO_K_FACTOR_UP2400 =  	 10.0;
    /** Default ELO k factor. */
    public static final double DEFAULT_ELO_K_FACTOR = 	 30.0;
    
    public final static int[][] ACHIEVEMENT_RANGE = { 
    	{0, 1200}, 
    	{1200, 1400},
    	{1400, 1600},
    	{1600, 1800},
    	{1800, 2000}, 
    	{2000, 2200}, 
    	{2200, 2400},
    	{2400, 2500}, 
    	{2500, 2800}, 
    	{2600, 8000}, 
    	{2700, 8000},
    	{2800, 8000} 
    	};
	
    public static final Map<String, Integer> initMap;
    static
    {
    	initMap = new HashMap<String, Integer>();
    	initMap.put(CH_KEY_RATIND, DEFAULT_ELO_START_RATING);
    	initMap.put(CH_KEY_PLAYED, 0);
    	initMap.put(CH_KEY_WON, 0);
    	initMap.put(CH_KEY_LOST, 0);
    	initMap.put(CH_KEY_DRAW, 0);
    }	
    
    public static final String SERIAL_VERSION = "9.0";

	public static int GMS_LEVELNUMBER = -1;
	
	public final static String COUNTDOWN_TIMER_TEXT_360 = "6:00";
	public final static String COUNTDOWN_TIMER_TEXT_340 = "5:";
	public final static String COUNTDOWN_TIMER_TEXT_310 = "5:0";
	public final static String COUNTDOWN_TIMER_TEXT_300 = "5:00";
	public final static String COUNTDOWN_TIMER_TEXT_280 = "4:";
	public final static String COUNTDOWN_TIMER_TEXT_250 = "4:0";
	public final static String COUNTDOWN_TIMER_TEXT_240 = "4:00";	
	public final static String COUNTDOWN_TIMER_TEXT_220 = "3:";
	public final static String COUNTDOWN_TIMER_TEXT_190 = "3:0";
	public final static String COUNTDOWN_TIMER_TEXT_180 = "3:00";
	public final static String COUNTDOWN_TIMER_TEXT_160 = "2:";
	public final static String COUNTDOWN_TIMER_TEXT_130 = "2:0";
	public final static String COUNTDOWN_TIMER_TEXT_120 = "2:00";
	public final static String COUNTDOWN_TIMER_TEXT_100 = "1:";
	public final static String COUNTDOWN_TIMER_TEXT_70 = "1:0";
	public final static String COUNTDOWN_TIMER_TEXT_60 = "0:";
	public final static String COUNTDOWN_TIMER_TEXT_9 = "0:0";
	
	// ads 
	// AdMob Interstitial
	public static String INTERSTITIAL_APPID = "a152e1fd934354e";
	
	// AdMob ADVIEW
	public static String ADVIEW_APPID = "a15301a48f1f9da";
	
	// ads 
	// Charboost
	public static String CHARBOOST_APPID = "52e1fbabf8975c225e939e3e";
	public static String CHARBOOST_APPSIGH = "c5e24175b3772f360f344ec322b4bcb08565f898";
	
	// No Lives
	public final static int COUNTDOWN_TIME_LIMIT = 11 * 1000;
	public final static String COUNTDOWN_TIMER_TEXT_10 = "00:";
	public final static String COUNTDOWN_TIMER_TEXT_1 = "00:0";
	
	// Animations
	public final static int AnimDurationTopBar = 250;
	public final static int AnimDurationButtons = 300;
	public final static int AnimDurationCongratsStatus = 400;
	public final static int AnimDurationCongratsGuessedTitle = 270;
	public final static int AnimDurationCongratsGuessedPuzzleNum = 380;
	public final static int AnimDurationCongratsRateAppBtn = 450;
	public final static int AnimDurationCongratsContinueBtn = 400;
	public final static int AnimDurationHelpButton = 450;
	public final static int AnimDurationLives = 400;
	public final static int AnimDurationSolution = 250;
	public final static int AnimDurationFadeOutSolution = 250;	
	
	public final static int AnimDurationWordCount = 550;
	public final static int AnimDurationLongWord = 700;
	
	// No Lives
	public final static int AnimDurationNoLivesStatus = 400;
	public final static int AnimDurationNoLivesTitle = 250;
	public final static int AnimDurationNoLivesWaitTitle = 360;
	public final static int AnimDurationNoLivesWaitTime = 470;
	public final static int AnimDurationNoLivesPuzzleNum = 600;
	public final static int AnimDurationNoLivesContinueBtn = 400;
	public final static float AnimTransDownBtn = 4.0f;	
	
	public final static int TURN_TIME_WORDBUTTON = 2 * 1000;
	
	public final static int TURN_GMS_TIME = 120 * 1000;
	
	public static Map<String, Integer> getInitStatsMap() {
		return initMap;
	}
	
	public static int getIndexFromCheckedRange(int rating) {
		int index = 1;
		for (int[] range : ACHIEVEMENT_RANGE) {
			if(range[0] == 0) continue; // for all newbie players unlock first achive
			if(rating > range[0] && rating <= range[1])
				return index;
			index++;
		}
		return 0;
	}
}
