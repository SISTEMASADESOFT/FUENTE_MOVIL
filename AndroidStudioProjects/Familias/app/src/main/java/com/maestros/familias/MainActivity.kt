package com.maestros.familias

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.OnBackPressedCallback

import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.Familia
import com.maestros.familias.data.repository.FamiliaRepository
import com.maestros.familias.ui.familias.FamiliaViewModelFactory
import com.maestros.familias.ui.familias.FamiliaAdapter
import com.maestros.familias.ui.familias.FamiliaViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var txtBuscar: TextInputEditText
    private lateinit var btnBuscar: com.google.android.material.button.MaterialButton
    private lateinit var btnNuevo: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var recyclerFamilias: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: FamiliaAdapter

    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout

    private lateinit var navView: com.google.android.material.navigation.NavigationView

    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    private var idFamiliaEdicion: String = "0"

    private val codEmpresaActual = "3"
    private val codUsuarioActual = "1"

    private val menuRepository = com.maestros.familias.data.repository.MenuRepository(
        com.maestros.familias.data.api.RetrofitClient.menuApi
    )

    private val viewModel: FamiliaViewModel by viewModels {
        FamiliaViewModelFactory(FamiliaRepository(RetrofitClient.familiaApi))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //menu
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

       cargarMenuDinamico()


        //boton para atras
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if(drawerLayout.isOpen){
                    drawerLayout.close()
                }else{
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        //familias
        txtBuscar = findViewById(R.id.txtBuscar)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnNuevo = findViewById(R.id.btnNuevo)
        iniciarAnimacionPulso(btnNuevo)
        progressBar = findViewById(R.id.progressBar)
        recyclerFamilias = findViewById(R.id.recyclerFamilias)

        adapter = FamiliaAdapter(
            emptyList(),
            onEditar = { familia -> mostrarDialogoFormulario(familia) },
            onEliminar = { familia -> confirmarEliminar(familia) }
        )
        recyclerFamilias.layoutManager = LinearLayoutManager(this)
        recyclerFamilias.adapter = adapter

        btnBuscar.setOnClickListener {
            viewModel.buscar(txtBuscar.text.toString())
        }
        btnNuevo.setOnClickListener {
            mostrarDialogoFormulario(null)
        }

        lifecycleScope.launch {
            viewModel.familias.collect { lista -> adapter.actualizar(lista) }
        }
        lifecycleScope.launch {
            viewModel.cargando.collect { cargando ->
                progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.mensaje.collect { mensaje ->
                if (!mensaje.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_LONG).show()
                    viewModel.limpiarMensaje()
                }
            }
        }

        viewModel.inicializar()
    }



    private fun cargarMenuDinamico(){
        lifecycleScope.launch {
            try{
                val modulos = menuRepository.listarMenu()
                val menu = navView.menu
                menu.clear()

                for (modulo in modulos) {
                    val subMenu = menu.addSubMenu(modulo.dscMenu.uppercase())
                    for (pagina in modulo.paginas) {
                        val item = subMenu.add(pagina.dscPagina)
                        item.setOnMenuItemClickListener {
                            if(pagina.dscPagina.equals("Familias", ignoreCase = true)){
                                drawerLayout.close()
                            }else{
                                Toast.makeText(this@MainActivity, "Proximamente: ${pagina.dscPagina}", Toast.LENGTH_SHORT).show()
                                drawerLayout.close()
                            }
                            true
                        }
                    }
                }
            }catch(e:Exception){
                Toast.makeText(this@MainActivity, "Error al cargar menu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun iniciarAnimacionPulso(view: View) {
        val scaleUpX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.12f)
        val scaleUpY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.12f)
        val scaleDownX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1.12f, 1f)
        val scaleDownY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1.12f, 1f)

        val pulseUp = android.animation.AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = 700
        }
        val pulseDown = android.animation.AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
            duration = 700
        }
        val fullPulse = android.animation.AnimatorSet().apply {
            playSequentially(pulseUp, pulseDown)
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        }

        fullPulse.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.postDelayed({ fullPulse.start() }, 1200)
            }
        })
        fullPulse.start()

    }

    private fun mostrarDialogoFormulario(familia: Familia?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_familia_form, null)

        val txtDscFamilia = dialogView.findViewById<EditText>(R.id.txtDscFamilia)
        val spEstado = dialogView.findViewById<Spinner>(R.id.spEstado)

        val estados = viewModel.estados.value


        spEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados.map { it.descripcion })

        if (familia != null) {
            idFamiliaEdicion = familia.idFamilia.toString()

            txtDscFamilia.setText(familia.dscFamilia)

            val posEstado = estados.indexOfFirst {
                it.codigo == familia.codEstado.toString()
            }

            if (posEstado >= 0) {
                spEstado.setSelection(posEstado)
            }
        } else {
            idFamiliaEdicion = "0"
            txtDscFamilia.setText("")

            val posActivo = estados.indexOfFirst {
                it.codigo == "1"
            }

            if (posActivo >= 0) {
                spEstado.setSelection(posActivo)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (familia == null) "Nueva Familia" else "Editar Familia")
            .setView(dialogView)
            .setPositiveButton("Grabar") { _, _ ->

                val codEstado = estados.getOrNull(spEstado.selectedItemPosition)?.codigo ?: "0"
                val dscFamilia = txtDscFamilia.text.toString()

                if (familia == null) {
                    viewModel.grabar(codEmpresaActual, dscFamilia, codEstado, codUsuarioActual) {}
                } else {
                    viewModel.editar(idFamiliaEdicion, codEmpresaActual, dscFamilia, codEstado, codUsuarioActual) {}

                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(familia: Familia) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Familia")
            .setMessage("Esta seguro de eliminar la familia ${familia.dscFamilia}?")
            .setPositiveButton("Si") { _, _ -> viewModel.eliminar(familia.idFamilia.toString()) }
            .setNegativeButton("No", null)
            .show()
    }
}