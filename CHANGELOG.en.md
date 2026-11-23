**English** · [Português](CHANGELOG.md)

# Changelog

## 0.6.4
- The shortcuts on the right were replaced by the **My car** panel with live OBD2 data: RPM, engine temperature, battery, instant fuel use, average and trip km. Temperature and battery turn red when out of range, and trouble codes show up as a warning.
- In the bottom strip, the "My car" button became **Apps**.

## 0.6.3
- Fixed: RetroArch didn't open. Opening RetroArch changed the Maps frame by 5 px, so Maps was relaunched and stole focus from RetroArch's loading screen. Now the Maps frame keeps the same size on every layout, and windows that are already open are only resized, never relaunched.

## 0.6.0 – 0.6.2
- **YouTube Music docked in the center**: the real app, in a window docked in the middle frame (switch in Settings → Map).
- If the app is already fullscreen, it is taken out of fullscreen and reopened in the frame.
- The player ignores the Google app's empty media session and stopped or failed sessions.
- Opening YouTube or RetroArch removes YouTube Music from the center, and it comes back when they close.

## 0.5.1 – 0.5.9
- No own top bar on the home screen (it duplicated the head unit's bar). The screen draws a RotivMedia brand strip behind the head unit's transparent bar.
- Strip below Maps with the buttons and the docked app's controls.
- The Maps frame got the same rounded border as the other frames.
- Docking retries when the frame changes size right after opening.

## 0.5.0
- RetroArch docks on the right like YouTube; closing it needs a second tap.
- Close buttons moved out of the window area, making the frame bigger.
- Welcome plays only once per boot (or when waking after 5 min with the screen off).

## 0.4.0 – 0.4.1
- RotivMedia as the head unit's default launcher.
- **Docked Maps**: Google Maps docked in the left frame in a freeform window.
- YouTube docked on the right.
- Settings, Apps and My car open on top of the docked windows.

## 0.3.0 – 0.3.6
- RotivMedia name.
- My car: car drawn in code (4 body styles, 13 colors or a free color) or a photo.
- OBD2 with ELM327 (read-only), fuel use for E27 gasoline and ethanol, trouble codes explained in Portuguese, and demo mode.
- Remote ADB: turns debugging and Tailscale back on at every boot.

## 0.2.0
- Welcome with the dragon, Settings, speed warning beep, 40/60 window mode and real Android version diagnostics.

## 0.1.0
- First version: GPS on the left, YouTube / YouTube Music on the right, shortcuts and app list.
