package com.example.meditrackservice.ui.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meditrackservice.data.model.AlarmaResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel,
    onBack: () -> Unit
) {
    val alarmas by viewModel.alarmas.observeAsState(emptyList())
    val estado by viewModel.estado.observeAsState()
    val semanaOffset by viewModel.semanaOffset.observeAsState(0)
    val rangoSemana by viewModel.rangoSemana.observeAsState()

    // Estadísticas
    val tomadas = alarmas.count { it.estado == "TOMADA" }
    val omitidas = alarmas.count { it.estado == "OMITIDA" }
    val pendientes = alarmas.count { it.estado == "PENDIENTE" }
    val total = alarmas.size
    val porcentaje = if (total > 0) (tomadas * 100f) / total else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            // Navegación semanal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.semanaAnterior() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Semana anterior")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (semanaOffset) {
                                0    -> "Esta semana"
                                -1   -> "Semana pasada"
                                1    -> "Próxima semana"
                                else -> if (semanaOffset < 0)
                                    "Hace ${-semanaOffset} semanas"
                                else
                                    "En $semanaOffset semanas"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        rangoSemana?.let { (inicio, fin) ->
                            val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale("es", "MX"))
                            Text(
                                text = "${inicio.format(formatter)} - ${fin.format(formatter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.semanaSiguiente() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Semana siguiente")
                    }
                }
            }

            // Estadísticas compactas
            if (alarmas.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            EstadisticaChip(
                                valor = tomadas,
                                label = "Tomadas",
                                color = Color(0xFF4CAF50)
                            )
                            EstadisticaChip(
                                valor = omitidas,
                                label = "Omitidas",
                                color = Color(0xFFF44336)
                            )
                            EstadisticaChip(
                                valor = pendientes,
                                label = "Pendientes",
                                color = Color(0xFF9E9E9E)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Adherencia",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(80.dp)
                            )
                            LinearProgressIndicator(
                                progress = { porcentaje / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp),
                                color = Color(0xFF4CAF50),
                                trackColor = Color(0xFFE0E0E0)
                            )
                            Text(
                                text = "${porcentaje.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // Contenido principal
            when (estado) {
                is HistorialEstado.Cargando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is HistorialEstado.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (estado as HistorialEstado.Error).mensaje,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    if (alarmas.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sin registros en este periodo",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val agrupadas = alarmas
                            .sortedByDescending { it.fechaHora }
                            .groupBy { it.fechaHora.substring(0, 10) }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            agrupadas.forEach { (fecha, alarmasDelDia) ->
                                item {
                                    Text(
                                        text = formatearFecha(fecha),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(
                                            top = 12.dp,
                                            bottom = 4.dp
                                        )
                                    )
                                }
                                items(alarmasDelDia) { alarma ->
                                    HistorialCard(alarma)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EstadisticaChip(valor: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun HistorialCard(alarma: AlarmaResponse) {
    val hora = alarma.fechaHora.substring(11, 16)

    val (colorEstado, icono, textoEstado) = when (alarma.estado) {
        "TOMADA"  -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Tomada")
        "OMITIDA" -> Triple(Color(0xFFF44336), Icons.Default.Cancel, "Omitida")
        else      -> Triple(Color(0xFF9E9E9E), Icons.Default.Alarm, "Pendiente")
    }

    val formaTexto = alarma.formaFarmaceutica
        ?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorEstado,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarma.medicinaNombre,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (formaTexto.isNotBlank()) {
                    Text(
                        text = formaTexto,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = hora,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = textoEstado,
                    color = colorEstado,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

fun formatearFecha(fecha: String): String {
    return try {
        val parsed = LocalDate.parse(fecha)
        val hoy = LocalDate.now()
        when (parsed) {
            hoy              -> "Hoy"
            hoy.minusDays(1) -> "Ayer"
            hoy.plusDays(1)  -> "Mañana"
            else -> parsed.format(
                DateTimeFormatter.ofPattern(
                    "EEEE dd 'de' MMMM",
                    Locale("es", "MX")
                )
            ).replaceFirstChar { it.uppercase() }
        }
    } catch (e: Exception) {
        fecha
    }
}