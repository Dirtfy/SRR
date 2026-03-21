package com.dirtfy.srr.view.compilation.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    items: List<Item>,
    availableFeatures: List<String>
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var featureX by remember { mutableStateOf(availableFeatures.getOrNull(0) ?: "") }
    var featureY by remember { mutableStateOf(availableFeatures.getOrNull(1) ?: "") }
    var selectedPoint by remember { mutableStateOf<Item?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Select Map Features", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()

                Text("X Axis Score", Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                availableFeatures.forEach { feature ->
                    NavigationDrawerItem(
                        label = { Text(feature) },
                        selected = feature == featureX,
                        onClick = { featureX = feature }
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("Y Axis Score", Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                availableFeatures.forEach { feature ->
                    NavigationDrawerItem(
                        label = { Text(feature) },
                        selected = feature == featureY,
                        onClick = { featureY = feature }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Feature Correlation Map") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (featureX.isNotEmpty() && featureY.isNotEmpty()) {
                    ScatterPlot(
                        items = items,
                        onPointClick = { selectedPoint = it }
                    )

                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Center is (0,0) | Range: -1 to 1",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                selectedPoint?.let { item ->
                    MapItemPopup(item = item, onDismiss = { selectedPoint = null })
                }
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
                        val xVal = item.primaryScore.toFloat()
                        val yVal = item.secondaryScore.toFloat()

                        // Map -1..1 to 0..size
                        val canvasX = ((xVal + 1f) / 2f) * size.width
                        val canvasY = size.height - (((yVal + 1f) / 2f) * size.height)

                        val distance = (Offset(canvasX, canvasY) - offset).getDistance()
                        if (distance < 40f) onPointClick(item)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        // Draw X-Axis (Horizontal line through center)
        drawLine(axisColor, Offset(0f, centerY), Offset(width, centerY), strokeWidth = 1.dp.toPx())
        // Draw Y-Axis (Vertical line through center)
        drawLine(axisColor, Offset(centerX, 0f), Offset(centerX, height), strokeWidth = 1.dp.toPx())

        // Draw scatter dots
        items.forEach { item ->
            val scoreX = item.primaryScore.toFloat()
            val scoreY = item.secondaryScore.toFloat()

            // Normalize -1..1 to 0..1 then multiply by size
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
    val mockFeatures = listOf("Performance", "Stability", "UI Design")

    // Removed 'id' parameter to match the Item data class definition
    val mockItems = listOf(
        Item(
            title = "High Performance",
            imageRes = android.R.drawable.ic_menu_gallery,
            primaryScore = "0.8",
            secondaryScore = "0.5"
        ),
        Item(
            title = "Low Stability",
            imageRes = android.R.drawable.ic_menu_manage,
            primaryScore = "-0.7",
            secondaryScore = "-0.3"
        ),
        Item(
            title = "Balanced",
            imageRes = android.R.drawable.ic_menu_compass,
            primaryScore = "0.0",
            secondaryScore = "0.0"
        ),
        Item(
            title = "Extreme Y",
            imageRes = android.R.drawable.ic_menu_agenda,
            primaryScore = "-0.2",
            secondaryScore = "0.9"
        )
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