package com.maestros.familias.data.api

import com.maestros.familias.data.model.DecimalResponse
import com.maestros.familias.data.model.TipoCambioRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TipoCambioApiService {
    @POST("Servicios/Servicios.asmx/F_TipoCambio_Obtener_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun obtener(@Body body: TipoCambioRequest): DecimalResponse

    @POST("Servicios/Servicios.asmx/F_Igv_Obtener_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun obtenerIgv(): DecimalResponse
}