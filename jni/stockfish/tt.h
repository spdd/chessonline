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

#ifndef TT_H_INCLUDED
#define TT_H_INCLUDED

#include "misc.h"
#include "types.h"

/// The TTEntry is the 128 bit transposition table entry, defined as below:
///
/// key: 32 bit
/// move: 16 bit
/// bound type: 8 bit
/// generation: 8 bit
/// value: 16 bit
/// depth: 16 bit
/// static value: 16 bit
/// static margin: 16 bit

struct TTEntry {

  void save(uint32_t k, Value v, Bound b, Depth d, Move m, int g, Value ev) {

    key32        = (uint32_t)k;
    move16       = (uint16_t)m;
    bound8       = (uint8_t)b;
    generation8  = (uint8_t)g;
    value16      = (int16_t)v;
    depth16      = (int16_t)d;
    evalValue    = (int16_t)ev;
  }
  void set_generation(uint8_t g) { generation8 = g; }

  uint32_t key() const      { return key32; }
  Depth depth() const       { return (Depth)depth16; }
  Move move() const         { return (Move)move16; }
  Value value() const       { return (Value)value16; }
  Bound bound() const       { return (Bound)bound8; }
  int generation() const    { return (int)generation8; }
  Value eval_value() const  { return (Value)evalValue; }

private:
  uint32_t key32;
  uint16_t move16;
  uint8_t bound8, generation8;
  int16_t value16, depth16, evalValue;
};


/// A TranspositionTable consists of a power of 2 number of clusters and each
/// cluster consists of ClusterSize number of TTEntry. Each non-empty entry
/// contains information of exactly one position. Size of a cluster shall not be
/// bigger than a cache line size. In case it is less, it should be padded to
/// guarantee always aligned accesses.

class TranspositionTable {

  static const unsigned ClusterSize = 4; // A cluster is 64 Bytes

public:
 ~TranspositionTable() { free(mem); }
  void new_search() { ++generation; }

  const TTEntry* probe(const Key key) const;
  TTEntry* first_entry(const Key key) const;
  void refresh(const TTEntry* tte) const;
  void set_size(size_t mbSize);
  void clear();
  void store(const Key key, Value v, Bound type, Depth d, Move m, Value statV);

private:
  uint32_t hashMask;
  TTEntry* table;
  void* mem;
  uint8_t generation; // Size must be not bigger than TTEntry::generation8
};

extern TranspositionTable TT;


/// TranspositionTable::first_entry() returns a pointer to the first entry of
/// a cluster given a position. The lowest order bits of the key are used to
/// get the index of the cluster.

inline TTEntry* TranspositionTable::first_entry(const Key key) const {

  return table + ((uint32_t)key & hashMask);
}


/// TranspositionTable::refresh() updates the 'generation' value of the TTEntry
/// to avoid aging. Normally called after a TT hit.

inline void TranspositionTable::refresh(const TTEntry* tte) const {

  const_cast<TTEntry*>(tte)->set_generation(generation);
}

#endif // #ifndef TT_H_INCLUDED
