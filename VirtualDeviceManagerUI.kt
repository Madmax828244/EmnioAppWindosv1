package com.emnio.windowspc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emnio.windowspc.model.HardwareConfig

/** Conteúdo da janela "Gerenciador de Dispositivos" com as specs escolhidas na tela 1. */
@Composable
fun VirtualDeviceManagerUI(config: HardwareConfig) {
    val categories = remember(config) {
        listOf(
            "Computador" to listOf("ACPI x64-based PC (EMNIO-PC)"),
            "Memória (RAM)" to listOf("${config.ramGb} GB DDR4 virtual · ${config.ramGb * 1024} MB"),
            "Processadores" to List(config.vcpu) { "Emnio Virtual CPU @ 3.60GHz" },
            "Adaptadores de vídeo" to listOf(config.gpu.deviceName, "Emnio Virtual Display Driver"),
            "Monitores" to listOf("Monitor genérico PnP ${config.resolution.w}x${config.resolution.h} @ ${config.fps} Hz"),
            "Unidades de disco" to listOf("Emnio Virtual SSD ${config.storageGb} GB (VHD dinâmico)"),
            "Adaptadores de rede" to listOf("Emnio Virtio Ethernet Adapter"),
            "Dispositivos de interface humana" to listOf("Mouse virtual (touchpad)", "Teclado virtual HID", "Gamepad virtual XInput"),
            "Controladores de som, vídeo e jogos" to listOf("Emnio Virtual Audio Device")
        )
    }
    val expanded = remember { mutableStateListOf("Processadores", "Adaptadores de vídeo", "Memória (RAM)") }

    LazyColumn(Modifier.fillMaxSize().background(Color.White).padding(6.dp)) {
        item {
            Text("EMNIO-PC · ${config.os.label}", color = Color(0xFF111111), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        categories.forEach { (name, devices) ->
            val open = name in expanded
            item(key = name) {
                Text(
                    (if (open) "▾ " else "▸ ") + name,
                    color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth().clickable { if (open) expanded.remove(name) else expanded.add(name) }
                        .padding(vertical = 3.dp)
                )
            }
            if (open) items(devices.size) { i ->
                Text("▪ ${devices[i]}", color = Color(0xFF333333), fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 1.dp, bottom = 1.dp))
            }
        }
    }
}
