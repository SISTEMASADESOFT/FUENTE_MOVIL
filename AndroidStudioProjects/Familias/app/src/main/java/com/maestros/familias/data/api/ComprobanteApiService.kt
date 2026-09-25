package com.maestros.familias.data.api

import com.maestros.familias.data.model.ComprobanteListarRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ComprobanteApiService {
    @POST("Servicios/Servicios.asmx/F_Cotizacion_Listar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarCotizaciones(@Body body: ComprobanteListarRequest): ListResponse
}