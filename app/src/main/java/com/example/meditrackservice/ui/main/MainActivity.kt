package com.example.meditrackservice.ui.main

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.meditrackservice.sync.SyncScheduler
import com.example.meditrackservice.ui.login.LoginActivity
import com.example.meditrackservice.ui.theme.MediTrackServiceTheme

// ui/main/MainActivity.kt
// ui/main/MainActivity.kt
class MainActivity : ComponentActivity() {

    private val viewModel: AlarmaViewModel by viewModels {
        AlarmaViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permiso de alarmas exactas en Android 12+
        solicitarPermisoAlarmas()

        // Solicitar permiso de notificaciones en Android 13+
        solicitarPermisoNotificaciones()
        SyncScheduler.iniciar(this)

        setContent {
            MediTrackServiceTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSesionExpirada = { irAlLogin() }
                )
            }
        }
    }

    private fun solicitarPermisoAlarmas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Lleva al usuario a la pantalla de configuración del sistema
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun irAlLogin() {
        SyncScheduler.cancelar(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}