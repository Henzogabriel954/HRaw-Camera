# HRaw Camera 📷

Fork otimizado do **Open Camera (v1.56.2)** voltado para fotografia **RAW (.DNG)** de alta fidelidade em aparelhos Samsung (especialmente **Galaxy A53 5G - SM-A536E**), com controle térmico avançado para eventos longos e pouca luz (cultos, palcos e apresentações).

- **ID do Aplicativo:** `online.hcraft.HRaw`
- **Nome de exibição:** `HRaw`
- **Pasta padrão de salvamento:** `DCIM/HRaw`

---

## Principais Recursos

### 1. Painel LCD Estilo DSLR & Modo Sem Prévia (Preview Off)
- **Tela 100% Preto AMOLED:** Desativa a renderização contínua do viewfinder com 1 toque, eliminando o consumo de energia da GPU e o aquecimento do display.
- **Painel Central DSLR:** Exibe todas as informações vitais de exposição em formato de LCD profissional:
  - Tempo de obturador (ex.: `1/80s`, `1/50s`)
  - Sensibilidade ISO (ex.: `ISO 800`)
  - Abertura da lente (ex.: `f/1.8`)
  - Modo ativo (`[A]` Automático / `[M]` Manual) e formato (`RAW 16MP`, `RAW+JPG`)
  - Armazenamento livre em GB e **estimativa de fotos RAW restantes**
  - Foco e Balanço de Branco (`AF • AWB` ou `MF 1.25m`)
  - Modo ECO e telemetria de bateria (`BAT % • °C`)
- **Fotômetro Adaptativo em Tempo Real:** Medição isolada de exposição automática em segundo plano sem religar a prévia em vídeo, mantendo o sensor frio e os dados de ISO e velocidade sempre atualizados na tela.

### 2. Modo Manual Pro (`M`) para Cultos e Ambientes Escuros
- Alternância rápida entre **Auto `[A]`** e **Manual `[M]`** com 1 toque, herdando os valores correntes de medição.
- **Predefinições Rápidas de Iluminação:**
  - `Palco / Stage`: ISO 800 • 1/80s
  - `Escuro / Dark`: ISO 1600 • 1/50s
  - `Estático / Static`: ISO 400 • 1/15s
- Controles finos de `-` e `+` por paradas fotográficas (stops) de 1/3 EV para ISO e obturador.
- Alternância instantânea de foco `AF` / `MF`.
- Brilho de tela discreto para não iluminar o rosto do fotógrafo durante cultos.

### 3. Otimizações Térmicas e de Prévia
- **Resolução de Viewfinder Econômica:** Prévia reduzida para `480p` (`640x480`) ou `240p` para não sobrecarregar o processador Exynos.
- **Captura sem perda:** As fotos RAW (`RAW_SENSOR`) e JPEG continuam sendo salvas na resolução total do sensor (`4624x3468`, 16 MP binned).
- **Limite de Quadros por Segundo (FPS):** Limite configurável (`12 FPS`, `15 FPS`, etc.) na prévia para manter o aparelho refrigerado.

---

## Compilação e Instalação

### Requisitos
- Android Studio / Android SDK (API 34)
- JDK 17 ou 21

```bash
# Compilar APK de debug e rodar testes unitários
./gradlew testDebugUnitTest assembleDebug

# Instalar no dispositivo conectado via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Licença

Este projeto é derivado do **Open Camera** desenvolvido por Mark Harman e distribuído sob a licença **GNU General Public License v3.0 (GPLv3)**. Consulte o arquivo [gpl-3.0.txt](gpl-3.0.txt) para mais detalhes.
