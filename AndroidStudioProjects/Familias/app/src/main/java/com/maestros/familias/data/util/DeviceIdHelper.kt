package com.maestros.familias.data.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

object DeviceIdHelper {

    @SuppressLint("HardwareIds", "MissingPermission")
    fun obtenerIdentificador(context: Context): String {
        //1.Intento real: IMEI, solo posible si el permiso esta concedido
        //y el sistema operativo lo permite (Android 9 o anterior)
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telephonyManager.imei
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.deviceId
                }
                if (!imei.isNullOrBlank()) return imei
            } catch (e: SecurityException) {
                // Android 10+ bloquea esto para apps normales, sin importar el permiso.
                // Caemos al identificador alterno.
            } catch (e: Exception) {

            }
        }

        // 2. Alternativa: identificador único por instalación de la app en este dispositivo.
        //    Disponible sin permisos especiales en cualquier version de Android.
        return Settings.Secure.getString(context.contentResolver,
            Settings.Secure.ANDROID_ID) ?: "SIN_IDENTIFICAR"
    }
}