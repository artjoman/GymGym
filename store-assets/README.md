# Play Store listing assets

Uploaded by hand in **Play Console → Grow → Store presence → Main store listing →
Graphics**. These are *not* part of the app bundle — the launcher icon lives in
`app/src/main/res/mipmap-*/ic_launcher_foreground.png` and is separate.

| File | Slot | Spec |
| --- | --- | --- |
| `play-icon-512.png` | App icon | 512×512, 32-bit PNG, ≤1 MB |
| `play-feature-graphic-1024x500.png` | Feature graphic | 1024×500, 32-bit PNG |

## Notes

- The icon is a **flat square with no rounded corners and no transparent margin**
  — Play applies its own mask, so rounding it here would double-round it. It is
  cropped inside the source artwork's rounded rectangle so no white corner shows.
- The feature graphic is deliberately **text-free**: Play overlays the app name
  and install button on it in several placements, which tends to collide with
  baked-in text.
- Background is the artwork's own yellow (sampled, ≈`#FEBD09`), matching the
  launcher icon background `#FFBE0A` and the app's default Amber accent.
