[English](en/unlocking-qf005.md) · **Português**

# Desbloqueio da central ROCO K706 / QF005

Passo a passo do que foi feito no Android da central para instalar e controlar o RotivMedia: ligar o ADB, acessar pela rede, tornar o launcher padrão e manter tudo funcionando depois de reiniciar.

**Não é root por exploit e não mexe no firmware.** Tudo foi feito com menus de fábrica, `settings`, `setprop` e permissões do Android. O `/system`, o bootloader, o MCU e o barramento CAN do carro não foram tocados. Cada passo pode ser desfeito (veja [Desfazer](#desfazer-tudo)).

> Faça por sua conta e risco. Antes de cada mudança, salve um backup (`adb shell settings list global|secure|system` e `adb shell getprop`).

## Resumo

| Etapa | O que resolve |
| --- | --- |
| 1. Menu de engenharia → Adb shell | Primeiro acesso de linha de comando, sem cabo USB de dados |
| 2. ADB pela rede | Conectar o PC à central por Wi-Fi |
| 3. Tailscale | Acessar a central de qualquer lugar por uma VPN privada |
| 4. Sobreviver ao reboot | A central desliga o ADB e mata apps a cada boot |
| 5. RotivMedia como launcher padrão | A QF guarda o launcher num serviço próprio |
| 6. Janelas livres | Maps e YouTube Music encaixados na tela inicial |
| 7. Limpeza e idioma | Remover o "Android 13" falso, teclado com anúncios e trocar para pt-BR |

## Antes de começar: o que é esta central

- Vendida como "Android 13" (às vezes "Android 16"), mas é **Android 10** (API 29). O número falso vem do app `mark.fakedevicetest2`.
- Plataforma Unisoc sp9863a / uis8581a, firmware QF005 da Qianfeng (Shenzhen), tela 1280×800.
- **Não existe** "Depuração sem fio" nas Opções do desenvolvedor.
- Relatos no XDA dizem que ligar o ADB pode derrubar o CarPlay / Android Auto (`com.zjinnova.zlink`). Neste caso não derrubou, mas teste o seu.

### Códigos do campo de senha

No app de ajustes da central há um campo de senha (do tópico "QF005 (ROCO K706)" do XDA):

| Código | Abre |
| --- | --- |
| 8888 | Ajustes avançados |
| 114477 | Modo programador |
| 4455 | Engenharia |
| **225588** | **NÃO USAR: apaga os dados da central** |
| **774411** | **NÃO USAR** |

## 1. Primeiro acesso: menu de engenharia

1. Ajustes da central → campo de senha → **4455**.
2. Entre em **Debug** → **Adb shell**. Abre um terminal que roda como o usuário `system`.
3. Nesse terminal, ligue o ADB pela rede na porta 5555:
   ```sh
   setprop service.adb.tcp.port 5555
   setprop persist.adb.tcp.port 5555
   setprop ctl.restart adbd
   ```

## 2. ADB pela rede

Com a central e o PC na mesma rede:

```bash
adb connect IP_DA_CENTRAL:5555
adb shell id
```

O resultado mostra o que torna esta central fácil de mexer, e também perigosa:

- o `adbd` entra como **root** (`uid=0`, contexto `su`);
- **SELinux permissivo**;
- `ro.adb.secure=0`: **aceita qualquer computador sem confirmação na tela**.

Ou seja, com o ADB ligado, qualquer aparelho na mesma rede vira root na central. Só ligue em redes suas (por exemplo, o roteador do seu celular) e nunca exponha a porta na internet.

Também foi verificado:
- A chave de plataforma é **própria da QF**, não a testkey pública do AOSP. Não dá para assinar um app como sistema.
- Verified boot verde e flash travado. Alterar o `/system` não é viável, e nem foi preciso.

## 3. Tailscale (acesso de qualquer lugar)

Para atualizar a central de casa enquanto ela está no carro:

1. Instale o **APK oficial do Tailscale** na central pelo ADB. Não precisa da Play Store nem de conta Google.
2. Entre na sua rede Tailscale com uma **auth key** gerada no painel do Tailscale. **Digite você mesmo na central**; nunca deixe a chave em arquivos ou no código.
3. A central aparece na sua rede com um IP `100.x.y.z`. Do PC (também no Tailscale):
   ```bash
   adb connect IP_TAILSCALE_DA_CENTRAL:PORTA
   ```

Assim a porta do ADB só fica acessível dentro da sua VPN privada.

## 4. Fazer tudo sobreviver ao reboot

A central "desfaz" as mudanças a cada boot:

- desliga `adb_enabled`;
- define `service.adb.tcp.port=9876` (a 5555 só volta com o `setprop` manual);
- o gerenciador de energia da Unisoc mata o Tailscale e bloqueia apps de iniciarem no boot.

O que foi feito:

**a) Bateria e início automático:** em Configurações → Bateria, marque o **Tailscale** e o **RotivMedia** como "não otimizar" e com **início automático**. Isso precisa ser feito na tela, pela interface da central. No arquivo `appPowerSaveConfig.xml` do sistema fica `optimize=0` e `autolaunch=2` para os dois.

**b) Lista de exceções do modo soneca:**
```bash
adb shell dumpsys deviceidle whitelist +com.tailscale.ipn
adb shell dumpsys deviceidle whitelist +com.rotivsecurity.launcher
```

