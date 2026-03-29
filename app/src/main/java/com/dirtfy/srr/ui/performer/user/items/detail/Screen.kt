package com.dirtfy.srr.ui.performer.user.items.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dirtfy.srr.ui.performer.base.theme.SRRTheme



@Composable
fun ItemDetailScreen(
    title: String,
    onBackClick: () -> Unit // Kept for logic, though TopBar is now in BaseFragment
) {
    val features = listOf(
        Item(1, "High Resolution Support", true),
        Item(2, "Cloud Backup", true),
        Item(3, "Offline Access", false),
        Item(4, "Ad-Free Experience", true),
        Item(5, "Premium Filters", false)
    )

    // REMOVED: Scaffold and TopAppBar.
    // This content now sits inside the BaseFragment's Scaffold via MineMainScreen.
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Big Header Section
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title, // Using the passed title as the headline
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Image(
                    painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }

        // 2. List Header
        item {
            Text(
                text = "Capabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 3. Feature List with status marks
        items(features) { feature ->
            FeatureStatusRow(feature = feature)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun FeatureStatusRow(feature: Item) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = feature.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (feature.isAvailable) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Available",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Not Available",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ItemDetailScreenPreview() {
    SRRTheme {
        Surface {
            ItemDetailScreen(
                title = "Premium Subscription",
                onBackClick = {}
            )
        }
    }
}