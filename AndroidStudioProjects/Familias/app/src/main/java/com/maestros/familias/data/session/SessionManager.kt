package com.maestros.familias.data.session

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREFS_NAME = "sesion_familias"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun guardarSesion(codUsuario: Int, codEmpresa: Int, codAlmacen: Int, nombreUsuario: String) {
        prefs.edit()
            .putInt("codUsuario", codUsuario)
            .putInt("codEmpresa", codEmpresa)
            .putInt("codAlmacen", codAlmacen)
            .putString("nombreUsuario", nombreUsuario)
            .apply()
    }

    fun guardarCodEmpleado(codEmpleado: Int) {
        prefs.edit().putInt("codEmpleado", codEmpleado).apply()
    }

    fun getCodEmpleado(): Int = prefs.getInt("codEmpleado", 0)

    fun getCodUsuario(): Int = prefs.getInt("codUsuario", 0)
    fun getCodEmpresa(): Int = prefs.getInt("codEmpresa", 0)
    fun getCodAlmacen(): Int = prefs.getInt("codAlmacen", 0)
    fun getNombreUsuario(): String = prefs.getString("nombreUsuario", "") ?: ""

    fun cerrarSesion() = prefs.edit().clear().apply()

    fun guardarSesion(codUsuario: Int, codEmpresa: Int, codAlmacen: Int, nombreUsuario: String, perfil: String) {
        prefs.edit()
            .putInt("codUsuario", codUsuario)
            .putInt("codEmpresa", codEmpresa)
            .putInt("codAlmacen", codAlmacen)
            .putString("nombreUsuario", nombreUsuario)
            .putString("perfil", perfil)
            .apply()
    }

    fun getPerfil(): String = prefs.getString("perfil", "") ?: ""
}