**c) VPN sempre ligada:**
```bash
adb shell settings put secure always_on_vpn_app com.tailscale.ipn
adb shell settings put secure always_on_vpn_lockdown 0
```
(`lockdown 0` mantém a internet funcionando mesmo se o Tailscale cair.)

**d) O RotivMedia religa o ADB e o Tailscale no boot** (`RemoteAdb.kt` + `BootReceiver`). Para isso ele precisa desta permissão, dada uma vez:
```bash
adb shell pm grant com.rotivsecurity.launcher android.permission.WRITE_SECURE_SETTINGS
```
A cada boot ele faz o equivalente a `settings put global adb_enabled 1` e envia `com.tailscale.ipn.CONNECT_VPN` para o `com.tailscale.ipn/.IPNReceiver`. A chave "ADB remoto" nos Ajustes do RotivMedia liga e desliga isso.

Resultado testado reiniciando a central sem tocar em nada: Tailscale de volta em ~46 s e ADB em ~64 s, na porta **9876**.

## 5. RotivMedia como launcher padrão

O "App de início" do Android não basta: a QF guarda o launcher no seu próprio `config_service` e numa propriedade.

```bash
# ver o atual
adb shell service call config_service 4 s16 launcher_default_app

# definir o RotivMedia
adb shell service call config_service 3 s16 launcher_default_app s16 com.rotivsecurity.launcher
adb shell setprop persist.sys.qf.launcher com.rotivsecurity.launcher
adb shell cmd package set-home-activity com.rotivsecurity.launcher/.HomeActivity
```

Os "temas" da loja da QF são launchers separados (`com.android.launcher.datai_ui1` … `datai_ui10`). Anote o seu antes de trocar, para poder voltar.

Acesso à música (para o player ler o que está tocando):
```bash
adb shell cmd notification allow_listener com.rotivsecurity.launcher/com.rotivsecurity.launcher.MediaListener
```

## 6. Janelas livres (Maps e YouTube Music encaixados)

O launcher de fábrica mostra o Maps "embutido" usando uma janela livre (`sys.qf.launcher.map.freeform=true`). Para o RotivMedia fazer o mesmo:

```bash
adb shell settings put global enable_freeform_support 1
adb shell settings put global force_resizable_activities 1
```

Reinicie a central depois. Detalhes de como as janelas são posicionadas: [central-qf005.md](central-qf005.md#janelas-encaixadas-maps-youtube-music-youtube-retroarch).

## 7. Limpeza e idioma

**Desativados** (não desinstalados; voltam com `pm enable`):
```bash
adb shell pm disable-user --user 0 mark.fakedevicetest2           # mostra "Android 13" falso
adb shell pm disable-user --user 0 com.emoji.keyboard.touchpal.go # teclado com anúncios
```
O teclado padrão passa a ser o do Android (LatinIME).

**Português do Brasil** (a central vem em pt-PT):
```bash
adb shell setprop persist.sys.locale pt-BR
adb shell settings put system system_locales pt-BR
```
e reinicie. Também funciona pelo app MoreLocale, dando a ele a permissão `CHANGE_CONFIGURATION` com `pm grant`.

## O que foi tentado e desfeito

- **Cor/imagem da barra de cima** pela pasta `/data/QF/protect_dir/project/picture/statusbarbg/` e por `persist.sys.qf.statusbar.color`: só funciona em modelos de tela em pé. Foi removido. Neste modelo a barra é transparente, então o RotivMedia desenha a marca atrás dela.
- **Overlay (RRO) no SystemUI:** o SystemUI da QF não declara `overlayable`, e não valia o risco de travar a interface. Não foi aplicado.

## O que nunca foi feito

- Usar os códigos 225588 ou 774411.
- Mexer no MCU, no CAN ou enviar comandos ao carro. O OBD2 do RotivMedia é só leitura.
- Gravar no `/system`, desbloquear o bootloader ou instalar root por exploit.
- Colocar senhas, auth keys ou IPs no código.

## Desfazer tudo

```bash
# launcher de fábrica (troque pelo tema que você usava)
adb shell service call config_service 3 s16 launcher_default_app s16 com.android.launcher.datai_ui10
adb shell setprop persist.sys.qf.launcher com.android.launcher.datai_ui10

# apps desativados
adb shell pm enable com.emoji.keyboard.touchpal.go
adb shell pm enable mark.fakedevicetest2

# VPN sempre ligada e exceções de energia
adb shell settings delete secure always_on_vpn_app
adb shell dumpsys deviceidle whitelist -com.tailscale.ipn
adb shell dumpsys deviceidle whitelist -com.rotivsecurity.launcher

# janelas livres
adb shell settings put global enable_freeform_support 0
adb shell settings put global force_resizable_activities 0

# idioma
adb shell setprop persist.sys.locale pt-PT
adb shell settings put system system_locales pt-PT

# ADB pela rede
adb shell setprop persist.adb.tcp.port ""
adb uninstall com.rotivsecurity.launcher
```

Por último, desligue a "Depuração USB" nas Opções do desenvolvedor e reinicie.
