package com.example.meditrackservice.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

    LaunchedEffect(Unit) {
        viewModel.cargarAlarmasHoy()
    }

    LaunchedEffect(estado) {
        if (estado is AlarmaEstado.SesionExpirada) onSesionExpirada()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Hoy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (estado) {

            is AlarmaEstado.Cargando -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                        Text("No tienes alarmas hoy")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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

    val hora = alarma.fechaHora.substring(11, 16)

    val (colorEstado, icono) = when (alarma.estado) {
        "TOMADA" -> Pair(Color(0xFF4CAF50), Icons.Default.CheckCircle)
        "OMITIDA" -> Pair(Color(0xFFF44336), Icons.Default.Cancel)
        else -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.Alarm)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Hora en círculo
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hora,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarma.medicinaNombre,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = alarma.estado,
                    color = colorEstado,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorEstado
            )
        }
    }
}