package com.maestros.familias.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object VencimientoHelper {

    //dias de crédito por condición de pago
    fun dias(codFormaPago: Int): Int = when (codFormaPago) {
        3 -> 30
        4 -> 15
        8 -> 45
        9 -> 60
        11 -> 7
        13 -> 75
        14 -> 90
        else -> 0   // 1, 6, 10, 12, 15 y las demás: mismo día de la emisión
    }

    fun calcular(emisionDdMmYyyy: String, codFormaPago: Int): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
        val fecha = try { sdf.parse(emisionDdMmYyyy) } catch (e: Exception) { null }
            ?: return emisionDdMmYyyy
        val cal = Calendar.getInstance().apply {
            time = fecha
            add(Calendar.DAY_OF_MONTH, dias(codFormaPago))
        }
        return sdf.format(cal.time)
    }
}