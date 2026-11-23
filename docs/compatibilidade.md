[English](en/compatibility.md) · **Português**

# Centrais compatíveis

O RotivMedia foi testado em **uma** central. Os outros modelos abaixo vêm de relatos de donos em fóruns públicos (fontes no final). Eles rodam o mesmo firmware da Qianfeng (QF) ou um parente próximo, então **provavelmente** são compatíveis, mas ninguém testou o RotivMedia neles ainda. Se você testar, abra uma issue com o seu modelo e o que funcionou.

## O que o RotivMedia precisa

| Requisito | Por quê |
| --- | --- |
| Android 10 ou mais novo | O app é feito para Android 10 (API 29) em diante. |
| Tela deitada, de uns 1280×720 a 1280×800 | O layout escala pela altura e foi desenhado para tela larga. Telas em pé ("verticais", estilo Tesla) não são suportadas. |
| Janelas livres (`enable_freeform_support`) | Para encaixar o Google Maps e o YouTube Music na tela inicial. Sem elas, aparecem o mapa desenhado e o player próprio. |
| ADB acessível na própria central (porta 9876 ou 5555) | O encaixe redimensiona as janelas com comandos `am` enviados pelo ADB para `127.0.0.1`. |
| Firmware QF (para as partes específicas da QF) | Launcher padrão pelo `config_service`, desenho atrás da barra, comportamento no boot. Veja o [guia de desbloqueio](desbloqueio-qf005.md). |

## Níveis de compatibilidade

### ✅ Testada

| Central | Firmware | Chip | Tela | Observações |
| --- | --- | --- | --- | --- |
| **ROCO K706** (a unidade testada foi vendida no Brasil como H-Tech HT-9864CA) | QF005 | Unisoc UIS8581A (sp9863a) | 1280×800 deitada | Android 10 real (diz ser Android 13). Tudo do README funciona. |

### 🟡 Provavelmente compatíveis: mesmo firmware QF005 no UIS8581A (sem teste)

Donos no XDA relatam estas centrais com firmware **QF005** e MCU **QF30** ou **qf05**:

| Central / vendedor | O que foi relatado |
| --- | --- |
| **A-Sure série Z84X3** (por exemplo, a versão para Skoda Fabia II) | QF005, MCU QF30, UIS8581A. O dono considera um "fork" da Hizpo S-Series. |
| **A-Sure K709** | QF005, MCU QF30, 4 GB / 64 GB. |
| **Hizpo K706** e **Hizpo S-Series** | QF005, MCU qf05. Variantes com chips de DSP/rádio diferentes (BU32107, TDA7708). |
| **Centrais 8581 "W5" / "W1"** (vendedores do AliExpress e do 1688) | Mesmos parâmetros das de cima. Inclui versões LTE de 2 GB / 32 GB. |
| Centrais **HiWorld QF005** específicas por carro | Relatada numa Opel/Vauxhall Vivaro, com caixa CAN HiWorld. |
| Centrais QF005 específicas por carro | Relatadas em BMW E92, Jeep e Skoda. |

Para ter em mente:
- O mesmo nome de modelo é vendido com chips de DSP, rádio e MCU diferentes. Isso não importa para o RotivMedia, que nunca mexe no MCU, mas importa se você for gravar firmware.
- Versões pequenas, com pouca RAM (2 GB), podem sofrer com o Maps e o YouTube Music encaixados ao mesmo tempo.
- Nessas centrais, outros launchers ficam "travados": ao apertar Home, volta o launcher de fábrica. A solução é o passo do `config_service` no [guia de desbloqueio](desbloqueio-qf005.md#5-rotivmedia-como-launcher-padrão).

### 🟠 Parcialmente compatíveis: firmware QF mais antigo no UIS7862 / UIS7862S (precisa de ajustes)

Algumas **ROCO K706** usam o firmware **QF001** (e as variantes QF002/QF003) no chip Unisoc UIS7862/7862S, mais rápido. Elas têm a mesma base QF (apps `com.qf.*`, `mcu_service`, `config_service`, `/great/protect_dir`), também com Android 10 real, mas com diferenças que o RotivMedia ainda não trata:
- O launcher padrão fica em `/great/protect_dir/car.config` (linha `launcher_default_app`), ou é definido pelo broadcast `com.qf.action.SET_LAUNCHER`.
- A porta 5555 vem fechada em algumas versões.
- Barras de cima e de navegação de tamanhos diferentes (160 px e 154 px numa tela de 1024 px de altura, num relato).

O app em si deve instalar e rodar. O encaixe das janelas e a faixa da marca atrás da barra provavelmente vão precisar de ajustes.

### ❌ Não suportadas

- **Telas em pé / verticais** (por exemplo, as QF005 "verticais" 2K de 11,5"): o layout é para tela deitada.
- **Centrais sem ADB root na própria central**: o encaixe das janelas não funciona. O launcher funciona só com os painéis próprios.

### ❔ Desconhecidas: outras centrais UIS8581A sem firmware QF

Centrais com o mesmo chip, mas outra base de firmware, **não** são QF. Por exemplo: as com MCU ZA001 (placas Hengchen), a plataforma K4811/JMANCE e as baseadas em FYT. O RotivMedia pode rodar nelas como um launcher Android 10 comum, mas as partes específicas da QF (launcher padrão, códigos, barra, ADB na porta 9876) não se aplicam.

## Como saber se a sua central é QF

1. **Configurações → Sobre:** a versão do sistema começa com `QF005.` (ou `QF001.`, `QF002.`, `QF003.`) e a do MCU com `QF30` ou `qf05`.
2. **Códigos:** no campo de senha dos ajustes do carro, 8888 abre os ajustes avançados e 4455 o menu de engenharia. **Nunca digite 225588** (apaga tudo) nem 774411.
3. **Com ADB:**
   ```bash
   adb shell getprop ro.build.fingerprint                    # ex.: ROCO/K706/K706:10/...
   adb shell service list | grep -E "config_service|mcu_service|qfaudio"
   adb shell pm list packages | grep com.qf.
   adb shell ls /great/protect_dir/
   ```
   `mcu_service`, `qfaudio`, `config_service` e pacotes `com.qf.*` juntos indicam uma ROM QF.

## Fontes

- XDA Forums: [[Firmware] QF005 (ROCO K706) Head Units with UIS 8581a](https://xdaforums.com/t/firmware-qf005-roco-k706-head-units-with-uis-8581a.4692447/), páginas 1 a 8 (relatos de donos de A-Sure, Hizpo, W5/W1, HiWorld e centrais específicas por carro).
- XDA Forums: [[Firmware] QF001 (ROCO K706) Head Units with UIS 7862a/7862s (not FYT based)](https://xdaforums.com/t/firmware-qf001-roco-k706-head-units-with-uis-7862a-7862s-not-fyt-based.4541261/).
- GitHub: [mason-coks/qf-headunit-notes](https://github.com/mason-coks/qf-headunit-notes), notas de campo de uma ROCO K706 QF001 / UIS7862S (identificação, launcher pelo `car.config`, barras).
- XDA Forums: [SPRD UIS8581a (SP9863) - Not FYT, Which firmware?](https://xdaforums.com/t/sprd-uis8581a-sp9863-not-fyt-which-firmware.4612245/) e [Android 10 UIS8581A (SC9863a), MCU: ZA001](https://xdaforums.com/t/android-10-uis8581a-sc9863a-mcu-za001.4679027/) (centrais não QF com o mesmo chip).

Os firmwares compartilhados nesses tópicos não são oficiais, e gravar o errado já inutilizou centrais. O RotivMedia nunca precisa de gravação de firmware.
