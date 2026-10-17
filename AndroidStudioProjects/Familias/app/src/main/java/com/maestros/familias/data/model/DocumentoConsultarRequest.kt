package com.maestros.familias.data.model

data class DocumentoConsultarRequest(
    val Numero: String,
    val CodEmpresa: Int
)

sealed class ConsultaDocumento {
    data class Nuevo(val cliente: Cliente) : ConsultaDocumento()
    data class Existe(val cliente: Cliente) : ConsultaDocumento()
}