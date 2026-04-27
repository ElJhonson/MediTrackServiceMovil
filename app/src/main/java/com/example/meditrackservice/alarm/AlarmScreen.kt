package com.example.meditrackservice.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// alarm/AlarmScreen.kt
@Composable
fun AlarmScreen(
    hora: String,
    medicinaNombre: String,
    formaFarmaceutica: String,
    onAccion: (String) -> Unit
) {
    var tiempoRestante by remember { mutableStateOf(300) } // 5 minutos en segundos
    var countdown by remember { mutableStateOf(true) }

    // Countdown automático
    LaunchedEffect(countdown) {
        while (tiempoRestante > 0) {
            delay(1000)
            tiempoRestante--
        }
        // Llegó a 0 → marcar como OMITIDA automáticamente
        onAccion("OMITIDA")
    }

    val minutos = tiempoRestante / 60
    val segundos = tiempoRestante % 60
    val tiempoFormato = "%02d:%02d".format(minutos, segundos)

    // Color del contador cambia según urgencia
    val colorContador = when {
        tiempoRestante > 180 -> Color.White               // > 3 min → blanco
        tiempoRestante > 60  -> Color(0xFFFFEB3B)         // > 1 min → amarillo
        else                 -> Color(0xFFF44336)         // < 1 min → rojo
    }

    val icono = when (formaFarmaceutica.uppercase()) {
        "TABLETA"  -> "⬜"
        "CAPSULA"  -> "💊"
        "JARABE"   -> "🧴"
        else       -> "💊"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A237E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = icono, fontSize = 80.sp)

            Text(
                text = hora,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = medicinaNombre,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = formaFarmaceutica,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            // ← Contador regresivo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Se marcará como omitida en:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = tiempoFormato,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorContador
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onAccion("OMITIDA") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text("❌ Omitir", fontSize = 18.sp, color = Color.White)
                }

                Button(
                    onClick = { onAccion("TOMADA") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text("✅ Tomada", fontSize = 18.sp, color = Color.White)
                }
            }
        }
    }
}