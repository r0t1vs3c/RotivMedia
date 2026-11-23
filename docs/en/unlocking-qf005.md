**English** · [Português](../desbloqueio-qf005.md)

# Unlocking the ROCO K706 / QF005 head unit

Step by step, this is what was done on the head unit's Android to install and control RotivMedia:
- turn on ADB and reach it over the network;
- make RotivMedia the default launcher;
- keep everything working after a reboot.

**This is not an exploit root, and it doesn't modify the firmware.** Everything was done with factory menus, `settings`, `setprop` and Android permissions. The `/system` partition, the bootloader, the MCU and the car's CAN bus were never touched. Every step can be undone (see [Undo everything](#undo-everything)).

> Do this at your own risk. Before each change, save a backup (`adb shell settings list global|secure|system` and `adb shell getprop`).

## Summary

| Step | What it solves |
| --- | --- |
| 1. Engineering menu → Adb shell | First command-line access, without a USB data cable |
| 2. Network ADB | Connect the PC to the head unit over Wi-Fi |
| 3. Tailscale | Reach the head unit from anywhere through a private VPN |
| 4. Surviving reboots | The head unit turns ADB off and kills apps at every boot |
| 5. RotivMedia as default launcher | QF stores the launcher in its own service |
| 6. Freeform windows | Docked Maps and YouTube Music on the home screen |
| 7. Cleanup and language | Remove the fake "Android 13" and an ad-filled keyboard, switch to pt-BR |

## Before you start: what this head unit is

- Sold as "Android 13" (sometimes "Android 16"), but it is **Android 10** (API 29). The fake number comes from the app `mark.fakedevicetest2`.
- Unisoc sp9863a / uis8581a platform, Qianfeng's (Shenzhen) QF005 firmware, 1280×800 screen.
- There is **no** "Wireless debugging" in Developer options.
- XDA reports say that turning on ADB can break CarPlay / Android Auto (`com.zjinnova.zlink`). It didn't here, but test yours.

### Password field codes

The head unit's settings app has a password field (codes from the "QF005 (ROCO K706)" XDA thread):

| Code | Opens |
| --- | --- |
| 8888 | Advanced settings |
| 114477 | Developer mode |
| 4455 | Engineering |
| **225588** | **DO NOT USE: wipes the head unit's data** |
| **774411** | **DO NOT USE** |

## 1. First access: engineering menu

1. Head unit settings → password field → **4455**.
2. Go to **Debug** → **Adb shell**. This opens a terminal running as the `system` user.
3. In that terminal, turn on network ADB on port 5555:
   ```sh
   setprop service.adb.tcp.port 5555
   setprop persist.adb.tcp.port 5555
   setprop ctl.restart adbd
   ```

## 2. Network ADB

With the head unit and the PC on the same network:

```bash
adb connect HEAD_UNIT_IP:5555
adb shell id
```

The result shows what makes this head unit easy to work with, and also dangerous:

- `adbd` logs in as **root** (`uid=0`, `su` context);
- **SELinux is permissive**;
- `ro.adb.secure=0`: it **accepts any computer without an on-screen confirmation**.

In other words, with ADB on, any device on the same network gets root on the head unit. Only turn it on in networks you own (for example, your phone's hotspot), and never expose the port to the internet.

Also checked:
- The platform key is **QF's own**, not the public AOSP testkey. You can't sign an app as a system app.
- Verified boot is green and flashing is locked. Changing `/system` isn't viable, and it wasn't needed.

## 3. Tailscale (access from anywhere)

To update the head unit from home while it sits in the car:

1. Install the **official Tailscale APK** on the head unit over ADB. No Play Store or Google account needed.
2. Join your Tailscale network with an **auth key** generated in the Tailscale admin panel. **Type it yourself on the head unit**; never keep the key in files or in code.
3. The head unit shows up in your network with a `100.x.y.z` IP. From the PC (also on Tailscale):
   ```bash
   adb connect HEAD_UNIT_TAILSCALE_IP:PORT
   ```

This way the ADB port is only reachable inside your private VPN.

## 4. Making everything survive a reboot

The head unit "undoes" changes at every boot:

- it turns `adb_enabled` off;
- it sets `service.adb.tcp.port=9876` (5555 only comes back with the manual `setprop`);
- Unisoc's power manager kills Tailscale and blocks apps from starting at boot.

What was done:

**a) Battery and auto-start:** in Settings → Battery, set **Tailscale** and **RotivMedia** to "don't optimize" and allow **auto-start**. This has to be done in the head unit's own interface. The system file `appPowerSaveConfig.xml` then holds `optimize=0` and `autolaunch=2` for both.

**b) Doze exception list:**
```bash
adb shell dumpsys deviceidle whitelist +com.tailscale.ipn
adb shell dumpsys deviceidle whitelist +com.rotivsecurity.launcher
```

**c) Always-on VPN:**
```bash
adb shell settings put secure always_on_vpn_app com.tailscale.ipn
adb shell settings put secure always_on_vpn_lockdown 0
```
(`lockdown 0` keeps the internet working even if Tailscale drops.)

