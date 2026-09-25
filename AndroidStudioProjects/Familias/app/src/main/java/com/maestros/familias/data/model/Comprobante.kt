package com.maestros.familias.data.model

data class Comprobante(
    val codigo: String,
    val numero: String,
    val cliente: String,
    val fechaEmision: String,
    val total: String,
    val estado: String,
    val moneda: String
)