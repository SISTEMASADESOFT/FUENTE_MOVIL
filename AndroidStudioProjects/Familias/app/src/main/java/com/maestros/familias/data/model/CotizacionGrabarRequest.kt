package com.maestros.familias.data.model

import java.math.BigDecimal

data class CotizacionGrabarRequest(
    val DetalleJson: String,
    val CodCliente: Int,
    val SerieDoc: String,
    val NumeroDoc: String,
    val FechaEmision: String,
    val CodMoneda: Int,
    val TipoCambio: BigDecimal,
    val CodTasa: Int,
    val TasaIgv: BigDecimal,
    val CodFormaPago: Int,
    val SubTotal: BigDecimal,
    val Igv: BigDecimal,
    val Total: BigDecimal,
    val CodAlmacen: Int,
    val CodEmpresa: Int,
    val CodUsuario: Int,
    val NroRuc: String,
    val RazonSocial: String,
    val Direccion: String,
    val CodDepartamento: Int,
    val CodProvincia: Int,
    val CodDistrito: Int,
    val CodDireccion: Int,
    val CodEmpleado: Int
)