**d) RotivMedia turns ADB and Tailscale back on at boot** (`RemoteAdb.kt` + `BootReceiver`). It needs this permission, granted once:
```bash
adb shell pm grant com.rotivsecurity.launcher android.permission.WRITE_SECURE_SETTINGS
```
At every boot it does the equivalent of `settings put global adb_enabled 1` and sends `com.tailscale.ipn.CONNECT_VPN` to `com.tailscale.ipn/.IPNReceiver`. The "ADB remoto" ("Remote ADB") switch in RotivMedia's settings turns this on and off.

Tested result after rebooting without touching anything: Tailscale was back in ~46 s and ADB in ~64 s, on port **9876**.

## 5. RotivMedia as the default launcher

Android's "Home app" setting isn't enough. QF stores the launcher in its own `config_service` and in a property.

```bash
# see the current one
adb shell service call config_service 4 s16 launcher_default_app

# set RotivMedia
adb shell service call config_service 3 s16 launcher_default_app s16 com.rotivsecurity.launcher
adb shell setprop persist.sys.qf.launcher com.rotivsecurity.launcher
adb shell cmd package set-home-activity com.rotivsecurity.launcher/.HomeActivity
```

The "themes" in QF's store are separate launchers (`com.android.launcher.datai_ui1` … `datai_ui10`). Write yours down before switching, so you can go back.

Music access (so the player can read what's playing):
```bash
adb shell cmd notification allow_listener com.rotivsecurity.launcher/com.rotivsecurity.launcher.MediaListener
```

## 6. Freeform windows (docked Maps and YouTube Music)

The stock launcher shows an "embedded" Maps by using a freeform window (`sys.qf.launcher.map.freeform=true`). For RotivMedia to do the same:

```bash
adb shell settings put global enable_freeform_support 1
adb shell settings put global force_resizable_activities 1
```

Restart the head unit afterwards. How the windows are positioned: [head-unit-qf005.md](head-unit-qf005.md#docked-windows-maps-youtube-music-youtube-retroarch).

## 7. Cleanup and language

**Disabled** (not uninstalled; they come back with `pm enable`):
```bash
adb shell pm disable-user --user 0 mark.fakedevicetest2           # shows the fake "Android 13"
adb shell pm disable-user --user 0 com.emoji.keyboard.touchpal.go # keyboard with ads
```
The default keyboard becomes Android's own (LatinIME).

**Brazilian Portuguese** (the head unit ships in pt-PT):
```bash
adb shell setprop persist.sys.locale pt-BR
adb shell settings put system system_locales pt-BR
```
then restart. It also works with the MoreLocale app, after granting it `CHANGE_CONFIGURATION` with `pm grant`.

## Tried and undone

- **Top bar color/image** through the folder `/data/QF/protect_dir/project/picture/statusbarbg/` and `persist.sys.qf.statusbar.color`: only works on portrait models, so it was removed. On this model the bar is transparent, so RotivMedia draws the brand behind it.
- **Overlay (RRO) on SystemUI:** QF's SystemUI declares no `overlayable`, and the risk of breaking the interface wasn't worth it. Not applied.

## Never done

- Using codes 225588 or 774411.
- Touching the MCU or CAN bus, or sending commands to the car. RotivMedia's OBD2 is read-only.
- Writing to `/system`, unlocking the bootloader or rooting with an exploit.
- Putting passwords, auth keys or IPs in the code.

## Undo everything

```bash
# stock launcher (replace with the theme you used)
adb shell service call config_service 3 s16 launcher_default_app s16 com.android.launcher.datai_ui10
adb shell setprop persist.sys.qf.launcher com.android.launcher.datai_ui10

# disabled apps
adb shell pm enable com.emoji.keyboard.touchpal.go
adb shell pm enable mark.fakedevicetest2

# always-on VPN and power exceptions
adb shell settings delete secure always_on_vpn_app
adb shell dumpsys deviceidle whitelist -com.tailscale.ipn
adb shell dumpsys deviceidle whitelist -com.rotivsecurity.launcher

# freeform windows
adb shell settings put global enable_freeform_support 0
adb shell settings put global force_resizable_activities 0

# language
adb shell setprop persist.sys.locale pt-PT
adb shell settings put system system_locales pt-PT

# network ADB
adb shell setprop persist.adb.tcp.port ""
adb uninstall com.rotivsecurity.launcher
```

Finally, turn off "USB debugging" in Developer options and restart.
