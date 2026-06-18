package com.glucode.gautimes.components.sharing

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.glucode.gautimes.utils.DateUtils

class ShareState {
    var shareInfo by mutableStateOf<ShareInfo?>(null)
        private set

    fun share(info: ShareInfo) {
        shareInfo = info
    }

    fun onShared() {
        shareInfo = null
    }
}

@Composable
fun rememberShareState(): ShareState {
    return remember { ShareState() }
}

@Composable
fun ShareHandler(state: ShareState) {
    val context = LocalContext.current

    LaunchedEffect(state.shareInfo) {
        state.shareInfo?.let { info ->
            val departure = DateUtils.formatIsoTime(info.departureTime)
            val arrival = DateUtils.formatIsoTime(info.arrivalTime)
            
            val mapsLink = if (info.latitude != null && info.longitude != null) {
                "\n\nDirections to pickup: https://www.google.com/maps/dir/?api=1&destination=${info.latitude},${info.longitude}"
            } else ""

            val departurePrefix = if (info.timeUntilDeparture != null) {
                "My train is departing in ${info.timeUntilDeparture} minutes"
            } else {
                "I'm taking a train"
            }

            val shareText = "$departurePrefix from ${info.from} to ${info.to}. Departure Time: $departure, Arrival Time: $arrival$mapsLink"
            
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
            state.onShared()
        }
    }
}
