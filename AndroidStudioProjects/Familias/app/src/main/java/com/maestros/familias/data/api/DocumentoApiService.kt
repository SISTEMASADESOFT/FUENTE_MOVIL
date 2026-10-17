package com.maestros.familias.data.api

import com.maestros.familias.data.model.LineasResponse
import com.maestros.familias.data.model.NumeroDocRequest
import com.maestros.familias.data.model.SeriesDocRequest
import com.maestros.familias.data.model.StringResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DocumentoApiService {

    @POST("Servicios/Servicios.asmx/F_Correlativo_Serie_Activas_App")
    suspend fun seriesActivas(@Body request: SeriesDocRequest): LineasResponse

    @POST("Servicios/Servicios.asmx/F_Correlativo_Numero_Obtener_App")
    suspend fun numero(@Body request: NumeroDocRequest): StringResponse
}