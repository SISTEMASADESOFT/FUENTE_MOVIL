package com.maestros.familias.data.api

import com.maestros.familias.data.model.CodUsuarioRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface EmpleadoApiService {
    @POST("Servicios/Servicios.asmx/F_Empleado_ObtenerPorUsuario_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun obtenerCodEmpleado(@Body body: CodUsuarioRequest): StringResponse
}