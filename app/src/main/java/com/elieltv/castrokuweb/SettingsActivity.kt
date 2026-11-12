package com.elieltv.castrokuweb

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val editIp = findViewById<EditText>(R.id.editIp)
        val editPort = findViewById<EditText>(R.id.editPort)
        val statusText = findViewById<TextView>(R.id.statusText)
        val checkBlockAds = findViewById<android.widget.CheckBox>(R.id.checkBlockAds)
        checkBlockAds.isChecked = AdBlockPrefs.isEnabled(this)
        val btnSave = findViewById<android.widget.Button>(R.id.btnSave)
        val btnDiscover = findViewById<android.widget.Button>(R.id.btnDiscover)

        editIp.setText(RokuPrefs.getIp(this))
        editPort.setText(RokuPrefs.getPort(this).toString())
        statusText.text = if (RokuPrefs.isConfigured(this))
            "Roku: ${RokuPrefs.getIp(this)}:${RokuPrefs.getPort(this)}"
        else "Abra o canal na TV e toque em Buscar TV Roku."

        btnDiscover.setOnClickListener {
            btnDiscover.isEnabled = false
            statusText.text = "Procurando Roku com o canal Cast Roku Web aberto..."
            RokuDiscovery.scan(this) { devices ->
                runOnUiThread {
                    btnDiscover.isEnabled = true
                    if (devices.isEmpty()) {
                        statusText.text = "Nenhum canal Cast Roku Web encontrado. Verifique se ambos estão na mesma rede Wi-Fi."
                        Toast.makeText(this, "Nenhuma TV com o canal aberto foi encontrada.", Toast.LENGTH_LONG).show()
                    } else {
                        val labels = devices.map { "${it.name}  •  ${it.ip}" }.toTypedArray()
                        AlertDialog.Builder(this).setTitle("Selecione sua TV Roku")
                            .setItems(labels) { _, which ->
                                val device = devices[which]
                                editIp.setText(device.ip)
                                editPort.setText("8060")
                                RokuPrefs.save(this, device.ip, 8060)
                                statusText.text = "TV selecionada: ${device.name} (${device.ip})"
                            }.setNegativeButton("Cancelar", null).show()
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val ip = editIp.text.toString().trim()
            val port = editPort.text.toString().trim().toIntOrNull() ?: 8060
            if (ip.isNotBlank() && port !in 1..65535) {
                Toast.makeText(this, "Porta inválida.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ip.isNotBlank()) RokuPrefs.save(this, ip, port)
            AdBlockPrefs.setEnabled(this, checkBlockAds.isChecked)
            statusText.text = "Configurações salvas."
            Toast.makeText(this, "Configurações salvas.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
