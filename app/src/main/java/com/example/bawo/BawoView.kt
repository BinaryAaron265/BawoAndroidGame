package com.example.bawo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import java.util.Random
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class BawoView(ctx: Context) : View(ctx) {

    private val dp = resources.displayMetrics.density
    private var board = Bawo.newBoard()
    private var turn = 0           
    private var busy = false       
    private var gameOver = false
    private var hl = -1             
    private var selected = -1      
    var onStatus: (String) -> Unit = {}

    private var speed = 1
    private val shortDelay = longArrayOf(800L, 500L, 230L)  
    private val longDelay = longArrayOf(400L, 250L, 110L)   
    private val speedNames = arrayOf("Slow", "Normal", "Fast")

    fun cycleSpeed(): String {
        speed = (speed + 1) % 3
        return speedNames[speed]
    }

    private val AA = Paint.ANTI_ALIAS_FLAG
    private val boardPaint = Paint(AA)
    private val framePaint = Paint(AA).apply { style = Paint.Style.STROKE; strokeWidth = 5 * dp; color = Color.rgb(58, 30, 12) }
    private val grainPaint = Paint(AA).apply { color = Color.argb(30, 40, 20, 5); strokeWidth = 2 * dp }
    private val dividerPaint = Paint(AA).apply { color = Color.argb(130, 40, 20, 5); strokeWidth = 3 * dp }
    private val pitPaint = Paint(AA)
    private val rimPaint = Paint(AA).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f * dp; color = Color.argb(110, 255, 210, 150) }
    private val seedPaint = Paint(AA)
    private val glow = Paint(AA).apply { color = Color.argb(70, 255, 200, 80) }
    private val ring = Paint(AA).apply { color = Color.rgb(250, 200, 80); style = Paint.Style.STROKE; strokeWidth = 3.5f * dp }
    private val faint = Paint(AA).apply { color = Color.argb(120, 250, 200, 80); style = Paint.Style.STROKE; strokeWidth = 2 * dp }
    private val badge = Paint(AA).apply { color = Color.argb(200, 25, 12, 4) }
    private val markerPaint = Paint(AA)
    private val num = Paint(AA).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val label = Paint(AA).apply {
        color = Color.rgb(230, 200, 150); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.2f
    }
    private val seedColors = intArrayOf(
        Color.rgb(245, 222, 179), Color.rgb(222, 170, 90), Color.rgb(196, 120, 60), Color.rgb(160, 82, 45)
    )

    
    private val sx = FloatArray(48)
    private val sy = FloatArray(48)

    private var cell = 0f
    private var ox = 0f
    private var oy = 0f
    private var pitR = 0f

    init {
        val r = Random(11)
        for (i in 0 until 48) {
            val a = r.nextDouble() * 2 * PI
            val d = sqrt(r.nextDouble()) * 0.62
            sx[i] = (cos(a) * d).toFloat()
            sy[i] = (sin(a) * d).toFloat()
        }
    }

    fun reset() {
        board = Bawo.newBoard(); turn = 0; busy = false; gameOver = false; hl = -1; selected = -1
        invalidate(); onStatus("Nthawi yanu : dinani/sankhani pa yenje lanu")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val labelH = 26 * dp
        val margin = 6 * dp
        cell = min((w - 2 * margin) / 8f, (h - 2 * labelH) / 4f)
        ox = (w - cell * 8) / 2f
        oy = (h - cell * 4) / 2f
        pitR = cell * 0.42f
        boardPaint.shader = LinearGradient(
            ox, oy, ox, oy + cell * 4, Color.rgb(156, 98, 52), Color.rgb(96, 54, 24), Shader.TileMode.CLAMP
        )
        pitPaint.shader = RadialGradient(
            0f, 0f, pitR,
            intArrayOf(Color.rgb(28, 14, 5), Color.rgb(58, 32, 14), Color.rgb(108, 68, 36)),
            floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        num.textSize = cell * 0.2f
        label.textSize = 13 * dp
    }
    private fun rowOf(p: Int, k: Int) = if (p == 0) (if (k < 8) 2 else 3) else (if (k < 8) 1 else 0)
    private fun colOf(p: Int, k: Int) =
        if (p == 0) (if (k < 8) k else 7 - (k - 8)) else (if (k < 8) 7 - k else k - 8)

    override fun onDraw(c: Canvas) {
        val rect = RectF(ox, oy, ox + cell * 8, oy + cell * 4)
        val corner = 22 * dp
        c.drawRoundRect(rect, corner, corner, boardPaint)
        for (i in 1..9) {
            val y = oy + rect.height() * i / 10f
            c.drawLine(ox + 12 * dp, y, ox + cell * 8 - 12 * dp, y, grainPaint)
        }
        c.drawRoundRect(rect, corner, corner, framePaint)
        c.drawLine(ox + 14 * dp, oy + cell * 2, ox + cell * 8 - 14 * dp, oy + cell * 2, dividerPaint)
        c.drawText("COMPUTER", width / 2f, oy - 9 * dp, label)
        c.drawText("INUYO", width / 2f, oy + cell * 4 + 19 * dp, label)

        val canPick = turn == 0 && !busy && !gameOver
        for (p in 0..1) for (k in 0..15) {
            val idx = p * 16 + k
            val cx = ox + colOf(p, k) * cell + cell / 2
            val cy = oy + rowOf(p, k) * cell + cell / 2
            c.save(); c.translate(cx, cy)
            c.drawCircle(0f, 0f, pitR, pitPaint)
            c.drawCircle(0f, 0f, pitR, rimPaint)
            c.restore()

            if (hl == idx) {
                c.drawCircle(cx, cy, pitR * 1.12f, glow)
                c.drawCircle(cx, cy, pitR * 1.08f, ring)
            }
            if (p == 0 && canPick) {
                if (k == selected) c.drawCircle(cx, cy, pitR * 1.08f, ring)
                else if (board[idx] >= 2) c.drawCircle(cx, cy, pitR, faint)
            }

            val n = board[idx]
            val seedR = cell * 0.07f
            for (i in 0 until min(n, 30)) {
                val j = (idx * 5 + i) % 48
                seedPaint.color = seedColors[(idx + i) % seedColors.size]
                c.drawCircle(cx + sx[j] * pitR, cy + sy[j] * pitR, seedR, seedPaint)
            }
            if (n > 0) {
                val bx = cx + pitR * 0.62f
                val by = cy - pitR * 0.62f
                c.drawCircle(bx, by, cell * 0.14f, badge)
                c.drawText(n.toString(), bx, by + num.textSize / 3f, num)
            }
        }

        if (selected >= 0 && canPick) {
            markFirstTarget((selected + 1) % 16, "↻", Color.rgb(90, 190, 110), c)
            markFirstTarget((selected + 15) % 16, "↺", Color.rgb(90, 150, 230), c)
        }
    }

    private fun markFirstTarget(k: Int, symbol: String, color: Int, c: Canvas) {
        val cx = ox + colOf(0, k) * cell + cell / 2
        val cy = oy + rowOf(0, k) * cell + cell / 2
        val bx = cx - pitR * 0.62f
        val by = cy - pitR * 0.62f
        markerPaint.color = color
        c.drawCircle(bx, by, cell * 0.15f, markerPaint)
        c.drawText(symbol, bx, by + num.textSize / 3f, num)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        if (turn != 0 || busy || gameOver) return true
        val col = ((e.x - ox) / cell).toInt()
        val row = ((e.y - oy) / cell).toInt()
        if (e.x < ox || e.y < oy || col !in 0..7 || row !in 0..3) return true
        if (row <= 1) {
            onStatus("Izi si nkhomo zanu . Sankhani za Mbali yanu")
            return true
        }
        val k = if (row == 2) col else 8 + (7 - col)
        val reason = Bawo.reasonInvalid(board, 0, k)
        if (reason != null) {
            selected = -1
            onStatus(reason)
        } else {
            selected = k
            onStatus(" Mwasankha Mbali yanu, pitani kumanja kapena kumanzele")
        }
        invalidate()
        return true
    }

    fun sow(dir: Int) {
        if (turn != 0 || busy || gameOver) return
        if (selected < 0) {
            onStatus("Sankhani yenje lanu poyamba, kenako sankhani njira.")
            return
        }
        val reason = Bawo.reasonInvalid(board, 0, selected)
        if (reason != null) {
            selected = -1; onStatus(reason); invalidate(); return
        }
        val k = selected
        selected = -1
        animate(Bawo.steps(board, 0, k, dir)) { turn = 1; next() }
    }

    private fun animate(steps: List<Bawo.Step>, done: () -> Unit) {
        busy = true
        val delay = if (steps.size > 60) longDelay[speed] else shortDelay[speed]
        var i = 0
        val frame = object : Runnable {
            override fun run() {
                if (i < steps.size) {
                    val s = steps[i++]
                    board = s.board
                    hl = s.pit
                    onStatus("M''manja Muli Nkhomo : ${s.hand}")
                    invalidate()
                    postDelayed(this, delay)
                } else {
                    hl = -1
                    invalidate()
                    done()
                }
            }
        }
        post(frame)
    }

    private fun next() {
        busy = false
        if (Bawo.over(board, turn)) {
            gameOver = true
            onStatus(if (turn == 1) "Mwawina!" else "Computer  yawina")
            invalidate()
            return
        }
        if (turn == 0) { onStatus("Nthawi yanu yosewela"); invalidate(); return }
        busy = true
        onStatus("Computer Ikuganizila kaye…")
        val snapshot = board.copyOf()
        thread {
            val m = Bawo.best(snapshot, 1)
            val steps = Bawo.steps(snapshot, 1, Bawo.pitOf(m), Bawo.dirOf(m))
            post { animate(steps) { turn = 0; next() } }
        }
    }
}
