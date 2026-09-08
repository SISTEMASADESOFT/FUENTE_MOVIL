package com.maestros.familias.data.repository

import com.maestros.familias.data.api.LoginApiService
import com.maestros.familias.data.model.Usuario

class LoginRepository(private val api: LoginApiService) {

    suspend fun login(usuario: String, clave: String, codAlmacen: String): Usuario {
        val body = mapOf(
            "NombreUsuario" to usuario,
            "Clave" to clave,
            "CodAlmacen" to codAlmacen
        )
        val response = api.login(body).d
        return Usuario(
            codUsuario = response.CodUsuario,
            nombreUsuario = response.NombreUsuario,
            codAlmacen = response.CodAlmacen,
            perfil = response.Perfil,
            apellidos = response.Apellidos,
            nombre = response.Nombre,
            codEmpresa = response.CodEmpresa,
            empresa = response.Empresa,
            pagina = response.Pagina,
            almacen = response.Almacen,
            msgError = response.MsgError
        )
    }

    suspend fun listarAlmacenes(): List<Pair<Int, String>> {
        return api.listarAlmacenes().d.map { it.CodAlmacen to it.DscAlmacen }
    }
}