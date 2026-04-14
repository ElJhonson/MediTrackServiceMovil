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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// alarm/AlarmScreen.kt
@Composable
fun AlarmScreen(
    hora: String,
    medicinaNombre: String,
    formaFarmaceutica: String,
    onAccion: (String) -> Unit
) {
    val icono = when (formaFarmaceutica.uppercase()) {
        "CAPSULA", "CÁPSULA" -> "💊"
        "INYECCION", "INYECCIÓN" -> "💉"
        "JARABE", "LIQUIDO", "LÍQUIDO" -> "🧴"
        "TABLETA", "COMPRIMIDO" -> "⬜"
        else -> "💊"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFF5722)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {

            // Ícono grande
            Text(text = icono, fontSize = 80.sp)

            // Hora
            Text(
                text = hora,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Nombre medicina
            Text(
                text = medicinaNombre,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Forma farmacéutica
            Text(
                text = formaFarmaceutica,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón OMITIDA
                Button(
                    onClick = { onAccion("OMITIDA") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text(
                        text = "❌ Omitir",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                // Botón TOMADA
                Button(
                    onClick = { onAccion("TOMADA") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text(
                        text = "✅ Tomada",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}