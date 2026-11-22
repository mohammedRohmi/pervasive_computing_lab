package com.morohmi.light_sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {

        private const val INITIAL_THRESHOLD = 50f


        private const val NIGHT_THRESHOLD = 40f
        private const val DAY_THRESHOLD = 60f
    }

    private lateinit var imageView: ImageView
    private lateinit var lightText: TextView
    private lateinit var statusText: TextView

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    private var nightPlayer: MediaPlayer? = null
    private var morningPlayer: MediaPlayer? = null

    private var isNight: Boolean? = null
    private var soundPausedForLifecycle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        imageView = findViewById(R.id.ivScene)
        lightText = findViewById(R.id.tvLight)
        statusText = findViewById(R.id.tvStatus)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)


        nightPlayer = MediaPlayer.create(this, R.raw.night_sound)?.apply {
            isLooping = true
        }

        morningPlayer = MediaPlayer.create(this, R.raw.morning_sound)?.apply {
            isLooping = true
        }

        if (lightSensor == null) {
            lightText.text = "Light sensor: unavailable"
            statusText.text = "This device has no light sensor."
        }
    }

    override fun onResume() {
        super.onResume()


        lightSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }


        if (soundPausedForLifecycle) {
            val currentPlayer =
                if (isNight == true) nightPlayer else morningPlayer

            runCatching {
                currentPlayer?.start()
            }

            soundPausedForLifecycle = false
        }
    }

    override fun onPause() {
        super.onPause()


        sensorManager.unregisterListener(this)


        val currentPlayer =
            if (isNight == true) nightPlayer else morningPlayer

        if (currentPlayer?.isPlaying == true) {
            currentPlayer.pause()
            soundPausedForLifecycle = true
        }
    }

    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type != Sensor.TYPE_LIGHT) {
            return
        }


        val lux = event.values[0]


        lightText.text =
            "Light: ${String.format(Locale.US, "%.1f", lux)} Lux"

        // Select the first mode.
        if (isNight == null) {
            changeMode(
                nightMode = lux < INITIAL_THRESHOLD,
                animate = false
            )
            return
        }


        if (isNight == true && lux >= DAY_THRESHOLD) {
            changeMode(nightMode = false, animate = true)
        }

        else if (isNight == false && lux <= NIGHT_THRESHOLD) {
            changeMode(nightMode = true, animate = true)
        }
    }

    private fun changeMode(nightMode: Boolean, animate: Boolean) {
        isNight = nightMode

        val imageResource: Int

        if (nightMode) {
            imageResource = R.drawable.night_icon
            statusText.text = "Night mode - night sound playing"
        } else {
            imageResource = R.drawable.day_icon
            statusText.text = "Day mode - morning sound playing"
        }


        playModeSound(nightMode)

        if (!animate) {
            imageView.setImageResource(imageResource)
            return
        }


        imageView.animate().cancel()
        imageView.alpha = 1f

        imageView.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                imageView.setImageResource(imageResource)

                imageView.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
            .start()
    }

    private fun playModeSound(nightMode: Boolean) {
        val selectedPlayer = if (nightMode) nightPlayer else morningPlayer
        val otherPlayer = if (nightMode) morningPlayer else nightPlayer

        if (selectedPlayer == null) {
            statusText.text = if (nightMode) {
                "Night sound failed to load"
            } else {
                "Morning sound failed to load"
            }
            return
        }

        try {

            otherPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
                player.seekTo(0)
            }

            selectedPlayer.setVolume(1f, 1f)
            selectedPlayer.isLooping = true

            if (selectedPlayer.isPlaying) {
                selectedPlayer.pause()
            }

            selectedPlayer.seekTo(0)
            selectedPlayer.start()

            statusText.text = if (nightMode) {
                "Night mode - night sound playing"
            } else {
                "Day mode - morning sound playing"
            }
        } catch (e: Exception) {
            statusText.text = "Audio error: ${e.message}"
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onDestroy() {
        super.onDestroy()

        // Release the media players.
        nightPlayer?.release()
        morningPlayer?.release()

        nightPlayer = null
        morningPlayer = null
    }
}
