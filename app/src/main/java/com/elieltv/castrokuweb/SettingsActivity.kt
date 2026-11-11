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
        val btnSave = findViewById<android.widget.Button>(R.id.btnSave)
        val btnDiscover = findViewById<android.widget.Button>(R.id.btnDiscover)

        editIp.setText(RokuPrefs.getIp(this))
        editPort.setText(RokuPrefs.getPort(this).toString())
        statusText.text = if (RokuPrefs.isConfigured(this))
            "Configurado: ${RokuPrefs.getIp(this)}:${RokuPrefs.getPort(this)}"
        else "Ainda não configurado. Abra o canal Cast Roku Web na TV e toque em Buscar TV."

        btnDiscover.setOnClickListener {
            btnDiscover.isEnabled = false
            statusText.text = "Procurando Roku com o canal Cast Roku Web aberto..."
            RokuDiscovery.scan(this) { devices ->
                runOnUiThread {
                    btnDiscover.isEnabled = true
                    if (devices.isEmpty()) {
                        statusText.text = "Nenhum canal Cast Roku Web encontrado. Confirme que o canal está aberto e que celular e Roku estão na mesma rede Wi-Fi."
                        Toast.makeText(this, "Nenhuma TV com o canal aberto foi encontrada.", Toast.LENGTH_LONG).show()
                    } else {
                        val labels = devices.map { "${it.name}  •  ${it.ip}" }.toTypedArray()
                        AlertDialog.Builder(this)
                            .setTitle("Selecione sua TV Roku")
                            .setItems(labels) { _, which ->
                                val device = devices[which]
                                editIp.setText(device.ip)
                                editPort.setText("8065")
                                RokuPrefs.save(this, device.ip, 8065)
                                statusText.text = "Conectado à TV: ${device.name} (${device.ip}:8065)"
                                Toast.makeText(this, "TV encontrada e selecionada.", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val ip = editIp.text.toString().trim()
            val port = editPort.text.toString().trim().toIntOrNull() ?: 8065
            if (ip.isBlank() || port !in 1..65535) {
                Toast.makeText(this, "Informe um IP e uma porta válidos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            RokuPrefs.save(this, ip, port)
            statusText.text = "Salvo: $ip:$port"
            Toast.makeText(this, "Configuração salva.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
