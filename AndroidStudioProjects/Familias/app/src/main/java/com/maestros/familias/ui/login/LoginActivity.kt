package com.maestros.familias.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.maestros.familias.MainActivity
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.CodUsuarioRequest
import com.maestros.familias.data.model.Usuario
import com.maestros.familias.data.repository.LoginRepository
import com.maestros.familias.data.session.SessionManager
import kotlinx.coroutines.launch
import com.maestros.familias.ui.ventas.InicioVentasActivity

class LoginActivity: AppCompatActivity() {
    private lateinit var txtUsuario: TextInputEditText
    private lateinit var txtClave: TextInputEditText
    private lateinit var spAlmacen: Spinner
    private lateinit var btnIngresar: MaterialButton
    private lateinit var progressLogin: ProgressBar

    private var codigosAlmacen: List<Int> = emptyList()

    private val viewModel: LoginViewModel by viewModels{
        LoginViewModelFactory(LoginRepository(RetrofitClient.loginApi))
    }

    override fun onCreate(savedInstanceState: Bundle?){
        SessionManager.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtUsuario = findViewById(R.id.txtUsuario)
        txtClave = findViewById(R.id.txtClave)
        spAlmacen = findViewById(R.id.spAlmacen)
        btnIngresar = findViewById(R.id.btnIngresar)
        progressLogin = findViewById(R.id.progressLogin)

        btnIngresar.setOnClickListener {
            val codAlmacen = codigosAlmacen.getOrNull(spAlmacen.selectedItemPosition)?.toString() ?: "0"
            viewModel.login(
                txtUsuario.text.toString(),
                txtClave.text.toString(),
                codAlmacen
            )
        }

        lifecycleScope.launch{
            viewModel.almacenes.collect { lista ->
                codigosAlmacen = lista.map {it.first}
                spAlmacen.adapter = ArrayAdapter(
                    this@LoginActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    lista.map {it.second}
                )
            }
        }

        lifecycleScope.launch{
            viewModel.cargando.collect {  cargando ->
                progressLogin.visibility = if(cargando) View.VISIBLE else View.GONE
                btnIngresar.isEnabled = !cargando
             }
        }

        lifecycleScope.launch{
            viewModel.mensaje.collect{mensaje ->
                if(!mensaje.isNullOrBlank()){
                    Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                    viewModel.limpiarMensaje()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.usuarioLogueado.collect { usuario ->
                if (usuario != null) {

                    val codAlmacenReal = codigosAlmacen.getOrNull(spAlmacen.selectedItemPosition)

                    if (codAlmacenReal == null) {
                        Toast.makeText(this@LoginActivity, "Error: no se pudo determinar el almacén. Vuelve a intentar.", Toast.LENGTH_LONG).show()
                        return@collect
                    }

                    SessionManager.guardarSesion(
                        codUsuario = usuario.codUsuario,
                        codEmpresa = usuario.codEmpresa,
                        codAlmacen = codAlmacenReal,
                        nombreUsuario = usuario.nombre,
                        perfil = usuario.perfil
                    )

                    lifecycleScope.launch {
                        try{
                            val codEmpleado = RetrofitClient.empleadoApi
                                .obtenerCodEmpleado(CodUsuarioRequest(usuario.codUsuario)).d
                            SessionManager.guardarCodEmpleado(codEmpleado.toIntOrNull()?: 0)
                        }catch(e: Exception){
                            Toast.makeText(this@LoginActivity, "Advertencia: no se pudo obtener el código de empleado", Toast.LENGTH_LONG).show()
                        }
                    }
                    Toast.makeText(this@LoginActivity, "Bienvenido ${usuario.nombre}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, InicioVentasActivity::class.java))
                    finish()
                }
            }
        }
        viewModel.cargarAlmacenes()
    }
}