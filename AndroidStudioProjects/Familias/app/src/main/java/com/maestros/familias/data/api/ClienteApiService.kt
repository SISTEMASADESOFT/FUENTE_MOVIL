package com.maestros.familias.data.api

import com.maestros.familias.data.model.ClienteBuscarRequest
import com.maestros.familias.data.model.DistritoBuscarRequest
import com.maestros.familias.data.model.DocumentoConsultarRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClienteApiService {
    @POST("Servicios/Servicios.asmx/F_ListarClientes_AutoComplete_Alvarado")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun buscarClientes(@Body body: ClienteBuscarRequest): ListResponse

    @POST("Servicios/Servicios.asmx/F_Documento_Consultar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun consultarDocumento(@Body body: DocumentoConsultarRequest): StringResponse

    @POST("Servicios/Servicios.asmx/F_TCDistrito_Listar")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarDistritos(@Body body: DistritoBuscarRequest): ListResponse

    @POST("Servicios/Servicios.asmx/F_Direcciones_Listar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarDirecciones(@Body body: Map<String, @JvmSuppressWildcards Any>): ListResponse

}