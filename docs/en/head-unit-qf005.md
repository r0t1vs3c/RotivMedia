**English** · [Português](../central-qf005.md)

# Technical notes: ROCO K706 / QF005 head unit

What we found while building RotivMedia on this head unit. It applies to Qianfeng's (Shenzhen) QF005 firmware; other firmwares may differ.

## Device

- Real Android 10 (API 29). The "About" screen says Android 13/16, but that's fake. It comes from the app `mark.fakedevicetest2`, which can be disabled.
- Unisoc sp9863a / uis8581a platform, 1280×800 screen at 160 dpi, landscape.
- Stock apps:
  - `com.qf.*`: car settings, Bluetooth, reverse camera, themes and more.
  - `com.zjinnova.zlink`: CarPlay / Android Auto.
  - `com.sprd.engineermode`.
- The platform key belongs to QF (it is not the AOSP testkey), so you can't sign an app as a system app.
- Verified boot is green and flashing is locked.

## Password field codes

Known from the "QF005 (ROCO K706)" XDA thread:

| Code | Opens |
| --- | --- |
| 8888 | Advanced settings |
| 114477 | Developer mode |
| 4455 | Engineering (has an "Adb shell" under Debug) |
| **225588** | **DO NOT USE: wipes the data** |
| **774411** | **DO NOT USE** |

## ADB

- There is no "Wireless debugging" menu.
- At every boot the head unit turns `adb_enabled` off and sets `service.adb.tcp.port=9876`.
- `adbd` runs as **root**, SELinux is permissive and `ro.adb.secure=0`, so it accepts connections without an on-screen prompt. Only use it on a trusted network.
- **First enable:**
  1. Engineering menu 4455 → Debug → Adb shell. This shell runs as `system`.
  2. Run `setprop service.adb.tcp.port 5555`.
  3. Run `setprop ctl.restart adbd`.
- **At every boot, RotivMedia (`RemoteAdb.kt`):**
  - turns `adb_enabled` back on, which needs `WRITE_SECURE_SETTINGS`;
  - sends `com.tailscale.ipn.CONNECT_VPN` so Tailscale connects.
- **Background killing:** Unisoc's power manager kills background apps. In Settings → Battery, set RotivMedia and Tailscale to "don't optimize" and allow auto-start.

## Default launcher

QF stores the launcher in its `config_service`, besides `persist.sys.qf.launcher`:

```bash
# read
adb shell service call config_service 4 s16 launcher_default_app
# write
adb shell service call config_service 3 s16 launcher_default_app s16 com.rotivsecurity.launcher
adb shell setprop persist.sys.qf.launcher com.rotivsecurity.launcher
```

To go back to the stock launcher, write the original theme's package (for example `com.android.launcher.datai_ui10`). The "themes" in QF's store are separate Launcher3 launchers.

## Docked windows (Maps, YouTube Music, YouTube, RetroArch)

The stock launcher shows an "embedded" Maps by using a freeform window (`sys.qf.launcher.map.freeform=true`). RotivMedia does the same:

1. It measures the frame on screen (`onGloballyPositioned`). Then it opens the app with `ActivityOptions.setLaunchBounds(rect)` and `setLaunchWindowingMode(5)` (freeform, set via reflection).
2. After about 1 s it checks `am stack list`:
   - if the window ended up fullscreen (`mWindowingMode=fullscreen`), it runs `am stack remove` and opens it again;
   - if the size doesn't match, it fixes it with `am task resize`.
3. If the frame changes size while the window is already open, it only resizes.
   - **Relaunching steals focus.** On Android 10 only the focused window is "resumed".
   - An app that is still on its loading screen, like RetroArch, gets stuck.
4. The `am` commands are sent through the ADB protocol itself to `127.0.0.1` (`LocalShell.kt`), because the app has no system permissions.

## Top bar (SystemUI)

- QF's SystemUI (`SystemUIWithLegacyRecents`) has a 72 px bar.
- It declares no `overlayable`, so RRO overlays don't work.
- The folder `/data/QF/protect_dir/project/picture/statusbarbg/` and `persist.sys.qf.statusbar.color` only apply to portrait models. On this model the bar is **transparent**.
- That's why the home screen draws `barra_central.png` behind it (`setDecorFitsSystemWindows(false)`).

## Language

The firmware ships in pt-PT. Changing `persist.sys.locale` and `system_locales` to `pt-BR` works.
