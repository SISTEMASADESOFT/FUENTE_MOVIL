package com.maestros.familias.data.api

import com.maestros.familias.data.model.ComprobanteAnularRequest
import com.maestros.familias.data.model.ComprobanteListarRequest
import com.maestros.familias.data.model.ComprobantePdfRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ComprobanteApiService {
    @POST("Servicios/Servicios.asmx/F_Cotizacion_Listar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarCotizaciones(@Body body: ComprobanteListarRequest): ListResponse

    @POST("Servicios/Servicios.asmx/F_Cotizacion_PDF_Obtener_App")
    suspend fun obtenerPdf(@Body request: ComprobantePdfRequest): StringResponse

    @POST("Servicios/Servicios.asmx/F_Cotizacion_Anular_App")
    suspend fun anular(@Body request: ComprobanteAnularRequest): StringResponse

}