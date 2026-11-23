**English** · [Português](../compatibilidade.md)

# Compatible head units

RotivMedia was tested on **one** head unit. The other models below come from owner reports in public forums (sources at the end). They run the same Qianfeng (QF) firmware or a close relative, so they are **likely** compatible, but nobody has tested RotivMedia on them yet. If you try one, please open an issue with your model and what worked.

## What RotivMedia needs

| Requirement | Why |
| --- | --- |
| Android 10 or newer | The app targets Android 10 (API 29) and up. |
| Landscape screen, about 1280×720 to 1280×800 | The layout scales by height and is designed for a wide screen. Portrait ("vertical", Tesla-style) screens are not supported. |
| Freeform windows (`enable_freeform_support`) | To dock Google Maps and YouTube Music on the home screen. Without them, the built-in map and player panels are used instead. |
| ADB reachable on the unit itself (port 9876 or 5555) | Docking resizes windows with `am` commands sent over ADB to `127.0.0.1`. |
| QF firmware (for the QF-specific parts) | Default launcher via `config_service`, status bar drawing, boot behavior. See the [unlocking guide](unlocking-qf005.md). |

## Compatibility levels

### ✅ Tested

| Head unit | Firmware | Chip | Screen | Notes |
| --- | --- | --- | --- | --- |
| **ROCO K706** (the tested unit was sold in Brazil as H-Tech HT-9864CA) | QF005 | Unisoc UIS8581A (sp9863a) | 1280×800 landscape | Real Android 10 (claims Android 13). Everything in the README works. |

### 🟡 Likely compatible: same QF005 firmware on UIS8581A (not tested)

Owners on XDA report these units running **QF005** firmware with MCU **QF30** or **qf05**:

| Head unit / seller | Reported details |
| --- | --- |
| **A-Sure Z84X3 series** (e.g. the Skoda Fabia II model) | QF005, MCU QF30, UIS8581A. The owner considers it a fork of the Hizpo S-Series. |
| **A-Sure K709** | QF005, MCU QF30, 4 GB / 64 GB. |
| **Hizpo K706** and the **Hizpo S-Series** | QF005, MCU qf05. Variants with different DSP/radio chips (BU32107, TDA7708). |
| **"W5" / "W1" 8581 units** (AliExpress and 1688 sellers) | Same parameters as the units above. Includes 2 GB / 32 GB LTE versions. |
| **HiWorld QF005** car-specific units | Reported in an Opel/Vauxhall Vivaro, with a HiWorld CAN box. |
| Car-specific QF005 units | Reported in BMW E92, Jeep and Skoda models. |

Things to keep in mind:
- The same model name is sold with different DSP, radio and MCU chips. That doesn't matter to RotivMedia, which never touches the MCU, but it does matter if you flash firmware.
- Small, low-RAM versions (2 GB) may struggle with Maps and YouTube Music docked at the same time.
- On these units, other launchers get "locked out": pressing Home goes back to the stock launcher. The fix is the `config_service` step in the [unlocking guide](unlocking-qf005.md#5-rotivmedia-as-the-default-launcher).

### 🟠 Partly compatible: older QF firmware on UIS7862 / UIS7862S (needs changes)

Some **ROCO K706** units use firmware **QF001** (and QF002/QF003 variants) on the faster Unisoc UIS7862/7862S chip. They have the same QF framework (`com.qf.*` apps, `mcu_service`, `config_service`, `/great/protect_dir`), also with real Android 10, but with differences that RotivMedia doesn't handle yet:
- The default launcher is stored in `/great/protect_dir/car.config` (line `launcher_default_app`), or set with the broadcast `com.qf.action.SET_LAUNCHER`.
- Port 5555 is closed on some builds.
- Different status bar and bottom navigation bar sizes (160 px and 154 px on a 1024 px tall screen in one report).

The app itself should install and run. Docking and the brand strip behind the status bar will probably need adjustments.

### ❌ Not supported

- **Portrait / vertical screens** (for example the 11.5" 2K "vertical" QF005 units): the layout is designed for landscape.
- **Head units without ADB root on the unit**: docking won't work. The launcher works with the built-in panels only.

### ❔ Unknown: other UIS8581A head units without QF firmware

Units on the same chip but other firmware bases are **not** QF: for example units with MCU ZA001 (Hengchen boards), the K4811/JMANCE platform, and FYT-based units. RotivMedia may run as a normal Android 10 launcher there, but the QF-specific parts (launcher setting, codes, status bar, ADB on port 9876) won't apply.

## How to tell if your head unit is QF

1. **Settings → About:** the system version starts with `QF005.` (or `QF001.`, `QF002.`, `QF003.`), and the MCU version with `QF30` or `qf05`.
2. **Codes:** in the car settings' password field, 8888 opens advanced settings and 4455 the engineering menu. **Never type 225588** (factory reset) or 774411.
3. **With ADB:**
   ```bash
   adb shell getprop ro.build.fingerprint                    # e.g. ROCO/K706/K706:10/...
   adb shell service list | grep -E "config_service|mcu_service|qfaudio"
   adb shell pm list packages | grep com.qf.
   adb shell ls /great/protect_dir/
   ```
   `mcu_service`, `qfaudio`, `config_service` and `com.qf.*` packages together mean a QF ROM.

## Sources

- XDA Forums: [[Firmware] QF005 (ROCO K706) Head Units with UIS 8581a](https://xdaforums.com/t/firmware-qf005-roco-k706-head-units-with-uis-8581a.4692447/), pages 1–8 (owner reports of A-Sure, Hizpo, W5/W1, HiWorld and car-specific units).
- XDA Forums: [[Firmware] QF001 (ROCO K706) Head Units with UIS 7862a/7862s (not FYT based)](https://xdaforums.com/t/firmware-qf001-roco-k706-head-units-with-uis-7862a-7862s-not-fyt-based.4541261/).
- GitHub: [mason-coks/qf-headunit-notes](https://github.com/mason-coks/qf-headunit-notes), field notes on a QF001 / UIS7862S ROCO K706 (identification, launcher setting through `car.config`, bars).
- XDA Forums: [SPRD UIS8581a (SP9863) - Not FYT, Which firmware?](https://xdaforums.com/t/sprd-uis8581a-sp9863-not-fyt-which-firmware.4612245/) and [Android 10 UIS8581A (SC9863a), MCU: ZA001](https://xdaforums.com/t/android-10-uis8581a-sc9863a-mcu-za001.4679027/) (non-QF units on the same chip).

Firmware files shared in those threads are unofficial. Flashing the wrong one has bricked units. RotivMedia never needs a firmware flash.
