package com.maestros.familias.data.api

import com.maestros.familias.data.model.ClienteBuscarRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClienteApiService {
    @POST("Servicios/Servicios.asmx/F_ListarClientes_AutoComplete_Alvarado")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun buscarClientes(@Body body: ClienteBuscarRequest): ListResponse
}