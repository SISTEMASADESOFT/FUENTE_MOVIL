package com.maestros.familias.data.model

data class ClienteBuscarRequest(
    val NroRuc: String = "",
    val RazonSocial: String = "",
    val CodTipoCtaCte: String = "1",
    val CodTipoCliente: String = "0"
)