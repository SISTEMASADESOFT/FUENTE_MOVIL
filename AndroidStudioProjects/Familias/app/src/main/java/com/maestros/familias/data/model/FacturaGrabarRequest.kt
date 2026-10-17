
package com.maestros.familias.data.model

import java.math.BigDecimal

data class FacturaGrabarRequest(
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
    val CodEmpleado: Int,
    val NroRuc: String,
    val RazonSocial: String,
    val NroOperacion: String,

    // ---- guía
    val FlagGuia: Int,
    val SerieGuia: String,
    val NumeroGuia: String,
    val CodTipoTransportista: Int,
    val FechaTraslado: String,
    val CodTransportista: Int,
    val NroRucTransportista: String,
    val RazonSocialTransportista: String,
    val PlacaTraslado: String,
    val MarcaVehiculo: String,
    val Licencia: String,
    val NroBultos: Int,
    val Peso: BigDecimal,
    val CodUnidadPeso: Int,
    val CodConductor: Int,
    val ObservacionGuia: String
)