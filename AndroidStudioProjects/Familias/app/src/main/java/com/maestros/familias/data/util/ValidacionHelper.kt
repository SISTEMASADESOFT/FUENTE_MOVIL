package com.maestros.familias.data.util

object ValidacionHelper {


    fun rucValido(valor: String): Boolean {
        val v = valor.trim()
        if (v.isEmpty() || !v.all { it.isDigit() }) return false
        if (v.length == 8) return true
        if (v.length != 11) return false

        val pesos = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        var suma = 0
        for (i in 0 until 10) suma += (v[i] - '0') * pesos[i]

        var resto = 11 - (suma % 11)
        if (resto >= 10) resto -= 10
        return resto == (v[10] - '0')
    }
}