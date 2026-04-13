package com.example.meditrackservice.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meditrackservice.data.model.AlarmaResponse

@Composable
fun MainScreen(
    viewModel: AlarmaViewModel,
    onSesionExpirada: () -> Unit
) {
    val alarmas by viewModel.alarmas.observeAsState(emptyList())
    val estado by viewModel.estado.observeAsState()

    // Cargar alarmas al entrar
    LaunchedEffect(Unit) {
        viewModel.cargarAlarmasHoy()
    }

    // Sesión expirada → regresar al login
    LaunchedEffect(estado) {
        if (estado is AlarmaEstado.SesionExpirada) onSesionExpirada()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mis alarmas de hoy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (estado) {
            is AlarmaEstado.Cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AlarmaEstado.Error -> {
                Text(
                    text = (estado as AlarmaEstado.Error).mensaje,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                if (alarmas.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tienes alarmas programadas para hoy")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(alarmas) { alarma ->
                            AlarmaCard(alarma)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmaCard(alarma: AlarmaResponse) {
    val hora = alarma.fechaHora.substring(11, 16) // "08:00"

    val colorEstado = when (alarma.estado) {
        "TOMADA"  -> Color(0xFF388E3C)
        "OMITIDA" -> Color(0xFFD32F2F)
        else      -> Color(0xFF1976D2) // PENDIENTE
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hora
            Text(
                text = hora,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(64.dp)
            )

            // Nombre medicina
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarma.medicinaNombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = alarma.estado,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorEstado
                )
            }

            // Ícono estado
            Icon(
                imageVector = when (alarma.estado) {
                    "TOMADA"  -> Icons.Default.CheckCircle
                    "OMITIDA" -> Icons.Default.Cancel
                    else      -> Icons.Default.Alarm
                },
                contentDescription = alarma.estado,
                tint = colorEstado
            )
        }
    }
}