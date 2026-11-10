package com.elieltv.castrokuweb

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Dialog de tela cheia listando os videos detectados na pagina atual,
 * cada um com um botao pra transmitir ("cast") pro canal Roku companheiro.
 */
class DetectedVideosDialog(
    context: Context,
    private val videos: List<DetectedVideo>,
    private val currentPageUrl: String,
    private val currentPageTitle: String
) : Dialog(context) {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_detected_videos)

        val emptyLabel = findViewById<TextView>(R.id.emptyLabel)
        val list = findViewById<RecyclerView>(R.id.videoList)
        val btnCastPage = findViewById<android.widget.Button>(R.id.btnCastCurrentPage)

        list.layoutManager = LinearLayoutManager(context)
        val adapter = VideoListAdapter(videos.toMutableList()) { video ->
            castVideo(video.url, video.title)
        }
        list.adapter = adapter

        emptyLabel.visibility = if (videos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        btnCastPage.setOnClickListener {
            castVideo(currentPageUrl, currentPageTitle)
        }
    }

    private fun castVideo(url: String, title: String) {
        val ip = RokuPrefs.getIp(context)
        val port = RokuPrefs.getPort(context)
        CastClient.sendCast(ip, port, url, title, object : CastClient.Callback {
            override fun onSuccess() {
                Toast.makeText(context, "Enviado pra Roku!", Toast.LENGTH_SHORT).show()
                dismiss()
            }

            override fun onError(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        })
    }
}
