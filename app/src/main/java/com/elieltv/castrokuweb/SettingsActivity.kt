package com.elieltv.castrokuweb

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val editIp = findViewById<EditText>(R.id.editIp)
        val editPort = findViewById<EditText>(R.id.editPort)
        val statusText = findViewById<TextView>(R.id.statusText)
        val btnSave = findViewById<android.widget.Button>(R.id.btnSave)

        editIp.setText(RokuPrefs.getIp(this))
        val savedPort = RokuPrefs.getPort(this)
        editPort.setText(savedPort.toString())

        if (RokuPrefs.isConfigured(this)) {
            statusText.text = "Configurado: ${RokuPrefs.getIp(this)}:${RokuPrefs.getPort(this)}"
        } else {
            statusText.text = "Ainda nao configurado."
        }

        btnSave.setOnClickListener {
            val ip = editIp.text.toString().trim()
            val portText = editPort.text.toString().trim()
            val port = portText.toIntOrNull() ?: 8065

            if (ip.isBlank()) {
                Toast.makeText(this, "Digite o IP da Roku.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RokuPrefs.save(this, ip, port)
            statusText.text = "Salvo: $ip:$port"
            Toast.makeText(this, "Configuracao salva.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
