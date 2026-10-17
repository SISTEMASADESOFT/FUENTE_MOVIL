package com.maestros.familias.ui.login

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.maestros.familias.MainActivity
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.CodUsuarioRequest
import com.maestros.familias.data.model.Usuario
import com.maestros.familias.data.repository.ImeiRepository
import com.maestros.familias.data.repository.LoginRepository
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.DeviceIdHelper
import kotlinx.coroutines.launch
import com.maestros.familias.ui.ventas.InicioVentasActivity

class LoginActivity: AppCompatActivity() {
    private lateinit var txtUsuario: TextInputEditText
    private lateinit var txtClave: TextInputEditText
    private lateinit var spAlmacen: Spinner
    private lateinit var btnIngresar: MaterialButton
    private lateinit var progressLogin: ProgressBar
    private lateinit var txtVerIdDispositivo: TextView

    private var codigosAlmacen: List<Int> = emptyList()

    private val viewModel: LoginViewModel by viewModels{
        LoginViewModelFactory(LoginRepository(RetrofitClient.loginApi))
    }

    override fun onCreate(savedInstanceState: Bundle?){
        SessionManager.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //permiso d imei
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
            ){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_PHONE_STATE), 100)
        }

        txtUsuario = findViewById(R.id.txtUsuario)
        txtClave = findViewById(R.id.txtClave)
        spAlmacen = findViewById(R.id.spAlmacen)
        btnIngresar = findViewById(R.id.btnIngresar)
        progressLogin = findViewById(R.id.progressLogin)
        txtVerIdDispositivo = findViewById(R.id.txtVerIdDispositivo)

        txtVerIdDispositivo.setOnClickListener{
            val identificador = DeviceIdHelper.obtenerIdentificador(this)

            AlertDialog.Builder(this)
                .setTitle("ID de este dispositivo")
                .setMessage("Envia este codigo para que autorice el acceso:\n\n$identificador")
                .setPositiveButton("Copiar"){_,_ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("ID dispositivo", identificador)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Copiado al portapapeles",Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cerrar",null)
                .show()
        }

        btnIngresar.setOnClickListener {
            //imei
            val identificador = DeviceIdHelper.obtenerIdentificador(this)
            lifecycleScope.launch {
                progressLogin.visibility = View.GONE
                btnIngresar.isEnabled = false
            try{
                val autorizado= ImeiRepository(RetrofitClient.imeiApi).estaAutorizado(identificador)
                progressLogin.visibility= View.GONE
                btnIngresar.isEnabled=true

                if (!autorizado){
                    AlertDialog.Builder(this@LoginActivity)
                        .setTitle("Dispositivo no autorizado")
                        .setMessage("Este dispositivo no tiene permiso. Contacta al administrador para registrarlo")
                        .setPositiveButton("Entendido",null)
                        .show()
                    return@launch
                }
                val codAlmacen = codigosAlmacen.getOrNull(spAlmacen.selectedItemPosition)?.toString() ?: "0"
                viewModel.login(
                    txtUsuario.text.toString(),
                    txtClave.text.toString(),
                    codAlmacen
                )
            }catch (e: Exception){
                progressLogin.visibility= View.GONE
                btnIngresar.isEnabled= true
                Toast.makeText(this@LoginActivity, "Error validando dispositivo: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
            }
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