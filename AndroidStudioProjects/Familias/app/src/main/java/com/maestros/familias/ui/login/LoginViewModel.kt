package com.maestros.familias.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maestros.familias.data.model.Usuario
import com.maestros.familias.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository): ViewModel() {
    private val _almacenes = MutableStateFlow<List<Pair<Int, String>>>(emptyList())
    val almacenes: StateFlow<List<Pair<Int, String>>> = _almacenes.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _usuarioLogueado = MutableStateFlow<Usuario?>(null)
    val usuarioLogueado: StateFlow<Usuario?> = _usuarioLogueado.asStateFlow()

    fun cargarAlmacenes() {
        viewModelScope.launch {
            try {
                _almacenes.value = repository.listarAlmacenes()
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al cargar sucursales"
            }
        }
    }

    fun login(usuario: String, clave: String, codAlmacen: String) {
        if (usuario.isBlank()) {
            _mensaje.value = "Ingrese su usuario"
            return
        }
        if (clave.isBlank()) {
            _mensaje.value = "Ingrese su contraseña"
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            try {
                val resultado = repository.login(usuario, clave, codAlmacen)
                if (resultado.msgError.isBlank() && resultado.codUsuario > 0) {
                    _usuarioLogueado.value = resultado
                } else {
                    _mensaje.value = resultado.msgError.ifBlank { "Los datos son incorrectos" }
                }
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al iniciar sesion"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }
}

class LoginViewModelFactory(private val repository: LoginRepository) :
        ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LoginViewModel(repository) as T
    }
        }



























































































































