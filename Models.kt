package com.emnio.windowspc.model

enum class WinEdition(val label: String, val isWin11: Boolean, val build: String) {
    W10_HOME("Windows 10 Home (x64)", false, "19045"),
    W10_PRO("Windows 10 Pro (x64)", false, "19045"),
    W10_LTSC("Windows 10 Enterprise LTSC (x64)", false, "19044"),
    W11_HOME("Windows 11 Home (x64)", true, "26100"),
    W11_PRO("Windows 11 Pro (x64)", true, "26100"),
    W11_ENT("Windows 11 Enterprise (x64)", true, "26100")
}

enum class VGpu(val label: String, val deviceName: String) {
    INTEGRATED("Integrated HD Graphics (Modo Econômico)", "Intel(R) HD Graphics"),
    ADRENO("Adreno Turbo Vulkan Direct3D Engine", "Adreno Turbo Vulkan D3D Engine"),
    RTX3060("NVIDIA GeForce RTX 3060 Cloud Adapter (Simulated Passthrough)", "NVIDIA GeForce RTX 3060 Cloud Adapter"),
    RX6700("AMD Radeon RX 6700 XT Virtual GPU", "AMD Radeon RX 6700 XT Virtual GPU")
}

enum class VResolution(val w: Int, val h: Int, val label: String) {
    HD(1280, 720, "1280x720"),
    FHD(1920, 1080, "1920x1080 (Full HD)"),
    QHD(2560, 1440, "2560x1440 (2K)")
}

data class HardwareConfig(
    val os: WinEdition = WinEdition.W11_PRO,
    val ramGb: Int = 16,
    val vcpu: Int = 8,
    val gpu: VGpu = VGpu.RTX3060,
    val storageGb: Int = 256,
    val fps: Int = 120,
    val resolution: VResolution = VResolution.FHD,
    /** Endereço de um PC/nuvem REAL (ex.: 192.168.0.10 ou meupc.exemplo.com:3389). Opcional. */
    val remoteHost: String = ""
)

object Options {
    val ram = listOf(4, 8, 12, 16, 24, 32)
    val vcpu = listOf(2, 4, 8, 16)
    val storage = listOf(64, 128, 256, 512)
    val fps = listOf(60, 120, 144, 240)
}
