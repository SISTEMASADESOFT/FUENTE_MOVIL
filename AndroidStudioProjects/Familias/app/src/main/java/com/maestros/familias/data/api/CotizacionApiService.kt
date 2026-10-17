package com.maestros.familias.data.api

import com.maestros.familias.data.model.CotizacionEditarRequest
import com.maestros.familias.data.model.CotizacionGrabarRequest
import com.maestros.familias.data.model.CotizacionObtenerRequest
import com.maestros.familias.data.model.FacturaGrabarRequest
import com.maestros.familias.data.model.GuiaBuscarRequest
import com.maestros.familias.data.model.GuiaCatalogosRequest
import com.maestros.familias.data.model.LineasResponse
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

    @POST("Servicios/Servicios.asmx/F_Cotizacion_Obtener_App")
    suspend fun obtener(@Body request: CotizacionObtenerRequest): LineasResponse

    @POST("Servicios/Servicios.asmx/F_Cotizacion_Editar_App")
    suspend fun editar(@Body request: CotizacionEditarRequest): StringResponse


    @POST("Servicios/Servicios.asmx/F_Factura_Grabar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun grabarFactura(@Body body: FacturaGrabarRequest): StringResponse

    @POST("Servicios/Servicios.asmx/F_Guia_Catalogos_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun guiaCatalogos(@Body body: GuiaCatalogosRequest): StringResponse

    @POST("Servicios/Servicios.asmx/F_Guia_Buscar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun guiaBuscar(@Body body: GuiaBuscarRequest): ListResponse

}