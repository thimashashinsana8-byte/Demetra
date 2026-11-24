package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun QrPinHandoverDialog(
    title: String,
    subtitle: String,
    expectedPin: String,
    qrPayload: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SoftWhite,
            border = BorderStroke(1.dp, CreamCardBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SageTint
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Verified Handover",
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestGreen
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = DarkText,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )

                // Deterministic QR code visual
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = WarmCream,
                    border = BorderStroke(1.dp, CreamCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QrPatternCanvas(
                            payload = qrPayload,
                            modifier = Modifier.size(124.dp)
                        )
                        Text(
                            text = "Handover PIN: $expectedPin",
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            enteredPin = input
                            pinError = null
                        }
                    },
                    label = { Text("Enter 4-digit handover PIN") },
                    placeholder = { Text(expectedPin) },
                    singleLine = true,
                    isError = pinError != null,
                    supportingText = pinError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("handover_pin_input")
                )

                Button(
                    onClick = {
                        if (enteredPin == expectedPin) {
                            onVerified()
                        } else {
                            pinError = "PIN does not match ($expectedPin for prototype demo)"
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_pin_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify PIN & Confirm")
                }

                OutlinedButton(
                    onClick = onVerified,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ForestGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("simulate_qr_verify_button")
                ) {
                    Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate QR Scan Match", color = ForestGreen)
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MutedText)
                }
            }
        }
    }
}

@Composable
private fun QrPatternCanvas(payload: String, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .background(SoftWhite)
            .drawWithCache {
                val gridSize = 11
                val cell = size.width / gridSize
                val cellSize = Size(cell - 2f, cell - 2f)
                val seed = abs(payload.hashCode())
                val cellOffsets = buildList {
                    for (row in 0 until gridSize) {
                        for (col in 0 until gridSize) {
                            val isCornerFinder = (row < 3 && col < 3) ||
                                (row < 3 && col >= gridSize - 3) ||
                                (row >= gridSize - 3 && col < 3)
                            if (isCornerFinder || ((seed + row * 31 + col * 17) % 3 == 0)) {
                                add(Offset(col * cell + 1f, row * cell + 1f))
                            }
                        }
                    }
                }
                onDrawBehind {
                    cellOffsets.forEach { topLeft ->
                        drawRect(color = DarkText, topLeft = topLeft, size = cellSize)
                    }
                }
            }
    )
}
