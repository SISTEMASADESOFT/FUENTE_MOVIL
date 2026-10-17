package com.maestros.familias.data.model

data class ComprobantePdfRequest(
    val CodProforma: Int,
    val CodEmpresa: Int,
    val CodAlmacen: Int,
    val SerieDoc: String
)
