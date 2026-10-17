package com.maestros.familias.data.model

import java.math.BigDecimal

data class CotizacionObtenerRequest(val CodProforma: Int)

data class LineasResponse(val d: List<String>)

data class CotizacionEditarRequest(
    val DetalleJson: String,
    val CodProformaAnterior: Int,
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
    val CodEmpleado: Int,
    val NroRuc: String,
    val RazonSocial: String,
    val Direccion: String,
    val CodDepartamento: Int,
    val CodProvincia: Int,
    val CodDistrito: Int,
    val CodDireccion: Int,
    val Placa: String,
    val KM: String,
    val Observacion: String,
    val Atencion: String,
    val Referencia: String,
    val NroOperacion: String,
    val FlagComisionable: Int,
    val FlagConCodigo: Int
)

data class DetalleCargado(
    val codProducto: String,
    val cantidad: Int,
    val precio: BigDecimal,
    val descripcion: String
)

data class CotizacionCargada(
    val codProforma: Int,
    val serie: String,
    val numero: String,
    val emision: String,
    val codMoneda: Int,
    val tipoCambio: BigDecimal,
    val codFormaPago: Int,
    val flagIgv: Int,
    val total: BigDecimal,
    val cliente: Cliente,
    val placa: String,
    val km: String,
    val observacion: String,
    val atencion: String,
    val referencia: String,
    val nroOperacion: String,
    val codEmpleado: Int,
    val flagComisionable: Int,
    val flagConCodigo: Int,
    val detalle: List<DetalleCargado>
)