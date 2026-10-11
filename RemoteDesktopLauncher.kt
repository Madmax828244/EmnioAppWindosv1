package com.emnio.windowspc.remote

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Conexão REAL: entrega o endereço ao app Microsoft Remote Desktop / Windows App via rdp://.
 * O PC remoto (físico ou na nuvem) é quem fornece a GPU e os FPS.
 */
object RemoteDesktopLauncher {
    private const val RD_PACKAGE = "com.microsoft.rdc.androidx"

    fun open(context: Context, host: String): Boolean {
        if (host.isBlank()) return false
        val uri = Uri.parse("rdp://full%20address=s:${Uri.encode(host.trim(), ":.")}&audiomode=i:0")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent); true
        } catch (e: ActivityNotFoundException) {
            val store = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$RD_PACKAGE"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { context.startActivity(store) } catch (_: ActivityNotFoundException) {}
            false
        }
    }
}
