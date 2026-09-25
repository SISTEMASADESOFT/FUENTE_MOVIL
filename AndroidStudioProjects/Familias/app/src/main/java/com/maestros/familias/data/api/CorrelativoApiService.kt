package com.maestros.familias.data.api

import com.maestros.familias.data.model.NumeroRequest
import com.maestros.familias.data.model.SerieRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface CorrelativoApiService {
    @POST("Servicios/Servicios.asmx/F_Correlativo_Serie_Listar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarSeries(@Body body: SerieRequest): ListResponse

    @POST("Servicios/Servicios.asmx/F_Correlativo_Numero_Obtener_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun obtenerNumero(@Body body: NumeroRequest): StringResponse
}

data class StringResponse(val d: String)