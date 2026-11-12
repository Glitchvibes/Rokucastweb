package com.elieltv.castrokuweb

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DetectedVideosDialog(
    context: Context,
    private val videos: List<DetectedVideo>,
    private val currentPageUrl: String,
    private val currentPageTitle: String
) : Dialog(context) {

    private lateinit var upscaleCheck: CheckBox

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_detected_videos)

        val emptyLabel = findViewById<TextView>(R.id.emptyLabel)
        val list = findViewById<RecyclerView>(R.id.videoList)
        val btnCastPage = findViewById<android.widget.Button>(R.id.btnCastCurrentPage)
        upscaleCheck = findViewById(R.id.checkAiUpscale)

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = VideoListAdapter(videos.toMutableList()) { video ->
            castVideo(video.url, video.title)
        }
        emptyLabel.visibility = if (videos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        btnCastPage.setOnClickListener { castVideo(currentPageUrl, currentPageTitle) }
    }

    private fun castVideo(url: String, title: String) {
        if (!upscaleCheck.isChecked) {
            sendToRoku(url, title)
            return
        }

        val cloudUrl = CloudPrefs.getUrl(context)
        val token = CloudPrefs.getToken(context)
        Toast.makeText(context, "Enviando vídeo para o Real-ESRGAN na nuvem...", Toast.LENGTH_LONG).show()
        CloudUpscaleClient.process(cloudUrl, token, url, object : CloudUpscaleClient.Callback {
            override fun onProgress(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(processedVideoUrl: String) {
                Toast.makeText(context, "Upscale concluído. Enviando vídeo aprimorado à Roku...", Toast.LENGTH_LONG).show()
                sendToRoku(processedVideoUrl, "$title (AI Upscale 2x)")
            }

            override fun onError(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun sendToRoku(url: String, title: String) {
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
