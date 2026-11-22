package com.morohmi.myapplicationn

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var board: FrameLayout
    private lateinit var avatar: View

    private lateinit var arrowLeft: TextView
    private lateinit var arrowRight: TextView
    private lateinit var arrowUp: TextView
    private lateinit var arrowDown: TextView


    private val directionThreshold = 2.0f
    private val zThreshold = 7.0f
    private val moveScale = 0.10f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        board = findViewById(R.id.board)


        arrowUp = createArrow("↑", Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        arrowDown = createArrow("↓", Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        arrowLeft = createArrow("←", Gravity.CENTER_VERTICAL or Gravity.LEFT)
        arrowRight = createArrow("→", Gravity.CENTER_VERTICAL or Gravity.RIGHT)


        avatar = RedPersonView(this)
        board.addView(
            avatar,
            FrameLayout.LayoutParams(dp(64), dp(80), Gravity.CENTER)
        )

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Toast.makeText(
                this,
                "هذا الجهاز لا يحتوي على حساس تسارع",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createArrow(symbol: String, gravity: Int): TextView {
        val arrow = TextView(this).apply {
            text = symbol
            textSize = 48f
            setTextColor(Color.RED)
            setGravity(Gravity.CENTER)
            visibility = View.GONE
        }

        val margin = dp(16)
        val params = FrameLayout.LayoutParams(dp(72), dp(72), gravity).apply {
            setMargins(margin, margin, margin, margin)
        }
        board.addView(arrow, params)
        return arrow
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        updateArrows(x, y, z)
        movePerson(x, y)
    }

    private fun updateArrows(x: Float, y: Float, z: Float) {
        if (abs(z) >= zThreshold) {
            arrowLeft.visibility = View.GONE
            arrowRight.visibility = View.GONE
            arrowUp.visibility = View.GONE
            arrowDown.visibility = View.GONE
            return
        }

        arrowLeft.visibility = if (x < -directionThreshold) View.VISIBLE else View.GONE
        arrowRight.visibility = if (x > directionThreshold) View.VISIBLE else View.GONE
        arrowUp.visibility = if (y < -directionThreshold) View.VISIBLE else View.GONE
        arrowDown.visibility = if (y > directionThreshold) View.VISIBLE else View.GONE
    }


    private fun movePerson(x: Float, y: Float) {
        if (board.width == 0 || board.height == 0 ||
            avatar.width == 0 || avatar.height == 0
        ) return

        val nextX = avatar.x + x * moveScale
        val nextY = avatar.y + y * moveScale

        val maxX = (board.width - avatar.width).toFloat().coerceAtLeast(0f)
        val maxY = (board.height - avatar.height).toFloat().coerceAtLeast(0f)

        avatar.x = nextX.coerceIn(0f, maxX)
        avatar.y = nextY.coerceIn(0f, maxY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()


    private inner class RedPersonView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = dp(4).toFloat()
            strokeCap = Paint.Cap.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()
            val centerX = w / 2f


            canvas.drawCircle(centerX, h * 0.18f, w * 0.13f, paint)
            canvas.drawLine(centerX, h * 0.32f, centerX, h * 0.68f, paint)


            canvas.drawLine(w * 0.22f, h * 0.43f, w * 0.78f, h * 0.43f, paint)


            canvas.drawLine(centerX, h * 0.68f, w * 0.25f, h * 0.95f, paint)
            canvas.drawLine(centerX, h * 0.68f, w * 0.75f, h * 0.95f, paint)
        }
    }
}
