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
import com.maestros.familias.data.model.Usuario
import com.maestros.familias.data.repository.LoginRepository
import kotlinx.coroutines.launch

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

        lifecycleScope.launch{
            viewModel.usuarioLogueado.collect{ usuario ->
                if(usuario !=null){
                    Toast.makeText(this@LoginActivity, "Bienvenido ${usuario.nombre}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
        viewModel.cargarAlmacenes()
    }
}