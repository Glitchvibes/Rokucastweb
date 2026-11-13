package com.elieltv.castrokuweb

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class RokuRemoteDialog(private val host: MainActivity) : Dialog(host) {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var time: TextView
    private var polling = false

    private val poll = object : Runnable {
        override fun run() {
            if (!polling || !isShowing) return
            RokuRemoteClient.query(RokuPrefs.getIp(host)) { info ->
                if (!isShowing) return@query
                if (info == null) {
                    status.text = "Não foi possível obter o estado do player."
                    time.text = "Use os botões para controlar a reprodução."
                } else {
                    status.text = "Estado: ${info.state}"
                    time.text = "${format(info.positionSeconds)} / ${format(info.durationSeconds)}"
                }
            }
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 20)
        }
        status = TextView(context).apply { textSize = 16f }
        time = TextView(context).apply { textSize = 22f; gravity = Gravity.CENTER; setPadding(0, 16, 0, 16) }
        root.addView(status)
        root.addView(time)
        row(root, "⏪ 10 s", "⏸ Pausar", "▶ Reproduzir", "10 s ⏩") { index ->
            val key = when(index) { 0 -> "Rev"; 1 -> "Pause"; 2 -> "Play"; else -> "Fwd" }
            RokuRemoteClient.key(RokuPrefs.getIp(host), key) { ok, error ->
                if (!ok) status.text = error ?: "Comando não enviado"
            }
        }
        row(root, "Voltar", "Avançar", "Legendas / Info") { index ->
            val key = when(index) { 0 -> "Left"; 1 -> "Right"; else -> "Info" }
            RokuRemoteClient.key(RokuPrefs.getIp(host), key) { ok, error ->
                if (!ok) status.text = error ?: "Comando não enviado"
            }
        }
        val close = Button(context).apply { text = "Fechar"; setOnClickListener { dismiss() } }
        root.addView(close)
        setContentView(root)
        window?.setLayout((context.resources.displayMetrics.widthPixels * 0.92).toInt(), -2)
    }

    private fun row(parent: LinearLayout, a: String, b: String, c: String, d: String? = null,
                    action: (Int) -> Unit) {
        val labels = if (d == null) listOf(a,b,c) else listOf(a,b,c,d)
        val line = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        labels.forEachIndexed { index, label ->
            line.addView(Button(context).apply {
                text = label
                isAllCaps = false
                setOnClickListener { action(index) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        parent.addView(line)
    }

    private fun format(seconds: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)

    override fun show() {
        super.show()
        polling = true
        handler.post(poll)
    }

    override fun dismiss() {
        polling = false
        handler.removeCallbacksAndMessages(null)
        super.dismiss()
    }
}
