package com.maestros.familias.data.model

data class ComprobanteListarRequest(
    val CodAlmacen: Int,
    val CodEmpresa: Int,
    val Texto: String,
    val UsarFecha: Boolean,
    val Desde: String,
    val Hasta: String
)