package com.maestros.familias.ui.ventas

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.AccesoRapido
import com.maestros.familias.data.repository.MenuRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InicioVentasActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    private val menuRepository = MenuRepository(RetrofitClient.menuApi)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_ventas)

        findViewById<android.widget.LinearLayout>(R.id.cardComprobantes).setOnClickListener {
            startActivity(Intent(this, ComprobantesActivity::class.java))
        }

        findViewById<TextView>(R.id.txtUsuarioLogueado).text="Usuario: ${SessionManager.getNombreUsuario()}"

        findViewById<TextView>(R.id.txtPerfilUsuario).text = SessionManager.getPerfil().ifBlank { SessionManager.getNombreUsuario() }

        val fechaActual = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()).format(Date())

        findViewById<TextView>(R.id.txtFecha).text="Hoy, $fechaActual"

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        cargarMenuDinamico()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isOpen) {
                    drawerLayout.close()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        val rvAccesosRapidos = findViewById<RecyclerView>(R.id.rvAccesosRapidos)

        val accesos = listOf(
            AccesoRapido(android.R.drawable.ic_menu_agenda, "Nueva Factura"),
            AccesoRapido(android.R.drawable.ic_menu_agenda, "Nueva Boleta"),
            AccesoRapido(android.R.drawable.ic_menu_edit, "Cotización"),
            AccesoRapido(android.R.drawable.ic_menu_myplaces, "Clientes"),
            AccesoRapido(android.R.drawable.ic_menu_gallery, "Productos"),
            AccesoRapido(android.R.drawable.ic_menu_send, "Cobranzas"),
            AccesoRapido(android.R.drawable.ic_menu_revert, "Notas de Crédito"),
            AccesoRapido(android.R.drawable.ic_menu_recent_history, "Reportes"),
            AccesoRapido(android.R.drawable.ic_menu_more, "Más opciones")
        )

        rvAccesosRapidos.layoutManager = GridLayoutManager(this, 3)
        rvAccesosRapidos.adapter = AccesoRapidoAdapter(accesos) { label ->
            when (label) {
                "Cotización" -> {
                    CotizacionEnCurso.limpiar()
                startActivity(Intent(this, NuevaCotizacionActivity::class.java))
            }
                "Comprobantes" ->
                    startActivity(Intent(this, ComprobantesActivity::class.java))
                else -> Toast.makeText(this,
                    "Próximamente: $label", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarMenuDinamico() {
        lifecycleScope.launch {
            try {
                val modulos = menuRepository.listarMenu()
                val menu = navView.menu
                menu.clear()

                for (modulo in modulos) {
                    val subMenu = menu.addSubMenu(modulo.dscMenu.uppercase())
                    for (pagina in modulo.paginas) {
                        val item = subMenu.add(pagina.dscPagina)
                        item.setOnMenuItemClickListener {
                            Toast.makeText(this@InicioVentasActivity, "Próximamente: ${pagina.dscPagina}", Toast.LENGTH_SHORT).show()
                            drawerLayout.close()
                            true
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@InicioVentasActivity, "Error al cargar menu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}