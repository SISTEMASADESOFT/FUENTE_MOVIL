package com.maestros.familias.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface LoginApiService {
    @POST("Servicios/Servicios.asmx/F_Login")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun login(@Body body: Map<String, String>): LoginResponse

    @POST("Servicios/Servicios.asmx/F_Login_Almacenes")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarAlmacenes(): AlmacenesResponse
}

data class LoginResponse(
    val d: LoginData
)

data class LoginData(
    val CodUsuario: Int = 0,
    val NombreUsuario: String = "",
    val CodAlmacen: Int = 0,
    val Perfil: String = "",
    val Apellidos: String = "",
    val Nombre: String = "",
    val CodEmpresa: Int = 0,
    val Empresa: String = "",
    val Pagina: String = "",
    val Almacen: String = "",
    val MsgError: String = ""
)

data class AlmacenesResponse(
    val d: List<AlmacenData>
)

data class AlmacenData(
    val CodAlmacen: Int = 0,
    val DscAlmacen: String = ""
)