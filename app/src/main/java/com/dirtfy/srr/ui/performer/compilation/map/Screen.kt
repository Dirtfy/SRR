package com.dirtfy.srr.ui.performer.compilation.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    items: List<Item>,
    availableFeatures: List<String>
) {
    // State for selected features (Axes)
    var featureX by remember { mutableStateOf(availableFeatures.getOrNull(0) ?: "") }
    var featureY by remember { mutableStateOf(availableFeatures.getOrNull(1) ?: "") }

    // Local state to manage the visibility of the popup
    var selectedPoint by remember { mutableStateOf<Item?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Feature Selection Headers (Dropdowns) ---
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureDropdown(
                    label = "X Axis",
                    selectedFeature = featureX,
                    options = availableFeatures,
                    onFeatureSelected = { featureX = it },
                    modifier = Modifier.weight(1f)
                )

                FeatureDropdown(
                    label = "Y Axis",
                    selectedFeature = featureY,
                    options = availableFeatures,
                    onFeatureSelected = { featureY = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- Map Content Area ---
        Box(modifier = Modifier.weight(1f)) {
            if (featureX.isNotEmpty() && featureY.isNotEmpty()) {
                ScatterPlot(
                    items = items,
                    onPointClick = {
                        selectedPoint = it
                    }
                )

                // Info Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$featureX vs $featureY",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Please select features for both axes",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Popup when a point is selected
            selectedPoint?.let { item ->
                MapItemPopup(
                    item = item,
                    onDismiss = { selectedPoint = null },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureDropdown(
    label: String,
    selectedFeature: String,
    options: List<String>,
    onFeatureSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedFeature,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onFeatureSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun ScatterPlot(
    items: List<Item>,
    onPointClick: (Item) -> Unit
) {
    val pointColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .pointerInput(items) {
                detectTapGestures { offset ->
                    items.forEach { item ->
                        val xVal = item.primaryScore.toFloatOrNull() ?: 0f
                        val yVal = item.secondaryScore.toFloatOrNull() ?: 0f

                        val canvasX = ((xVal + 1f) / 2f) * size.width
                        val canvasY = size.height - (((yVal + 1f) / 2f) * size.height)

                        val distance = (Offset(canvasX, canvasY) - offset).getDistance()
                        if (distance < 50f) onPointClick(item)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        drawLine(axisColor, Offset(0f, centerY), Offset(width, centerY), strokeWidth = 1.dp.toPx())
        drawLine(axisColor, Offset(centerX, 0f), Offset(centerX, height), strokeWidth = 1.dp.toPx())

        items.forEach { item ->
            val scoreX = item.primaryScore.toFloatOrNull() ?: 0f
            val scoreY = item.secondaryScore.toFloatOrNull() ?: 0f

            val x = ((scoreX + 1f) / 2f) * width
            val y = height - (((scoreY + 1f) / 2f) * height)

            drawCircle(
                color = pointColor,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    val mockFeatures = listOf("Performance", "Stability", "UI Design", "Security")
    val mockItems = listOf(
        Item("High Performance", android.R.drawable.ic_menu_gallery, "0.8", "0.5"),
        Item("Low Stability", android.R.drawable.ic_menu_manage, "-0.7", "-0.3")
    )

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MapScreen(
                items = mockItems,
                availableFeatures = mockFeatures
            )
        }
    }
}