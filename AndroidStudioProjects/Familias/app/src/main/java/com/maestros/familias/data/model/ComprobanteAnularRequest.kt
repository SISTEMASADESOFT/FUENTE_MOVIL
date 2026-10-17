package com.maestros.familias.data.model

data class ComprobanteAnularRequest(
    val CodProforma: Int,
    val CodUsuario: Int,
    val Observacion: String
)

data class StringResponse(
    val d: String
)