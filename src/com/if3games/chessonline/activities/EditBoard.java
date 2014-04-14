/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2013  Peter Österlund, peterosterlund2@gmail.com

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

package com.if3games.chessonline.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.if3games.chessonline.ChessBoard;
import com.if3games.chessonline.DroidFish;
import com.if3games.chessonline.R;
import com.if3games.chessonline.Util;
import com.if3games.chessonline.ChessBoard.SquareDecoration;
import com.if3games.chessonline.Util.MaterialDiff;
import com.if3games.chessonline.gamelogic.ChessParseError;
import com.if3games.chessonline.gamelogic.Move;
import com.if3games.chessonline.gamelogic.Pair;
import com.if3games.chessonline.gamelogic.Piece;
import com.if3games.chessonline.gamelogic.Position;
import com.if3games.chessonline.gamelogic.TextIO;
import com.if3games.chessonline.gtb.Probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EditBoard extends Activity {
    private ChessBoardEdit cb;
    private TextView status;
    private Button okButton;
    private Button cancelButton;

    private boolean egtbHints;
    private boolean autoScrollTitle;
    private boolean boardGestures;
    private TextView whiteFigText;
    private TextView blackFigText;
    private Typeface figNotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        figNotation = Typeface.createFromAsset(getAssets(), "fonts/DroidFishChessNotationDark.otf");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        egtbHints = settings.getBoolean("tbHintsEdit", false);
        autoScrollTitle = settings.getBoolean("autoScrollTitle", true);
        boardGestures = settings.getBoolean("boardGestures", true);

        initUI();

        Util.setFullScreenMode(this, settings);

        Intent i = getIntent();
        Position pos = null;
        try {
            pos = TextIO.readFEN(i.getAction());
        } catch (ChessParseError e) {
            pos = e.pos;
        }
        if (pos != null)
            cb.setPosition(pos);
        checkValidAndUpdateMaterialDiff();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ChessBoardEdit oldCB = cb;
        String statusStr = status.getText().toString();
        initUI();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        setSelection(oldCB.selectedSquare);
        cb.userSelectedSquare = oldCB.userSelectedSquare;
        status.setText(statusStr);
        checkValidAndUpdateMaterialDiff();
    }

    private final void initUI() {
        setContentView(R.layout.editboard);
        Util.overrideFonts(findViewById(android.R.id.content));

        cb = (ChessBoardEdit)findViewById(R.id.eb_chessboard);
        status = (TextView)findViewById(R.id.eb_status);
        okButton = (Button)findViewById(R.id.eb_ok);
        cancelButton = (Button)findViewById(R.id.eb_cancel);

        TextView whiteTitleText = (TextView)findViewById(R.id.white_clock);
        whiteTitleText.setVisibility(View.GONE);
        TextView blackTitleText = (TextView)findViewById(R.id.black_clock);
        blackTitleText.setVisibility(View.GONE);
        TextView engineTitleText = (TextView)findViewById(R.id.title_text);
        engineTitleText.setVisibility(View.GONE);
        whiteFigText = (TextView) findViewById(R.id.white_pieces);
        whiteFigText.setTypeface(figNotation);
        whiteFigText.setSelected(true);
        whiteFigText.setTextColor(whiteTitleText.getTextColors());
        blackFigText = (TextView) findViewById(R.id.black_pieces);
        blackFigText.setTypeface(figNotation);
        blackFigText.setSelected(true);
        blackFigText.setTextColor(blackTitleText.getTextColors());
        TextView summaryTitleText = (TextView) findViewById(R.id.title_text_summary);
        summaryTitleText.setText(R.string.edit_board);

        TextUtils.TruncateAt where = autoScrollTitle ? TextUtils.TruncateAt.MARQUEE
                                                     : TextUtils.TruncateAt.END;
        engineTitleText.setEllipsize(where);
        whiteFigText.setEllipsize(where);
        blackFigText.setEllipsize(where);

        okButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendBackResult();
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        status.setFocusable(false);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        final GestureDetector gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                if (!boardGestures) {
                    handleClick(e);
                    return true;
                }
                return false;
            }
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!boardGestures)
                    return false;
                cb.cancelLongPress();
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
            @Override
            public void onLongPress(MotionEvent e) {
                if (!boardGestures)
                    return;
                ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(20);
                showDialog(EDIT_DIALOG);
            }
            private final void handleClick(MotionEvent e) {
                int sq = cb.eventToSquare(e);
                Move m = cb.mousePressed(sq);
                if (m != null)
                    doMove(m);
                setEgtbHints(cb.getSelectedSquare());
            }
        });
        cb.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gd.onTouchEvent(event);
            }
        });
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
            public void onTrackballEvent(MotionEvent event) {
                Move m = cb.handleTrackballEvent(event);
                if (m != null)
                    doMove(m);
                setEgtbHints(cb.getSelectedSquare());
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        showDialog(EDIT_DIALOG);
        return false;
    }

    private final void setSelection(int sq) {
        cb.setSelection(sq);
        setEgtbHints(sq);
    }

    private final void setEgtbHints(int sq) {
        if (!egtbHints || (sq < 0)) {
            cb.setSquareDecorations(null);
            return;
        }

        Probe gtbProbe = Probe.getInstance();
        ArrayList<Pair<Integer, Integer>> x = gtbProbe.relocatePieceProbe(cb.pos, sq);
        if (x == null) {
            cb.setSquareDecorations(null);
            return;
        }

        ArrayList<SquareDecoration> sd = new ArrayList<SquareDecoration>();
        for (Pair<Integer,Integer> p : x)
            sd.add(new SquareDecoration(p.first, p.second));
        cb.setSquareDecorations(sd);
    }

    private void doMove(Move m) {
        if (m.to < 0) {
            if ((m.from < 0) || (cb.pos.getPiece(m.from) == Piece.EMPTY)) {
                setSelection(m.to);
                return;
            }
        }
        Position pos = new Position(cb.pos);
        int piece = Piece.EMPTY;
        if (m.from >= 0) {
            piece = pos.getPiece(m.from);
        } else {
            piece = -(m.from + 2);
        }
        if (m.to >= 0) {
            int oPiece = Piece.swapColor(piece);
            if ((m.from < 0) && (pos.getPiece(m.to) == oPiece))
                pos.setPiece(m.to, Piece.EMPTY);
            else if ((m.from < 0) && (pos.getPiece(m.to) == piece))
                pos.setPiece(m.to, oPiece);
            else
                pos.setPiece(m.to, piece);
        }
        if (m.from >= 0)
            pos.setPiece(m.from, Piece.EMPTY);
        cb.setPosition(pos);
        if (m.from >= 0)
            setSelection(-1);
        else
            setSelection(m.from);
        checkValidAndUpdateMaterialDiff();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            sendBackResult();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private final void sendBackResult() {
        if (checkValidAndUpdateMaterialDiff()) {
            setPosFields();
            String fen = TextIO.toFEN(cb.pos);
            setResult(RESULT_OK, (new Intent()).setAction(fen));
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private final void setPosFields() {
        setEPFile(getEPFile()); // To handle sideToMove change
        TextIO.fixupEPSquare(cb.pos);
        TextIO.removeBogusCastleFlags(cb.pos);
    }

    private final int getEPFile() {
        int epSquare = cb.pos.getEpSquare();
        if (epSquare < 0) return 8;
        return Position.getX(epSquare);
    }

    private final void setEPFile(int epFile) {
        int epSquare = -1;
        if ((epFile >= 0) && (epFile < 8)) {
            int epRank = cb.pos.whiteMove ? 5 : 2;
            epSquare = Position.getSquare(epFile, epRank);
        }
        cb.pos.setEpSquare(epSquare);
    }

    /** Test if a position is valid and update material diff display. */
    private final boolean checkValidAndUpdateMaterialDiff() {
        try {
            MaterialDiff md = Util.getMaterialDiff(cb.pos);
            whiteFigText.setText(md.white);
            blackFigText.setText(md.black);

            String fen = TextIO.toFEN(cb.pos);
            TextIO.readFEN(fen);
            status.setText("");
            return true;
        } catch (ChessParseError e) {
            status.setText(getParseErrString(e));
        }
        return false;
    }

    private final String getParseErrString(ChessParseError e) {
        if (e.resourceId == -1)
            return e.getMessage();
        else
            return getString(e.resourceId);
    }

    static final int EDIT_DIALOG = 0;
    static final int SIDE_DIALOG = 1;
    static final int CASTLE_DIALOG = 2;
    static final int EP_DIALOG = 3;
    static final int MOVCNT_DIALOG = 4;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case EDIT_DIALOG: {
            final int SIDE_TO_MOVE    = 0;
            final int CLEAR_BOARD     = 1;
            final int INITIAL_POS     = 2;
            final int CASTLING_FLAGS  = 3;
            final int EN_PASSANT_FILE = 4;
            final int MOVE_COUNTERS   = 5;
            final int COPY_POSITION   = 6;
            final int PASTE_POSITION  = 7;
            final int GET_FEN         = 8;

            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.side_to_move));     actions.add(SIDE_TO_MOVE);
            lst.add(getString(R.string.clear_board));      actions.add(CLEAR_BOARD);
            lst.add(getString(R.string.initial_position)); actions.add(INITIAL_POS);
            lst.add(getString(R.string.castling_flags));   actions.add(CASTLING_FLAGS);
            lst.add(getString(R.string.en_passant_file));  actions.add(EN_PASSANT_FILE);
            lst.add(getString(R.string.move_counters));    actions.add(MOVE_COUNTERS);
            lst.add(getString(R.string.copy_position));    actions.add(COPY_POSITION);
            lst.add(getString(R.string.paste_position));   actions.add(PASTE_POSITION);
            if (DroidFish.hasFenProvider(getPackageManager())) {
                lst.add(getString(R.string.get_fen)); actions.add(GET_FEN);
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.edit_board);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (finalActions.get(item)) {
                    case SIDE_TO_MOVE:
                        showDialog(SIDE_DIALOG);
                        setSelection(-1);
                        checkValidAndUpdateMaterialDiff();
                        break;
                    case CLEAR_BOARD: {
                        Position pos = new Position();
                        cb.setPosition(pos);
                        setSelection(-1);
                        checkValidAndUpdateMaterialDiff();
                        break;
                    }
                    case INITIAL_POS: {
                        try {
                            Position pos = TextIO.readFEN(TextIO.startPosFEN);
                            cb.setPosition(pos);
                            setSelection(-1);
                            checkValidAndUpdateMaterialDiff();
                        } catch (ChessParseError e) {
                        }
                        break;
                    }
                    case CASTLING_FLAGS:
                        removeDialog(CASTLE_DIALOG);
                        showDialog(CASTLE_DIALOG);
                        setSelection(-1);
                        checkValidAndUpdateMaterialDiff();
                        break;
                    case EN_PASSANT_FILE:
                        removeDialog(EP_DIALOG);
                        showDialog(EP_DIALOG);
                        setSelection(-1);
                        checkValidAndUpdateMaterialDiff();
                        break;
                    case MOVE_COUNTERS:
                        removeDialog(MOVCNT_DIALOG);
                        showDialog(MOVCNT_DIALOG);
                        setSelection(-1);
                        checkValidAndUpdateMaterialDiff();
                        break;
                    case COPY_POSITION: {
                        setPosFields();
                        String fen = TextIO.toFEN(cb.pos) + "\n";
                        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(fen);
                        setSelection(-1);
                        break;
                    }
                    case PASTE_POSITION: {
                        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        if (clipboard.hasText()) {
                            String fen = clipboard.getText().toString();
                            setFEN(fen);
                        }
                        break;
                    }
                    case GET_FEN:
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT); 
                        i.setType("application/x-chess-fen");
                        try {
                            startActivityForResult(i, RESULT_GET_FEN);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case SIDE_DIALOG: {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_side_to_move_first);
            final int selectedItem = (cb.pos.whiteMove) ? 0 : 1;
            builder.setSingleChoiceItems(new String[]{getString(R.string.white), getString(R.string.black)}, selectedItem, new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (id == 0) { // white to move
                        cb.pos.setWhiteMove(true);
                        checkValidAndUpdateMaterialDiff();
                        dialog.cancel();
                    } else {
                        cb.pos.setWhiteMove(false);
                        checkValidAndUpdateMaterialDiff();
                        dialog.cancel();
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case CASTLE_DIALOG: {
            final CharSequence[] items = {
                getString(R.string.white_king_castle), getString(R.string.white_queen_castle),
                getString(R.string.black_king_castle), getString(R.string.black_queen_castle)
            };
            boolean[] checkedItems = {
                    cb.pos.h1Castle(), cb.pos.a1Castle(),
                    cb.pos.h8Castle(), cb.pos.a8Castle()
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.castling_flags);
            builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    Position pos = new Position(cb.pos);
                    boolean a1Castle = pos.a1Castle();
                    boolean h1Castle = pos.h1Castle();
                    boolean a8Castle = pos.a8Castle();
                    boolean h8Castle = pos.h8Castle();
                    switch (which) {
                    case 0: h1Castle = isChecked; break;
                    case 1: a1Castle = isChecked; break;
                    case 2: h8Castle = isChecked; break;
                    case 3: a8Castle = isChecked; break;
                    }
                    int castleMask = 0;
                    if (a1Castle) castleMask |= 1 << Position.A1_CASTLE;
                    if (h1Castle) castleMask |= 1 << Position.H1_CASTLE;
                    if (a8Castle) castleMask |= 1 << Position.A8_CASTLE;
                    if (h8Castle) castleMask |= 1 << Position.H8_CASTLE;
                    pos.setCastleMask(castleMask);
                    cb.setPosition(pos);
                    checkValidAndUpdateMaterialDiff();
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case EP_DIALOG: {
            final CharSequence[] items = {
                    "A", "B", "C", "D", "E", "F", "G", "H", getString(R.string.none)
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_en_passant_file);
            builder.setSingleChoiceItems(items, getEPFile(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    setEPFile(item);
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case MOVCNT_DIALOG: {
            View content = View.inflate(this, R.layout.edit_move_counters, null);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setView(content);
            builder.setTitle(R.string.edit_move_counters);
            final EditText halfMoveClock = (EditText)content.findViewById(R.id.ed_cnt_halfmove);
            final EditText fullMoveCounter = (EditText)content.findViewById(R.id.ed_cnt_fullmove);
            halfMoveClock.setText(String.format(Locale.US, "%d", cb.pos.halfMoveClock));
            fullMoveCounter.setText(String.format(Locale.US, "%d", cb.pos.fullMoveCounter));
            final Runnable setCounters = new Runnable() {
                public void run() {
                    try {
                        int halfClock = Integer.parseInt(halfMoveClock.getText().toString());
                        int fullCount = Integer.parseInt(fullMoveCounter.getText().toString());
                        cb.pos.halfMoveClock = halfClock;
                        cb.pos.fullMoveCounter = fullCount;
                    } catch (NumberFormatException nfe) {
                        Toast.makeText(getApplicationContext(), R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
                    }
                }
            };
            builder.setPositiveButton("Ok", new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    setCounters.run();
                }
            });
            builder.setNegativeButton("Cancel", null);

            final Dialog dialog = builder.create();

            fullMoveCounter.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        setCounters.run();
                        dialog.cancel();
                        return true;
                    }
                    return false;
                }
            });
            return dialog;
        }
        }
        return null;
    }

    private final void setFEN(String fen) {
        if (fen == null)
            return;
        try {
            Position pos = TextIO.readFEN(fen);
            cb.setPosition(pos);
        } catch (ChessParseError e) {
            if (e.pos != null)
                cb.setPosition(e.pos);
            Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
        }
        setSelection(-1);
        checkValidAndUpdateMaterialDiff();
    }

    static private final int RESULT_GET_FEN  = 0;
    static private final int RESULT_LOAD_FEN = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case RESULT_GET_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getStringExtra(Intent.EXTRA_TEXT);
                if (fen == null) {
                    String pathName = DroidFish.getFilePathFromUri(data.getData());
                    Intent i = new Intent(EditBoard.this, LoadFEN.class);
                    i.setAction("org.petero.droidfish.loadFen");
                    i.putExtra("org.petero.droidfish.pathname", pathName);
                    startActivityForResult(i, RESULT_LOAD_FEN);
                }
                setFEN(fen);
            }
            break;
        case RESULT_LOAD_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getAction();
                setFEN(fen);
            }
            break;
        }
    }
}
