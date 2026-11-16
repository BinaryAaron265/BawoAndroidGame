package com.example.bawo


 
object Bawo {
    fun newBoard() = IntArray(32) { 2 }

    fun pitOf(m: Int) = m / 2
    fun dirOf(m: Int) = if (m % 2 == 0) 1 else -1

    fun moves(b: IntArray, p: Int): List<Int> = (0..15).filter { b[p * 16 + it] >= 2 }

    private fun allMoves(b: IntArray, p: Int): List<Int> =
        moves(b, p).flatMap { listOf(it * 2, it * 2 + 1) }

    fun reasonInvalid(b: IntArray, p: Int, k: Int): String? = when (b[p * 16 + k]) {
        0 -> "Palibe Nkhomo,sankhani poti pali nkhomo zoposela ziwiri."
        1 -> "Pali nkhomo imodzi yokha. Mukufunika kuyambila ziwili."
        else -> null
    }

    
    fun over(b: IntArray, p: Int): Boolean =
        moves(b, p).isEmpty() || (0..7).all { b[p * 16 + it] == 0 }

    fun play(b: IntArray, p: Int, k: Int, dir: Int): IntArray {
        val n = b.copyOf()
        var pos = k
        var seeds = n[p * 16 + k]
        n[p * 16 + k] = 0
        var guard = 0
        while (guard++ < 300) {
            while (seeds > 0) {
                pos = (pos + dir + 16) % 16
                n[p * 16 + pos]++
                seeds--
            }
            if (n[p * 16 + pos] == 1) break 
            val opp = (1 - p) * 16 + (7 - pos)
            if (pos < 8 && n[opp] > 0) {
                seeds = n[opp]            
            } else {
                seeds = n[p * 16 + pos]   
                n[p * 16 + pos] = 0
            }
        }
        return n
    }

    class Step(val board: IntArray, val pit: Int, val hand: Int)

    /** Same rules as play(), but records every pickup and seed drop. */
    fun steps(b: IntArray, p: Int, k: Int, dir: Int): List<Step> {
        val n = b.copyOf()
        val out = ArrayList<Step>()
        var pos = k
        var seeds = n[p * 16 + k]
        n[p * 16 + k] = 0
        out.add(Step(n.copyOf(), p * 16 + k, seeds))
        var guard = 0
        while (guard++ < 300) {
            while (seeds > 0) {
                pos = (pos + dir + 16) % 16
                n[p * 16 + pos]++
                seeds--
                out.add(Step(n.copyOf(), p * 16 + pos, seeds))
            }
            if (n[p * 16 + pos] == 1) break
            val opp = (1 - p) * 16 + (7 - pos)
            if (pos < 8 && n[opp] > 0) {
                seeds = n[opp]; n[opp] = 0
                out.add(Step(n.copyOf(), opp, seeds))
            } else {
                seeds = n[p * 16 + pos]; n[p * 16 + pos] = 0
                out.add(Step(n.copyOf(), p * 16 + pos, seeds))
            }
        }
        return out
    }

    private fun eval(b: IntArray, me: Int): Int {
        var mine = 0; var theirs = 0
        for (k in 0..15) { mine += b[me * 16 + k]; theirs += b[(1 - me) * 16 + k] }
        return mine - theirs
    }

    private fun search(b: IntArray, turn: Int, depth: Int, a: Int, bt: Int, me: Int): Int {
        if (over(b, turn)) return if (turn == me) -1000 - depth else 1000 + depth
        if (depth == 0) return eval(b, me)
        var alpha = a
        var beta = bt
        if (turn == me) {
            var best = Int.MIN_VALUE
            for (m in allMoves(b, turn)) {
                best = maxOf(best, search(play(b, turn, pitOf(m), dirOf(m)), 1 - turn, depth - 1, alpha, beta, me))
                alpha = maxOf(alpha, best)
                if (alpha >= beta) break
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (m in allMoves(b, turn)) {
                best = minOf(best, search(play(b, turn, pitOf(m), dirOf(m)), 1 - turn, depth - 1, alpha, beta, me))
                beta = minOf(beta, best)
                if (alpha >= beta) break
            }
            return best
        }
    }
    fun best(b: IntArray, p: Int, depth: Int = 4): Int {
        var bestMove = allMoves(b, p).first()
        var bestScore = Int.MIN_VALUE
        for (m in allMoves(b, p)) {
            val s = search(play(b, p, pitOf(m), dirOf(m)), 1 - p, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, p)
            if (s > bestScore) { bestScore = s; bestMove = m }
        }
        return bestMove
    }
}
