# Notas técnicas: central ROCO K706 / QF005

O que foi descoberto ao desenvolver o RotivMedia nesta central. Vale para o firmware QF005 da Qianfeng (Shenzhen); outros firmwares podem ser diferentes.

## Aparelho

- Android 10 real (API 29). A tela "Sobre" diz Android 13/16, mas é falso (vem de um app `mark.fakedevicetest2`, que pode ser desativado).
- Plataforma Unisoc sp9863a / uis8581a, tela 1280×800 a 160 dpi, deitada.
- Apps de fábrica: `com.qf.*` (ajustes do carro, Bluetooth, câmera de ré, temas…), `com.zjinnova.zlink` (CarPlay/Android Auto) e `com.sprd.engineermode`.
- A chave de plataforma é própria da QF (não é a testkey do AOSP): não dá para assinar um app como sistema. Verified boot verde e flash travado.

## Códigos do campo de senha

Conhecidos no tópico "QF005 (ROCO K706)" do XDA:

| Código | Abre |
| --- | --- |
| 8888 | Ajustes avançados |
| 114477 | Modo programador |
| 4455 | Engenharia (tem um "Adb shell" em Debug) |
| **225588** | **NÃO USAR: apaga os dados** |
| **774411** | **NÃO USAR** |

## ADB

- Não há "depuração sem fio" no menu. A cada boot a central desliga `adb_enabled` e define `service.adb.tcp.port=9876`.
- O `adbd` roda como **root**, SELinux permissivo e `ro.adb.secure=0`: aceita conexões sem confirmação na tela. Só use em rede de confiança.
- Primeira ativação: menu 4455 → Debug → Adb shell (roda como `system`) → `setprop service.adb.tcp.port 5555` e `setprop ctl.restart adbd`.
- O RotivMedia (`RemoteAdb.kt`) religa `adb_enabled` no boot (precisa de `WRITE_SECURE_SETTINGS`) e manda `com.tailscale.ipn.CONNECT_VPN` para o Tailscale conectar.
- O gerenciador de energia da Unisoc mata apps em segundo plano: em Configurações → Bateria, marque o RotivMedia e o Tailscale como "não otimizar" e com início automático.

## Launcher padrão

A QF guarda o launcher no `config_service`, além de `persist.sys.qf.launcher`:

```bash
# ler
adb shell service call config_service 4 s16 launcher_default_app
# gravar
adb shell service call config_service 3 s16 launcher_default_app s16 com.rotivsecurity.launcher
adb shell setprop persist.sys.qf.launcher com.rotivsecurity.launcher
```

Para voltar ao de fábrica, grave o pacote do tema original (por exemplo `com.android.launcher.datai_ui10`). Os "temas" da loja da QF são launchers Launcher3 separados.

## Janelas encaixadas (Maps, YouTube Music, YouTube, RetroArch)

O launcher de fábrica mostra o Maps "embutido" usando uma janela livre (`sys.qf.launcher.map.freeform=true`). O RotivMedia faz o mesmo:

1. Mede o quadro na tela (`onGloballyPositioned`) e abre o app com `ActivityOptions.setLaunchBounds(rect)` e `setLaunchWindowingMode(5)` (freeform, por reflexão).
2. Depois de ~1 s confere no `am stack list`:
   - se a janela ficou em tela cheia (`mWindowingMode=fullscreen`), faz `am stack remove` e abre de novo;
   - se o tamanho não bate, corrige com `am task resize`.
3. Se o quadro muda de tamanho com a janela já aberta, só redimensiona. **Reabrir rouba o foco**: no Android 10 só a janela com foco fica "resumed", e um app que ainda está na tela de carregamento (como o RetroArch) trava.
4. Os comandos `am` são enviados pelo próprio protocolo ADB para `127.0.0.1` (`LocalShell.kt`), porque o app não tem permissão de sistema.

## Barra de cima (SystemUI)

- SystemUI da QF (`SystemUIWithLegacyRecents`), barra de 72 px, sem `overlayable` declarado (RRO não serve).
- A pasta `/data/QF/protect_dir/project/picture/statusbarbg/` e `persist.sys.qf.statusbar.color` só valem para modelos de tela em pé. Neste modelo a barra é **transparente**.
- Por isso a tela inicial desenha atrás dela (`setDecorFitsSystemWindows(false)`) a imagem `barra_central.png`.

## Idioma

O firmware vem em pt-PT. Trocar `persist.sys.locale` e `system_locales` para `pt-BR` funciona.
