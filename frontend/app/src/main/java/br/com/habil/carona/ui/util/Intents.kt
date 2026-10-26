package br.com.habil.carona.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

fun openRouteInMaps(context: Context, origin: String, destination: String): Boolean {
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(origin)}" +
            "&destination=${Uri.encode(destination)}&travelmode=driving",
    )
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
