package com.maestros.familias.ui.ventas

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.repository.CotizacionRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.CotizacionFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComprobanteEmitidoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprobante_emitido)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            irAInicioVentas()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                irAInicioVentas()
            }
        })

        findViewById<TextView>(R.id.txtSerieNumeroExito).text =
            "${CotizacionEnCurso.serie} - ${CotizacionEnCurso.numero}"

        val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "PE"))
        findViewById<TextView>(R.id.txtFechaAceptacion).text = "Guardado el ${sdf.format(Date())}"

        findViewById<MaterialButton>(R.id.btnVerPdf).setOnClickListener {
            lifecycleScope.launch {
                try {
                    val base64 = CotizacionRepository(RetrofitClient.cotizacionApi).obtenerPdfBase64(
                        CotizacionEnCurso.codProformaGrabada,
                        SessionManager.getCodAlmacen(),
                        CotizacionEnCurso.serie
                    )

                    if (base64.startsWith("ERROR:")) {
                        Toast.makeText(this@ComprobanteEmitidoActivity, base64, Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    val archivo = java.io.File(cacheDir, "cotizacion_${CotizacionEnCurso.numero}.pdf")
                    archivo.writeBytes(bytes)

                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this@ComprobanteEmitidoActivity,
                        "${packageName}.fileprovider",
                        archivo
                    )

                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@ComprobanteEmitidoActivity, "Error al obtener PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        findViewById<MaterialButton>(R.id.btnEnviarCorreo).setOnClickListener {
            val texto = CotizacionFormatter.generarCuerpoCorreo()+ CotizacionFormatter.asuntoCorreo()
            val cliente = CotizacionEnCurso.cliente
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(cliente?.let { "" } ?: ""))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Cotización ${CotizacionEnCurso.serie}-${CotizacionEnCurso.numero}")
                putExtra(android.content.Intent.EXTRA_TEXT, texto)
            }
            try {
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "No hay app de correo instalada", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<MaterialButton>(R.id.btnEnviarWhatsapp).setOnClickListener {
            val texto = CotizacionFormatter.generarTextoWhatsApp()
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, texto)
                setPackage("com.whatsapp")
            }
            try {
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<TextView>(R.id.txtNuevoComprobante).setOnClickListener {
            CotizacionEnCurso.limpiar()
            startActivity(android.content.Intent(this, InicioVentasActivity::class.java))
            finish()
        }

    }
    private fun irAInicioVentas() {
        CotizacionEnCurso.limpiar()
        val intent = Intent(this, InicioVentasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}