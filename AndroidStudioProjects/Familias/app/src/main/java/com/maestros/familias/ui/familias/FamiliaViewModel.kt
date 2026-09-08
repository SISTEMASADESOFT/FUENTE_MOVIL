package com.maestros.familias.ui.familias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maestros.familias.data.model.Combo
import com.maestros.familias.data.model.Familia
import com.maestros.familias.data.repository.FamiliaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FamiliaViewModel(
    private val repository: FamiliaRepository
) : ViewModel() {

    private val _familias = MutableStateFlow<List<Familia>>(emptyList())
    val familias: StateFlow<List<Familia>> = _familias.asStateFlow()



    private val _estados = MutableStateFlow<List<Combo>>(emptyList())
    val estados: StateFlow<List<Combo>> = _estados.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun inicializar() {
        viewModelScope.launch {
            try {

                _estados.value = repository.listarEstados()
                _familias.value = repository.listar("")
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al cargar familias"
            }
        }
    }

    fun buscar(descripcion: String) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                _familias.value = repository.listar(descripcion)
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al listar familias"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun grabar(codEmpresa: String, dscFamilia: String, codEstado: String, codUsuario: String, onExito: () -> Unit) {
        if (dscFamilia.isBlank()) {
            _mensaje.value = "Ingresar los sgtes. Datos: Descripcion"
            return
        }
        if (codEstado.isBlank() || codEstado == "0") {
            _mensaje.value = "Ingresar los sgtes. Datos: Estado"
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            try {
                val resultado = repository.insertar(codEmpresa = codEmpresa.toIntOrNull() ?: 0,
                    dscFamilia = dscFamilia,
                    codEstado = codEstado.toIntOrNull() ?: 0,
                    codUsuario = codUsuario.toIntOrNull() ?: 0
                )
                _mensaje.value = resultado.mensaje
                if (resultado.exito) {
                    buscar("")
                    onExito()
                }

            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al grabar"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun editar(idFamilia: String, codEmpresa: String, dscFamilia: String, codEstado: String, codUsuario: String, onExito: () -> Unit) {
        if (dscFamilia.isBlank()) {
            _mensaje.value = "Ingresar los sgtes. Datos: Descripcion"
            return
        }
        if (codEstado.isBlank() || codEstado == "0") {
            _mensaje.value = "Ingresar los sgtes. Datos: Estado"
            return
        }
        viewModelScope.launch {
            _cargando.value = true
            try {
                val resultado = repository.actualizar(idFamilia = idFamilia.toIntOrNull() ?: 0,
                    codEmpresa = codEmpresa.toIntOrNull() ?: 0,
                    dscFamilia = dscFamilia,
                    codEstado = codEstado.toIntOrNull() ?: 0,
                    codUsuario = codUsuario.toIntOrNull() ?: 0
                )
                _mensaje.value = resultado.mensaje
                if (resultado.exito) {
                    buscar("")
                    onExito()
                }
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al editar"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun eliminar(idFamilia: String) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val resultado = repository.eliminar(
                    idFamilia = idFamilia.toIntOrNull() ?: 0
                )

                _mensaje.value = resultado.mensaje
                buscar("")
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Error al eliminar"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }
}