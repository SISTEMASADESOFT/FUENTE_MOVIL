package com.maestros.familias.data.model

data class SeriesDocRequest(
    val CodTipoDoc: Int,
    val CodAlmacen: Int,
    val CodEmpresa: Int
)

data class NumeroDocRequest(
    val CodTipoDoc: Int,
    val CodAlmacen: Int,
    val CodEmpresa: Int,
    val SerieDoc: String
)

data class SerieDocumento(
    val codSerie: String,
    val serieDoc: String
)