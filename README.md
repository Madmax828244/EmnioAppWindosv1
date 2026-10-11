# Emnio Windows PC (Android · Kotlin + Jetpack Compose)

Dashboard de hardware virtual → boot simulado → desktop Windows 10/11 com papel de parede do SO escolhido,
janelas, touchpad, teclado e gamepad. Conexão REAL opcional a um PC/nuvem via Microsoft Remote Desktop (rdp://).

## Compilar no TERMUX (ARM64, ~6 GB de RAM livre recomendados)
1. Copie o zip para o celular e extraia: `unzip EmnioWindowsPC.zip && cd EmnioWindowsPC`
2. `bash termux/setup.sh`   (uma vez: Java 17, Gradle, SDK ARM64)
3. `bash termux/build.sh`   (gera o APK e copia para Download/EmnioWindowsPC.apk)
4. Abra o APK em Download e instale.

Se der erro de `aapt2`: o aapt2 do SDK ARM64 precisa ser compatível com o AGP 8.5.x (build-tools 34.x).
Rode `ls ~/android-sdk/build-tools` e, se só houver versões muito diferentes, instale a 34.0.0 pelo sdkmanager.
Erro de memória (OOM): feche outros apps e reduza `-Xmx2g` em gradle.properties para `-Xmx1536m`.

## Alternativa sem sofrimento: compilar na nuvem (GitHub Actions)
Suba esta pasta num repositório GitHub → aba Actions → "Build APK" → baixe o artefato `EmnioWindowsPC-apk`.

## Android Studio (PC)
File > Open > pasta do projeto > Build > Build APK(s). Saída: app/build/outputs/apk/debug/app-debug.apk

## O que é simulado e o que é real
- Simulado: boot, desktop, janelas, navegador, "Roblox Launcher", vGPU/RAM/vCPU (só alimentam a interface).
- Real: medidor "UI FPS" (taxa real de renderização do app), bateria e relógio do celular, conexão rdp:// ao Remote Desktop.
- Os papéis de parede são imagens ORIGINAIS geradas para o app (estilo Win10/Win11), não as fotos oficiais da Microsoft.
  Para usar as oficiais, substitua `res/drawable-nodpi/wallpaper_win10.jpg` e `wallpaper_win11.jpg` (mesmo nome).
