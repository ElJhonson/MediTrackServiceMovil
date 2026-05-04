package com.example.meditrackservice.ui.main


import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.meditrackservice.alarm.AlarmForegroundService
import com.example.meditrackservice.data.local.AlarmasProgramadasStore
import com.example.meditrackservice.sync.SyncScheduler
import com.example.meditrackservice.ui.historial.HistorialActivity
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
        solicitarPermisoFullScreen()
        SyncScheduler.iniciar(this)

        val serviceIntent = Intent(this, AlarmForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }


        setContent {
            MediTrackServiceTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSesionExpirada = { irAlLogin() },
                    onVerHistorial = { irAHistorial() } // ← agregar
                )
            }
        }

    }
    private fun irAHistorial() {
        startActivity(Intent(this, HistorialActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarAlarmasHoy()
        viewModel.iniciarSyncEnTiempoReal()
    }

    override fun onPause() {
        super.onPause()
        viewModel.detenerSyncEnTiempoReal()
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

    // ui/main/MainActivity.kt
    private fun solicitarPermisoFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {
                try {
                    val intent = Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENTS").apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Si el dispositivo no soporta esta pantalla, abrir ajustes generales
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
            }
        }
    }

    private fun irAlLogin() {
        SyncScheduler.cancelar(this)
        AlarmasProgramadasStore.limpiar(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}