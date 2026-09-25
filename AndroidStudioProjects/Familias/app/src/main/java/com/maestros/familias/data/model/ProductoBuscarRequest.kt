package com.maestros.familias.data.model

data class ProductoBuscarRequest(
    val Descripcion: String,
    val CodAlmacen: String,
    val CodMoneda: Int
)