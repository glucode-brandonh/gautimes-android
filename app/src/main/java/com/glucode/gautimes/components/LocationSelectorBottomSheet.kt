package com.glucode.gautimes.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.screens.home.Location
import com.glucode.gautimes.ui.theme.GautimesTheme

data class LocationSelectorBottomSheetData(val locations: List<Location> = emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectorBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    data: LocationSelectorBottomSheetData = LocationSelectorBottomSheetData(),
    onDismissRequest: () -> Unit,
    onLocationSelected: (Location) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        LocationSelectorContent(
            locations = data.locations,
            onLocationSelected = onLocationSelected
        )
    }
}

@Composable
fun LocationSelectorContent(
    modifier: Modifier = Modifier,
    locations: List<Location>,
    onLocationSelected: (Location) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                text = "Select Location",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.size(16.dp))
        }
        items(locations) { location ->
            LocationSelectionCard(
                location = location,
                onClick = { onLocationSelected(location) }
            )
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
fun LocationSelectionCard(
    modifier: Modifier = Modifier,
    location: Location,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !location.disabled) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = location.name,
                color = if (location.disabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
            )
            if (location.selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview
@Composable
fun LocationSelectionCardPreview() {
    GautimesTheme {
        LocationSelectionCard(location = Location(name = "Sandton", selected = true))
    }
}

@Preview(showBackground = true)
@Composable
fun LocationSelectorBottomSheetPreview() {
    GautimesTheme {
        LocationSelectorContent(
            locations = list,
            onLocationSelected = {}
        )
    }
}

private val list = listOf(
    Location(name = "Sandton", selected = true),
    Location(name = "Rosebank"),
    Location(name = "Marlboro", disabled = true),
    Location(name = "Pretoria")
)
