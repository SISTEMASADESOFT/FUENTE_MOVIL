
package com.maestros.familias.data.model

import java.math.BigDecimal


data class GuiaCatalogosRequest(val codEmpresa: Int, val codAlmacen: Int)
data class GuiaBuscarRequest(val texto: String, val tipo: Int)   // tipo 1 = transportista, 2 = conductor

data class ItemCatalogo(val codigo: Int, val descripcion: String)

data class GuiaCatalogos(val tipos: List<ItemCatalogo>, val unidades: List<ItemCatalogo>)


data class PersonaGuia(
    val codCtaCte: Int,
    val ruc: String,
    val razonSocial: String,
    val nombres: String,
    val placa: String,
    val licencia: String
) {

    val nombre: String get() = nombres.ifBlank { razonSocial }
}


data class GuiaDatos(
    val serie: String,
    val numero: String,
    val fechaTraslado: String,
    val codTipoTransportista: Int,
    val transportista: PersonaGuia?,
    val conductor: PersonaGuia?,
    val placa: String,
    val marca: String,
    val licencia: String,
    val bultos: Int,
    val peso: BigDecimal,
    val codUnidadPeso: Int,
    val observacion: String
)