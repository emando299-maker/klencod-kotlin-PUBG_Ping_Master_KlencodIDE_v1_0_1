package com.pingmaster.pubg

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var running = false
    private var samples = mutableListOf<Long>()
    private var losses = 0
    private var attempts = 0
    private lateinit var pingText: TextView
    private lateinit var jitterText: TextView
    private lateinit var lossText: TextView
    private lateinit var statusText: TextView
    private lateinit var historyText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val endpoints = listOf(
        PingTester.Endpoint("Cloudflare", "1.1.1.1", 443),
        PingTester.Endpoint("Google", "8.8.8.8", 443)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        updateNetworkStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 22)
            setBackgroundColor(Color.rgb(10, 15, 28))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(label("PUBG PING MASTER", 25, Color.WHITE, true))
        content.addView(label("NETWORK DIAGNOSTICS", 12, Color.rgb(0, 220, 170), true).apply {
            setPadding(0, 4, 0, 24)
        })
        statusText = label("Checking connection…", 14, Color.LTGRAY)
        content.addView(statusText)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 24, 20, 24)
            setBackgroundColor(Color.rgb(22, 31, 49))
        }
        card.addView(label("TCP LATENCY", 12, Color.LTGRAY, true))
        pingText = label("-- ms", 48, Color.rgb(0, 230, 160), true)
        card.addView(pingText)
        card.addView(label("Measured to public test endpoints — not PUBG game server ping", 11, Color.LTGRAY))
        content.addView(card, params(top = 18, bottom = 14))

        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val jitterCard = metricCard("JITTER", "-- ms")
        jitterText = jitterCard.second
        val lossCard = metricCard("PACKET LOSS", "-- %")
        lossText = lossCard.second
        metrics.addView(jitterCard.first, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = 7 })
        metrics.addView(lossCard.first, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = 7 })
        content.addView(metrics)

        startButton = Button(this).apply {
            text = "START TEST"
            setOnClickListener { startTest() }
        }
        stopButton = Button(this).apply {
            text = "STOP"
            isEnabled = false
            setOnClickListener { stopTest() }
        }
        content.addView(startButton, params(top = 22))
        content.addView(stopButton, params(top = 8))
        content.addView(label("RECENT RESULTS", 15, Color.WHITE, true).apply {
            setPadding(0, 26, 0, 8)
        })
        historyText = label("No tests yet", 13, Color.LTGRAY)
        content.addView(historyText)
        content.addView(label(
            "Important: PUBG Mobile does not provide a public endpoint for third-party apps to read its live in-game ping. This app measures TCP connection time to public endpoints. Use the in-game ping indicator for the actual PUBG ping.",
            12, Color.rgb(255, 195, 95)
        ).apply { setPadding(0, 26, 0, 12) })

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

    private fun metricCard(title: String, value: String): Pair<View, TextView> {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(10, 18, 10, 18)
            setBackgroundColor(Color.rgb(22, 31, 49))
        }
        box.addView(label(title, 11, Color.LTGRAY, true))
        val v = label(value, 25, Color.WHITE, true)
        box.addView(v)
        return Pair(box, v)
    }

    private fun params(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, top, 0, bottom) }

    private fun updateNetworkStatus() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val active = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(active)
        val kind = when {
            caps == null -> "No network connection detected"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Connected via Wi-Fi"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected via mobile data"
            else -> "Connected to network"
        }
        statusText.text = kind
    }

    private fun startTest() {
        if (running) return
        running = true
        samples.clear()
        losses = 0
        attempts = 0
        historyText.text = "Testing…"
        startButton.isEnabled = false
        stopButton.isEnabled = true
        runOneSample()
    }

    private fun runOneSample() {
        if (!running) return
        executor.execute {
            val results = endpoints.map { PingTester.measure(it) }
            val best = results.filterNotNull().minOrNull()
            handler.post {
                if (!running) return@post
                attempts++
                if (best == null) {
                    losses++
                    pingText.text = "TIMEOUT"
                } else {
                    samples.add(best)
                    pingText.text = "$best ms"
                    pingText.setTextColor(when {
                        best < 60 -> Color.rgb(0, 230, 160)
                        best < 120 -> Color.rgb(255, 205, 75)
                        else -> Color.rgb(255, 100, 100)
                    })
                }
                val jitter = calculateJitter(samples)
                jitterText.text = if (jitter == null) "-- ms" else "%.1f ms".format(jitter)
                lossText.text = if (attempts == 0) "-- %" else "%.1f %%".format(losses * 100.0 / attempts)
                historyText.text = buildString {
                    append("Samples: ${samples.size} / $attempts attempts\n")
                    append("Recent latency: ")
                    append(samples.takeLast(10).joinToString("  •  ") { "$it ms" }.ifEmpty { "No response" })
                }
                handler.postDelayed({ runOneSample() }, 1000)
            }
        }
    }

    private fun calculateJitter(values: List<Long>): Double? {
        if (values.size < 2) return null
        val diffs = values.zipWithNext { a, b -> abs(b - a).toDouble() }
        return diffs.average()
    }

    private fun stopTest() {
        running = false
        startButton.isEnabled = true
        stopButton.isEnabled = false
        historyText.append("\nTest stopped.")
    }

    override fun onDestroy() {
        running = false
        executor.shutdownNow()
        super.onDestroy()
    }
}