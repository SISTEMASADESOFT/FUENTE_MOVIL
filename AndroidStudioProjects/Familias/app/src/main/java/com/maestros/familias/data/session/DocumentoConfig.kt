package com.maestros.familias.data.session

data class DocumentoConfig(
    val tipoDoc: Int,
    val titulo: String,
    val nombre: String,
    val textoEmitir: String?,
    val soloRuc: Boolean,
    val exigeDireccion: Boolean,
    val fechaEditable: Boolean,
    val esVenta: Boolean,
    val formaPagoPorDefecto: String,
    val formasPagoExcluidas: Set<String>,
    val usaGuia: Boolean
)

object Documentos {
    val COTIZACION = DocumentoConfig(
        tipoDoc = 15,
        titulo = "Nueva Cotización",
        nombre = "Cotización",
        textoEmitir = null,
        soloRuc = false,
        exigeDireccion = false,
        fechaEditable = true,
        esVenta = false,
        formaPagoPorDefecto = "",
        formasPagoExcluidas = emptySet(),
        usaGuia = false
    )

    val FACTURA = DocumentoConfig(
        tipoDoc = 1,
        titulo = "Nueva Factura",
        nombre = "Factura",
        textoEmitir = "Emitir factura",
        soloRuc = true,
        exigeDireccion = true,
        fechaEditable = false,
        esVenta = true,
        formaPagoPorDefecto = "1",
        formasPagoExcluidas = setOf("10"),
        usaGuia = true
    )

    fun porTipo(tipoDoc: Int): DocumentoConfig = when (tipoDoc) {
        1 -> FACTURA
        else -> COTIZACION
    }
}