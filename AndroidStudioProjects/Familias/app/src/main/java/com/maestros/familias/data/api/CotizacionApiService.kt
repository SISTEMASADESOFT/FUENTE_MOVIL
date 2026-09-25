package com.maestros.familias.data.api

import com.maestros.familias.data.model.CotizacionGrabarRequest
import com.maestros.familias.data.model.PdfRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface CotizacionApiService {
    @POST("Servicios/Servicios.asmx/F_Cotizacion_Grabar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun grabar(@Body body: CotizacionGrabarRequest): StringResponse

    @POST("Servicios/Servicios.asmx/F_Cotizacion_PDF_Obtener_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun obtenerPdf(@Body body: PdfRequest): StringResponse
}