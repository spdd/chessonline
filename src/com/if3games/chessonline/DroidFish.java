/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2013  Peter Г–sterlund, peterosterlund2@gmail.com
    Copyright (C) 2012 Leo Mayer

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.if3games.chessonline;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer.ReliableMessageSentCallback;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.appstate.AppStateManager;
import com.google.android.gms.appstate.AppStateStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.if3games.chessonline.ChessBoard.SquareDecoration;
import com.if3games.chessonline.activities.CPUWarning;
import com.if3games.chessonline.activities.EditBoard;
import com.if3games.chessonline.activities.EditPGNLoad;
import com.if3games.chessonline.activities.EditPGNSave;
import com.if3games.chessonline.activities.LoadFEN;
import com.if3games.chessonline.activities.LoadScid;
import com.if3games.chessonline.activities.Preferences;
import com.if3games.chessonline.book.BookOptions;
import com.if3games.chessonline.data.ConstantsData;
import com.if3games.chessonline.data.SaveGame;
import com.if3games.chessonline.engine.EngineUtil;
import com.if3games.chessonline.gamelogic.ChessParseError;
import com.if3games.chessonline.gamelogic.DroidChessController;
import com.if3games.chessonline.gamelogic.Move;
import com.if3games.chessonline.gamelogic.Pair;
import com.if3games.chessonline.gamelogic.PgnToken;
import com.if3games.chessonline.gamelogic.Piece;
import com.if3games.chessonline.gamelogic.Position;
import com.if3games.chessonline.gamelogic.TextIO;
import com.if3games.chessonline.gamelogic.TimeControlData;
import com.if3games.chessonline.gamelogic.GameTree.Node;
import com.if3games.chessonline.gtb.Probe;
import com.if3games.chessonline.other.PauseTimer;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DroidFish extends BaseGameActivity implements GUIInterface, OnClickListener, RealTimeMessageReceivedListener, 
 RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener, ReliableMessageSentCallback {
    // FIXME!!! book.txt (and test classes) should not be included in apk

    // FIXME!!! PGN view option: game continuation (for training)
    // FIXME!!! Remove invalid playerActions in PGN import (should be done in verifyChildren)
    // FIXME!!! Implement bookmark mechanism for positions in pgn files
    // FIXME!!! Add support for "Chess Leipzig" font

    // FIXME!!! Computer clock should stop if phone turned off (computer stops thinking if unplugged)
    // FIXME!!! Add support for "no time control" and "hour-glass time control" as defined by the PGN standard

    // FIXME!!! Online play on FICS
    // FIXME!!! Add chess960 support
    // FIXME!!! Implement "hint" feature

    // FIXME!!! Show extended book info. (Win percent, number of games, performance rating, etc.)
    // FIXME!!! Green color for "main move". Red color for "don't play in tournaments" moves.
    // FIXME!!! ECO opening codes

    // FIXME!!! Remember multi-PV analysis setting when program restarted.
    // FIXME!!! Option to display coordinates in border outside chess board.

    // FIXME!!! Better behavior if engine is terminated. How exactly?
    // FIXME!!! Handle PGN non-file intents with more than one game.
    // FIXME!!! Save position to fen/epd file

    // FIXME!!! Strength setting for external engines
    // FIXME!!! Selection dialog for going into variation
    // FIXME!!! Use two engines in engine/engine games

    private ChessBoardPlay cb;
    private static DroidChessController ctrl = null;
    private boolean mShowThinking;
    private boolean mShowStats;
    private boolean mWhiteBasedScores;
    private boolean mShowBookHints;
    private int maxNumArrows;
    private GameMode gameMode;
    private boolean mPonderMode;
    private int timeControl;
    private int movesPerSession;
    private int timeIncrement;
    private int mEngineThreads;
    private String playerName;
    private boolean boardFlipped;
    private boolean autoSwapSides;
    private boolean playerNameFlip;
    private boolean discardVariations;

    private TextView status;
    private ScrollView moveListScroll;
    private TextView moveList;
    private TextView thinking;
    private ImageButton custom1Button, custom2Button, custom3Button;
    private ImageButton modeButton, undoButton, redoButton;
    private ButtonActions custom1ButtonActions, custom2ButtonActions, custom3ButtonActions;
    private TextView whiteTitleText, blackTitleText, engineTitleText;
    private View secondTitleLine;
    private TextView whiteFigText, blackFigText, summaryTitleText;
    private static Dialog moveListMenuDlg;

    SharedPreferences settings;

    private boolean boardGestures;
    private float scrollSensitivity;
    private boolean invertScrollDirection;

    private boolean leftHanded;
    private boolean soundEnabled;
    private MediaPlayer moveSound;
    private boolean vibrateEnabled;
    private boolean animateMoves;
    private boolean autoScrollTitle;
    private boolean showMaterialDiff;
    private boolean showVariationLine;

    private final static String bookDir = "DroidFish";
    private final static String pgnDir = "DroidFish/pgn";
    private final static String fenDir = "DroidFish/epd";
    private final static String engineDir = "DroidFish/uci";
    private final static String gtbDefaultDir = "DroidFish/gtb";
    private BookOptions bookOptions = new BookOptions();
    private PGNOptions pgnOptions = new PGNOptions();
    private EngineOptions engineOptions = new EngineOptions();

    private long lastVisibleMillis; // Time when GUI became invisible. 0 if currently visible.
    private long lastComputationMillis; // Time when engine last showed that it was computing.

    PgnScreenText gameTextListener;

    private WakeLock wakeLock = null;
    private boolean useWakeLock = false;

    private Typeface figNotation;
    private Typeface defaultMoveListTypeFace;
    private Typeface defaultThinkingListTypeFace;


    /** Defines all configurable button actions. */
    private ActionFactory actionFactory = new ActionFactory() {
        private HashMap<String, UIAction> actions;

        private void addAction(UIAction a) {
            actions.put(a.getId(), a);
        }

        {
            actions = new HashMap<String, UIAction>();
            addAction(new UIAction() {
                public String getId() { return "flipboard"; }
                public int getName() { return R.string.flip_board; }
                public int getIcon() { return R.raw.flip; }
                public boolean enabled() { return true; }
                public void run() {
                    boardFlipped = !cb.flipped;
                    setBooleanPref("boardFlipped", boardFlipped);
                    cb.setFlipped(boardFlipped);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "showThinking"; }
                public int getName() { return R.string.toggle_show_thinking; }
                public int getIcon() { return R.raw.thinking; }
                public boolean enabled() { return true; }
                public void run() {
                    mShowThinking = toggleBooleanPref("showThinking");
                    updateThinkingInfo();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "bookHints"; }
                public int getName() { return R.string.toggle_book_hints; }
                public int getIcon() { return R.raw.book; }
                public boolean enabled() { return true; }
                public void run() {
                    mShowBookHints = toggleBooleanPref("bookHints");
                    updateThinkingInfo();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "viewVariations"; }
                public int getName() { return R.string.toggle_pgn_variations; }
                public int getIcon() { return R.raw.variation; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.variations = toggleBooleanPref("viewVariations");
                    gameTextListener.clear();
                    ctrl.prefsChanged(false);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "viewComments"; }
                public int getName() { return R.string.toggle_pgn_comments; }
                public int getIcon() { return R.raw.comment; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.comments = toggleBooleanPref("viewComments");
                    gameTextListener.clear();
                    ctrl.prefsChanged(false);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "viewHeaders"; }
                public int getName() { return R.string.toggle_pgn_headers; }
                public int getIcon() { return R.raw.header; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.headers = toggleBooleanPref("viewHeaders");
                    gameTextListener.clear();
                    ctrl.prefsChanged(false);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "toggleAnalysis"; }
                public int getName() { return R.string.toggle_analysis; }
                public int getIcon() { return R.raw.analyze; }
                public boolean enabled() { return true; }
                private int oldGameModeType = GameMode.EDIT_GAME;
                public void run() {
                    int gameModeType;
                    if (ctrl.analysisMode()) {
                        gameModeType = oldGameModeType;
                    } else {
                        oldGameModeType = ctrl.getGameMode().getModeNr();
                        gameModeType = GameMode.ANALYSIS;
                    }
                    newGameMode(gameModeType);
                    setBoardFlip(true);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "largeButtons"; }
                public int getName() { return R.string.toggle_large_buttons; }
                public int getIcon() { return R.raw.magnify; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.headers = toggleBooleanPref("largeButtons");
                    updateButtons();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "blindMode"; }
                public int getName() { return R.string.blind_mode; }
                public int getIcon() { return R.raw.blind; }
                public boolean enabled() { return true; }
                public void run() {
                    boolean blindMode = !cb.blindMode;
                    setBooleanPref("blindMode", blindMode);
                    cb.setBlindMode(blindMode);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "loadLastFile"; }
                public int getName() { return R.string.load_last_file; }
                public int getIcon() { return R.raw.open_last_file; }
                public boolean enabled() { return currFileType() != FT_NONE; }
                public void run() {
                    loadLastFile();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "selectEngine"; }
                public int getName() { return R.string.select_engine; }
                public int getIcon() { return R.raw.engine; }
                public boolean enabled() { return true; }
                public void run() {
                    removeDialog(SELECT_ENGINE_DIALOG_NOMANAGE);
                    showDialog(SELECT_ENGINE_DIALOG_NOMANAGE);
                }
            });
        }

        @Override
        public UIAction getAction(String actionId) {
            return actions.get(actionId);
        }
    };
    
    // GMS Multiplayer  
    private boolean isSinglePlayer;
    private boolean unlockLetterGridBtn = false;
    
    final static String TAG = "MULTIPLAER";
    
    // request codes we use when invoking an external activity
    final int RC_RESOLVE = 5000, RC_UNUSED = 5001;

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_WAITING_ROOM = 10002;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // Are we playing in multiplayer mode?
    boolean mMultiplayer = false;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    String mIncomingInvitationId = null;
    
    //private ExpiredTimeDialog nLevelDialog;
    private boolean imReady = false;
    private boolean opponentReady = false;
    private boolean isOpponentResign = false;
    private boolean isOpponentTimeOut = false;
    private boolean isLeaveRoom = false;
    private Handler h;
    private boolean isMatch = false;
    
    // For Cloud Save
    private static final int OUR_STATE_KEY = 0;
    // whether we already loaded the state the first time (so we don't reload
    // every time the activity goes to the background and comes back to the foreground)
    boolean mAlreadyLoadedState = false;
    boolean mAlreadyLocalState = false;
    // current save game
    private SaveGame mSaveGame = new SaveGame();
    private GoogleApiClient mClient;
    private Map<String,Integer> mOpponentStats = new HashMap<String,Integer>(ConstantsData.initMap);
    private String opponentName;
    private int opponentRating;
    private int gameTypeMode = 0;
    private int gmsGameVariantNumber = -1;
    
    private TextView player1TitleText, player2TitleText;
    
    private int imFirstType = -1;
    private boolean myTurn = false;
    private boolean invalidMove = false;
    
    private boolean opponentLeave = true;
    
	private InterstitialAd interstitial;
	private AdRequest adRequest;
	private AdView adView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		int isGMS = getIntent().getExtras().getInt("gms");
		if (isGMS != 1) {
			isSinglePlayer = true;
		} else {
			isSinglePlayer = false;
			
		    GoogleApiClient.Builder builder = 
		            new GoogleApiClient.Builder(this);
		        builder.addApi(Games.API)
		               .addApi(Plus.API)
		               .addApi(AppStateManager.API)
		               .addScope(Games.SCOPE_GAMES)
		               .addScope(Plus.SCOPE_PLUS_LOGIN)
		               .addScope(AppStateManager.SCOPE_APP_STATE);
		        mClient = builder.build();
		        
		    loadLocal();
		        
			if(isSignedIn()) {
		        //onFetchPlayerScoreAndAchive();
		        //displayPlayerNameScoreRank();
				//Toast.makeText(this, "I Connected", Toast.LENGTH_SHORT).show();
			}
		}

        Pair<String,String> pair = getPgnOrFenIntent();
        String intentPgnOrFen = pair.first;
        String intentFilename = pair.second;

        createDirectories();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            	try {
            		handlePrefsChange();
				} catch (Exception e) {
					// TODO: handle exception
				}             
            }
        });

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        setWakeLock(false);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "droidfish");
        wakeLock.setReferenceCounted(false);

        custom1ButtonActions = new ButtonActions("custom1", CUSTOM1_BUTTON_DIALOG,
                                                 R.string.select_action);
        custom2ButtonActions = new ButtonActions("custom2", CUSTOM2_BUTTON_DIALOG,
                                                 R.string.select_action);
        custom3ButtonActions = new ButtonActions("custom3", CUSTOM3_BUTTON_DIALOG,
                                                 R.string.select_action);

        figNotation = Typeface.createFromAsset(getAssets(), "fonts/DroidFishChessNotationDark.otf");
        setPieceNames(PGNOptions.PT_LOCAL);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        initUI();

        gameTextListener = new PgnScreenText(pgnOptions);
        if (ctrl != null)
            ctrl.shutdownEngine();
        ctrl = new DroidChessController(this, gameTextListener, pgnOptions);
        egtbForceReload = true;
        readPrefs();
        TimeControlData tcData = new TimeControlData();
        tcData.setTimeControl(timeControl, movesPerSession, timeIncrement);
        if(isSinglePlayer) 
        {
        	myTurn = true;
	        ctrl.newGame(gameMode, tcData);
	        {
	            byte[] data = null;
	            int version = 1;
	            if (savedInstanceState != null) {
	                data = savedInstanceState.getByteArray("gameState");
	                version = savedInstanceState.getInt("gameStateVersion", version);
	            } else {
	                String dataStr = settings.getString("gameState", null);
	                version = settings.getInt("gameStateVersion", version);
	                if (dataStr != null)
	                    data = strToByteArr(dataStr);
	            }
	            if (data != null)
	                ctrl.fromByteArray(data, version);
	        }
	        ctrl.setGuiPaused(true);
	        ctrl.setGuiPaused(false);
	        ctrl.startGame();
	        //startNewGame(0);
	        if (intentPgnOrFen != null) {
	            try {
	                ctrl.setFENOrPGN(intentPgnOrFen);
	                setBoardFlip(true);
	            } catch (ChessParseError e) {
	                // If FEN corresponds to illegal chess position, go into edit board mode.
	                try {
	                    TextIO.readFEN(intentPgnOrFen);
	                } catch (ChessParseError e2) {
	                    if (e2.pos != null)
	                        startEditBoard(intentPgnOrFen);
	                }
	            }
	        } else if (intentFilename != null) {
	            if (intentFilename.toLowerCase(Locale.US).endsWith(".fen") ||
	                intentFilename.toLowerCase(Locale.US).endsWith(".epd"))
	                loadFENFromFile(intentFilename);
	            else
	                loadPGNFromFile(intentFilename);
	        }
        } 
        else
        {
        	int rnd = new Random().nextInt(2);
        	startMultiplayerGameMode(rnd);	
        }
    }

    // Unicode code points for chess pieces
    private static final String figurinePieceNames = Piece.NOTATION_PAWN   + " " +
                                                     Piece.NOTATION_KNIGHT + " " +
                                                     Piece.NOTATION_BISHOP + " " +
                                                     Piece.NOTATION_ROOK   + " " +
                                                     Piece.NOTATION_QUEEN  + " " +
                                                     Piece.NOTATION_KING;

    private final void setPieceNames(int pieceType) {
        if (pieceType == PGNOptions.PT_FIGURINE) {
            TextIO.setPieceNames(figurinePieceNames);
        } else {
            TextIO.setPieceNames(getString(R.string.piece_names));
        }
    }

    /** Create directory structure on SD card. */
    private final void createDirectories() {
        File extDir = Environment.getExternalStorageDirectory();
        String sep = File.separator;
        new File(extDir + sep + bookDir).mkdirs();
        new File(extDir + sep + pgnDir).mkdirs();
        new File(extDir + sep + fenDir).mkdirs();
        new File(extDir + sep + engineDir).mkdirs();
        new File(extDir + sep + gtbDefaultDir).mkdirs();
    }

    /**
     * Return PGN/FEN data or filename from the Intent. Both can not be non-null.
     * @return Pair of PGN/FEN data and filename.
     */
    private final Pair<String,String> getPgnOrFenIntent() {
        String pgnOrFen = null;
        String filename = null;
        try {
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data == null) {
                Bundle b = intent.getExtras();
                if (b != null) {
                    Object strm = b.get(Intent.EXTRA_STREAM);
                    if (strm instanceof Uri) {
                        data = (Uri)strm;
                        if ("file".equals(data.getScheme())) {
                            filename = data.getEncodedPath();
                            if (filename != null)
                                filename = Uri.decode(filename);
                        }
                    }
                }
            }
            if (data == null) {
                if ((Intent.ACTION_SEND.equals(intent.getAction()) ||
                     Intent.ACTION_VIEW.equals(intent.getAction())) &&
                    ("application/x-chess-pgn".equals(intent.getType()) ||
                     "application/x-chess-fen".equals(intent.getType())))
                    pgnOrFen = intent.getStringExtra(Intent.EXTRA_TEXT);
            } else {
                String scheme = intent.getScheme();
                if ("file".equals(scheme)) {
                    filename = data.getEncodedPath();
                    if (filename != null)
                        filename = Uri.decode(filename);
                }
                if ((filename == null) &&
                    ("content".equals(scheme) ||
                     "file".equals(scheme))) {
                    ContentResolver resolver = getContentResolver();
                    InputStream in = resolver.openInputStream(intent.getData());
                    StringBuilder sb = new StringBuilder();
                    while (true) {
                        byte[] buffer = new byte[16384];
                        int len = in.read(buffer);
                        if (len <= 0)
                            break;
                        sb.append(new String(buffer, 0, len));
                    }
                    pgnOrFen = sb.toString();
                }
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), R.string.failed_to_read_pgn_data,
                           Toast.LENGTH_SHORT).show();
        }
        return new Pair<String,String>(pgnOrFen,filename);
    }

    private final byte[] strToByteArr(String str) {
        if (str == null)
            return null;
        int nBytes = str.length() / 2;
        byte[] ret = new byte[nBytes];
        for (int i = 0; i < nBytes; i++) {
            int c1 = str.charAt(i * 2) - 'A';
            int c2 = str.charAt(i * 2 + 1) - 'A';
            ret[i] = (byte)(c1 * 16 + c2);
        }
        return ret;
    }

    private final String byteArrToString(byte[] data) {
        if (data == null)
            return null;
        StringBuilder ret = new StringBuilder(32768);
        int nBytes = data.length;
        for (int i = 0; i < nBytes; i++) {
            int b = data[i]; if (b < 0) b += 256;
            char c1 = (char)('A' + (b / 16));
            char c2 = (char)('A' + (b & 15));
            ret.append(c1);
            ret.append(c2);
        }
        return ret.toString();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reInitUI();
    }

    /** Re-initialize UI when layout should change because of rotation or handedness change. */
    private final void reInitUI() {
        ChessBoardPlay oldCB = cb;
        String statusStr = status.getText().toString();
        initUI();
        readPrefs();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        cb.setFlipped(oldCB.flipped);
        cb.setDrawSquareLabels(oldCB.drawSquareLabels);
        cb.oneTouchMoves = oldCB.oneTouchMoves;
        cb.toggleSelection = oldCB.toggleSelection;
        cb.highlightLastMove = oldCB.highlightLastMove;
        cb.setBlindMode(oldCB.blindMode);
        setSelection(oldCB.selectedSquare);
        cb.userSelectedSquare = oldCB.userSelectedSquare;
        setStatusString(statusStr);
        moveListUpdated();
        updateThinkingInfo();
        ctrl.updateRemainingTime();
        ctrl.updateMaterialDiffList();
    }

    /** Return true if left-handed layout should be used. */
    private final boolean leftHandedView() {
        return settings.getBoolean("leftHanded", false) &&
               (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    /** Re-read preferences settings. */
    private final void handlePrefsChange() {
        if (leftHanded != leftHandedView())
            reInitUI();
        else
            readPrefs();
        ctrl.setGameMode(gameMode);
    }

    private final void initUI() {
        leftHanded = leftHandedView();
        if(!isSinglePlayer) {
        	setContentView(leftHanded ? R.layout.main_left_handed_gms : R.layout.main_gms);
			for (int id : CLICKABLES) {
	            findViewById(id).setOnClickListener(this);
	        }	
        }
        else {
        	setContentView(leftHanded ? R.layout.main_left_handed : R.layout.main);
        }
        Util.overrideFonts(findViewById(android.R.id.content));

        // title lines need to be regenerated every time due to layout changes (rotations)
        secondTitleLine = findViewById(R.id.second_title_line);
        whiteTitleText = (TextView)findViewById(R.id.white_clock);
        whiteTitleText.setSelected(true);
        blackTitleText = (TextView)findViewById(R.id.black_clock);
        blackTitleText.setSelected(true);
        engineTitleText = (TextView)findViewById(R.id.title_text);
        whiteFigText = (TextView)findViewById(R.id.white_pieces);
        whiteFigText.setTypeface(figNotation);
        whiteFigText.setSelected(true);
        whiteFigText.setTextColor(whiteTitleText.getTextColors());
        blackFigText = (TextView)findViewById(R.id.black_pieces);
        blackFigText.setTypeface(figNotation);
        blackFigText.setSelected(true);
        blackFigText.setTextColor(blackTitleText.getTextColors());
        summaryTitleText = (TextView)findViewById(R.id.title_text_summary);
        
        player1TitleText = (TextView)findViewById(R.id.player1);
        player2TitleText = (TextView)findViewById(R.id.player2);

        status = (TextView)findViewById(R.id.status);
        moveListScroll = (ScrollView)findViewById(R.id.scrollView);
        moveList = (TextView)findViewById(R.id.moveList);
        defaultMoveListTypeFace = moveList.getTypeface();
        thinking = (TextView)findViewById(R.id.thinking);
        defaultThinkingListTypeFace = thinking.getTypeface();
        status.setFocusable(false);
        moveListScroll.setFocusable(false);
        moveList.setFocusable(false);
        moveList.setMovementMethod(LinkMovementMethod.getInstance());
        thinking.setFocusable(false);

        cb = (ChessBoardPlay)findViewById(R.id.chessboard);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        cb.setPgnOptions(pgnOptions);

        final GestureDetector gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            private float scrollX = 0;
            private float scrollY = 0;
            @Override
            public boolean onDown(MotionEvent e) {
                if (!boardGestures) {
                    handleClick(e);
                    return true;
                }
                scrollX = 0;
                scrollY = 0;
                return false;
            }
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!boardGestures)
                    return false;
                cb.cancelLongPress();
                if (invertScrollDirection) {
                    distanceX = -distanceX;
                    distanceY = -distanceY;
                }
                if ((scrollSensitivity > 0) && (cb.sqSize > 0)) {
                    scrollX += distanceX;
                    scrollY += distanceY;
                    float scrollUnit = cb.sqSize * scrollSensitivity;
                    if (Math.abs(scrollX) >= Math.abs(scrollY)) {
                        // Undo/redo
                        int nRedo = 0, nUndo = 0;
                        while (scrollX > scrollUnit) {
                            nRedo++;
                            scrollX -= scrollUnit;
                        }
                        while (scrollX < -scrollUnit) {
                            nUndo++;
                            scrollX += scrollUnit;
                        }
                        if (nUndo + nRedo > 0)
                            scrollY = 0;
                        if (nRedo + nUndo > 1) {
                            boolean analysis = gameMode.analysisMode();
                            boolean human = gameMode.playerWhite() || gameMode.playerBlack();
                            if (analysis || !human)
                                ctrl.setGameMode(new GameMode(GameMode.TWO_PLAYERS));
                        }
                        for (int i = 0; i < nRedo; i++) ctrl.redoMove();
                        for (int i = 0; i < nUndo; i++) ctrl.undoMove();
                        ctrl.setGameMode(gameMode);
                    } else {
                        // Next/previous variation
                        int varDelta = 0;
                        while (scrollY > scrollUnit) {
                            varDelta++;
                            scrollY -= scrollUnit;
                        }
                        while (scrollY < -scrollUnit) {
                            varDelta--;
                            scrollY += scrollUnit;
                        }
                        if (varDelta != 0)
                            scrollX = 0;
                        ctrl.changeVariation(varDelta);
                    }
                }
                return true;
            }
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!boardGestures)
                    return false;
                cb.cancelLongPress();
                handleClick(e);
                return true;
            }
            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                if (!boardGestures)
                    return false;
                if (e.getAction() == MotionEvent.ACTION_UP)
                    handleClick(e);
                return true;
            }
            private final void handleClick(MotionEvent e) {
                if (ctrl.humansTurn() && myTurn) {
                    int sq = cb.eventToSquare(e);
                    Move m = cb.mousePressed(sq);
                    if (m != null) {
                        ctrl.makeHumanMove(m);
                        if(!isSinglePlayer) {
	                        if(!invalidMove)
	                        	broadcastMove(m.to, m.from);
	                        else
	                        	invalidMove = false;
                        }
                    }
                    setEgtbHints(cb.getSelectedSquare());
                }
            }
            @Override
            public void onLongPress(MotionEvent e) {
                if (!boardGestures)
                    return;
                ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(20);
                removeDialog(BOARD_MENU_DIALOG);
                showDialog(BOARD_MENU_DIALOG);
            }
        });
        cb.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gd.onTouchEvent(event);
            }
        });
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
            public void onTrackballEvent(MotionEvent event) {
                if (ctrl.humansTurn()) {
                    Move m = cb.handleTrackballEvent(event);
                    if (m != null)
                        ctrl.makeHumanMove(m);
                    setEgtbHints(cb.getSelectedSquare());
                }
            }
        });

        moveList.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                removeDialog(MOVELIST_MENU_DIALOG);
                showDialog(MOVELIST_MENU_DIALOG);
                return true;
            }
        });
        thinking.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (mShowThinking || gameMode.analysisMode()) {
                    if (!pvMoves.isEmpty()) {
                        removeDialog(THINKING_MENU_DIALOG);
                        showDialog(THINKING_MENU_DIALOG);
                    }
                }
                return true;
            }
        });

        custom1Button = (ImageButton)findViewById(R.id.custom1Button);
        custom1ButtonActions.setImageButton(custom1Button, this);
        custom2Button = (ImageButton)findViewById(R.id.custom2Button);
        custom2ButtonActions.setImageButton(custom2Button, this);
        custom3Button = (ImageButton)findViewById(R.id.custom3Button);
        custom3ButtonActions.setImageButton(custom3Button, this);

        modeButton = (ImageButton)findViewById(R.id.modeButton);
        modeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(isSinglePlayer)
            		showDialog(GAME_MODE_DIALOG);
            	else
            		showDialog(GAME_GMS_MODE_DIALOG);
            }
        });
        undoButton = (ImageButton)findViewById(R.id.undoButton);
        redoButton = (ImageButton)findViewById(R.id.redoButton);
        
        if(isSinglePlayer) {
	        undoButton.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) {
	                ctrl.undoMove();
	            }
	        });
	        undoButton.setOnLongClickListener(new OnLongClickListener() {
	            @Override
	            public boolean onLongClick(View v) {
	                removeDialog(GO_BACK_MENU_DIALOG);
	                showDialog(GO_BACK_MENU_DIALOG);
	                return true;
	            }
	        });
	        
	        redoButton.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) {
	                ctrl.redoMove();
	            }
	        });
	        redoButton.setOnLongClickListener(new OnLongClickListener() {
	            @Override
	            public boolean onLongClick(View v) {
	                removeDialog(GO_FORWARD_MENU_DIALOG);
	                showDialog(GO_FORWARD_MENU_DIALOG);
	                return true;
	            }
	        });
        } else {
        	undoButton.setVisibility(View.GONE);
        	redoButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(isSinglePlayer) {
	        if (ctrl != null) {
	            byte[] data = ctrl.toByteArray();
	            outState.putByteArray("gameState", data);
	            outState.putInt("gameStateVersion", 3);
	        }
        }
    }

    @Override
    protected void onResume() {
        lastVisibleMillis = 0;
        if (ctrl != null)
            ctrl.setGuiPaused(false);
        notificationActive = true;
        updateNotification();
        setWakeLock(useWakeLock);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (ctrl != null) {
            ctrl.setGuiPaused(true);
            if(isSinglePlayer) {
	            byte[] data = ctrl.toByteArray();
	            Editor editor = settings.edit();
	            String dataStr = byteArrToString(data);
	            editor.putString("gameState", dataStr);
	            editor.putInt("gameStateVersion", 3);
	            editor.commit();
            }
        }
        lastVisibleMillis = System.currentTimeMillis();
        updateNotification();
        setWakeLock(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ctrl != null)
            ctrl.shutdownEngine();
        setNotification(false);
        super.onDestroy();
    }

    private final int getIntSetting(String settingName, int defaultValue) {
        String tmp = settings.getString(settingName, String.format(Locale.US, "%d", defaultValue));
        int value = Integer.parseInt(tmp);
        return value;
    }

    private final void readPrefs() {
    	if (isSinglePlayer) {
            int modeNr = getIntSetting("gameMode", 1);
            gameMode = new GameMode(modeNr);
            String oldPlayerName = playerName;
            playerName = settings.getString("playerName", "Player");
            boardFlipped = settings.getBoolean("boardFlipped", false);
            autoSwapSides = settings.getBoolean("autoSwapSides", false);
            playerNameFlip = settings.getBoolean("playerNameFlip", true);
            setBoardFlip(!playerName.equals(oldPlayerName));
            boolean drawSquareLabels = settings.getBoolean("drawSquareLabels", false);
            cb.setDrawSquareLabels(drawSquareLabels);
            cb.oneTouchMoves = settings.getBoolean("oneTouchMoves", false);
            cb.toggleSelection = getIntSetting("squareSelectType", 0) == 1;
            cb.highlightLastMove = settings.getBoolean("highlightLastMove", true);
            cb.setBlindMode(settings.getBoolean("blindMode", false));

            mShowThinking = settings.getBoolean("showThinking", false);
            mShowStats = settings.getBoolean("showStats", true);
            mWhiteBasedScores = settings.getBoolean("whiteBasedScores", false);
            maxNumArrows = getIntSetting("thinkingArrows", 2);
            mShowBookHints = settings.getBoolean("bookHints", false);

            mEngineThreads = getIntSetting("threads", 1);

            String engine = settings.getString("engine", "stockfish");
            int strength = settings.getInt("strength", 1000);
            setEngineStrength(engine, strength);

            mPonderMode = settings.getBoolean("ponderMode", false);
            if (!mPonderMode)
                ctrl.stopPonder();

            timeControl = getIntSetting("timeControl", 120000);
            movesPerSession = getIntSetting("movesPerSession", 60);
            timeIncrement = getIntSetting("timeIncrement", 0);

            boardGestures = settings.getBoolean("boardGestures", true);
            scrollSensitivity = Float.parseFloat(settings.getString("scrollSensitivity", "2"));
            invertScrollDirection = settings.getBoolean("invertScrollDirection", false);
            discardVariations = settings.getBoolean("discardVariations", false);
            Util.setFullScreenMode(this, settings);
            useWakeLock = settings.getBoolean("wakeLock", false);
            setWakeLock(useWakeLock);

            // Visible Options 
            
            int fontSize = getIntSetting("fontSize", 12);
            int statusFontSize = fontSize;
            Configuration config = getResources().getConfiguration();
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT)
                statusFontSize = Math.min(statusFontSize, 16);
            status.setTextSize(statusFontSize);
            moveList.setTextSize(fontSize);
            thinking.setTextSize(fontSize);
            soundEnabled = settings.getBoolean("soundEnabled", false);
            vibrateEnabled = settings.getBoolean("vibrateEnabled", false);
            animateMoves = settings.getBoolean("animateMoves", true);
            autoScrollTitle = settings.getBoolean("autoScrollTitle", true);
            setTitleScrolling();
            
            ColorTheme.instance().readColors(settings);
            cb.setColors();
            Util.overrideFonts(findViewById(android.R.id.content));
            
            // End Visible Options

            custom1ButtonActions.readPrefs(settings, actionFactory);
            custom2ButtonActions.readPrefs(settings, actionFactory);
            custom3ButtonActions.readPrefs(settings, actionFactory);
            updateButtons();

            bookOptions.filename = settings.getString("bookFile", "");
            bookOptions.maxLength = getIntSetting("bookMaxLength", 1000000);
            bookOptions.preferMainLines = settings.getBoolean("bookPreferMainLines", false);
            bookOptions.tournamentMode = settings.getBoolean("bookTournamentMode", false);
            bookOptions.random = (settings.getInt("bookRandom", 500) - 500) * (3.0 / 500);
            setBookOptions();

            engineOptions.hashMB = getIntSetting("hashMB", 16);
            engineOptions.hints = settings.getBoolean("tbHints", false);
            engineOptions.hintsEdit = settings.getBoolean("tbHintsEdit", false);
            engineOptions.rootProbe = settings.getBoolean("tbRootProbe", true);
            engineOptions.engineProbe = settings.getBoolean("tbEngineProbe", true);
            String gtbPath = settings.getString("gtbPath", "").trim();
            if (gtbPath.length() == 0) {
                File extDir = Environment.getExternalStorageDirectory();
                String sep = File.separator;
                gtbPath = extDir.getAbsolutePath() + sep + gtbDefaultDir;
            }
            engineOptions.gtbPath = gtbPath;
            setEngineOptions(false);
            setEgtbHints(cb.getSelectedSquare());

            updateThinkingInfo();

            pgnOptions.view.variations  = settings.getBoolean("viewVariations",     true);
            pgnOptions.view.comments    = settings.getBoolean("viewComments",       true);
            pgnOptions.view.nag         = settings.getBoolean("viewNAG",            true);
            pgnOptions.view.headers     = settings.getBoolean("viewHeaders",        false);
            final int oldViewPieceType = pgnOptions.view.pieceType;
            pgnOptions.view.pieceType   = getIntSetting("viewPieceType", PGNOptions.PT_LOCAL);
            showVariationLine           = settings.getBoolean("showVariationLine",  false);
            pgnOptions.imp.variations   = settings.getBoolean("importVariations",   true);
            pgnOptions.imp.comments     = settings.getBoolean("importComments",     true);
            pgnOptions.imp.nag          = settings.getBoolean("importNAG",          true);
            pgnOptions.exp.variations   = settings.getBoolean("exportVariations",   true);
            pgnOptions.exp.comments     = settings.getBoolean("exportComments",     true);
            pgnOptions.exp.nag          = settings.getBoolean("exportNAG",          true);
            pgnOptions.exp.playerAction = settings.getBoolean("exportPlayerAction", false);
            pgnOptions.exp.clockInfo    = settings.getBoolean("exportTime",         false);

            gameTextListener.clear();
            setPieceNames(pgnOptions.view.pieceType);
            ctrl.prefsChanged(oldViewPieceType != pgnOptions.view.pieceType);
            // update the typeset in case of a change anyway, cause it could occur
            // as well in rotation
            setFigurineNotation(pgnOptions.view.pieceType == PGNOptions.PT_FIGURINE, fontSize);

            showMaterialDiff = settings.getBoolean("materialDiff", false);
            secondTitleLine.setVisibility(showMaterialDiff ? View.VISIBLE : View.GONE);
		}
    	else {
            int modeNr = 1;
            gameMode = new GameMode(modeNr);
            String oldPlayerName = playerName;
            playerName = "Player";
            boardFlipped = false;
            autoSwapSides = false;
            playerNameFlip = true;
            setBoardFlip(!playerName.equals(oldPlayerName));
            boolean drawSquareLabels = false;
            cb.setDrawSquareLabels(drawSquareLabels);
            cb.oneTouchMoves = false;
            cb.toggleSelection = getIntSetting("squareSelectType", 0) == 1;
            cb.highlightLastMove = settings.getBoolean("highlightLastMove", true);
            cb.setBlindMode(false);

            mShowThinking = false;
            mShowStats = settings.getBoolean("showStats", true);
            mWhiteBasedScores = false;
            maxNumArrows = getIntSetting("thinkingArrows", 2);
            mShowBookHints = false;

            mEngineThreads = 1;

            String engine = "stockfish";
            int strength = 1000;
            setEngineStrength(engine, strength);

            mPonderMode = false;
            if (!mPonderMode)
                ctrl.stopPonder();

            // Toto: add time control to gms 
            timeControl = 120000;
            movesPerSession = 60;
            timeIncrement = 0;

            boardGestures = false;
            scrollSensitivity = Float.parseFloat(settings.getString("scrollSensitivity", "2"));
            invertScrollDirection = settings.getBoolean("invertScrollDirection", false);
            discardVariations = false;
            Util.setFullScreenMode(this, settings);
            useWakeLock = false;
            setWakeLock(useWakeLock);

            // Visible Options 
            
            int fontSize = getIntSetting("fontSize", 12);
            int statusFontSize = fontSize;
            Configuration config = getResources().getConfiguration();
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT)
                statusFontSize = Math.min(statusFontSize, 16);
            status.setTextSize(statusFontSize);
            moveList.setTextSize(fontSize);
            thinking.setTextSize(fontSize);
            soundEnabled = settings.getBoolean("soundEnabled", false);
            vibrateEnabled = settings.getBoolean("vibrateEnabled", false);
            animateMoves = settings.getBoolean("animateMoves", true);
            autoScrollTitle = settings.getBoolean("autoScrollTitle", true);
            setTitleScrolling();
            
            ColorTheme.instance().readColors(settings);
            cb.setColors();
            Util.overrideFonts(findViewById(android.R.id.content));
            
            // End Visible Options

            custom1ButtonActions.readPrefs(settings, actionFactory);
            custom2ButtonActions.readPrefs(settings, actionFactory);
            custom3ButtonActions.readPrefs(settings, actionFactory);
            updateButtons();

            bookOptions.filename = "";
            bookOptions.maxLength = 1000000;
            bookOptions.preferMainLines = false;
            bookOptions.tournamentMode = false;
            bookOptions.random = (settings.getInt("bookRandom", 500) - 500) * (3.0 / 500);
            setBookOptions();

            engineOptions.hashMB = 16;
            engineOptions.hints = false;
            engineOptions.hintsEdit = false;
            engineOptions.rootProbe = true;
            engineOptions.engineProbe = true;
            String gtbPath = settings.getString("gtbPath", "").trim();
            if (gtbPath.length() == 0) {
                File extDir = Environment.getExternalStorageDirectory();
                String sep = File.separator;
                gtbPath = extDir.getAbsolutePath() + sep + gtbDefaultDir;
            }
            engineOptions.gtbPath = gtbPath;
            setEngineOptions(false);
            setEgtbHints(cb.getSelectedSquare());

            updateThinkingInfo();

            pgnOptions.view.variations  = settings.getBoolean("viewVariations",     true);
            pgnOptions.view.comments    = settings.getBoolean("viewComments",       true);
            pgnOptions.view.nag         = settings.getBoolean("viewNAG",            true);
            pgnOptions.view.headers     = settings.getBoolean("viewHeaders",        false);
            final int oldViewPieceType = pgnOptions.view.pieceType;
            pgnOptions.view.pieceType   = getIntSetting("viewPieceType", PGNOptions.PT_LOCAL);
            showVariationLine           = settings.getBoolean("showVariationLine",  false);
            pgnOptions.imp.variations   = settings.getBoolean("importVariations",   true);
            pgnOptions.imp.comments     = settings.getBoolean("importComments",     true);
            pgnOptions.imp.nag          = settings.getBoolean("importNAG",          true);
            pgnOptions.exp.variations   = settings.getBoolean("exportVariations",   true);
            pgnOptions.exp.comments     = settings.getBoolean("exportComments",     true);
            pgnOptions.exp.nag          = settings.getBoolean("exportNAG",          true);
            pgnOptions.exp.playerAction = settings.getBoolean("exportPlayerAction", false);
            pgnOptions.exp.clockInfo    = settings.getBoolean("exportTime",         false);

            gameTextListener.clear();
            setPieceNames(pgnOptions.view.pieceType);
            ctrl.prefsChanged(oldViewPieceType != pgnOptions.view.pieceType);
            // update the typeset in case of a change anyway, cause it could occur
            // as well in rotation
            setFigurineNotation(pgnOptions.view.pieceType == PGNOptions.PT_FIGURINE, fontSize);

            showMaterialDiff = settings.getBoolean("materialDiff", true);
            secondTitleLine.setVisibility(showMaterialDiff ? View.VISIBLE : View.GONE);
    	}

    }

    /**
     * Change the Pieces into figurine or regular (i.e. letters) display
     */
    private final void setFigurineNotation(boolean displayAsFigures, int fontSize) {
        if (displayAsFigures) {
            // increase the font cause it has different kerning and looks small
            float increaseFontSize = fontSize * 1.1f;
            moveList.setTypeface(figNotation);
            moveList.setTextSize(increaseFontSize);
            thinking.setTypeface(figNotation);
            thinking.setTextSize(increaseFontSize);
        } else {
            moveList.setTypeface(defaultMoveListTypeFace);
            thinking.setTypeface(defaultThinkingListTypeFace);
        }
    }

    /** Enable/disable title bar scrolling. */
    private final void setTitleScrolling() {
        TextUtils.TruncateAt where = autoScrollTitle ? TextUtils.TruncateAt.MARQUEE
                                                     : TextUtils.TruncateAt.END;
        whiteTitleText.setEllipsize(where);
        blackTitleText.setEllipsize(where);
        whiteFigText.setEllipsize(where);
        blackFigText.setEllipsize(where);
    }

    private final void updateButtons() {
        boolean largeButtons = settings.getBoolean("largeButtons", false);
        Resources r = getResources();
        int bWidth  = (int)Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, r.getDisplayMetrics()));
        int bHeight = (int)Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics()));
        if (largeButtons) {
            if (custom1ButtonActions.isEnabled() &&
                custom2ButtonActions.isEnabled() &&
                custom3ButtonActions.isEnabled()) {
                Configuration config = getResources().getConfiguration();
                if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    bWidth  = bWidth  * 6 / 5;
                    bHeight = bHeight * 6 / 5;
                } else {
                    bWidth  = bWidth  * 5 / 4;
                    bHeight = bHeight * 5 / 4;
                }
            } else {
                bWidth  = bWidth  * 3 / 2;
                bHeight = bHeight * 3 / 2;
            }
        }
        SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.touch);
        setButtonData(custom1Button, bWidth, bHeight, custom1ButtonActions.getIcon(), svg);
        setButtonData(custom2Button, bWidth, bHeight, custom2ButtonActions.getIcon(), svg);
        setButtonData(custom3Button, bWidth, bHeight, custom3ButtonActions.getIcon(), svg);
        setButtonData(modeButton, bWidth, bHeight, R.raw.mode, svg);
        setButtonData(undoButton, bWidth, bHeight, R.raw.left, svg);
        setButtonData(redoButton, bWidth, bHeight, R.raw.right, svg);
    }

    private final void setButtonData(ImageButton button, int bWidth, int bHeight,
                                     int svgResId, SVG touched) {
        SVG svg = SVGParser.getSVGFromResource(getResources(), svgResId);
        button.setBackgroundDrawable(new SVGPictureDrawable(svg));

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, new SVGPictureDrawable(touched));
        button.setImageDrawable(sld);

        LayoutParams lp = button.getLayoutParams();
        lp.height = bHeight;
        lp.width = bWidth;
        button.setLayoutParams(lp);
        button.setPadding(0,0,0,0);
        button.setScaleType(ScaleType.FIT_XY);
    }

    private synchronized final void setWakeLock(boolean enableLock) {
        WakeLock wl = wakeLock;
        if (wl != null) {
            if (wl.isHeld())
                wl.release();
            if (enableLock)
                wl.acquire();
        }
    }

    private final void setEngineStrength(String engine, int strength) {
        ctrl.setEngineStrength(engine, strength);
        setEngineTitle(engine, strength);
    }

    private final void setEngineTitle(String engine, int strength) {
        if (engine.contains("/")) {
            int idx = engine.lastIndexOf('/');
            String eName = engine.substring(idx + 1);
            engineTitleText.setText(eName);
        } else {
            String eName = getString(engine.equals("cuckoochess") ?
                                     R.string.cuckoochess_engine :
                                     R.string.stockfish_engine);
            boolean analysis = (ctrl != null) && ctrl.analysisMode();
            if ((strength < 1000) && !analysis) {
                engineTitleText.setText(String.format(Locale.US, "%s: %d%%", eName, strength / 10));
            } else {
                engineTitleText.setText(eName);
            }
        }
    }

    /** Update center field in second header line. */
    public final void updateTimeControlTitle() {
        int[] tmpInfo = ctrl.getTimeLimit();
        StringBuilder sb = new StringBuilder();
        int tc = tmpInfo[0];
        int mps = tmpInfo[1];
        int inc = tmpInfo[2];
        if (mps > 0) {
            sb.append(mps);
            sb.append("/");
        }
        sb.append(timeToString(tc));
        if ((inc > 0) || (mps <= 0)) {
            sb.append("+");
            sb.append(tmpInfo[2] / 1000);
        }
        summaryTitleText.setText(sb.toString());
    }

    @Override
    public void updateEngineTitle() {
        String engine = settings.getString("engine", "stockfish");
        int strength = settings.getInt("strength", 1000);
        setEngineTitle(engine, strength);
    }

    @Override
    public void updateMaterialDifferenceTitle(Util.MaterialDiff diff) {
        whiteFigText.setText(diff.white);
        blackFigText.setText(diff.black);
    }

    private final void setBookOptions() {
        BookOptions options = new BookOptions(bookOptions);
        if (options.filename.length() > 0) {
            File extDir = Environment.getExternalStorageDirectory();
            String sep = File.separator;
            options.filename = extDir.getAbsolutePath() + sep + bookDir + sep + options.filename;
        }
        ctrl.setBookOptions(options);
    }

    private boolean egtbForceReload = false;

    private final void setEngineOptions(boolean restart) {
        computeNetEngineID();
        ctrl.setEngineOptions(new EngineOptions(engineOptions), restart);
        Probe.getInstance().setPath(engineOptions.gtbPath, egtbForceReload);
        egtbForceReload = false;
    }

    private final void computeNetEngineID() {
        String id = "";
        try {
            String engine = settings.getString("engine", "stockfish");
            String[] lines = Util.readFile(engine);
            if (lines.length >= 3)
                id = lines[1] + ":" + lines[2];
        } catch (IOException e) {
        }
        engineOptions.networkID = id;
    }

    private final void setEgtbHints(int sq) {
        if (!engineOptions.hints || (sq < 0)) {
            cb.setSquareDecorations(null);
            return;
        }

        Probe gtbProbe = Probe.getInstance();
        ArrayList<Pair<Integer, Integer>> x = gtbProbe.movePieceProbe(cb.pos, sq);
        if (x == null) {
            cb.setSquareDecorations(null);
            return;
        }

        ArrayList<SquareDecoration> sd = new ArrayList<SquareDecoration>();
        for (Pair<Integer,Integer> p : x)
            sd.add(new SquareDecoration(p.first, p.second));
        cb.setSquareDecorations(sd);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        if(isSinglePlayer)
        	return true;
        else
        	return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.item_file_menu);
        item.setTitle(boardGestures ? R.string.option_file : R.string.tools_menu);
        if(isSinglePlayer)
        	return true;
        else
        	return false;
    }

    static private final int RESULT_EDITBOARD = 0;
    static private final int RESULT_SETTINGS = 1;
    static private final int RESULT_LOAD_PGN = 2;
    static private final int RESULT_LOAD_FEN = 3;
    static private final int RESULT_SELECT_SCID = 4;
    static private final int RESULT_OI_PGN_SAVE = 5;
    static private final int RESULT_OI_PGN_LOAD = 6;
    static private final int RESULT_OI_FEN_LOAD = 7;
    static private final int RESULT_GET_FEN = 8;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.item_new_game:
            showDialog(NEW_GAME_DIALOG);
            return true;
        case R.id.item_editboard: {
            startEditBoard(ctrl.getFEN());
            return true;
        }
        case R.id.item_settings: {
            Intent i = new Intent(DroidFish.this, Preferences.class);
            startActivityForResult(i, RESULT_SETTINGS);
            return true;
        }
        case R.id.item_file_menu: {
            int dialog = boardGestures ? FILE_MENU_DIALOG : BOARD_MENU_DIALOG;
            removeDialog(dialog);
            showDialog(dialog);
            return true;
        }
        case R.id.item_goto_move: {
            showDialog(SELECT_MOVE_DIALOG);
            return true;
        }
        case R.id.item_force_move: {
            ctrl.stopSearch();
            return true;
        }
        case R.id.item_draw: {
            if (ctrl.humansTurn()) {
                if (ctrl.claimDrawIfPossible()) {
                    ctrl.stopPonder();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.offer_draw, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        }
        case R.id.item_resign: {
            if (ctrl.humansTurn()) {
                ctrl.resignGame();
            }
            return true;
        }
        case R.id.select_book:
            removeDialog(SELECT_BOOK_DIALOG);
            showDialog(SELECT_BOOK_DIALOG);
            return true;
        case R.id.manage_engines:
            showDialog(MANAGE_ENGINES_DIALOG);
            return true;
        case R.id.set_color_theme:
            showDialog(SET_COLOR_THEME_DIALOG);
            return true;
        case R.id.item_about:
            showDialog(ABOUT_DIALOG);
            return true;
        }
        return false;
    }

    private void startEditBoard(String fen) {
        Intent i = new Intent(DroidFish.this, EditBoard.class);
        i.setAction(fen);
        startActivityForResult(i, RESULT_EDITBOARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case RESULT_SETTINGS:
            handlePrefsChange();
            break;
        case RESULT_EDITBOARD:
            if (resultCode == RESULT_OK) {
                try {
                    String fen = data.getAction();
                    ctrl.setFENOrPGN(fen);
                    setBoardFlip(false);
                } catch (ChessParseError e) {
                }
            }
            break;
        case RESULT_LOAD_PGN:
            if (resultCode == RESULT_OK) {
                try {
                    String pgn = data.getAction();
                    int modeNr = ctrl.getGameMode().getModeNr();
                    if ((modeNr != GameMode.ANALYSIS) && (modeNr != GameMode.EDIT_GAME))
                        newGameMode(GameMode.EDIT_GAME);
                    ctrl.setFENOrPGN(pgn);
                    setBoardFlip(true);
                } catch (ChessParseError e) {
                    Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
                }
            }
            break;
        case RESULT_SELECT_SCID:
            if (resultCode == RESULT_OK) {
                String pathName = data.getAction();
                if (pathName != null) {
                    Editor editor = settings.edit();
                    editor.putString("currentScidFile", pathName);
                    editor.putInt("currFT", FT_SCID);
                    editor.commit();
                    Intent i = new Intent(DroidFish.this, LoadScid.class);
                    i.setAction("com.if3games.chessonline.loadScid");
                    i.putExtra("com.if3games.chessonline.pathname", pathName);
                    startActivityForResult(i, RESULT_LOAD_PGN);
                }
            }
            break;
        case RESULT_OI_PGN_LOAD:
            if (resultCode == RESULT_OK) {
                String pathName = getFilePathFromUri(data.getData());
                if (pathName != null)
                    loadPGNFromFile(pathName);
            }
            break;
        case RESULT_OI_PGN_SAVE:
            if (resultCode == RESULT_OK) {
                String pathName = getFilePathFromUri(data.getData());
                if (pathName != null) {
                    if ((pathName.length() > 0) && !pathName.contains("."))
                        pathName += ".pgn";
                    savePGNToFile(pathName, false);
                }
            }
            break;
        case RESULT_OI_FEN_LOAD:
            if (resultCode == RESULT_OK) {
                String pathName = getFilePathFromUri(data.getData());
                if (pathName != null)
                    loadFENFromFile(pathName);
            }
            break;
        case RESULT_GET_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getStringExtra(Intent.EXTRA_TEXT);
                if (fen == null) {
                    String pathName = getFilePathFromUri(data.getData());
                    loadFENFromFile(pathName);
                }
                setFenHelper(fen);
            }
            break;
        case RESULT_LOAD_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getAction();
                setFenHelper(fen);
            }
            break;
            // GMS
        case RC_SELECT_PLAYERS:
            // we got the result from the "select players" UI -- ready to create the room
            handleSelectPlayersResult(resultCode, data, gmsGameVariantNumber);
            break;
        case RC_INVITATION_INBOX:
            // we got the result from the "select invitation" UI (invitation inbox). We're
            // ready to accept the selected invitation:
            handleInvitationInboxResult(resultCode, data);
            break;
        case RC_WAITING_ROOM:
            // we got the result from the "waiting room" UI.
            if (resultCode == Activity.RESULT_OK) {
                // ready to start playing
                //Log.d(TAG, "Starting game (waiting room returned OK).");
                //if(!imNotFirst)
                	//sendImFirstLevelNumberForStart();
            	if(gmsGameVariantNumber != -1)
            		startGame(true, gmsGameVariantNumber);
            } else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player indicated that they want to leave the room
                leaveRoom();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Dialog was cancelled (user pressed back key, for instance). In our game,
                // this means leaving the room too. In more elaborate games, this could mean
                // something else (like minimizing the waiting room UI).
                leaveRoom();
            }
            break;
        }
    }

    /** Set new game mode. */
    private final void newGameMode(int gameModeType) {
        Editor editor = settings.edit();
        String gameModeStr = String.format(Locale.US, "%d", gameModeType);
        editor.putString("gameMode", gameModeStr);
        editor.commit();
        gameMode = new GameMode(gameModeType);
        ctrl.setGameMode(gameMode);
    }

    public static String getFilePathFromUri(Uri uri) {
        if (uri == null)
            return null;
        return uri.getPath();
    }

    private final String getParseErrString(ChessParseError e) {
        if (e.resourceId == -1)
            return e.getMessage();
        else
            return getString(e.resourceId);
    }

    private final int nameMatchScore(String name, String match) {
        if (name == null)
            return 0;
        String lName = name.toLowerCase(Locale.US);
        String lMatch = match.toLowerCase(Locale.US);
        if (name.equals(match))
            return 6;
        if (lName.equals(lMatch))
            return 5;
        if (name.startsWith(match))
            return 4;
        if (lName.startsWith(lMatch))
            return 3;
        if (name.contains(match))
            return 2;
        if (lName.contains(lMatch))
            return 1;
        return 0;
    }

    private final void setBoardFlip() {
        setBoardFlip(false);
    }

    /** Set a boolean preference setting. */
    private final void setBooleanPref(String name, boolean value) {
        Editor editor = settings.edit();
        editor.putBoolean(name, value);
        editor.commit();
    }

    /** Toggle a boolean preference setting. Return new value. */
    private final boolean toggleBooleanPref(String name) {
        boolean value = !settings.getBoolean(name, false);
        setBooleanPref(name, value);
        return value;
    }

    private final void setBoardFlip(boolean matchPlayerNames) {
        boolean flipped = boardFlipped;
        if (playerNameFlip && matchPlayerNames && (ctrl != null)) {
            final TreeMap<String,String> headers = new TreeMap<String,String>();
            ctrl.getHeaders(headers);
            int whiteMatch = nameMatchScore(headers.get("White"), playerName);
            int blackMatch = nameMatchScore(headers.get("Black"), playerName);
            if (( flipped && (whiteMatch > blackMatch)) ||
                (!flipped && (whiteMatch < blackMatch))) {
                flipped = !flipped;
                boardFlipped = flipped;
                setBooleanPref("boardFlipped", flipped);
            }
        }
        if (autoSwapSides) {
            if (gameMode.analysisMode()) {
                flipped = !cb.pos.whiteMove;
            } else if (gameMode.playerWhite() && gameMode.playerBlack()) {
                flipped = !cb.pos.whiteMove;
            } else if (gameMode.playerWhite()) {
                flipped = false;
            } else if (gameMode.playerBlack()) {
                flipped = true;
            } else { // two computers
                flipped = !cb.pos.whiteMove;
            }
        }
        cb.setFlipped(flipped);
    }

    @Override
    public void setSelection(int sq) {
        cb.setSelection(cb.highlightLastMove ? sq : -1);
        cb.userSelectedSquare = false;
        setEgtbHints(sq);
    }

    @Override
    public void setStatus(GameStatus s) {
        String str;
        switch (s.state) {
        case ALIVE:
            str = Integer.valueOf(s.moveNr).toString();
            if (s.white)
                str += ". " + getString(R.string.whites_move);
            else
                str += "... " + getString(R.string.blacks_move);
            if (s.ponder) str += " (" + getString(R.string.ponder) + ")";
            if (s.thinking) str += " (" + getString(R.string.thinking) + ")";
            if (s.analyzing) str += " (" + getString(R.string.analyzing) + ")";
            break;
        case WHITE_MATE:
            str = getString(R.string.white_mate);
            if(!isSinglePlayer) {
            	if(imFirstType == 0)
            		handleGMSMatchComplete(ConstantsData.GAME_LOSS);
            	else
            		handleGMSMatchComplete(ConstantsData.GAME_WON);
            }
            break;
        case BLACK_MATE:
            str = getString(R.string.black_mate);
        	if(imFirstType == 1)
        		handleGMSMatchComplete(ConstantsData.GAME_LOSS);
        	else
        		handleGMSMatchComplete(ConstantsData.GAME_WON);
            break;
        case WHITE_STALEMATE:
        	str = getString(R.string.stalemate);
            if(!isSinglePlayer)
            	handleGMSMatchComplete(ConstantsData.GAME_DRAW);
        case BLACK_STALEMATE:
            str = getString(R.string.stalemate);
            if(!isSinglePlayer)
            	handleGMSMatchComplete(ConstantsData.GAME_DRAW);
            break;
        case DRAW_REP: {
            str = getString(R.string.draw_rep);
            if (s.drawInfo.length() > 0)
                str = str + " [" + s.drawInfo + "]";
            if(!isSinglePlayer)
            	handleGMSMatchComplete(ConstantsData.GAME_DRAW);
            break;
        }
        case DRAW_50: {
            str = getString(R.string.draw_50);
            if (s.drawInfo.length() > 0)
                str = str + " [" + s.drawInfo + "]";
            break;
        }
        case DRAW_NO_MATE:
            str = getString(R.string.draw_no_mate);
            if(!isSinglePlayer)
            	handleGMSMatchComplete(ConstantsData.GAME_DRAW);
            break;
        case DRAW_AGREE:
            str = getString(R.string.draw_agree);
            if(!isSinglePlayer)
            	handleGMSMatchComplete(ConstantsData.GAME_DRAW);
            break;
        case RESIGN_WHITE:
        	str = getString(R.string.resign_white);
        	if(isSinglePlayer) {
        		str = getString(R.string.resign_white);
        	} else {
        		if(!myTurn && (imFirstType == 1) && isOpponentResign) {
        			str = getString(R.string.resign_white);
        			handleGMSMatchComplete(ConstantsData.GAME_WON);
        		} 
        		else if(!myTurn && (imFirstType == 1) && isOpponentTimeOut) {
        			str = getString(R.string.gms_black_win_time);
        			handleGMSMatchComplete(ConstantsData.GAME_WON);
        		}
        		else if(!myTurn && (imFirstType == 1)) {
        			str = getString(R.string.resign_black);
        			handleGMSMatchComplete(ConstantsData.GAME_LOSS);
        		}
        		else if(myTurn && (imFirstType == 0) && isOpponentResign) {
        			str = getString(R.string.resign_black);
        			handleGMSMatchComplete(ConstantsData.GAME_WON);
        		} 
        		else if(myTurn && (imFirstType == 0)) {
        			if((mSecondsLeft <= 0) && (gmsGameVariantNumber != ConstantsData.GAME_VARIANT_LONG))
        				str = getString(R.string.gms_black_win_time);
        			else
        				str = getString(R.string.resign_white);
        			handleGMSMatchComplete(ConstantsData.GAME_LOSS);
        		}
        	}
            break;
        case RESIGN_BLACK:
            str = getString(R.string.resign_black);
        	if(isSinglePlayer) {
        		str = getString(R.string.resign_black);
        	} else {
        		if(!myTurn && (imFirstType == 0) && isOpponentResign) {
        			str = getString(R.string.resign_black);
        			handleGMSMatchComplete(ConstantsData.GAME_WON);
        		}
        		else if(!myTurn && (imFirstType == 0) && isOpponentTimeOut) {
        			str = getString(R.string.gms_white_win_time);
        			handleGMSMatchComplete(ConstantsData.GAME_WON);
        		}
        		else if(!myTurn && (imFirstType == 0)) {
        			str = getString(R.string.resign_white);
        			handleGMSMatchComplete(ConstantsData.GAME_LOSS);
        		}
        		else if(myTurn && (imFirstType == 1) && isOpponentResign) {
        			str = getString(R.string.resign_white);
        			handleGMSMatchComplete(ConstantsData.GAME_WON);
        		}
        		else if(myTurn && (imFirstType == 1)) {
        			if((mSecondsLeft <= 0) && (gmsGameVariantNumber != ConstantsData.GAME_VARIANT_LONG))
        				str = getString(R.string.gms_white_win_time);
        			else
        				str = getString(R.string.resign_black);
        			handleGMSMatchComplete(ConstantsData.GAME_LOSS);
        		}
        	}
            break;
        default:
            throw new RuntimeException();
        }
        setStatusString(str);
    }

    private final void setStatusString(String str) {
        status.setText(str);
    }

    @Override
    public void moveListUpdated() {
        moveList.setText(gameTextListener.getSpannableData());
        Layout layout = moveList.getLayout();
        if (layout != null) {
            int currPos = gameTextListener.getCurrPos();
            int line = layout.getLineForOffset(currPos);
            int y = (int) ((line - 1.5) * moveList.getLineHeight());
            moveListScroll.scrollTo(0, y);
        }
    }

    @Override
    public boolean whiteBasedScores() {
        return mWhiteBasedScores;
    }

    @Override
    public boolean ponderMode() {
        return mPonderMode;
    }

    @Override
    public int engineThreads() {
        return mEngineThreads;
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public String playerName() {
        return playerName;
    }

    @Override
    public boolean discardVariations() {
        return discardVariations;
    }

    /** Report a move made that is a candidate for GUI animation. */
    public void setAnimMove(Position sourcePos, Move move, boolean forward) {
        if (animateMoves && (move != null))
            cb.setAnimMove(sourcePos, move, forward);
    }

    @Override
    public void setPosition(Position pos, String variantInfo, ArrayList<Move> variantMoves) {
        variantStr = variantInfo;
        this.variantMoves = variantMoves;
        cb.setPosition(pos);
        setBoardFlip();
        updateThinkingInfo();
        setEgtbHints(cb.getSelectedSquare());
    }

    private String thinkingStr1 = "";
    private String thinkingStr2 = "";
    private String bookInfoStr = "";
    private String variantStr = "";
    private ArrayList<ArrayList<Move>> pvMoves = new ArrayList<ArrayList<Move>>();
    private ArrayList<Move> bookMoves = null;
    private ArrayList<Move> variantMoves = null;

    @Override
    public void setThinkingInfo(String pvStr, String statStr, String bookInfo,
                                ArrayList<ArrayList<Move>> pvMoves, ArrayList<Move> bookMoves) {
        thinkingStr1 = pvStr;
        thinkingStr2 = statStr;
        bookInfoStr = bookInfo;
        this.pvMoves = pvMoves;
        this.bookMoves = bookMoves;
        updateThinkingInfo();

        if (ctrl.computerBusy()) {
            lastComputationMillis = System.currentTimeMillis();
        } else {
            lastComputationMillis = 0;
        }
        updateNotification();
    }

    private final void updateThinkingInfo() {
        boolean thinkingEmpty = true;
        {
            String s = "";
            if (mShowThinking || gameMode.analysisMode()) {
                s = thinkingStr1;
                if (s.length() > 0) thinkingEmpty = false;
                if (mShowStats) {
                    if (!thinkingEmpty)
                        s += "\n";
                    s += thinkingStr2;
                    if (s.length() > 0) thinkingEmpty = false;
                }
            }
            thinking.setText(s, TextView.BufferType.SPANNABLE);
        }
        if (mShowBookHints && (bookInfoStr.length() > 0)) {
            String s = "";
            if (!thinkingEmpty)
                s += "<br>";
            s += Util.boldStart + getString(R.string.book) + Util.boldStop + bookInfoStr;
            thinking.append(Html.fromHtml(s));
            thinkingEmpty = false;
        }
        if (showVariationLine && (variantStr.indexOf(' ') >= 0)) {
            String s = "";
            if (!thinkingEmpty)
                s += "<br>";
            s += Util.boldStart + getString(R.string.variation) + Util.boldStop + variantStr;
            thinking.append(Html.fromHtml(s));
            thinkingEmpty = false;
        }
        thinking.setVisibility(thinkingEmpty ? View.GONE : View.VISIBLE);

        List<Move> hints = null;
        if (mShowThinking || gameMode.analysisMode()) {
            ArrayList<ArrayList<Move>> pvMovesTmp = pvMoves;
            if (pvMovesTmp.size() == 1) {
                hints = pvMovesTmp.get(0);
            } else if (pvMovesTmp.size() > 1) {
                hints = new ArrayList<Move>();
                for (ArrayList<Move> pv : pvMovesTmp)
                    if (!pv.isEmpty())
                        hints.add(pv.get(0));
            }
        }
        if ((hints == null) && mShowBookHints)
            hints = bookMoves;
        if (((hints == null) || hints.isEmpty()) &&
            (variantMoves != null) && variantMoves.size() > 1) {
            hints = variantMoves;
        }
        if ((hints != null) && (hints.size() > maxNumArrows)) {
            hints = hints.subList(0, maxNumArrows);
        }
        cb.setMoveHints(hints);
    }

    static private final int PROMOTE_DIALOG = 0;
    static private final int BOARD_MENU_DIALOG = 1;
    static private final int ABOUT_DIALOG = 2;
    static private final int SELECT_MOVE_DIALOG = 3;
    static private final int SELECT_BOOK_DIALOG = 4;
    static private final int SELECT_ENGINE_DIALOG = 5;
    static private final int SELECT_ENGINE_DIALOG_NOMANAGE = 6;
    static private final int SELECT_PGN_FILE_DIALOG = 7;
    static private final int SELECT_PGN_FILE_SAVE_DIALOG = 8;
    static private final int SET_COLOR_THEME_DIALOG = 9;
    static private final int GAME_MODE_DIALOG = 10;
    static private final int SELECT_PGN_SAVE_NEWFILE_DIALOG = 11;
    static private final int MOVELIST_MENU_DIALOG = 12;
    static private final int THINKING_MENU_DIALOG = 13;
    static private final int GO_BACK_MENU_DIALOG = 14;
    static private final int GO_FORWARD_MENU_DIALOG = 15;
    static private final int FILE_MENU_DIALOG = 16;
    static private final int NEW_GAME_DIALOG = 17;
    static private final int CUSTOM1_BUTTON_DIALOG = 18;
    static private final int CUSTOM2_BUTTON_DIALOG = 19;
    static private final int CUSTOM3_BUTTON_DIALOG = 20;
    static private final int MANAGE_ENGINES_DIALOG = 21;
    static private final int NETWORK_ENGINE_DIALOG = 22;
    static private final int NEW_NETWORK_ENGINE_DIALOG = 23;
    static private final int NETWORK_ENGINE_CONFIG_DIALOG = 24;
    static private final int DELETE_NETWORK_ENGINE_DIALOG = 25;
    static private final int CLIPBOARD_DIALOG = 26;
    static private final int SELECT_FEN_FILE_DIALOG = 27;
    
    // gms
    static private final int NEW_GMS_GAME_DIALOG = 28;
    static private final int GAME_GMS_MODE_DIALOG = 29;
    static private final int GAME_GMS_DRAW_ASK = 30;
    static private final int GAME_GMS_AUTOMATCH_VARIANT_OPT = 31;
    static private final int GAME_GMS_INVITE_VARIANT_OPT = 32;
    static private final int GAME_GMS_EXIT = 33;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case NEW_GAME_DIALOG:                return newGameDialog();
        case PROMOTE_DIALOG:                 return promoteDialog();
        case BOARD_MENU_DIALOG:              return boardMenuDialog();
        case FILE_MENU_DIALOG:               return fileMenuDialog();
        case ABOUT_DIALOG:                   return aboutDialog();
        case SELECT_MOVE_DIALOG:             return selectMoveDialog();
        case SELECT_BOOK_DIALOG:             return selectBookDialog();
        case SELECT_ENGINE_DIALOG:           return selectEngineDialog(false);
        case SELECT_ENGINE_DIALOG_NOMANAGE:  return selectEngineDialog(true);
        case SELECT_PGN_FILE_DIALOG:         return selectPgnFileDialog();
        case SELECT_PGN_FILE_SAVE_DIALOG:    return selectPgnFileSaveDialog();
        case SELECT_PGN_SAVE_NEWFILE_DIALOG: return selectPgnSaveNewFileDialog();
        case SET_COLOR_THEME_DIALOG:         return setColorThemeDialog();
        case GAME_MODE_DIALOG:               return gameModeDialog();
        case MOVELIST_MENU_DIALOG:           return moveListMenuDialog();
        case THINKING_MENU_DIALOG:           return thinkingMenuDialog();
        case GO_BACK_MENU_DIALOG:            return goBackMenuDialog();
        case GO_FORWARD_MENU_DIALOG:         return goForwardMenuDialog();
        case CUSTOM1_BUTTON_DIALOG:          return makeButtonDialog(custom1ButtonActions);
        case CUSTOM2_BUTTON_DIALOG:          return makeButtonDialog(custom2ButtonActions);
        case CUSTOM3_BUTTON_DIALOG:          return makeButtonDialog(custom3ButtonActions);
        case MANAGE_ENGINES_DIALOG:          return manageEnginesDialog();
        case NETWORK_ENGINE_DIALOG:          return networkEngineDialog();
        case NEW_NETWORK_ENGINE_DIALOG:      return newNetworkEngineDialog();
        case NETWORK_ENGINE_CONFIG_DIALOG:   return networkEngineConfigDialog();
        case DELETE_NETWORK_ENGINE_DIALOG:   return deleteNetworkEngineDialog();
        case CLIPBOARD_DIALOG:               return clipBoardDialog();
        case SELECT_FEN_FILE_DIALOG:         return selectFenFileDialog();
        
        case NEW_GMS_GAME_DIALOG:            return newGmsGameDialog();
        case GAME_GMS_MODE_DIALOG:           return gameGmsModeDialog();
        case GAME_GMS_DRAW_ASK:              return drawGmsGameDialog();
        case GAME_GMS_AUTOMATCH_VARIANT_OPT: return gameGmsAutoMatchVariantDialog();
        case GAME_GMS_INVITE_VARIANT_OPT:    return gameGmsInviteVariantDialog();
        case GAME_GMS_EXIT:                  return exitGmsGameDialog();
        }
        return null;
    }

    private final Dialog newGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.option_new_game);
        builder.setMessage(R.string.start_new_game);
        builder.setPositiveButton(R.string.yes, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame(2);
            }
        });
        builder.setNeutralButton(R.string.white, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame(0);
            }
        });
        builder.setNegativeButton(R.string.black, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame(1);
            }
        });
        return builder.create();
    }

    private final void startNewGame(int type) {
        if (type != 2) {
            int gameModeType = (type == 0) ? GameMode.PLAYER_WHITE : GameMode.PLAYER_BLACK;
            Editor editor = settings.edit();
            String gameModeStr = String.format(Locale.US, "%d", gameModeType);
            editor.putString("gameMode", gameModeStr);
            editor.commit();
            gameMode = new GameMode(gameModeType);
        }
//        savePGNToFile(".autosave.pgn", true);
        TimeControlData tcData = new TimeControlData();
        tcData.setTimeControl(timeControl, movesPerSession, timeIncrement);
        ctrl.newGame(gameMode, tcData);
        ctrl.startGame();
        setBoardFlip(true);
        updateEngineTitle();
    }

    private final Dialog promoteDialog() {
        final CharSequence[] items = {
            getString(R.string.queen), getString(R.string.rook),
            getString(R.string.bishop), getString(R.string.knight)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.promote_pawn_to);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ctrl.reportPromotePiece(item);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog clipBoardDialog() {
        final int COPY_GAME      = 0;
        final int COPY_POSITION  = 1;
        final int PASTE          = 2;

        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.copy_game));     actions.add(COPY_GAME);
        lst.add(getString(R.string.copy_position)); actions.add(COPY_POSITION);
        lst.add(getString(R.string.paste));         actions.add(PASTE);
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tools_menu);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case COPY_GAME: {
                    String pgn = ctrl.getPGN();
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setText(pgn);
                    break;
                }
                case COPY_POSITION: {
                    String fen = ctrl.getFEN() + "\n";
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setText(fen);
                    break;
                }
                case PASTE: {
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard.hasText()) {
                        String fenPgn = clipboard.getText().toString();
                        try {
                            ctrl.setFENOrPGN(fenPgn);
                            setBoardFlip(true);
                        } catch (ChessParseError e) {
                            Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                }
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog boardMenuDialog() {
        final int CLIPBOARD = 0;
        final int FILEMENU  = 1;
        final int SHARE     = 2;
        final int GET_FEN   = 3;

        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.clipboard));     actions.add(CLIPBOARD);
        lst.add(getString(R.string.option_file));   actions.add(FILEMENU);
        lst.add(getString(R.string.share));         actions.add(SHARE);
        if (hasFenProvider(getPackageManager())) {
            lst.add(getString(R.string.get_fen)); actions.add(GET_FEN);
        }
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tools_menu);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case CLIPBOARD: {
                    showDialog(CLIPBOARD_DIALOG);
                    break;
                }
                case FILEMENU: {
                    removeDialog(FILE_MENU_DIALOG);
                    showDialog(FILE_MENU_DIALOG);
                    break;
                }
                case SHARE: {
                    shareGame();
                    break;
                }
                case GET_FEN:
                    getFen();
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final void shareGame() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        i.setType("text/plain");
        //i.putExtra(Intent.EXTRA_TEXT, ctrl.getPGN());
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name) + " " + ConstantsData.MARKET_URL_HTTP);
        startActivity(Intent.createChooser(i, getString(R.string.share_pgn_game)));
    }

    private final Dialog fileMenuDialog() {
        final int LOAD_LAST_FILE = 0;
        final int LOAD_GAME      = 1;
        final int LOAD_POS       = 2;
        final int LOAD_SCID_GAME = 3;
        final int SAVE_GAME      = 4;

        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        if (currFileType() != FT_NONE) {
            lst.add(getString(R.string.load_last_file)); actions.add(LOAD_LAST_FILE);
        }
        lst.add(getString(R.string.load_game));     actions.add(LOAD_GAME);
        lst.add(getString(R.string.load_position)); actions.add(LOAD_POS);
        if (hasScidProvider()) {
            lst.add(getString(R.string.load_scid_game)); actions.add(LOAD_SCID_GAME);
        }
        lst.add(getString(R.string.save_game));     actions.add(SAVE_GAME);
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.load_save_menu);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case LOAD_LAST_FILE:
                    loadLastFile();
                    break;
                case LOAD_GAME:
                    selectFile(R.string.select_pgn_file, R.string.pgn_load, "currentPGNFile", pgnDir,
                                  SELECT_PGN_FILE_DIALOG, RESULT_OI_PGN_LOAD);
                    break;
                case SAVE_GAME:
                    selectFile(R.string.select_pgn_file_save, R.string.pgn_save, "currentPGNFile", pgnDir,
                                  SELECT_PGN_FILE_SAVE_DIALOG, RESULT_OI_PGN_SAVE);
                    break;
                case LOAD_POS:
                    selectFile(R.string.select_fen_file, R.string.pgn_load, "currentFENFile", fenDir,
                                  SELECT_FEN_FILE_DIALOG, RESULT_OI_FEN_LOAD);
                    break;
                case LOAD_SCID_GAME:
                    selectScidFile();
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    /** Open dialog to select a game/position from the last used file. */
    final private void loadLastFile() {
        String path = currPathName();
        if (path.length() == 0)
            return;
        switch (currFileType()) {
        case FT_PGN:
            loadPGNFromFile(path);
            break;
        case FT_SCID: {
            Intent data = new Intent(path);
            onActivityResult(RESULT_SELECT_SCID, RESULT_OK, data);
            break;
        }
        case FT_FEN:
            loadFENFromFile(path);
            break;
        }
    }

    private final Dialog aboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = getString(R.string.app_name);
        WebView wv = new WebView(this);
        builder.setView(wv);
        InputStream is = getResources().openRawResource(R.raw.about);
        String data = Util.readFromStream(is);
        if (data == null)
            data = "";
        try { is.close(); } catch (IOException e1) {}
        wv.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo("com.if3games.chessonline", 0);
            title += " " + pi.versionName;
        } catch (NameNotFoundException e) {
        }
        builder.setTitle(title);
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog selectMoveDialog() {
        View content = View.inflate(this, R.layout.select_move_number, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(content);
        builder.setTitle(R.string.goto_move);
        final EditText moveNrView = (EditText)content.findViewById(R.id.selmove_number);
        moveNrView.setText("1");
        final Runnable gotoMove = new Runnable() {
            public void run() {
                try {
                    int moveNr = Integer.parseInt(moveNrView.getText().toString());
                    ctrl.gotoMove(moveNr);
                } catch (NumberFormatException nfe) {
                    Toast.makeText(getApplicationContext(), R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
                }
            }
        };
        builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                gotoMove.run();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();

        moveNrView.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    gotoMove.run();
                    dialog.cancel();
                    return true;
                }
                return false;
            }
        });
        return dialog;
    }

    private final Dialog selectBookDialog() {
        String[] fileNames = findFilesInDirectory(bookDir, new FileNameFilter() {
            @Override
            public boolean accept(String filename) {
                int dotIdx = filename.lastIndexOf(".");
                if (dotIdx < 0)
                    return false;
                String ext = filename.substring(dotIdx+1);
                return (ext.equals("ctg") || ext.equals("bin"));
            }
        });
        final int numFiles = fileNames.length;
        CharSequence[] items = new CharSequence[numFiles + 1];
        for (int i = 0; i < numFiles; i++)
            items[i] = fileNames[i];
        items[numFiles] = getString(R.string.internal_book);
        final CharSequence[] finalItems = items;
        int defaultItem = numFiles;
        for (int i = 0; i < numFiles; i++) {
            if (bookOptions.filename.equals(items[i])) {
                defaultItem = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_opening_book_file);
        builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Editor editor = settings.edit();
                String bookFile = "";
                if (item < numFiles)
                    bookFile = finalItems[item].toString();
                editor.putString("bookFile", bookFile);
                editor.commit();
                bookOptions.filename = bookFile;
                setBookOptions();
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final static boolean internalEngine(String name) {
        return "cuckoochess".equals(name) ||
               "stockfish".equals(name);
    }

    private final Dialog selectEngineDialog(final boolean abortOnCancel) {
        String[] fileNames = findFilesInDirectory(engineDir, new FileNameFilter() {
            @Override
            public boolean accept(String filename) {
                return !internalEngine(filename);
            }
        });
        final int numFiles = fileNames.length;
        boolean haveSf = EngineUtil.internalStockFishName() != null;
        final int nEngines = numFiles + 1 + (haveSf ? 1 : 0);
        final String[] items = new String[nEngines];
        final String[] ids = new String[nEngines];
        int idx = 0;
        if (haveSf) {
            ids[idx] = "stockfish"; items[idx] = getString(R.string.stockfish_engine); idx++;
        }
        ids[idx] = "cuckoochess"; items[idx] = getString(R.string.cuckoochess_engine); idx++;
        String sep = File.separator;
        String base = Environment.getExternalStorageDirectory() + sep + engineDir + sep;
        for (int i = 0; i < numFiles; i++) {
            ids[idx] = base + fileNames[i];
            items[idx] = fileNames[i];
            idx++;
        }
        String currEngine = ctrl.getEngine();
        int defaultItem = 0;
        for (int i = 0; i < nEngines; i++) {
            if (ids[i].equals(currEngine)) {
                defaultItem = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_chess_engine);
        builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if ((item < 0) || (item >= nEngines))
                    return;
                Editor editor = settings.edit();
                String engine = ids[item];
                editor.putString("engine", engine);
                editor.commit();
                dialog.dismiss();
                int strength = settings.getInt("strength", 1000);
                setEngineOptions(false);
                setEngineStrength(engine, strength);
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!abortOnCancel) {
                    removeDialog(MANAGE_ENGINES_DIALOG);
                    showDialog(MANAGE_ENGINES_DIALOG);
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private static interface Loader {
        void load(String pathName);
    }

    private final Dialog selectPgnFileDialog() {
        return selectFileDialog(pgnDir, R.string.select_pgn_file, R.string.no_pgn_files,
                                "currentPGNFile", new Loader() {
            @Override
            public void load(String pathName) {
                loadPGNFromFile(pathName);
            }
        });
    }

    private final Dialog selectFenFileDialog() {
        return selectFileDialog(fenDir, R.string.select_fen_file, R.string.no_fen_files,
                                "currentFENFile", new Loader() {
            @Override
            public void load(String pathName) {
                loadFENFromFile(pathName);
            }
        });
    }

    private final Dialog selectFileDialog(final String defaultDir, int selectFileMsg, int noFilesMsg,
                                          String settingsName, final Loader loader) {
        final String[] fileNames = findFilesInDirectory(defaultDir, null);
        final int numFiles = fileNames.length;
        if (numFiles == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name).setMessage(noFilesMsg);
            AlertDialog alert = builder.create();
            return alert;
        }
        int defaultItem = 0;
        String currentFile = settings.getString(settingsName, "");
        currentFile = new File(currentFile).getName();
        for (int i = 0; i < numFiles; i++) {
            if (currentFile.equals(fileNames[i])) {
                defaultItem = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectFileMsg);
        builder.setSingleChoiceItems(fileNames, defaultItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                String sep = File.separator;
                String fn = fileNames[item].toString();
                String pathName = Environment.getExternalStorageDirectory() + sep + defaultDir + sep + fn;
                loader.load(pathName);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog selectPgnFileSaveDialog() {
        final String[] fileNames = findFilesInDirectory(pgnDir, null);
        final int numFiles = fileNames.length;
        int defaultItem = 0;
        String currentPGNFile = settings.getString("currentPGNFile", "");
        currentPGNFile = new File(currentPGNFile).getName();
        for (int i = 0; i < numFiles; i++) {
            if (currentPGNFile.equals(fileNames[i])) {
                defaultItem = i;
                break;
            }
        }
        CharSequence[] items = new CharSequence[numFiles + 1];
        for (int i = 0; i < numFiles; i++)
            items[i] = fileNames[i];
        items[numFiles] = getString(R.string.new_file);
        final CharSequence[] finalItems = items;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_pgn_file_save);
        builder.setSingleChoiceItems(finalItems, defaultItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                String pgnFile;
                if (item >= numFiles) {
                    dialog.dismiss();
                    showDialog(SELECT_PGN_SAVE_NEWFILE_DIALOG);
                } else {
                    dialog.dismiss();
                    pgnFile = fileNames[item].toString();
                    String sep = File.separator;
                    String pathName = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + pgnFile;
                    savePGNToFile(pathName, false);
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog selectPgnSaveNewFileDialog() {
        View content = View.inflate(this, R.layout.create_pgn_file, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(content);
        builder.setTitle(R.string.select_pgn_file_save);
        final EditText fileNameView = (EditText)content.findViewById(R.id.create_pgn_filename);
        fileNameView.setText("");
        final Runnable savePGN = new Runnable() {
            public void run() {
                String pgnFile = fileNameView.getText().toString();
                if ((pgnFile.length() > 0) && !pgnFile.contains("."))
                    pgnFile += ".pgn";
                String sep = File.separator;
                String pathName = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + pgnFile;
                savePGNToFile(pathName, false);
            }
        };
        builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                savePGN.run();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        final Dialog dialog = builder.create();
        fileNameView.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    savePGN.run();
                    dialog.cancel();
                    return true;
                }
                return false;
            }
        });
        return dialog;
    }

    private final Dialog setColorThemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_color_theme);
        String[] themeNames = new String[ColorTheme.themeNames.length];
        for (int i = 0; i < themeNames.length; i++)
            themeNames[i] = getString(ColorTheme.themeNames[i]);
        builder.setSingleChoiceItems(themeNames, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ColorTheme.instance().setTheme(settings, item);
                cb.setColors();
                gameTextListener.clear();
                ctrl.prefsChanged(false);
                dialog.dismiss();
                Util.overrideFonts(findViewById(android.R.id.content));
            }
        });
        return builder.create();
    }

    private final Dialog gameModeDialog() {
        final CharSequence[] items = {
            getString(R.string.analysis_mode),
            getString(R.string.edit_replay_game),
            getString(R.string.play_white),
            getString(R.string.play_black),
            getString(R.string.two_players),
            getString(R.string.comp_vs_comp)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_game_mode);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                int gameModeType = -1;
                /* only flip site in case the player was specified resp. changed */
                boolean flipSite = false;
                switch (item) {
                case 0: gameModeType = GameMode.ANALYSIS;      break;
                case 1: gameModeType = GameMode.EDIT_GAME;     break;
                case 2: gameModeType = GameMode.PLAYER_WHITE; flipSite = true; break;
                case 3: gameModeType = GameMode.PLAYER_BLACK; flipSite = true; break;
                case 4: gameModeType = GameMode.TWO_PLAYERS;   break;
                case 5: gameModeType = GameMode.TWO_COMPUTERS; break;
                default: break;
                }
                dialog.dismiss();
                if (gameModeType >= 0) {
                    Editor editor = settings.edit();
                    String gameModeStr = String.format(Locale.US, "%d", gameModeType);
                    editor.putString("gameMode", gameModeStr);
                    editor.commit();
                    gameMode = new GameMode(gameModeType);
                    ctrl.setGameMode(gameMode);
                    setBoardFlip(flipSite);
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog moveListMenuDialog() {
        final int EDIT_HEADERS   = 0;
        final int EDIT_COMMENTS  = 1;
        final int REMOVE_SUBTREE = 2;
        final int MOVE_VAR_UP    = 3;
        final int MOVE_VAR_DOWN  = 4;
        final int ADD_NULL_MOVE  = 5;

        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.edit_headers));      actions.add(EDIT_HEADERS);
        if (ctrl.humansTurn()) {
            lst.add(getString(R.string.edit_comments)); actions.add(EDIT_COMMENTS);
        }
        lst.add(getString(R.string.truncate_gametree)); actions.add(REMOVE_SUBTREE);
        if (ctrl.numVariations() > 1) {
            lst.add(getString(R.string.move_var_up));   actions.add(MOVE_VAR_UP);
            lst.add(getString(R.string.move_var_down)); actions.add(MOVE_VAR_DOWN);
        }

        boolean allowNullMove =
            gameMode.analysisMode() ||
            (gameMode.playerWhite() && gameMode.playerBlack() && !gameMode.clocksActive());
        if (allowNullMove) {
            lst.add(getString(R.string.add_null_move)); actions.add(ADD_NULL_MOVE);
        }
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_game);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case EDIT_HEADERS: {
                    final TreeMap<String,String> headers = new TreeMap<String,String>();
                    ctrl.getHeaders(headers);

                    AlertDialog.Builder builder = new AlertDialog.Builder(DroidFish.this);
                    builder.setTitle(R.string.edit_headers);
                    View content = View.inflate(DroidFish.this, R.layout.edit_headers, null);
                    builder.setView(content);

                    final TextView event, site, date, round, white, black;

                    event = (TextView)content.findViewById(R.id.ed_header_event);
                    site = (TextView)content.findViewById(R.id.ed_header_site);
                    date = (TextView)content.findViewById(R.id.ed_header_date);
                    round = (TextView)content.findViewById(R.id.ed_header_round);
                    white = (TextView)content.findViewById(R.id.ed_header_white);
                    black = (TextView)content.findViewById(R.id.ed_header_black);

                    event.setText(headers.get("Event"));
                    site .setText(headers.get("Site"));
                    date .setText(headers.get("Date"));
                    round.setText(headers.get("Round"));
                    white.setText(headers.get("White"));
                    black.setText(headers.get("Black"));

                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            headers.put("Event", event.getText().toString().trim());
                            headers.put("Site",  site .getText().toString().trim());
                            headers.put("Date",  date .getText().toString().trim());
                            headers.put("Round", round.getText().toString().trim());
                            headers.put("White", white.getText().toString().trim());
                            headers.put("Black", black.getText().toString().trim());
                            ctrl.setHeaders(headers);
                            setBoardFlip(true);
                        }
                    });

                    builder.show();
                    break;
                }
                case EDIT_COMMENTS: {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DroidFish.this);
                    builder.setTitle(R.string.edit_comments);
                    View content = View.inflate(DroidFish.this, R.layout.edit_comments, null);
                    builder.setView(content);

                    DroidChessController.CommentInfo commInfo = ctrl.getComments();

                    final TextView preComment, moveView, nag, postComment;
                    preComment = (TextView)content.findViewById(R.id.ed_comments_pre);
                    moveView = (TextView)content.findViewById(R.id.ed_comments_move);
                    nag = (TextView)content.findViewById(R.id.ed_comments_nag);
                    postComment = (TextView)content.findViewById(R.id.ed_comments_post);

                    preComment.setText(commInfo.preComment);
                    postComment.setText(commInfo.postComment);
                    moveView.setText(commInfo.move);
                    String nagStr = Node.nagStr(commInfo.nag).trim();
                    if ((nagStr.length() == 0) && (commInfo.nag > 0))
                        nagStr = String.format(Locale.US, "%d", commInfo.nag);
                    nag.setText(nagStr);

                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String pre = preComment.getText().toString().trim();
                            String post = postComment.getText().toString().trim();
                            int nagVal = Node.strToNag(nag.getText().toString());

                            DroidChessController.CommentInfo commInfo = new DroidChessController.CommentInfo();
                            commInfo.preComment = pre;
                            commInfo.postComment = post;
                            commInfo.nag = nagVal;
                            ctrl.setComments(commInfo);
                        }
                    });

                    builder.show();
                    break;
                }
                case REMOVE_SUBTREE:
                    ctrl.removeSubTree();
                    break;
                case MOVE_VAR_UP:
                    ctrl.moveVariation(-1);
                    break;
                case MOVE_VAR_DOWN:
                    ctrl.moveVariation(1);
                    break;
                case ADD_NULL_MOVE:
                    ctrl.makeHumanNullMove();
                    break;
                }
                moveListMenuDlg = null;
            }
        });
        AlertDialog alert = builder.create();
        moveListMenuDlg = alert;
        return alert;
    }

    private final Dialog thinkingMenuDialog() {
        final int ADD_ANALYSIS = 0;
        final int MULTIPV_DEC = 1;
        final int MULTIPV_INC = 2;
        final int HIDE_STATISTICS = 3;
        final int SHOW_STATISTICS = 4;
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.add_analysis)); actions.add(ADD_ANALYSIS);
        final int numPV = ctrl.getNumPV();
        if (gameMode.analysisMode()) {
            int maxPV = ctrl.maxPV();
            if (numPV > 1) {
                lst.add(getString(R.string.fewer_variations)); actions.add(MULTIPV_DEC);
            }
            if (numPV < maxPV) {
                lst.add(getString(R.string.more_variations)); actions.add(MULTIPV_INC);
            }
        }
        if (thinkingStr1.length() > 0) {
            if (mShowStats) {
                lst.add(getString(R.string.hide_statistics)); actions.add(HIDE_STATISTICS);
            } else {
                lst.add(getString(R.string.show_statistics)); actions.add(SHOW_STATISTICS);
            }
        }
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.analysis);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case ADD_ANALYSIS: {
                    ArrayList<ArrayList<Move>> pvMovesTmp = pvMoves;
                    String[] pvStrs = thinkingStr1.split("\n");
                    for (int i = 0; i < pvMovesTmp.size(); i++) {
                        ArrayList<Move> pv = pvMovesTmp.get(i);
                        StringBuilder preComment = new StringBuilder();
                        if (i < pvStrs.length) {
                            String[] tmp = pvStrs[i].split(" ");
                            for (int j = 0; j < 2; j++) {
                                if (j < tmp.length) {
                                    if (j > 0) preComment.append(' ');
                                    preComment.append(tmp[j]);
                                }
                            }
                            if (preComment.length() > 0) preComment.append(':');
                        }
                        boolean updateDefault = (i == 0);
                        ctrl.addVariation(preComment.toString(), pv, updateDefault);
                    }
                    break;
                }
                case MULTIPV_DEC:
                    ctrl.setMultiPVMode(numPV - 1);
                    break;
                case MULTIPV_INC:
                    ctrl.setMultiPVMode(numPV + 1);
                    break;
                case HIDE_STATISTICS:
                case SHOW_STATISTICS: {
                    mShowStats = finalActions.get(item) == SHOW_STATISTICS;
                    Editor editor = settings.edit();
                    editor.putBoolean("showStats", mShowStats);
                    editor.commit();
                    updateThinkingInfo();
                    break;
                }
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog goBackMenuDialog() {
        final int GOTO_START_GAME = 0;
        final int GOTO_START_VAR  = 1;
        final int GOTO_PREV_VAR   = 2;
        final int LOAD_PREV_GAME  = 3;

        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.goto_start_game));      actions.add(GOTO_START_GAME);
        lst.add(getString(R.string.goto_start_variation)); actions.add(GOTO_START_VAR);
        if (ctrl.currVariation() > 0) {
            lst.add(getString(R.string.goto_prev_variation)); actions.add(GOTO_PREV_VAR);
        }
        final int currFT = currFileType();
        final String currPathName = currPathName();
        if ((currFT != FT_NONE) && !gameMode.clocksActive()) {
            lst.add(getString(R.string.load_prev_game)); actions.add(LOAD_PREV_GAME);
        }
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.go_back);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case GOTO_START_GAME: ctrl.gotoMove(0); break;
                case GOTO_START_VAR:  ctrl.gotoStartOfVariation(); break;
                case GOTO_PREV_VAR:   ctrl.changeVariation(-1); break;
                case LOAD_PREV_GAME:
                    Intent i;
                    if (currFT == FT_PGN) {
                        i = new Intent(DroidFish.this, EditPGNLoad.class);
                        i.setAction("com.if3games.chessonline.loadFilePrevGame");
                        i.putExtra("com.if3games.chessonline.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_SCID) {
                        i = new Intent(DroidFish.this, LoadScid.class);
                        i.setAction("com.if3games.chessonline.loadScidPrevGame");
                        i.putExtra("com.if3games.chessonline.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_FEN) {
                        i = new Intent(DroidFish.this, LoadFEN.class);
                        i.setAction("com.if3games.chessonline.loadPrevFen");
                        i.putExtra("com.if3games.chessonline.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_FEN);
                    }
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog goForwardMenuDialog() {
        final int GOTO_END_VAR   = 0;
        final int GOTO_NEXT_VAR  = 1;
        final int LOAD_NEXT_GAME = 2;

        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.goto_end_variation)); actions.add(GOTO_END_VAR);
        if (ctrl.currVariation() < ctrl.numVariations() - 1) {
            lst.add(getString(R.string.goto_next_variation)); actions.add(GOTO_NEXT_VAR);
        }
        final int currFT = currFileType();
        final String currPathName = currPathName();
        if ((currFT != FT_NONE) && !gameMode.clocksActive()) {
            lst.add(getString(R.string.load_next_game)); actions.add(LOAD_NEXT_GAME);
        }
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.go_forward);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case GOTO_END_VAR:  ctrl.gotoMove(Integer.MAX_VALUE); break;
                case GOTO_NEXT_VAR: ctrl.changeVariation(1); break;
                case LOAD_NEXT_GAME:
                    Intent i;
                    if (currFT == FT_PGN) {
                        i = new Intent(DroidFish.this, EditPGNLoad.class);
                        i.setAction("com.if3games.chessonline.loadFileNextGame");
                        i.putExtra("com.if3games.chessonline.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_SCID) {
                        i = new Intent(DroidFish.this, LoadScid.class);
                        i.setAction("com.if3games.chessonline.loadScidNextGame");
                        i.putExtra("com.if3games.chessonline.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_FEN) {
                        i = new Intent(DroidFish.this, LoadFEN.class);
                        i.setAction("com.if3games.chessonline.loadNextFen");
                        i.putExtra("com.if3games.chessonline.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_FEN);
                    }
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private Dialog makeButtonDialog(ButtonActions buttonActions) {
        List<CharSequence> names = new ArrayList<CharSequence>();
        final List<UIAction> actions = new ArrayList<UIAction>();

        HashSet<String> used = new HashSet<String>();
        for (UIAction a : buttonActions.getMenuActions()) {
            if ((a != null) && a.enabled() && !used.contains(a.getId())) {
                names.add(getString(a.getName()));
                actions.add(a);
                used.add(a.getId());
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(buttonActions.getMenuTitle());
        builder.setItems(names.toArray(new CharSequence[names.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                UIAction a = actions.get(item);
                a.run();
            }
        });
        return builder.create();
    }

    private final Dialog manageEnginesDialog() {
        final CharSequence[] items = {
                getString(R.string.select_engine),
                getString(R.string.configure_network_engine)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.option_manage_engines);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                case 0:
                    removeDialog(SELECT_ENGINE_DIALOG);
                    showDialog(SELECT_ENGINE_DIALOG);
                    break;
                case 1:
                    removeDialog(NETWORK_ENGINE_DIALOG);
                    showDialog(NETWORK_ENGINE_DIALOG);
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog networkEngineDialog() {
        String[] fileNames = findFilesInDirectory(engineDir, new FileNameFilter() {
            @Override
            public boolean accept(String filename) {
                if (internalEngine(filename))
                    return false;
                try {
                    InputStream inStream = new FileInputStream(filename);
                    InputStreamReader inFile = new InputStreamReader(inStream);
                    char[] buf = new char[4];
                    boolean ret = (inFile.read(buf) == 4) && "NETE".equals(new String(buf));
                    inFile.close();
                    return ret;
                } catch (IOException e) {
                    return false;
                }
            }
        });
        final int numFiles = fileNames.length;
        final int numItems = numFiles + 1;
        final String[] items = new String[numItems];
        final String[] ids = new String[numItems];
        int idx = 0;
        String sep = File.separator;
        String base = Environment.getExternalStorageDirectory() + sep + engineDir + sep;
        for (int i = 0; i < numFiles; i++) {
            ids[idx] = base + fileNames[i];
            items[idx] = fileNames[i];
            idx++;
        }
        ids[idx] = ""; items[idx] = getString(R.string.new_engine); idx++;
        String currEngine = ctrl.getEngine();
        int defaultItem = 0;
        for (int i = 0; i < numItems; i++)
            if (ids[i].equals(currEngine)) {
                defaultItem = i;
                break;
            }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.configure_network_engine);
        builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if ((item < 0) || (item >= numItems))
                    return;
                dialog.dismiss();
                if (item == numItems - 1) {
                    showDialog(NEW_NETWORK_ENGINE_DIALOG);
                } else {
                    networkEngineToConfig = ids[item];
                    removeDialog(NETWORK_ENGINE_CONFIG_DIALOG);
                    showDialog(NETWORK_ENGINE_CONFIG_DIALOG);
                }
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(MANAGE_ENGINES_DIALOG);
                showDialog(MANAGE_ENGINES_DIALOG);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    // Filename of network engine to configure
    private String networkEngineToConfig = "";

    // Ask for name of new network engine
    private final Dialog newNetworkEngineDialog() {
        View content = View.inflate(this, R.layout.create_network_engine, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(content);
        builder.setTitle(R.string.create_network_engine);
        final EditText engineNameView = (EditText)content.findViewById(R.id.create_network_engine);
        engineNameView.setText("");
        final Runnable createEngine = new Runnable() {
            public void run() {
                String engineName = engineNameView.getText().toString();
                String sep = File.separator;
                String pathName = Environment.getExternalStorageDirectory() + sep + engineDir + sep + engineName;
                File file = new File(pathName);
                boolean nameOk = true;
                int errMsg = -1;
                if (engineName.contains("/")) {
                    nameOk = false;
                    errMsg = R.string.slash_not_allowed;
                } else if (internalEngine(engineName) || file.exists()) {
                    nameOk = false;
                    errMsg = R.string.engine_name_in_use;
                }
                if (!nameOk) {
                    Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
                    removeDialog(NETWORK_ENGINE_DIALOG);
                    showDialog(NETWORK_ENGINE_DIALOG);
                    return;
                }
                networkEngineToConfig = pathName;
                removeDialog(NETWORK_ENGINE_CONFIG_DIALOG);
                showDialog(NETWORK_ENGINE_CONFIG_DIALOG);
            }
        };
        builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                createEngine.run();
            }
        });
        builder.setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });

        final Dialog dialog = builder.create();
        engineNameView.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    createEngine.run();
                    dialog.cancel();
                    return true;
                }
                return false;
            }
        });
        return dialog;
    }

    // Configure network engine settings
    private final Dialog networkEngineConfigDialog() {
        View content = View.inflate(this, R.layout.network_engine_config, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(content);
        builder.setTitle(R.string.configure_network_engine);
        final EditText hostNameView = (EditText)content.findViewById(R.id.network_engine_host);
        final EditText portView = (EditText)content.findViewById(R.id.network_engine_port);
        String hostName = "";
        String port = "0";
        try {
            String[] lines = Util.readFile(networkEngineToConfig);
            if ((lines.length >= 1) && lines[0].equals("NETE")) {
                if (lines.length > 1)
                    hostName = lines[1];
                if (lines.length > 2)
                    port = lines[2];
            }
        } catch (IOException e1) {
        }
        hostNameView.setText(hostName);
        portView.setText(port);
        final Runnable writeConfig = new Runnable() {
            public void run() {
                String hostName = hostNameView.getText().toString();
                String port = portView.getText().toString();
                try {
                    FileWriter fw = new FileWriter(new File(networkEngineToConfig), false);
                    fw.write("NETE\n");
                    fw.write(hostName); fw.write("\n");
                    fw.write(port); fw.write("\n");
                    fw.close();
                    setEngineOptions(true);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
        builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeConfig.run();
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setNeutralButton(R.string.delete, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(DELETE_NETWORK_ENGINE_DIALOG);
                showDialog(DELETE_NETWORK_ENGINE_DIALOG);
            }
        });

        final Dialog dialog = builder.create();
        portView.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    writeConfig.run();
                    dialog.cancel();
                    removeDialog(NETWORK_ENGINE_DIALOG);
                    showDialog(NETWORK_ENGINE_DIALOG);
                    return true;
                }
                return false;
            }
        });
        return dialog;
    }

    private Dialog deleteNetworkEngineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_network_engine);
        String msg = networkEngineToConfig;
        if (msg.lastIndexOf('/') >= 0)
            msg = msg.substring(msg.lastIndexOf('/')+1);
        builder.setMessage(getString(R.string.network_engine) + ": " + msg);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                new File(networkEngineToConfig).delete();
                String engine = settings.getString("engine", "stockfish");
                if (engine.equals(networkEngineToConfig)) {
                    engine = "stockfish";
                    Editor editor = settings.edit();
                    editor.putString("engine", engine);
                    editor.commit();
                    dialog.dismiss();
                    int strength = settings.getInt("strength", 1000);
                    setEngineOptions(false);
                    setEngineStrength(engine, strength);
                }
                dialog.cancel();
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    /** Open a load/save file dialog. Uses OI file manager if available. */
    private void selectFile(int titleMsg, int buttonMsg, String settingsName, String defaultDir,
                            int dialog, int result) {
        String action = "org.openintents.action.PICK_FILE";
        Intent i = new Intent(action);
        String currentFile = settings.getString(settingsName, "");
        String sep = File.separator;
        if (!currentFile.contains(sep))
            currentFile = Environment.getExternalStorageDirectory() +
                          sep + defaultDir + sep + currentFile;
        i.setData(Uri.fromFile(new File(currentFile)));
        i.putExtra("org.openintents.extra.TITLE", getString(titleMsg));
        i.putExtra("org.openintents.extra.BUTTON_TEXT", getString(buttonMsg));
        try {
            startActivityForResult(i, result);
        } catch (ActivityNotFoundException e) {
            removeDialog(dialog);
            showDialog(dialog);
        }
    }

    private final boolean hasScidProvider() {
        List<ProviderInfo> providers = getPackageManager().queryContentProviders(null, 0, 0);
        for (ProviderInfo info : providers)
            if (info.authority.equals("org.scid.database.scidprovider"))
                return true;
        return false;
    }

    private final void selectScidFile() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("org.scid.android",
                                              "org.scid.android.SelectFileActivity"));
        intent.setAction(".si4");
        try {
            startActivityForResult(intent, RESULT_SELECT_SCID);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public final static boolean hasFenProvider(PackageManager manager) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); 
        i.setType("application/x-chess-fen");
        List<ResolveInfo> resolvers = manager.queryIntentActivities(i, 0);
        return (resolvers != null) && (resolvers.size() > 0);
    }

    private final void getFen() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); 
        i.setType("application/x-chess-fen");
        try {
            startActivityForResult(i, RESULT_GET_FEN);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    final static int FT_NONE = 0;
    final static int FT_PGN  = 1;
    final static int FT_SCID = 2;
    final static int FT_FEN  = 3;

    private final int currFileType() {
        return settings.getInt("currFT", FT_NONE);
    }

    /** Return path name for the last used PGN or SCID file. */
    private final String currPathName() {
        int ft = settings.getInt("currFT", FT_NONE);
        switch (ft) {
        case FT_PGN: {
            String ret = settings.getString("currentPGNFile", "");
            String sep = File.separator;
            if (!ret.contains(sep))
                ret = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + ret;
            return ret;
        }
        case FT_SCID:
            return settings.getString("currentScidFile", "");
        case FT_FEN:
            return settings.getString("currentFENFile", "");
        default:
            return "";
        }
    }

    private static interface FileNameFilter {
        boolean accept(String filename);
    }

    private final String[] findFilesInDirectory(String dirName, final FileNameFilter filter) {
        File extDir = Environment.getExternalStorageDirectory();
        String sep = File.separator;
        File dir = new File(extDir.getAbsolutePath() + sep + dirName);
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (!pathname.isFile())
                    return false;
                return (filter == null) || filter.accept(pathname.getAbsolutePath());
            }
        });
        if (files == null)
            files = new File[0];
        final int numFiles = files.length;
        String[] fileNames = new String[numFiles];
        for (int i = 0; i < files.length; i++)
            fileNames[i] = files[i].getName();
        Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
        return fileNames;
    }

    /** Save current game to a PGN file. */
    private final void savePGNToFile(String pathName, boolean silent) {
        String pgn = ctrl.getPGN();
        Editor editor = settings.edit();
        editor.putString("currentPGNFile", pathName);
        editor.putInt("currFT", FT_PGN);
        editor.commit();
        Intent i = new Intent(DroidFish.this, EditPGNSave.class);
        i.setAction("com.if3games.chessonline.saveFile");
        i.putExtra("com.if3games.chessonline.pathname", pathName);
        i.putExtra("com.if3games.chessonline.pgn", pgn);
        i.putExtra("com.if3games.chessonline.silent", silent);
        startActivity(i);
    }

    /** Load a PGN game from a file. */
    private final void loadPGNFromFile(String pathName) {
        Editor editor = settings.edit();
        editor.putString("currentPGNFile", pathName);
        editor.putInt("currFT", FT_PGN);
        editor.commit();
        Intent i = new Intent(DroidFish.this, EditPGNLoad.class);
        i.setAction("com.if3games.chessonline.loadFile");
        i.putExtra("com.if3games.chessonline.pathname", pathName);
        startActivityForResult(i, RESULT_LOAD_PGN);
    }

    /** Load a FEN position from a file. */
    private final void loadFENFromFile(String pathName) {
        if (pathName == null)
            return;
        Editor editor = settings.edit();
        editor.putString("currentFENFile", pathName);
        editor.putInt("currFT", FT_FEN);
        editor.commit();
        Intent i = new Intent(DroidFish.this, LoadFEN.class);
        i.setAction("com.if3games.chessonline.loadFen");
        i.putExtra("com.if3games.chessonline.pathname", pathName);
        startActivityForResult(i, RESULT_LOAD_FEN);
    }

    private final void setFenHelper(String fen) {
        if (fen == null)
            return;
        try {
            ctrl.setFENOrPGN(fen);
        } catch (ChessParseError e) {
            // If FEN corresponds to illegal chess position, go into edit board mode.
            try {
                TextIO.readFEN(fen);
            } catch (ChessParseError e2) {
                if (e2.pos != null)
                    startEditBoard(fen);
            }
        }
    }

    @Override
    public void requestPromotePiece() {
        showDialog(PROMOTE_DIALOG);
    }

    @Override
    public void reportInvalidMove(Move m) {
    	invalidMove = true;
        String msg = String.format(Locale.US, "%s %s-%s",
                    getString(R.string.invalid_move),
                    TextIO.squareToString(m.from), TextIO.squareToString(m.to));
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportEngineName(String engine) {
        String msg = String.format(Locale.US, "%s: %s",
                getString(R.string.engine), engine);
        if(isSinglePlayer)
        	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportEngineError(String errMsg) {
        String msg = String.format(Locale.US, "%s: %s",
                getString(R.string.engine_error), errMsg);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void computerMoveMade() {
        if (soundEnabled) {
            if (moveSound != null)
                moveSound.release();
            try {
                moveSound = MediaPlayer.create(this, R.raw.movesound);
                if (moveSound != null)
                    moveSound.start();
            } catch (NotFoundException ex) {
            }
        }
        if (vibrateEnabled) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
        }
    }

    @Override
    public void runOnUIThread(Runnable runnable) {
        runOnUiThread(runnable);
    }

    /** Decide if user should be warned about heavy CPU usage. */
    private final void updateNotification() {
        boolean warn = false;
        if (lastVisibleMillis != 0) { // GUI not visible
            warn = lastComputationMillis >= lastVisibleMillis + 90000;
        }
        setNotification(warn);
    }

    private boolean notificationActive = false;

    /** Set/clear the "heavy CPU usage" notification. */
    private final void setNotification(boolean show) {
        if (notificationActive == show)
            return;
        notificationActive = show;
        final int cpuUsage = 1;
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(ns);
        if (show) {
            int icon = R.drawable.icon;
            CharSequence tickerText = getString(R.string.heavy_cpu_usage);
            long when = System.currentTimeMillis();
            Notification notification = new Notification(icon, tickerText, when);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;

            Context context = getApplicationContext();
            CharSequence contentTitle = getString(R.string.background_processing);
            CharSequence contentText = getString(R.string.lot_cpu_power);
            Intent notificationIntent = new Intent(this, CPUWarning.class);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

            mNotificationManager.notify(cpuUsage, notification);
        } else {
            mNotificationManager.cancel(cpuUsage);
        }
    }

    private final String timeToString(int time) {
        int secs = (int)Math.floor((time + 999) / 1000.0);
        boolean neg = false;
        if (secs < 0) {
            neg = true;
            secs = -secs;
        }
        int mins = secs / 60;
        secs -= mins * 60;
        StringBuilder ret = new StringBuilder();
        if (neg) ret.append('-');
        ret.append(mins);
        ret.append(':');
        if (secs < 10) ret.append('0');
        ret.append(secs);
        return ret.toString();
    }

    private Handler handlerTimer = new Handler();
    private Runnable r = new Runnable() {
        public void run() {
            ctrl.updateRemainingTime();
        }
    };

    @Override
    public void setRemainingTime(int wTime, int bTime, int nextUpdate) {
        if (ctrl.getGameMode().clocksActive()) {
            whiteTitleText.setText(getString(R.string.white_square_character) + " " + timeToString(wTime));
            blackTitleText.setText(getString(R.string.black_square_character) + " " + timeToString(bTime));
        } else {
            TreeMap<String,String> headers = new TreeMap<String,String>();
            ctrl.getHeaders(headers);
            whiteTitleText.setText(headers.get("White"));
            blackTitleText.setText(headers.get("Black"));
        }
        handlerTimer.removeCallbacks(r);
        if (nextUpdate > 0)
            handlerTimer.postDelayed(r, nextUpdate);
    }

    /** PngTokenReceiver implementation that renders PGN data for screen display. */
    static class PgnScreenText implements PgnToken.PgnTokenReceiver {
        private SpannableStringBuilder sb = new SpannableStringBuilder();
        private int prevType = PgnToken.EOF;
        int nestLevel = 0;
        boolean col0 = true;
        Node currNode = null;
        final static int indentStep = 15;
        int currPos = 0, endPos = 0;
        boolean upToDate = false;
        PGNOptions options;

        private static class NodeInfo {
            int l0, l1;
            NodeInfo(int ls, int le) {
                l0 = ls;
                l1 = le;
            }
        }
        HashMap<Node, NodeInfo> nodeToCharPos;

        PgnScreenText(PGNOptions options) {
            nodeToCharPos = new HashMap<Node, NodeInfo>();
            this.options = options;
        }

        public final SpannableStringBuilder getSpannableData() {
            return sb;
        }
        public final int getCurrPos() {
            return currPos;
        }

        public boolean isUpToDate() {
            return upToDate;
        }

        int paraStart = 0;
        int paraIndent = 0;
        boolean paraBold = false;
        private final void newLine() { newLine(false); }
        private final void newLine(boolean eof) {
            if (!col0) {
                if (paraIndent > 0) {
                    int paraEnd = sb.length();
                    int indent = paraIndent * indentStep;
                    sb.setSpan(new LeadingMarginSpan.Standard(indent), paraStart, paraEnd,
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (paraBold) {
                    int paraEnd = sb.length();
                    sb.setSpan(new StyleSpan(Typeface.BOLD), paraStart, paraEnd,
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (!eof)
                    sb.append('\n');
                paraStart = sb.length();
                paraIndent = nestLevel;
                paraBold = false;
            }
            col0 = true;
        }

        boolean pendingNewLine = false;

        /** Makes moves in the move list clickable. */
        private final static class MoveLink extends ClickableSpan {
            private Node node;
            MoveLink(Node n) {
                node = n;
            }
            @Override
            public void onClick(View widget) {
                if (ctrl != null) {
                    // On android 4.1 this onClick method is called
                    // even when you long click the move list. The test
                    // below works around the problem.
                    Dialog mlmd = moveListMenuDlg;
                    if ((mlmd == null) || !mlmd.isShowing())
                        ctrl.goNode(node);
                }
            }
            @Override
            public void updateDrawState(TextPaint ds) {
            }
        }

        public void processToken(Node node, int type, String token) {
            if (    (prevType == PgnToken.RIGHT_BRACKET) &&
                    (type != PgnToken.LEFT_BRACKET))  {
                if (options.view.headers) {
                    col0 = false;
                    newLine();
                } else {
                    sb.clear();
                    paraBold = false;
                }
            }
            if (pendingNewLine) {
                if (type != PgnToken.RIGHT_PAREN) {
                    newLine();
                    pendingNewLine = false;
                }
            }
            switch (type) {
            case PgnToken.STRING:
                sb.append(" \"");
                sb.append(token);
                sb.append('"');
                break;
            case PgnToken.INTEGER:
                if (    (prevType != PgnToken.LEFT_PAREN) &&
                        (prevType != PgnToken.RIGHT_BRACKET) && !col0)
                    sb.append(' ');
                sb.append(token);
                col0 = false;
                break;
            case PgnToken.PERIOD:
                sb.append('.');
                col0 = false;
                break;
            case PgnToken.ASTERISK:      sb.append(" *");  col0 = false; break;
            case PgnToken.LEFT_BRACKET:  sb.append('[');   col0 = false; break;
            case PgnToken.RIGHT_BRACKET: sb.append("]\n"); col0 = false; break;
            case PgnToken.LEFT_PAREN:
                nestLevel++;
                if (col0)
                    paraIndent++;
                newLine();
                sb.append('(');
                col0 = false;
                break;
            case PgnToken.RIGHT_PAREN:
                sb.append(')');
                nestLevel--;
                pendingNewLine = true;
                break;
            case PgnToken.NAG:
                sb.append(Node.nagStr(Integer.parseInt(token)));
                col0 = false;
                break;
            case PgnToken.SYMBOL: {
                if ((prevType != PgnToken.RIGHT_BRACKET) && (prevType != PgnToken.LEFT_BRACKET) && !col0)
                    sb.append(' ');
                int l0 = sb.length();
                sb.append(token);
                int l1 = sb.length();
                nodeToCharPos.put(node, new NodeInfo(l0, l1));
                sb.setSpan(new MoveLink(node), l0, l1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (endPos < l0) endPos = l0;
                col0 = false;
                if (nestLevel == 0) paraBold = true;
                break;
            }
            case PgnToken.COMMENT:
                if (prevType == PgnToken.RIGHT_BRACKET) {
                } else if (nestLevel == 0) {
                    nestLevel++;
                    newLine();
                    nestLevel--;
                } else {
                    if ((prevType != PgnToken.LEFT_PAREN) && !col0) {
                        sb.append(' ');
                    }
                }
                int l0 = sb.length();
                sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
                int l1 = sb.length();
                int color = ColorTheme.instance().getColor(ColorTheme.PGN_COMMENT);
                sb.setSpan(new ForegroundColorSpan(color), l0, l1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                col0 = false;
                if (nestLevel == 0)
                    newLine();
                break;
            case PgnToken.EOF:
                newLine(true);
                upToDate = true;
                break;
            }
            prevType = type;
        }

        @Override
        public void clear() {
            sb.clear();
            prevType = PgnToken.EOF;
            nestLevel = 0;
            col0 = true;
            currNode = null;
            currPos = 0;
            endPos = 0;
            nodeToCharPos.clear();
            paraStart = 0;
            paraIndent = 0;
            paraBold = false;
            pendingNewLine = false;

            upToDate = false;
        }

        BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

        @Override
        public void setCurrent(Node node) {
            sb.removeSpan(bgSpan);
            NodeInfo ni = nodeToCharPos.get(node);
            if ((ni == null) && (node != null) && (node.getParent() != null))
                ni = nodeToCharPos.get(node.getParent());
            if (ni != null) {
                int color = ColorTheme.instance().getColor(ColorTheme.CURRENT_MOVE);
                bgSpan = new BackgroundColorSpan(color);
                sb.setSpan(bgSpan, ni.l0, ni.l1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                currPos = ni.l0;
            } else {
                currPos = 0;
            }
            currNode = node;
        }
    }
    
    
    /*
     * GMS Logic Initial SECTION. Methods that implement the game's multiplayer.
     */
    
    private void startMultiplayerGameMode(int n) 
    {   	
    	isMatch = true;
    	h = null;
    	if(n == 0)
    		myTurn = true;
    	else
    		myTurn = false;
    	if(n>=0 && n<=1)
    		startNewGame(n);
    	
        int gameModeType = -1;
        /* only flip site in case the player was specified resp. changed */
        boolean flipSite = false;
        gameModeType = GameMode.TWO_PLAYERS;

        if (gameModeType >= 0) {
            Editor editor = settings.edit();
            String gameModeStr = String.format(Locale.US, "%d", gameModeType);
            editor.putString("gameMode", gameModeStr);
            editor.commit();
            GameMode gmsGameMode = new GameMode(gameModeType);
            ctrl.setGameMode(gmsGameMode);
            setBoardFlip(flipSite);
        }      
        if(mSaveGame.getStatsFromName(ConstantsData.CH_KEY_PLAYED) == 0) loadFromCloud();
        else {
            player1TitleText.setText(getString(R.string.str_gms_you_title) + "(" + Integer.toString(mSaveGame.getStatsFromName("rating"))  + ")");
            player2TitleText.setText(opponentName + "(" + Integer.toString(opponentRating)  + ")");
        }
        if(mSaveGame.getStatsFromName(ConstantsData.CH_KEY_PLAYED) == 0) {
            player1TitleText.setText(getString(R.string.str_gms_you_title));
            player2TitleText.setText(opponentName);
        }
    }
    
    /*
     * GMS Network SECTION. Methods that implement the game's multiplayer.
     */

	@Override
	public void onP2PConnected(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onP2PDisconnected(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeerDeclined(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeerInvitedToRoom(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeerJoined(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeerLeft(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeersConnected(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeersDisconnected(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRoomAutoMatching(Room arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRoomConnecting(Room arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.button_sign_in:
                // user wants to sign in
                beginUserInitiatedSignIn();
                break;
            case R.id.button_sign_out:
                // user wants to sign out
                signOut();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
            	if(!mAlreadyLoadedState)
            		loadFromCloud();            
            	
            	showDialog(GAME_GMS_INVITE_VARIANT_OPT);
                break;
            case R.id.button_see_invitations:
            	if(!mAlreadyLoadedState)
            		loadFromCloud();
                // show list of pending invitations
            	intent = Games.Invitations.getInvitationInboxIntent(getApiClient());
                switchToScreen(R.id.screen_wait);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                break;
            case R.id.button_accept_popup_invitation:
            	if(!mAlreadyLoadedState)
            		loadFromCloud();
                // user wants to accept the invitation shown on the invitation popup
                // (the one we got through the OnInvitationReceivedListener).
                acceptInviteToRoom(mIncomingInvitationId);
                mIncomingInvitationId = null;
                break;
            case R.id.button_quick_game:
            	if(!mAlreadyLoadedState)
            		loadFromCloud();
                // user wants to play against a random opponent right now
            	showDialog(GAME_GMS_AUTOMATCH_VARIANT_OPT);
                break;   
            case R.id.button_leaderboard:
            	startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(getApiClient()), RC_UNUSED);         	
                break;
            case R.id.button_achive:
            	startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), RC_UNUSED);
                break;                 
        }
	}
	
	@Override
	public void onSignInFailed() {
		if(!isSinglePlayer) {
			// Switch to screen sigh in
			switchToScreen(R.id.screen_sign_in);
		}
	}

	@Override
	public void onSignInSucceeded() {
		if(!isSinglePlayer) {
			Games.Invitations.registerInvitationListener(getApiClient(), this);
	        if (getInvitationId() != null) {
	            acceptInviteToRoom(getInvitationId());
	            return;
	        }
	        
	        if (!mAlreadyLoadedState) {
	            loadFromCloud();
	        }
	        displayPlayerNameScoreRank();
			// Switch to screen buttons (random, invite, invite inbox)
	        switchToMainScreen();
		}
	}
	
	void displayPlayerNameScoreRank() {
		((TextView) findViewById(R.id.playerDispNameId)).setText(getString(R.string.str_gms_hi) 
				+ " " + Games.Players.getCurrentPlayer(getApiClient()).getDisplayName() + "!");
		if(mSaveGame.getStatsFromName(ConstantsData.CH_KEY_PLAYED) != 0) {
			((TextView) findViewById(R.id.playerDispScoreId)).setText(
					getString(R.string.str_gms_disp_rating) + mSaveGame.getStatsFromName(ConstantsData.CH_KEY_RATIND) +
					" (" + getString(R.string.str_gms_disp_win) + mSaveGame.getStatsFromName(ConstantsData.CH_KEY_WON) +
					", " + getString(R.string.str_gms_disp_draw) + mSaveGame.getStatsFromName(ConstantsData.CH_KEY_DRAW) +
					", " + getString(R.string.str_gms_disp_loss) + mSaveGame.getStatsFromName(ConstantsData.CH_KEY_LOST) + ")");
		} else {
			((TextView) findViewById(R.id.playerDispScoreId)).setVisibility(View.GONE);
		}
	}
		
	
    void startQuickGame(int variant) {
        // quick-start a game with 1 randomly selected opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
                MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        rtmConfigBuilder.setVariant(variant);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        //resetGameVars();
        Games.RealTimeMultiplayer.create(getApiClient(), rtmConfigBuilder.build());
    }
    
    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data, int variant) {
        if (response != Activity.RESULT_OK) {
            //Log.w(TAG, "*** select players UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        //Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            //Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        rtmConfigBuilder.setVariant(variant);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        resetGameBoolVars();
        Games.RealTimeMultiplayer.create(getApiClient(), rtmConfigBuilder.build());
        Log.d(TAG, "Room created, waiting for it to be ready...");
    }
    
    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }
    
    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGameVars();
        resetGameBoolVars();
        Games.RealTimeMultiplayer.join(getApiClient(), roomConfigBuilder.build());
    }
    
    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            if(!isLeaveRoom && isMatch)
            	showDialog(GAME_GMS_EXIT);
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
    	isLeaveRoom = true;
        stopKeepingScreenOn();
        if (mRoomId != null) {
        	Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
            switchToMainScreen();
        }
    }
    
    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(), room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }
    
	@Override
	public void onInvitationReceived(Invitation invitation) {
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitationId = invitation.getInvitationId();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
                invitation.getInviter().getDisplayName() + " " +
                        getString(R.string.is_inviting_you));
        switchToScreen(mCurScreen); // This will show the invitation popup
		
	}

	@Override
	public void onInvitationRemoved(String arg0) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

	@Override
	public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        // get room ID, participants and my ID:
        mRoomId = room.getRoomId();
        mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(getApiClient()));

        // print out the list of participants (for debug purposes)
        //Log.d(TAG, "Room ID: " + mRoomId);
        //Log.d(TAG, "My ID " + mMyId);
        //Log.d(TAG, "<< CONNECTED TO ROOM>>");
        gmsGameVariantNumber = room.getVariant();        
        
        if(isServer()) {
        	imFirstType = -1;
        	sendFirstTypeForStart();
        }
        sendMyStats();
	}

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        if(opponentLeave) {
        	Toast.makeText(this, getString(R.string.str_gms_leave_opponent_toast), Toast.LENGTH_SHORT).show();
        	leaveRoom();
        } else
        	showGameError();
        
    }
    
    // Show error message about game being cancelled and return to main screen.
    void showGameError() {
        //showAlert(getString(R.string.error), getString(R.string.game_problem));
        switchToMainScreen();
    }
	
    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
    }
    
    /*
     * GAME LOGIC
     */
    
    // Reset game variables in preparation for a new game.
    void resetGameVars() {
        mParticipantScore.clear();
        mFinishedParticipants.clear();
    }
    
    void resetGameBoolVars() {
        imReady = false;
        opponentReady = false;
        isOpponentResign = false;
        isOpponentTimeOut = false;
    }
    // Current state of the game:
    int mSecondsLeft = -1; // how long until the game ends (seconds)
    int GAME_DURATION = -1;
    
    // Start the gameplay phase of the game.
    void startGame(boolean multiplayer, int variant) {
    	resetGameVars();
    	
    	switch (variant) {
		case ConstantsData.GAME_VARIANT_LONG:
			mSecondsLeft = -1;
			GAME_DURATION = -1;
			((TextView) findViewById(R.id.countdown)).setText("0:00");
			break;
		case ConstantsData.GAME_VARIANT_1MIN:
			mSecondsLeft = ConstantsData.GAME_DURATION_1MIN;
			GAME_DURATION = ConstantsData.GAME_DURATION_1MIN;
			break;
		case ConstantsData.GAME_VARIANT_2MIN:
			mSecondsLeft = ConstantsData.GAME_DURATION_2MIN;
			GAME_DURATION = ConstantsData.GAME_DURATION_2MIN;
			break;
		case ConstantsData.GAME_VARIANT_3MIN:
			mSecondsLeft = ConstantsData.GAME_DURATION_3MIN;
			GAME_DURATION = ConstantsData.GAME_DURATION_3MIN;
			break;
		default:
			break;
		}
    	
    	isLeaveRoom = false;
        mMultiplayer = multiplayer;

        switchToScreen(R.id.screen_game);
        startMultiplayerGameMode(imFirstType);
        
        if(mSecondsLeft > 0) {
	        // run the gameTick() method every second to update the game.
	        h = new Handler();
	        h.postDelayed(new Runnable() {
	            @Override
	            public void run() {
	                if (mSecondsLeft <= 0)
	                    return;
	                gameTick();
	                h.postDelayed(this, 1000);
	            }
	        }, 1000);
        }
        resetGameBoolVars();       
    }
    
    // Game tick -- update countdown, check if game ended.
    void gameTick() {
        if (mSecondsLeft > 0) {
        	if(myTurn)
        		--mSecondsLeft;
        	else
        		mSecondsLeft = GAME_DURATION;
        	
        	if(mSecondsLeft <= 10)
        		((TextView) findViewById(R.id.countdown)).setTextColor(Color.RED);
        	else
        		((TextView) findViewById(R.id.countdown)).setTextColor(Color.WHITE);
        }
        // update countdown
        ((TextView) findViewById(R.id.countdown)).setText(getCountDownText(mSecondsLeft));

        if (mSecondsLeft <= 0) {
            // finish game
        	timeOutGMSGame();
        }
    }
    
    String getCountDownText(int shownTime) {
    	String result = null; 
	    if (shownTime == 180)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_180;
	    else if (shownTime < 180 && shownTime >= 130)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_160 + Integer.toString(shownTime - 120);
	    else if (shownTime < 130 && shownTime >= 120)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_130 + Integer.toString(shownTime - 120);		      
	    else if (shownTime == 120)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_120;
	    else if(shownTime < 120 && shownTime >= 70)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_100 + Integer.toString(shownTime - 60);		      
	    else if(shownTime < 70 && shownTime >= 60)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_70 + Integer.toString(shownTime - 60);
	    else if(shownTime < 60 && shownTime >= 10)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_60 + Integer.toString(shownTime);
	    else if(shownTime < 10)
	    	result = ConstantsData.COUNTDOWN_TIMER_TEXT_9 + Integer.toString(shownTime);
		return result;
    }
    
    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

    // Score of other participants. We update this as we receive their scores
    // from the network.
    Map<String, Integer> mParticipantScore = new HashMap<String, Integer>();

    // Participants who sent us their final score.
    Set<String> mFinishedParticipants = new HashSet<String>();
    
    // Called when we receive a real-time message from the network.
    // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
    // indicating
    // whether it's a final or interim score. The second byte is the score.
    // There is also the
    // 'S' message, which indicates that the game should start.
	@Override
	public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        //Log.d(TAG, "Message received: " + (char) buf[0] + "/" + (int) buf[1]);
        
        if (buf[0] == 'S') {
        		if ((int) buf[1] == 1)
					imFirstType = 0;
				else
					imFirstType = 1;     		
        }
        
        if (buf[0] == 'F') {
        	if(imReady) {
        		startGame(true, gmsGameVariantNumber);
        	} else {
        		opponentReady = true;
        	}
        }
        
        if (buf[0] == 'M') {
        	handleGMSClick((int) buf[1], (int) buf[2]);
        }
        
        if (buf[0] == 'D') {
			showDialog(GAME_GMS_DRAW_ASK);
		}
        
        if (buf[0] == '=') {
        	ctrl.acceptDrawGmsGame();
        	Toast.makeText(this, getString(R.string.draw_agree), Toast.LENGTH_SHORT).show(); 
		}
        
        if (buf[0] == '!') {
        	Toast.makeText(this, "Not draw!", Toast.LENGTH_SHORT).show(); 
		}
        
        if (buf[0] == 'R') {
        	isOpponentResign = true;
        	ctrl.resignGame();
        	if(imFirstType == 0)
        		Toast.makeText(this, getString(R.string.resign_black), Toast.LENGTH_SHORT).show();
        	else
        		Toast.makeText(this, getString(R.string.resign_white), Toast.LENGTH_SHORT).show();
		}
        if (buf[0] == 'T') {
        	isOpponentTimeOut = true;
        	ctrl.resignGame();
        	if(imFirstType == 0)
        		Toast.makeText(this, getString(R.string.gms_white_win_time), Toast.LENGTH_SHORT).show();
        	else
        		Toast.makeText(this, getString(R.string.gms_black_win_time), Toast.LENGTH_SHORT).show();
		}
        if(buf.length > 10) {
	        if (buf[2] == 's' && buf[3] == 't' && buf[4] == 'a' && buf[5] == 't') {
	        	try {
	        		//sendMyStats();
	        		loadOppStatsFromJson(new String(buf));
				} catch (Exception e) {
					Toast.makeText(this, "Error load opponent stats", Toast.LENGTH_SHORT).show();
				}
	        }
        }
	}
	
	/**
	 * Broadcast move square.
	 * 
	 * @param sq         move selected square to this position/square  (to)       
	 *           
	 * @param selSquare  selected square (from)
	 *            
	 */
    void broadcastMove(int sq, int selSquare) {
    	myTurn = false;
        // Message buffer for sending messages
        byte[] mMsgBuf = new byte[3];
        
        if (!mMultiplayer)
            return; // playing single-player mode

        // First byte in message indicates whether it's a final score or not
        mMsgBuf[0] = (byte)'M';
        mMsgBuf[1] = (byte)sq;
        mMsgBuf[2] = (byte)selSquare;

        // Send to every other participant.
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mMsgBuf, 
            		mRoomId, p.getParticipantId());
        }
    }
    
    private final void handleGMSClick(int sq, int selSquare) {
    		myTurn = true;    		
    		Move m = cb.mousePressedGMS2(sq, selSquare);
    		if(m != null) {
    			ctrl.makeHumanMoveGMS(m, true);
    		}
    		setEgtbHints(cb.getSelectedSquare());		
    } 
	/**
	 * if the current player is server, it generates a random number (0=WHITE or 1=BLACK) and sends an opponent
	 */
    void sendFirstTypeForStart() {
    	if(imFirstType == -1) {
	    	byte[] mStartMsg = new byte[2];
	    	mStartMsg[0] = (byte) 'S';
	    	imFirstType = new Random().nextInt(2);
	    	mStartMsg[1] = (byte) imFirstType;
	        for (Participant p : mParticipants) {
	            if (p.getParticipantId().equals(mMyId))
	                continue;
	            if (p.getStatus() != Participant.STATUS_JOINED)
	                continue;
                Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mStartMsg, 
                		mRoomId, p.getParticipantId());
	        }
    	}
    }    
    
    void sendImReady() {
	    	byte[] mStartMsg = new byte[1];
	    	mStartMsg[0] = (byte) 'F';
	        for (Participant p : mParticipants) {
	            if (p.getParticipantId().equals(mMyId))
	                continue;
	            if (p.getStatus() != Participant.STATUS_JOINED)
	                continue;
	            Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mStartMsg, 
                		mRoomId, p.getParticipantId());
	        }
    }
    
    void sendAskDrawIsPossible() {
    	byte[] mStartMsg = new byte[1];
    	mStartMsg[0] = (byte) 'D';
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mStartMsg, 
            		mRoomId, p.getParticipantId());
        }
    }
    
    void sendAnswerDrawIsPossible(boolean answer) {
    	byte[] mStartMsg = new byte[1];
    	if (answer)
    		mStartMsg[0] = (byte) '=';
		else 
			mStartMsg[0] = (byte) '!';
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mStartMsg, 
            		mRoomId, p.getParticipantId());
        }
    }
    
    void sendResignGame() {
    	byte[] mStartMsg = new byte[1];
    	mStartMsg[0] = (byte) 'R';
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mStartMsg, 
            		mRoomId, p.getParticipantId());
        }
    }
	/**
	 * Sending network packet if the current player ran out of time (GMS mode).
	 */
    void sendTimeOutGame() {
    	byte[] mStartMsg = new byte[1];
    	mStartMsg[0] = (byte) 'T';
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mStartMsg, 
            		mRoomId, p.getParticipantId());
        }
    }
	/**
	 * Send current player stats to other player (GMS mode).
	 */
    void sendMyStats() {
    	mSaveGame.setUserName(Games.Players.getCurrentPlayer(getApiClient()).getDisplayName());
    	byte[] mStartMsg = mSaveGame.toBytes();
        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId))
                continue;
            if (p.getStatus() != Participant.STATUS_JOINED)
                continue;
            Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), this, mStartMsg, 
            		mRoomId, p.getParticipantId());
        }
    }
    /*
     * UI SECTION. Methods that implement the game's UI.
     */
    
    // This array lists all the individual screens our game has.
    
    final static int[] CLICKABLES = {
        R.id.button_accept_popup_invitation, R.id.button_invite_players,
        R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
        R.id.button_sign_out, R.id.button_leaderboard, R.id.button_achive
    };
    
    final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait
    };
    int mCurScreen = -1;

    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }

        mCurScreen = screenId;
        // should we show the invitation popup?
        boolean showInvPopup;
        if (mIncomingInvitationId == null) {
            // no invitation, so no popup
            showInvPopup = false;
        } else if (mMultiplayer) {
            // if in multiplayer, only show invitation on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        } else {
            // single-player: show on main screen and gameplay screen
            showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
        }
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }
    
    void switchToMainScreen() {
        switchToScreen(isSignedIn() ? R.id.screen_main : R.id.screen_sign_in);
    }
    
    void showAdsGMS() {
	    // Begin loading your 
		interstitial = new InterstitialAd(this);
		interstitial.setAdUnitId(ConstantsData.INTERSTITIAL_APPID);
	    //adRequest = new AdRequest.Builder().build();
	    //interstitial.loadAd(adRequest);
	    // Set Ad Listener to use the callbacks below
	    //interstitialsetAdListener(this);  
	    // End AdMob
		// AdMob
		//if (synth.getId() % 2 == 0) {
		//	if(interstitial.isLoaded())
		//		interstitial.show();
		//}
    }  
    
    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @Override
    public void onStart() {   	
    	if(!isSinglePlayer) {
    		switchToScreen(R.id.screen_wait);
    		mClient.connect();
    	}
    	super.onStart();
    }

    @Override
    public void onStop() {    	
    	if(!isSinglePlayer) {
	    	leaveRoom();
	        switchToScreen(R.id.screen_wait);
    	}
    	super.onStop();
    }

	@Override
	public void onRealTimeMessageSent(int statusCode, int tokenId,
			String recipientParticipantId) {
		// TODO Auto-generated method stub
		
	}
	
	private boolean isServer()
	{
	    for(Participant p : mParticipants )
	    {
            String pid = p.getParticipantId();
            if (pid.equals(mMyId)) {
                continue;
            }
	        if(pid.compareTo(mMyId)<0)
	            return false;
	    }
	    return true;
	}
	
	private void storeScoreToLeaderBoard(long score) {
		Games.Leaderboards.submitScore(getApiClient(), getString(R.string.leaderboard_score),
                score);
	}
	
	private void storeWinnersToLeaderBoard(long win) {
		Games.Leaderboards.submitScore(getApiClient(), getString(R.string.leaderboard_winners),
                win);
	}
	
    final static int[] ACHIEVEMENT = {
        R.string.achievement_1200, R.string.achievement_1400, R.string.achievement_1600, 
        R.string.achievement_1800, R.string.achievement_2000,R.string.achievement_2200, 
        R.string.achievement_2400, R.string.achievement_2500,R.string.achievement_2500_GM, 
        R.string.achievement_2600_PCM_1, R.string.achievement_2700_PCM_2, R.string.achievement_2800_CM
    };
	
    void unlockAchievement(int rating) {
    	int index = ConstantsData.getIndexFromCheckedRange(rating);
		Games.Achievements.unlock(getApiClient(), getString(ACHIEVEMENT[index]));
    }
	
    private final Dialog gameGmsModeDialog() {
        final CharSequence[] items = {
            getString(R.string.option_resign_game),
            getString(R.string.option_draw),
            getString(R.string.load_save_menu)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_game_mode);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                int gameModeType = -1;
                /* only flip site in case the player was specified resp. changed */
                boolean flipSite = false;
                switch (item) {
                case 0: 
                	resighGMSGame();
                	break;
                case 1: 
                	if(!isLeaveRoom) {
	                    if (ctrl.humansTurn()) {
	                        if (ctrl.claimDrawIfPossible()) {
	                            ctrl.stopPonder();
	                        } else {
	                            Toast.makeText(getApplicationContext(), R.string.offer_draw, Toast.LENGTH_SHORT).show();
	                        }
	                    }
	                    sendAskDrawIsPossible();
                	}
                	break;
                case 2: 
                    removeDialog(FILE_MENU_DIALOG);
                    showDialog(FILE_MENU_DIALOG);
                	break;
                default: break;
                }
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }
    
    private void resighGMSGame() {
    	if(!isLeaveRoom) {
            if (ctrl.humansTurn()) {
                ctrl.resignGame();
                sendResignGame();
            }      
    	}
    } 
    
    private void timeOutGMSGame() {
    	if(!isLeaveRoom) {
            if (ctrl.humansTurn()) {
                ctrl.resignGame();
                sendTimeOutGame();
            }      
    	}
    } 
	
    private final Dialog newGmsGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.option_new_game);
        builder.setMessage(R.string.start_new_game);
        builder.setPositiveButton(R.string.yes, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	if(imFirstType == 0)
            		imFirstType = 1;
            	else
            		imFirstType = 0;

				if (!opponentReady && !isLeaveRoom) {
					imReady = true;
					sendImReady();
					Toast.makeText(getApplicationContext(), getString(R.string.draw_gms_try_again), Toast.LENGTH_SHORT).show();
				} else {
					if(!isLeaveRoom) {
						sendImReady();
						startGame(true, gmsGameVariantNumber);
					}
				}
            }
        });
        builder.setNeutralButton(R.string.no, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	leaveRoom();
            }
        });
        return builder.create();
    }
    
    private final Dialog drawGmsGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.draw_gms_option_title);
        builder.setMessage(R.string.draw_gms_option_msg);
        builder.setPositiveButton(R.string.yes, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	if(!isLeaveRoom) {
            		sendAnswerDrawIsPossible(true);
            		ctrl.acceptDrawGmsGame();
            	}
            }
        });
        builder.setNeutralButton(R.string.no, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	if(!isLeaveRoom)
            		sendAnswerDrawIsPossible(false);
            }
        });
        return builder.create();
    }
    
    private final Dialog gameGmsAutoMatchVariantDialog() {
        final CharSequence[] items = {
            getString(R.string.gms_variant_opt_long_game),
            getString(R.string.gms_variant_opt_1min_game),
            getString(R.string.gms_variant_opt_2min_game),
            getString(R.string.gms_variant_opt_3min_game)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gms_variant_opt_title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                int gameModeType = -1;
                /* only flip site in case the player was specified resp. changed */
                boolean flipSite = false;
                switch (item) {
                case 0: 
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_LONG;
                	startQuickGame(ConstantsData.GAME_VARIANT_LONG);
                	break;
                case 1: 
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_1MIN;
                	startQuickGame(ConstantsData.GAME_VARIANT_1MIN);
                	break;
                case 2: 
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_2MIN;
                	startQuickGame(ConstantsData.GAME_VARIANT_2MIN);
                	break;
                case 3: 
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_3MIN;
                	startQuickGame(ConstantsData.GAME_VARIANT_3MIN);
                	break;
                default: break;
                }
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }
    
    private final Dialog exitGmsGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gms_exit_game_dialog_title);
        builder.setMessage(R.string.gms_exit_game_dialog_msg);
        builder.setPositiveButton(R.string.yes, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	resighGMSGame();
            	leaveRoom();
            }
        });
        builder.setNeutralButton(R.string.no, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        return builder.create();
    }
    
    private final Dialog gameGmsInviteVariantDialog() {
        final CharSequence[] items = {
            getString(R.string.gms_variant_opt_long_game),
            getString(R.string.gms_variant_opt_1min_game),
            getString(R.string.gms_variant_opt_2min_game),
            getString(R.string.gms_variant_opt_3min_game)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gms_variant_opt_title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                int gameModeType = -1;
                /* only flip site in case the player was specified resp. changed */
                boolean flipSite = false;
                switch (item) {
                case 0: 
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_LONG;
                	invateGMSPlayers();
                	break;
                case 1: 
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_1MIN;
                	invateGMSPlayers();
                	break;
                case 2:
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_2MIN;
                	invateGMSPlayers();
                	break;
                case 3: 
                	gmsGameVariantNumber = ConstantsData.GAME_VARIANT_3MIN;
                	invateGMSPlayers();
                	break;
                default: break;
                }
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }
    
    private void invateGMSPlayers() {
    	// show list of invitable players
    	Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(getApiClient(), 1, 1);
        switchToScreen(R.id.screen_wait);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }
    
	/**
	 * Cloud Save section
	 */
    private void loadLocal() {
        SharedPreferences sp = getSharedPreferences("gameStateGMS", Context.MODE_PRIVATE);
        mSaveGame = new SaveGame(sp, "gameStateGMS");
        String json = sp.getString("gameStateGMS", "");
        if (json == null || json.trim().equals("")) mAlreadyLocalState = false;
        else mAlreadyLocalState = true;
    }

    private void saveLocal() {
        SharedPreferences sp = getSharedPreferences("gameStateGMS", Context.MODE_PRIVATE);
        if(getApiClient().isConnected())
        	mSaveGame.setUserName(Games.Players.getCurrentPlayer(getApiClient()).getDisplayName());
        if(mClient.isConnected())
        	mSaveGame.setUserName(Games.Players.getCurrentPlayer(getApiClient()).getDisplayName());
        mSaveGame.save(sp, "gameStateGMS");
    }
    
    ResultCallback<AppStateManager.StateResult> mResultCallback = new
            ResultCallback<AppStateManager.StateResult>() {
        @Override
        public void onResult(AppStateManager.StateResult result) {
            AppStateManager.StateConflictResult conflictResult = result.getConflictResult();
            AppStateManager.StateLoadedResult loadedResult = result.getLoadedResult();
            if (loadedResult != null) {
                processStateLoaded(loadedResult);
            } else if (conflictResult != null) {
                processStateConflict(conflictResult);
            }
        }
    };
    
    void loadFromCloud() {
        AppStateManager.load(mClient, OUR_STATE_KEY).setResultCallback(mResultCallback);
    }
    
    void saveToCloud() {
    	if (mClient.isConnected()) {
    		mSaveGame.setUserName(Games.Players.getCurrentPlayer(getApiClient()).getDisplayName());
            AppStateManager.update(mClient, OUR_STATE_KEY, mSaveGame.toBytes());
            //Toast.makeText(getApplicationContext(), "Сохранение в облако вроде успешно 1: " + new String(mSaveGame.toBytes()), Toast.LENGTH_SHORT).show();
		} else {

		}

        // Note: this is a fire-and-forget call. It will NOT trigger a call to any callbacks!
    }

    private void processStateConflict(AppStateManager.StateConflictResult result) {
        // Need to resolve conflict between the two states.
        // We do that by taking the union of the two sets of cleared levels,
        // which means preserving the maximum star rating of each cleared
        // level:
        byte[] serverData = result.getServerData();
        byte[] localData = result.getLocalData();

        SaveGame localGame = new SaveGame(localData);
        SaveGame serverGame = new SaveGame(serverData);
        SaveGame resolvedGame = localGame.unionWith(serverGame);

        AppStateManager.resolve(mClient, result.getStateKey(), result.getResolvedVersion(),
                resolvedGame.toBytes()).setResultCallback(mResultCallback);
    }

    private void processStateLoaded(AppStateManager.StateLoadedResult result) {
        switch (result.getStatus().getStatusCode()) {
        case AppStateStatusCodes.STATUS_OK:
            // Data was successfully loaded from the cloud: merge with local data.
            mSaveGame = mSaveGame.unionWith(new SaveGame(result.getLocalData()));
            mAlreadyLoadedState = true;
            saveLocal();
            //Toast.makeText(getApplicationContext(), "Загрузка из облака успешна: " + new String(result.getLocalData()), Toast.LENGTH_SHORT).show();
            //hideAlertBar();
            break;
        case AppStateStatusCodes.STATUS_STATE_KEY_NOT_FOUND:
            // key not found means there is no saved data. To us, this is the same as
            // having empty data, so we treat this as a success.
            mAlreadyLoadedState = true;
            //hideAlertBar();
            break;
        case AppStateStatusCodes.STATUS_NETWORK_ERROR_NO_DATA:
            // can't reach cloud, and we have no local state. Warn user that
            // they may not see their existing progress, but any new progress won't be lost.
            //showAlertBar(R.string.no_data_warning);
            break;
        case AppStateStatusCodes.STATUS_NETWORK_ERROR_STALE_DATA:
            // can't reach cloud, but we have locally cached data.
            //showAlertBar(R.string.stale_data_warning);
            break;
        case AppStateStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
            // need to reconnect AppStateClient
            reconnectClient();
            break;
        default:
            // error
            //showAlertBar(R.string.load_error_warning);
            break;
        }

        updateRatingUi();
    }
    
    private void showAlertBar(int resId) {
       // ((TextView) findViewById(R.id.alert_bar)).setText(getString(resId));
       // ((TextView) findViewById(R.id.alert_bar)).setVisibility(View.VISIBLE);
    }

    private void hideAlertBar() {
        //((TextView) findViewById(R.id.alert_bar)).setVisibility(View.GONE);
    }
    
	/**
	 * refresh Rating in game after match
	 */
    private void updateRatingUi() {
    	//todo
    }
    
    private void handleGMSMatchComplete(int result) {
    	isMatch = false;
    	if(mSaveGame.getStatsFromName(ConstantsData.CH_KEY_PLAYED) == 0) {
    		unlockAchievement(0);
    		loadFromCloud();
    	}
    	mSaveGame.setStatsFromResult(result, opponentRating, gameTypeMode);
    	saveLocal();
    	saveToCloud();
    	sendMyStats();
    	storeScoreToLeaderBoard(mSaveGame.getStatsFromName(ConstantsData.CH_KEY_RATIND));
    	storeWinnersToLeaderBoard(mSaveGame.getStatsFromName(ConstantsData.CH_KEY_WON));
    	unlockAchievement(mSaveGame.getStatsFromName(ConstantsData.CH_KEY_RATIND));
    	showDialog(NEW_GMS_GAME_DIALOG);
    }
    
    public void loadOppStatsFromJson(String json) {
        //mOpponentStats.clear();
        if (json == null || json.trim().equals("")) return;

        try {
            JSONObject obj = new JSONObject(json);
            String format = obj.getString("version");
            if (!format.equals(ConstantsData.SERIAL_VERSION)) {
                throw new RuntimeException("Unexpected loot format " + format);
            }
            opponentName = obj.getString("username");
            
            JSONObject stats = obj.getJSONObject("stats");
            Iterator<?> iter = stats.keys();

            while (iter.hasNext()) {
                String statName = (String)iter.next();
                mOpponentStats.put(statName, stats.getInt(statName));
            }
            Integer r = 0;
            r = mOpponentStats.get("rating");
            opponentRating = r.intValue();
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Opponent stats data has a syntax error: " + json, ex);
        }
        catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Opponent stats has an invalid number in it: " + json, ex);
        }
    }
}
