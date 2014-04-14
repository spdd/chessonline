/*
  Stockfish, a UCI chess playing engine derived from Glaurung 2.1
  Copyright (C) 2004-2008 Tord Romstad (Glaurung author)
  Copyright (C) 2008-2013 Marco Costalba, Joona Kiiski, Tord Romstad

  Stockfish is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Stockfish is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <cassert>
#include <iomanip>
#include <sstream>
#include <algorithm>

#include "bitcount.h"
#include "evaluate.h"
#include "material.h"
#include "pawns.h"
#include "thread.h"
#include "ucioption.h"

namespace {

  enum ExtendedPieceType { // Used for tracing
    PST = 8, IMBALANCE, MOBILITY, THREAT, PASSED, SPACE, TOTAL
  };

  namespace Tracing {

    Score scores[COLOR_NB][TOTAL + 1];
    std::stringstream stream;

    void add(int idx, Score term_w, Score term_b = SCORE_ZERO);
    void row(const char* name, int idx);
    std::string do_trace(const Position& pos);
  }

  // Struct EvalInfo contains various information computed and collected
  // by the evaluation functions.
  struct EvalInfo {

    // Pointers to material and pawn hash table entries
    Material::Entry* mi;
    Pawns::Entry* pi;

    // attackedBy[color][piece type] is a bitboard representing all squares
    // attacked by a given color and piece type, attackedBy[color][ALL_PIECES]
    // contains all squares attacked by the given color.
    Bitboard attackedBy[COLOR_NB][PIECE_TYPE_NB];

    // kingRing[color] is the zone around the king which is considered
    // by the king safety evaluation. This consists of the squares directly
    // adjacent to the king, and the three (or two, for a king on an edge file)
    // squares two ranks in front of the king. For instance, if black's king
    // is on g8, kingRing[BLACK] is a bitboard containing the squares f8, h8,
    // f7, g7, h7, f6, g6 and h6.
    Bitboard kingRing[COLOR_NB];

    // kingAttackersCount[color] is the number of pieces of the given color
    // which attack a square in the kingRing of the enemy king.
    int kingAttackersCount[COLOR_NB];

    // kingAttackersWeight[color] is the sum of the "weight" of the pieces of the
    // given color which attack a square in the kingRing of the enemy king. The
    // weights of the individual piece types are given by the variables
    // QueenAttackWeight, RookAttackWeight, BishopAttackWeight and
    // KnightAttackWeight in evaluate.cpp
    int kingAttackersWeight[COLOR_NB];

    // kingAdjacentZoneAttacksCount[color] is the number of attacks to squares
    // directly adjacent to the king of the given color. Pieces which attack
    // more than one square are counted multiple times. For instance, if black's
    // king is on g8 and there's a white knight on g5, this knight adds
    // 2 to kingAdjacentZoneAttacksCount[BLACK].
    int kingAdjacentZoneAttacksCount[COLOR_NB];

    Bitboard pinnedPieces[COLOR_NB];
  };

  // Evaluation grain size, must be a power of 2
  const int GrainSize = 4;

  // Evaluation weights, initialized from UCI options
  enum { Mobility, PawnStructure, PassedPawns, Space, KingDangerUs, KingDangerThem };
  Score Weights[6];

  typedef Value V;
  #define S(mg, eg) make_score(mg, eg)

  // Internal evaluation weights. These are applied on top of the evaluation
  // weights read from UCI parameters. The purpose is to be able to change
  // the evaluation weights while keeping the default values of the UCI
  // parameters at 100, which looks prettier.
  //
  // Values modified by Joona Kiiski
  const Score WeightsInternal[] = {
      S(289, 344), S(233, 201), S(221, 273), S(46, 0), S(271, 0), S(307, 0)
  };

  // MobilityBonus[PieceType][attacked] contains bonuses for middle and end
  // game, indexed by piece type and number of attacked squares not occupied by
  // friendly pieces.
  const Score MobilityBonus[][32] = {
     {}, {},
     { S(-35,-30), S(-22,-20), S(-9,-10), S( 3,  0), S(15, 10), S(27, 20), // Knights
       S( 37, 28), S( 42, 31), S(44, 33) },
     { S(-22,-27), S( -8,-13), S( 6,  1), S(20, 15), S(34, 29), S(48, 43), // Bishops
       S( 60, 55), S( 68, 63), S(74, 68), S(77, 72), S(80, 75), S(82, 77),
       S( 84, 79), S( 86, 81) },
     { S(-17,-33), S(-11,-16), S(-5,  0), S( 1, 16), S( 7, 32), S(13, 48), // Rooks
       S( 18, 64), S( 22, 80), S(26, 96), S(29,109), S(31,115), S(33,119),
       S( 35,122), S( 36,123), S(37,124) },
     { S(-12,-20), S( -8,-13), S(-5, -7), S(-2, -1), S( 1,  5), S( 4, 11), // Queens
       S(  7, 17), S( 10, 23), S(13, 29), S(16, 34), S(18, 38), S(20, 40),
       S( 22, 41), S( 23, 41), S(24, 41), S(25, 41), S(25, 41), S(25, 41),
       S( 25, 41), S( 25, 41), S(25, 41), S(25, 41), S(25, 41), S(25, 41),
       S( 25, 41), S( 25, 41), S(25, 41), S(25, 41) }
  };

  // Outpost[PieceType][Square] contains bonuses of knights and bishops, indexed
  // by piece type and square (from white's point of view).
  const Value Outpost[][SQUARE_NB] = {
  {
  //  A     B     C     D     E     F     G     H
    V(0), V(0), V(0), V(0), V(0), V(0), V(0), V(0), // Knights
    V(0), V(0), V(0), V(0), V(0), V(0), V(0), V(0),
    V(0), V(0), V(4), V(8), V(8), V(4), V(0), V(0),
    V(0), V(4),V(17),V(26),V(26),V(17), V(4), V(0),
    V(0), V(8),V(26),V(35),V(35),V(26), V(8), V(0),
    V(0), V(4),V(17),V(17),V(17),V(17), V(4), V(0) },
  {
    V(0), V(0), V(0), V(0), V(0), V(0), V(0), V(0), // Bishops
    V(0), V(0), V(0), V(0), V(0), V(0), V(0), V(0),
    V(0), V(0), V(5), V(5), V(5), V(5), V(0), V(0),
    V(0), V(5),V(10),V(10),V(10),V(10), V(5), V(0),
    V(0),V(10),V(21),V(21),V(21),V(21),V(10), V(0),
    V(0), V(5), V(8), V(8), V(8), V(8), V(5), V(0) }
  };

  // Threat[attacking][attacked] contains bonuses according to which piece
  // type attacks which one.
  const Score Threat[][PIECE_TYPE_NB] = {
    {}, {},
    { S(0, 0), S( 7, 39), S( 0,  0), S(24, 49), S(41,100), S(41,100) }, // KNIGHT
    { S(0, 0), S( 7, 39), S(24, 49), S( 0,  0), S(41,100), S(41,100) }, // BISHOP
    { S(0, 0), S( 0, 22), S(15, 49), S(15, 49), S( 0,  0), S(24, 49) }, // ROOK
    { S(0, 0), S(15, 39), S(15, 39), S(15, 39), S(15, 39), S( 0,  0) }  // QUEEN
  };

  // ThreatenedByPawn[PieceType] contains a penalty according to which piece
  // type is attacked by an enemy pawn.
  const Score ThreatenedByPawn[] = {
    S(0, 0), S(0, 0), S(56, 70), S(56, 70), S(76, 99), S(86, 118)
  };

  #undef S

  const Score Tempo            = make_score(24, 11);
  const Score BishopPin        = make_score(66, 11);
  const Score RookOn7th        = make_score(11, 20);
  const Score QueenOn7th       = make_score( 3,  8);
  const Score RookOnPawn       = make_score(10, 28);
  const Score QueenOnPawn      = make_score( 4, 20);
  const Score RookOpenFile     = make_score(43, 21);
  const Score RookSemiopenFile = make_score(19, 10);
  const Score BishopPawns      = make_score( 8, 12);
  const Score KnightPawns      = make_score( 8,  4);
  const Score MinorBehindPawn  = make_score(16,  0);
  const Score UndefendedMinor  = make_score(25, 10);
  const Score TrappedRook      = make_score(90,  0);
  const Score Unstoppable      = make_score( 0, 20);

  // Penalty for a bishop on a1/h1 (a8/h8 for black) which is trapped by
  // a friendly pawn on b2/g2 (b7/g7 for black). This can obviously only
  // happen in Chess960 games.
  const Score TrappedBishopA1H1 = make_score(50, 50);

  // The SpaceMask[Color] contains the area of the board which is considered
  // by the space evaluation. In the middle game, each side is given a bonus
  // based on how many squares inside this area are safe and available for
  // friendly minor pieces.
  const Bitboard SpaceMask[] = {
    (FileCBB | FileDBB | FileEBB | FileFBB) & (Rank2BB | Rank3BB | Rank4BB),
    (FileCBB | FileDBB | FileEBB | FileFBB) & (Rank7BB | Rank6BB | Rank5BB)
  };

  // King danger constants and variables. The king danger scores are taken
  // from the KingDanger[]. Various little "meta-bonuses" measuring
  // the strength of the enemy attack are added up into an integer, which
  // is used as an index to KingDanger[].
  //
  // KingAttackWeights[PieceType] contains king attack weights by piece type
  const int KingAttackWeights[] = { 0, 0, 2, 2, 3, 5 };

  // Bonuses for enemy's safe checks
  const int QueenContactCheck = 24;
  const int RookContactCheck  = 16;
  const int QueenCheck        = 12;
  const int RookCheck         = 8;
  const int BishopCheck       = 2;
  const int KnightCheck       = 3;

  // KingExposed[Square] contains penalties based on the position of the
  // defending king, indexed by king's square (from white's point of view).
  const int KingExposed[] = {
     2,  0,  2,  5,  5,  2,  0,  2,
     2,  2,  4,  8,  8,  4,  2,  2,
     7, 10, 12, 12, 12, 12, 10,  7,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15
  };

  // KingDanger[Color][attackUnits] contains the actual king danger weighted
  // scores, indexed by color and by a calculated integer number.
  Score KingDanger[COLOR_NB][128];

  // Function prototypes
  template<bool Trace>
  Value do_evaluate(const Position& pos);

  template<Color Us>
  void init_eval_info(const Position& pos, EvalInfo& ei);

  template<Color Us, bool Trace>
  Score evaluate_pieces_of_color(const Position& pos, EvalInfo& ei, Score* mobility);

  template<Color Us, bool Trace>
  Score evaluate_king(const Position& pos, const EvalInfo& ei);

  template<Color Us, bool Trace>
  Score evaluate_threats(const Position& pos, const EvalInfo& ei);

  template<Color Us, bool Trace>
  Score evaluate_passed_pawns(const Position& pos, const EvalInfo& ei);

  template<Color Us>
  int evaluate_space(const Position& pos, const EvalInfo& ei);

  Score evaluate_unstoppable_pawns(const Position& pos, Color us, const EvalInfo& ei);

  Value interpolate(const Score& v, Phase ph, ScaleFactor sf);
  Score apply_weight(Score v, Score w);
  Score weight_option(const std::string& mgOpt, const std::string& egOpt, Score internalWeight);
  double to_cp(Value v);
}


namespace Eval {

  /// evaluate() is the main evaluation function. It always computes two
  /// values, an endgame score and a middle game score, and interpolates
  /// between them based on the remaining material.

  Value evaluate(const Position& pos) {
    return do_evaluate<false>(pos);
  }


  /// trace() is like evaluate() but instead of a value returns a string suitable
  /// to be print on stdout with the detailed descriptions and values of each
  /// evaluation term. Used mainly for debugging.
  std::string trace(const Position& pos) {
    return Tracing::do_trace(pos);
  }


  /// init() computes evaluation weights from the corresponding UCI parameters
  /// and setup king tables.

  void init() {

    Weights[Mobility]       = weight_option("Mobility (Midgame)", "Mobility (Endgame)", WeightsInternal[Mobility]);
    Weights[PawnStructure]  = weight_option("Pawn Structure (Midgame)", "Pawn Structure (Endgame)", WeightsInternal[PawnStructure]);
    Weights[PassedPawns]    = weight_option("Passed Pawns (Midgame)", "Passed Pawns (Endgame)", WeightsInternal[PassedPawns]);
    Weights[Space]          = weight_option("Space", "Space", WeightsInternal[Space]);
    Weights[KingDangerUs]   = weight_option("Cowardice", "Cowardice", WeightsInternal[KingDangerUs]);
    Weights[KingDangerThem] = weight_option("Aggressiveness", "Aggressiveness", WeightsInternal[KingDangerThem]);

    const int MaxSlope = 30;
    const int Peak = 1280;

    for (int t = 0, i = 1; i < 100; ++i)
    {
        t = std::min(Peak, std::min(int(0.4 * i * i), t + MaxSlope));

        KingDanger[1][i] = apply_weight(make_score(t, 0), Weights[KingDangerUs]);
        KingDanger[0][i] = apply_weight(make_score(t, 0), Weights[KingDangerThem]);
    }
  }

} // namespace Eval


namespace {

template<bool Trace>
Value do_evaluate(const Position& pos) {

  assert(!pos.checkers());

  EvalInfo ei;
  Score score, mobility[2] = { SCORE_ZERO, SCORE_ZERO };
  Thread* th = pos.this_thread();

  // Initialize score by reading the incrementally updated scores included
  // in the position object (material + piece square tables) and adding
  // Tempo bonus. Score is computed from the point of view of white.
  score = pos.psq_score() + (pos.side_to_move() == WHITE ? Tempo : -Tempo);

  // Probe the material hash table
  ei.mi = Material::probe(pos, th->materialTable, th->endgames);
  score += ei.mi->material_value();

  // If we have a specialized evaluation function for the current material
  // configuration, call it and return.
  if (ei.mi->specialized_eval_exists())
      return ei.mi->evaluate(pos);

  // Probe the pawn hash table
  ei.pi = Pawns::probe(pos, th->pawnsTable);
  score += apply_weight(ei.pi->pawns_value(), Weights[PawnStructure]);

  // Initialize attack and king safety bitboards
  init_eval_info<WHITE>(pos, ei);
  init_eval_info<BLACK>(pos, ei);

  // Evaluate pieces and mobility
  score +=  evaluate_pieces_of_color<WHITE, Trace>(pos, ei, mobility)
          - evaluate_pieces_of_color<BLACK, Trace>(pos, ei, mobility);

  score += apply_weight(mobility[WHITE] - mobility[BLACK], Weights[Mobility]);

  // Evaluate kings after all other pieces because we need complete attack
  // information when computing the king safety evaluation.
  score +=  evaluate_king<WHITE, Trace>(pos, ei)
          - evaluate_king<BLACK, Trace>(pos, ei);

  // Evaluate tactical threats, we need full attack information including king
  score +=  evaluate_threats<WHITE, Trace>(pos, ei)
          - evaluate_threats<BLACK, Trace>(pos, ei);

  // Evaluate passed pawns, we need full attack information including king
  score +=  evaluate_passed_pawns<WHITE, Trace>(pos, ei)
          - evaluate_passed_pawns<BLACK, Trace>(pos, ei);

  // If one side has only a king, score for potential unstoppable pawns
  if (!pos.non_pawn_material(WHITE) || !pos.non_pawn_material(BLACK))
      score +=  evaluate_unstoppable_pawns(pos, WHITE, ei)
              - evaluate_unstoppable_pawns(pos, BLACK, ei);

  // Evaluate space for both sides, only in middle-game.
  if (ei.mi->space_weight())
  {
      int s = evaluate_space<WHITE>(pos, ei) - evaluate_space<BLACK>(pos, ei);
      score += apply_weight(s * ei.mi->space_weight(), Weights[Space]);
  }

  // Scale winning side if position is more drawish that what it appears
  ScaleFactor sf = eg_value(score) > VALUE_DRAW ? ei.mi->scale_factor(pos, WHITE)
                                                : ei.mi->scale_factor(pos, BLACK);

  // If we don't already have an unusual scale factor, check for opposite
  // colored bishop endgames, and use a lower scale for those.
  if (   ei.mi->game_phase() < PHASE_MIDGAME
      && pos.opposite_bishops()
      && sf == SCALE_FACTOR_NORMAL)
  {
      // Only the two bishops ?
      if (   pos.non_pawn_material(WHITE) == BishopValueMg
          && pos.non_pawn_material(BLACK) == BishopValueMg)
      {
          // Check for KBP vs KB with only a single pawn that is almost
          // certainly a draw or at least two pawns.
          bool one_pawn = (pos.count<PAWN>(WHITE) + pos.count<PAWN>(BLACK) == 1);
          sf = one_pawn ? ScaleFactor(8) : ScaleFactor(32);
      }
      else
          // Endgame with opposite-colored bishops, but also other pieces. Still
          // a bit drawish, but not as drawish as with only the two bishops.
           sf = ScaleFactor(50);
  }

  Value v = interpolate(score, ei.mi->game_phase(), sf);

  // In case of tracing add all single evaluation contributions for both white and black
  if (Trace)
  {
      Tracing::add(PST, pos.psq_score());
      Tracing::add(IMBALANCE, ei.mi->material_value());
      Tracing::add(PAWN, ei.pi->pawns_value());
      Score w = ei.mi->space_weight() * evaluate_space<WHITE>(pos, ei);
      Score b = ei.mi->space_weight() * evaluate_space<BLACK>(pos, ei);
      Tracing::add(SPACE, apply_weight(w, Weights[Space]), apply_weight(b, Weights[Space]));
      Tracing::add(TOTAL, score);
      Tracing::stream << "\nScaling: " << std::noshowpos
                      << std::setw(6) << 100.0 * ei.mi->game_phase() / 128.0 << "% MG, "
                      << std::setw(6) << 100.0 * (1.0 - ei.mi->game_phase() / 128.0) << "% * "
                      << std::setw(6) << (100.0 * sf) / SCALE_FACTOR_NORMAL << "% EG.\n"
                      << "Total evaluation: " << to_cp(v);
  }

  return pos.side_to_move() == WHITE ? v : -v;
}


  // init_eval_info() initializes king bitboards for given color adding
  // pawn attacks. To be done at the beginning of the evaluation.

  template<Color Us>
  void init_eval_info(const Position& pos, EvalInfo& ei) {

    const Color  Them = (Us == WHITE ? BLACK : WHITE);
    const Square Down = (Us == WHITE ? DELTA_S : DELTA_N);

    ei.pinnedPieces[Us] = pos.pinned_pieces(Us);

    Bitboard b = ei.attackedBy[Them][KING] = pos.attacks_from<KING>(pos.king_square(Them));
    ei.attackedBy[Us][PAWN] = ei.pi->pawn_attacks(Us);

    // Init king safety tables only if we are going to use them
    if (pos.count<QUEEN>(Us) && pos.non_pawn_material(Us) > QueenValueMg + PawnValueMg)
    {
        ei.kingRing[Them] = b | shift_bb<Down>(b);
        b &= ei.attackedBy[Us][PAWN];
        ei.kingAttackersCount[Us] = b ? popcount<Max15>(b) / 2 : 0;
        ei.kingAdjacentZoneAttacksCount[Us] = ei.kingAttackersWeight[Us] = 0;
    }
    else
        ei.kingRing[Them] = ei.kingAttackersCount[Us] = 0;
  }


  // evaluate_outposts() evaluates bishop and knight outposts squares

  template<PieceType Piece, Color Us>
  Score evaluate_outposts(const Position& pos, EvalInfo& ei, Square s) {

    const Color Them = (Us == WHITE ? BLACK : WHITE);

    assert (Piece == BISHOP || Piece == KNIGHT);

    // Initial bonus based on square
    Value bonus = Outpost[Piece == BISHOP][relative_square(Us, s)];

    // Increase bonus if supported by pawn, especially if the opponent has
    // no minor piece which can exchange the outpost piece.
    if (bonus && (ei.attackedBy[Us][PAWN] & s))
    {
        if (   !pos.pieces(Them, KNIGHT)
            && !(squares_of_color(s) & pos.pieces(Them, BISHOP)))
            bonus += bonus + bonus / 2;
        else
            bonus += bonus / 2;
    }

    return make_score(bonus, bonus);
  }


  // evaluate_pieces() assigns bonuses and penalties to the pieces of a given color

  template<PieceType Piece, Color Us, bool Trace>
  Score evaluate_pieces(const Position& pos, EvalInfo& ei, Score* mobility, Bitboard mobilityArea) {

    Bitboard b;
    Square s;
    Score score = SCORE_ZERO;

    const Color Them = (Us == WHITE ? BLACK : WHITE);
    const Square* pl = pos.list<Piece>(Us);

    ei.attackedBy[Us][Piece] = 0;

    while ((s = *pl++) != SQ_NONE)
    {
        // Find attacked squares, including x-ray attacks for bishops and rooks
        b = Piece == BISHOP ? attacks_bb<BISHOP>(s, pos.pieces() ^ pos.pieces(Us, QUEEN))
          : Piece ==   ROOK ? attacks_bb<  ROOK>(s, pos.pieces() ^ pos.pieces(Us, ROOK, QUEEN))
                            : pos.attacks_from<Piece>(s);

        if (ei.pinnedPieces[Us] & s)
            b &= LineBB[pos.king_square(Us)][s];

        ei.attackedBy[Us][Piece] |= b;

        if (b & ei.kingRing[Them])
        {
            ei.kingAttackersCount[Us]++;
            ei.kingAttackersWeight[Us] += KingAttackWeights[Piece];
            Bitboard bb = b & ei.attackedBy[Them][KING];
            if (bb)
                ei.kingAdjacentZoneAttacksCount[Us] += popcount<Max15>(bb);
        }

        int mob = Piece != QUEEN ? popcount<Max15>(b & mobilityArea)
                                 : popcount<Full >(b & mobilityArea);

        mobility[Us] += MobilityBonus[Piece][mob];

        // Decrease score if we are attacked by an enemy pawn. Remaining part
        // of threat evaluation must be done later when we have full attack info.
        if (ei.attackedBy[Them][PAWN] & s)
            score -= ThreatenedByPawn[Piece];

        // Otherwise give a bonus if we are a bishop and can pin a piece or can
        // give a discovered check through an x-ray attack.
        else if (    Piece == BISHOP
                 && (PseudoAttacks[Piece][pos.king_square(Them)] & s)
                 && !more_than_one(BetweenBB[s][pos.king_square(Them)] & pos.pieces()))
                 score += BishopPin;

        // Penalty for bishop with same coloured pawns
        if (Piece == BISHOP)
            score -= BishopPawns * ei.pi->pawns_on_same_color_squares(Us, s);

        // Penalty for knight when there are few enemy pawns
        if (Piece == KNIGHT)
            score -= KnightPawns * std::max(5 - pos.count<PAWN>(Them), 0);

        if (Piece == BISHOP || Piece == KNIGHT)
        {
            // Bishop and knight outposts squares
            if (!(pos.pieces(Them, PAWN) & pawn_attack_span(Us, s)))
                score += evaluate_outposts<Piece, Us>(pos, ei, s);

            // Bishop or knight behind a pawn
            if (    relative_rank(Us, s) < RANK_5
                && (pos.pieces(PAWN) & (s + pawn_push(Us))))
                score += MinorBehindPawn;
        }

        if (  (Piece == ROOK || Piece == QUEEN)
            && relative_rank(Us, s) >= RANK_5)
        {
            // Major piece on 7th rank and enemy king trapped on 8th
            if (   relative_rank(Us, s) == RANK_7
                && relative_rank(Us, pos.king_square(Them)) == RANK_8)
                score += Piece == ROOK ? RookOn7th : QueenOn7th;

            // Major piece attacking enemy pawns on the same rank/file
            Bitboard pawns = pos.pieces(Them, PAWN) & PseudoAttacks[ROOK][s];
            if (pawns)
                score += popcount<Max15>(pawns) * (Piece == ROOK ? RookOnPawn : QueenOnPawn);
        }

        // Special extra evaluation for rooks
        if (Piece == ROOK)
        {
            // Give a bonus for a rook on a open or semi-open file
            if (ei.pi->semiopen(Us, file_of(s)))
                score += ei.pi->semiopen(Them, file_of(s)) ? RookOpenFile : RookSemiopenFile;

            if (mob > 3 || ei.pi->semiopen(Us, file_of(s)))
                continue;

            Square ksq = pos.king_square(Us);

            // Penalize rooks which are trapped inside a king. Penalize more if
            // king has lost right to castle.
            if (   ((file_of(ksq) < FILE_E) == (file_of(s) < file_of(ksq)))
                && (rank_of(ksq) == rank_of(s) || relative_rank(Us, ksq) == RANK_1)
                && !ei.pi->semiopen_on_side(Us, file_of(ksq), file_of(ksq) < FILE_E))
                score -= (TrappedRook - make_score(mob * 8, 0)) * (pos.can_castle(Us) ? 1 : 2);
        }

        // An important Chess960 pattern: A cornered bishop blocked by a friendly
        // pawn diagonally in front of it is a very serious problem, especially
        // when that pawn is also blocked.
        if (   Piece == BISHOP
            && pos.is_chess960()
            && (s == relative_square(Us, SQ_A1) || s == relative_square(Us, SQ_H1)))
        {
            const enum Piece P = make_piece(Us, PAWN);
            Square d = pawn_push(Us) + (file_of(s) == FILE_A ? DELTA_E : DELTA_W);
            if (pos.piece_on(s + d) == P)
                score -= !pos.empty(s + d + pawn_push(Us)) ? TrappedBishopA1H1 * 4
                        : pos.piece_on(s + d + d) == P     ? TrappedBishopA1H1 * 2
                                                           : TrappedBishopA1H1;
        }
    }

    if (Trace)
        Tracing::scores[Us][Piece] = score;

    return score;
  }


  // evaluate_pieces_of_color() assigns bonuses and penalties to all the
  // pieces of a given color.

  template<Color Us, bool Trace>
  Score evaluate_pieces_of_color(const Position& pos, EvalInfo& ei, Score* mobility) {

    const Color Them = (Us == WHITE ? BLACK : WHITE);

    // Do not include in mobility squares protected by enemy pawns or occupied by our pieces
    const Bitboard mobilityArea = ~(ei.attackedBy[Them][PAWN] | pos.pieces(Us, PAWN, KING));

    Score score =  evaluate_pieces<KNIGHT, Us, Trace>(pos, ei, mobility, mobilityArea)
                 + evaluate_pieces<BISHOP, Us, Trace>(pos, ei, mobility, mobilityArea)
                 + evaluate_pieces<ROOK,   Us, Trace>(pos, ei, mobility, mobilityArea)
                 + evaluate_pieces<QUEEN,  Us, Trace>(pos, ei, mobility, mobilityArea);

    // Sum up all attacked squares (updated in evaluate_pieces)
    ei.attackedBy[Us][ALL_PIECES] =  ei.attackedBy[Us][PAWN]   | ei.attackedBy[Us][KNIGHT]
                                   | ei.attackedBy[Us][BISHOP] | ei.attackedBy[Us][ROOK]
                                   | ei.attackedBy[Us][QUEEN]  | ei.attackedBy[Us][KING];
    if (Trace)
        Tracing::scores[Us][MOBILITY] = apply_weight(mobility[Us], Weights[Mobility]);

    return score;
  }


  // evaluate_king() assigns bonuses and penalties to a king of a given color

  template<Color Us, bool Trace>
  Score evaluate_king(const Position& pos, const EvalInfo& ei) {

    const Color Them = (Us == WHITE ? BLACK : WHITE);

    Bitboard undefended, b, b1, b2, safe;
    int attackUnits;
    const Square ksq = pos.king_square(Us);

    // King shelter and enemy pawns storm
    Score score = ei.pi->king_safety<Us>(pos, ksq);

    // Main king safety evaluation
    if (   ei.kingAttackersCount[Them] >= 2
        && ei.kingAdjacentZoneAttacksCount[Them])
    {
        // Find the attacked squares around the king which has no defenders
        // apart from the king itself
        undefended =  ei.attackedBy[Them][ALL_PIECES]
                    & ei.attackedBy[Us][KING]
                    & ~(  ei.attackedBy[Us][PAWN]   | ei.attackedBy[Us][KNIGHT]
                        | ei.attackedBy[Us][BISHOP] | ei.attackedBy[Us][ROOK]
                        | ei.attackedBy[Us][QUEEN]);

        // Initialize the 'attackUnits' variable, which is used later on as an
        // index to the KingDanger[] array. The initial value is based on the
        // number and types of the enemy's attacking pieces, the number of
        // attacked and undefended squares around our king, the square of the
        // king, and the quality of the pawn shelter.
        attackUnits =  std::min(20, (ei.kingAttackersCount[Them] * ei.kingAttackersWeight[Them]) / 2)
                     + 3 * (ei.kingAdjacentZoneAttacksCount[Them] + popcount<Max15>(undefended))
                     + KingExposed[relative_square(Us, ksq)]
                     - mg_value(score) / 32;

        // Analyse enemy's safe queen contact checks. First find undefended
        // squares around the king attacked by enemy queen...
        b = undefended & ei.attackedBy[Them][QUEEN] & ~pos.pieces(Them);
        if (b)
        {
            // ...then remove squares not supported by another enemy piece
            b &= (  ei.attackedBy[Them][PAWN]   | ei.attackedBy[Them][KNIGHT]
                  | ei.attackedBy[Them][BISHOP] | ei.attackedBy[Them][ROOK]);
            if (b)
                attackUnits +=  QueenContactCheck
                              * popcount<Max15>(b)
                              * (Them == pos.side_to_move() ? 2 : 1);
        }

        // Analyse enemy's safe rook contact checks. First find undefended
        // squares around the king attacked by enemy rooks...
        b = undefended & ei.attackedBy[Them][ROOK] & ~pos.pieces(Them);

        // Consider only squares where the enemy rook gives check
        b &= PseudoAttacks[ROOK][ksq];

        if (b)
        {
            // ...then remove squares not supported by another enemy piece
            b &= (  ei.attackedBy[Them][PAWN]   | ei.attackedBy[Them][KNIGHT]
                  | ei.attackedBy[Them][BISHOP] | ei.attackedBy[Them][QUEEN]);
            if (b)
                attackUnits +=  RookContactCheck
                              * popcount<Max15>(b)
                              * (Them == pos.side_to_move() ? 2 : 1);
        }

        // Analyse enemy's safe distance checks for sliders and knights
        safe = ~(pos.pieces(Them) | ei.attackedBy[Us][ALL_PIECES]);

        b1 = pos.attacks_from<ROOK>(ksq) & safe;
        b2 = pos.attacks_from<BISHOP>(ksq) & safe;

        // Enemy queen safe checks
        b = (b1 | b2) & ei.attackedBy[Them][QUEEN];
        if (b)
            attackUnits += QueenCheck * popcount<Max15>(b);

        // Enemy rooks safe checks
        b = b1 & ei.attackedBy[Them][ROOK];
        if (b)
            attackUnits += RookCheck * popcount<Max15>(b);

        // Enemy bishops safe checks
        b = b2 & ei.attackedBy[Them][BISHOP];
        if (b)
            attackUnits += BishopCheck * popcount<Max15>(b);

        // Enemy knights safe checks
        b = pos.attacks_from<KNIGHT>(ksq) & ei.attackedBy[Them][KNIGHT] & safe;
        if (b)
            attackUnits += KnightCheck * popcount<Max15>(b);

        // To index KingDanger[] attackUnits must be in [0, 99] range
        attackUnits = std::min(99, std::max(0, attackUnits));

        // Finally, extract the king danger score from the KingDanger[]
        // array and subtract the score from evaluation.
        score -= KingDanger[Us == Search::RootColor][attackUnits];
    }

    if (Trace)
        Tracing::scores[Us][KING] = score;

    return score;
  }


  // evaluate_threats() assigns bonuses according to the type of attacking piece
  // and the type of attacked one.

  template<Color Us, bool Trace>
  Score evaluate_threats(const Position& pos, const EvalInfo& ei) {

    const Color Them = (Us == WHITE ? BLACK : WHITE);

    Bitboard b, undefendedMinors, weakEnemies;
    Score score = SCORE_ZERO;

    // Undefended minors get penalized even if not under attack
    undefendedMinors =  pos.pieces(Them, BISHOP, KNIGHT)
                      & ~ei.attackedBy[Them][ALL_PIECES];

    if (undefendedMinors)
        score += UndefendedMinor;

    // Enemy pieces not defended by a pawn and under our attack
    weakEnemies =  pos.pieces(Them)
                 & ~ei.attackedBy[Them][PAWN]
                 & ei.attackedBy[Us][ALL_PIECES];

    // Add bonus according to type of attacked enemy piece and to the
    // type of attacking piece, from knights to queens. Kings are not
    // considered because are already handled in king evaluation.
    if (weakEnemies)
        for (PieceType pt1 = KNIGHT; pt1 < KING; ++pt1)
        {
            b = ei.attackedBy[Us][pt1] & weakEnemies;
            if (b)
                for (PieceType pt2 = PAWN; pt2 < KING; ++pt2)
                    if (b & pos.pieces(pt2))
                        score += Threat[pt1][pt2];
        }

    if (Trace)
        Tracing::scores[Us][THREAT] = score;

    return score;
  }


  // evaluate_passed_pawns() evaluates the passed pawns of the given color

  template<Color Us, bool Trace>
  Score evaluate_passed_pawns(const Position& pos, const EvalInfo& ei) {

    const Color Them = (Us == WHITE ? BLACK : WHITE);

    Bitboard b, squaresToQueen, defendedSquares, unsafeSquares, supportingPawns;
    Score score = SCORE_ZERO;

    b = ei.pi->passed_pawns(Us);

    while (b)
    {
        Square s = pop_lsb(&b);

        assert(pos.pawn_passed(Us, s));

        int r = int(relative_rank(Us, s) - RANK_2);
        int rr = r * (r - 1);

        // Base bonus based on rank
        Value mbonus = Value(17 * rr);
        Value ebonus = Value(7 * (rr + r + 1));

        if (rr)
        {
            Square blockSq = s + pawn_push(Us);

            // Adjust bonus based on kings proximity
            ebonus +=  Value(square_distance(pos.king_square(Them), blockSq) * 5 * rr)
                     - Value(square_distance(pos.king_square(Us  ), blockSq) * 2 * rr);

            // If blockSq is not the queening square then consider also a second push
            if (relative_rank(Us, blockSq) != RANK_8)
                ebonus -= Value(square_distance(pos.king_square(Us), blockSq + pawn_push(Us)) * rr);

            // If the pawn is free to advance, increase bonus
            if (pos.empty(blockSq))
            {
                squaresToQueen = forward_bb(Us, s);

                // If there is an enemy rook or queen attacking the pawn from behind,
                // add all X-ray attacks by the rook or queen. Otherwise consider only
                // the squares in the pawn's path attacked or occupied by the enemy.
                if (    unlikely(forward_bb(Them, s) & pos.pieces(Them, ROOK, QUEEN))
                    && (forward_bb(Them, s) & pos.pieces(Them, ROOK, QUEEN) & pos.attacks_from<ROOK>(s)))
                    unsafeSquares = squaresToQueen;
                else
                    unsafeSquares = squaresToQueen & (ei.attackedBy[Them][ALL_PIECES] | pos.pieces(Them));

                if (    unlikely(forward_bb(Them, s) & pos.pieces(Us, ROOK, QUEEN))
                    && (forward_bb(Them, s) & pos.pieces(Us, ROOK, QUEEN) & pos.attacks_from<ROOK>(s)))
                    defendedSquares = squaresToQueen;
                else
                    defendedSquares = squaresToQueen & ei.attackedBy[Us][ALL_PIECES];

                // If there aren't enemy attacks huge bonus, a bit smaller if at
                // least block square is not attacked, otherwise smallest bonus.
                int k = !unsafeSquares ? 15 : !(unsafeSquares & blockSq) ? 9 : 3;

                // Big bonus if the path to queen is fully defended, a bit less
                // if at least block square is defended.
                if (defendedSquares == squaresToQueen)
                    k += 6;

                else if (defendedSquares & blockSq)
                    k += (unsafeSquares & defendedSquares) == unsafeSquares ? 4 : 2;

                mbonus += Value(k * rr), ebonus += Value(k * rr);
            }
        } // rr != 0

        // Increase the bonus if the passed pawn is supported by a friendly pawn
        // on the same rank and a bit smaller if it's on the previous rank.
        supportingPawns = pos.pieces(Us, PAWN) & adjacent_files_bb(file_of(s));
        if (supportingPawns & rank_bb(s))
            ebonus += Value(r * 20);

        else if (supportingPawns & rank_bb(s - pawn_push(Us)))
            ebonus += Value(r * 12);

        // Rook pawns are a special case: They are sometimes worse, and
        // sometimes better than other passed pawns. It is difficult to find
        // good rules for determining whether they are good or bad. For now,
        // we try the following: Increase the value for rook pawns if the
        // other side has no pieces apart from a knight, and decrease the
        // value if the other side has a rook or queen.
        if (file_of(s) == FILE_A || file_of(s) == FILE_H)
        {
            if (pos.non_pawn_material(Them) <= KnightValueMg)
                ebonus += ebonus / 4;

            else if (pos.pieces(Them, ROOK, QUEEN))
                ebonus -= ebonus / 4;
        }

        if (pos.count<PAWN>(Us) < pos.count<PAWN>(Them))
            ebonus += ebonus / 4;

        score += make_score(mbonus, ebonus);

    }

    if (Trace)
        Tracing::scores[Us][PASSED] = apply_weight(score, Weights[PassedPawns]);

    // Add the scores to the middle game and endgame eval
    return apply_weight(score, Weights[PassedPawns]);
  }


  // evaluate_unstoppable_pawns() scores the most advanced among the passed and
  // candidate pawns. In case opponent has no pieces but pawns, this is somewhat
  // related to the possibility pawns are unstoppable.

  Score evaluate_unstoppable_pawns(const Position& pos, Color us, const EvalInfo& ei) {

    Bitboard b = ei.pi->passed_pawns(us) | ei.pi->candidate_pawns(us);

    if (!b || pos.non_pawn_material(~us))
        return SCORE_ZERO;

    return Unstoppable * int(relative_rank(us, frontmost_sq(us, b)));
  }


  // evaluate_space() computes the space evaluation for a given side. The
  // space evaluation is a simple bonus based on the number of safe squares
  // available for minor pieces on the central four files on ranks 2--4. Safe
  // squares one, two or three squares behind a friendly pawn are counted
  // twice. Finally, the space bonus is scaled by a weight taken from the
  // material hash table. The aim is to improve play on game opening.
  template<Color Us>
  int evaluate_space(const Position& pos, const EvalInfo& ei) {

    const Color Them = (Us == WHITE ? BLACK : WHITE);

    // Find the safe squares for our pieces inside the area defined by
    // SpaceMask[]. A square is unsafe if it is attacked by an enemy
    // pawn, or if it is undefended and attacked by an enemy piece.
    Bitboard safe =   SpaceMask[Us]
                   & ~pos.pieces(Us, PAWN)
                   & ~ei.attackedBy[Them][PAWN]
                   & (ei.attackedBy[Us][ALL_PIECES] | ~ei.attackedBy[Them][ALL_PIECES]);

    // Find all squares which are at most three squares behind some friendly pawn
    Bitboard behind = pos.pieces(Us, PAWN);
    behind |= (Us == WHITE ? behind >>  8 : behind <<  8);
    behind |= (Us == WHITE ? behind >> 16 : behind << 16);

    // Since SpaceMask[Us] is fully on our half of the board
    assert(unsigned(safe >> (Us == WHITE ? 32 : 0)) == 0);

    // Count safe + (behind & safe) with a single popcount
    return popcount<Full>((Us == WHITE ? safe << 32 : safe >> 32) | (behind & safe));
  }


  // interpolate() interpolates between a middle game and an endgame score,
  // based on game phase. It also scales the return value by a ScaleFactor array.

  Value interpolate(const Score& v, Phase ph, ScaleFactor sf) {

    assert(mg_value(v) > -VALUE_INFINITE && mg_value(v) < VALUE_INFINITE);
    assert(eg_value(v) > -VALUE_INFINITE && eg_value(v) < VALUE_INFINITE);
    assert(ph >= PHASE_ENDGAME && ph <= PHASE_MIDGAME);

    int e = (eg_value(v) * int(sf)) / SCALE_FACTOR_NORMAL;
    int r = (mg_value(v) * int(ph) + e * int(PHASE_MIDGAME - ph)) / PHASE_MIDGAME;
    return Value((r / GrainSize) * GrainSize); // Sign independent
  }

  // apply_weight() weights score v by score w trying to prevent overflow
  Score apply_weight(Score v, Score w) {
    return make_score((int(mg_value(v)) * mg_value(w)) / 0x100,
                      (int(eg_value(v)) * eg_value(w)) / 0x100);
  }

  // weight_option() computes the value of an evaluation weight, by combining
  // two UCI-configurable weights (midgame and endgame) with an internal weight.

  Score weight_option(const std::string& mgOpt, const std::string& egOpt, Score internalWeight) {

    // Scale option value from 100 to 256
    int mg = Options[mgOpt] * 256 / 100;
    int eg = Options[egOpt] * 256 / 100;

    return apply_weight(make_score(mg, eg), internalWeight);
  }


  // Tracing functions definitions

  double to_cp(Value v) { return double(v) / double(PawnValueMg); }

  void Tracing::add(int idx, Score wScore, Score bScore) {

    scores[WHITE][idx] = wScore;
    scores[BLACK][idx] = bScore;
  }

  void Tracing::row(const char* name, int idx) {

    Score wScore = scores[WHITE][idx];
    Score bScore = scores[BLACK][idx];

    switch (idx) {
    case PST: case IMBALANCE: case PAWN: case TOTAL:
        stream << std::setw(20) << name << " |   ---   --- |   ---   --- | "
               << std::setw(6)  << to_cp(mg_value(wScore)) << " "
               << std::setw(6)  << to_cp(eg_value(wScore)) << " \n";
        break;
    default:
        stream << std::setw(20) << name << " | " << std::noshowpos
               << std::setw(5)  << to_cp(mg_value(wScore)) << " "
               << std::setw(5)  << to_cp(eg_value(wScore)) << " | "
               << std::setw(5)  << to_cp(mg_value(bScore)) << " "
               << std::setw(5)  << to_cp(eg_value(bScore)) << " | "
               << std::showpos
               << std::setw(6)  << to_cp(mg_value(wScore - bScore)) << " "
               << std::setw(6)  << to_cp(eg_value(wScore - bScore)) << " \n";
    }
  }

  std::string Tracing::do_trace(const Position& pos) {

    stream.str("");
    stream << std::showpoint << std::showpos << std::fixed << std::setprecision(2);
    std::memset(scores, 0, 2 * (TOTAL + 1) * sizeof(Score));

    do_evaluate<true>(pos);

    std::string totals = stream.str();
    stream.str("");

    stream << std::setw(21) << "Eval term " << "|    White    |    Black    |     Total     \n"
                    <<             "                     |   MG    EG  |   MG    EG  |   MG     EG   \n"
                    <<             "---------------------+-------------+-------------+---------------\n";

    row("Material, PST, Tempo", PST);
    row("Material imbalance", IMBALANCE);
    row("Pawns", PAWN);
    row("Knights", KNIGHT);
    row("Bishops", BISHOP);
    row("Rooks", ROOK);
    row("Queens", QUEEN);
    row("Mobility", MOBILITY);
    row("King safety", KING);
    row("Threats", THREAT);
    row("Passed pawns", PASSED);
    row("Space", SPACE);

    stream <<             "---------------------+-------------+-------------+---------------\n";
    row("Total", TOTAL);
    stream << totals;

    return stream.str();
  }
}
