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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.screens.home.LocationTarget
import com.glucode.gautimes.ui.theme.GautimesTheme


data class LocationSelectorBottomSheetData(
    val locations: List<String> = emptyList(),
    val disabledLocation: String = "",
    val selectedLocation: String = "",
    val locationTarget: LocationTarget = LocationTarget.FROM
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectorBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    data: LocationSelectorBottomSheetData = LocationSelectorBottomSheetData(),
    onDismissRequest: (String, LocationTarget) -> Unit,
) {
    var selectedLocation by remember { mutableStateOf(data.selectedLocation) }
    ModalBottomSheet(
        onDismissRequest = {
            onDismissRequest.invoke(selectedLocation, data.locationTarget)
        },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        LocationSelectorContent(
            data = data,
            selectedLocation = selectedLocation,
            onDismissRequest = onDismissRequest,
            onSelectionChange = { location ->
                selectedLocation = location
            }
        )
    }
}

@Composable
fun LocationSelectorContent(
    modifier: Modifier = Modifier,
    selectedLocation: String = "",
    onSelectionChange: (String) -> Unit = {},
    data: LocationSelectorBottomSheetData,
    onDismissRequest: (String, LocationTarget) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Select Location",
                    style = MaterialTheme.typography.titleLarge,
                )
                Button(onClick = {
                    onDismissRequest.invoke(selectedLocation, data.locationTarget)
                }) {
                    Text("Done")
                }
            }
            Spacer(Modifier.size(16.dp))
        }
        items(data.locations) { location ->
            LocationSelectionCard(
                selected = location == selectedLocation,
                name = location,
                disabled = location == data.disabledLocation,
                onClick = {
                    onSelectionChange(location)
                }
            )
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
fun LocationSelectionCard(
    modifier: Modifier = Modifier,
    name: String = "",
    selected: Boolean = false,
    disabled: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
            )
            if (selected) {
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
        LocationSelectionCard(name = "Sandton")
    }
}

@Preview(showBackground = true)
@Composable
fun LocationSelectorBottomSheetPreview() {
    GautimesTheme {
        LocationSelectorContent(
            data = LocationSelectorBottomSheetData(locations = listOf("Sandton", "Rosebank")),
            onDismissRequest = { test1, test2 ->

            }
        )
    }
}
