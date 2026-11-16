package com.example.bawo

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun styledButton(label: String, top: Int, bottom: Int, onTap: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)).apply {
            cornerRadius = dp(14).toFloat()
        }
        setOnClickListener { onTap() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE // always landscape

        val status = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(250, 232, 200))
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val game = BawoView(this).apply { onStatus = { status.text = it } }

        val clockwise = styledButton("↻  Kumanzele", Color.rgb(72, 170, 96), Color.rgb(40, 120, 64)) { game.sow(1) }
        val anticlockwise = styledButton("↺  Kumanja", Color.rgb(78, 140, 220), Color.rgb(44, 94, 168)) { game.sow(-1) }
        lateinit var speedBtn: Button
        speedBtn = styledButton("Kathamangidwe Normal", Color.rgb(120, 100, 160), Color.rgb(80, 62, 120)) {
            speedBtn.text = "Speed: " + game.cycleSpeed()
        }
        val newGame = styledButton("Tiyambilenso", Color.rgb(190, 120, 50), Color.rgb(140, 80, 26)) { game.reset() }

        // right-hand control panel: status on top, buttons below
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(8), dp(8), dp(8))
            addView(status, LinearLayout.LayoutParams(-1, 0, 1f))
            val lp = { LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) } }
            addView(clockwise, lp())
            addView(anticlockwise, lp())
            addView(speedBtn, lp())
            addView(newGame, lp())
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(52, 32, 18), Color.rgb(24, 14, 8))
            )
            addView(game, LinearLayout.LayoutParams(0, -1, 1f))
            addView(panel, LinearLayout.LayoutParams(dp(190), -1))
        }
        setContentView(root)
        game.reset()
    }
}
