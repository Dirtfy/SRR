package com.dirtfy.srr.ui.performer.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SRRBaseScreen(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    navigationItems: List<Item>,
    currentRoute: String,
    onTabClick: (Item) -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (navigationItems.isNotEmpty()) {
                NavigationBar {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { onTabClick(item) },
                            label = { Text(item.label) },
                            icon = { Icon(item.icon, contentDescription = item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content(Modifier)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SRRBaseScreenPreview() {
    val mockTabs = listOf(
        Item("my", "My", Icons.Default.Person),
        Item("result", "Result", Icons.Default.List)
    )

    SRRTheme {
        SRRBaseScreen(
            title = "My Dashboard",
            showBackButton = true,
            onBackClick = {},
            navigationItems = mockTabs,
            currentRoute = "my",
            onTabClick = {}
        ) { modifier ->
            Box(modifier = modifier.padding(16.dp)) {
                Text("Content Area")
            }
        }
    }
}