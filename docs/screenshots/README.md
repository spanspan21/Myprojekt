# Screenshots

Drop the images referenced by the root `README.md` here. Expected files:

| File | Screen | Tip |
|---|---|---|
| `home.png` | Home / Today dashboard (hero) | Capture right after opening — greeting + 3 mission tiles |
| `prime.png` | Prime index ring | Best "wow" shot; a GIF of the ring sweeping up is even better |
| `train.png` | Active workout | Show a pose figure + a prescription line |
| `fuel.png` | Nutrition | Macro reactor rings + hydration wave card |
| `guard.png` | Focus overlay / focus score | The overlay thrown over a blocked app |
| `calendar.png` | Calendar timeline | Morning training auto-scheduled before school |
| `dock-morph.gif` *(optional)* | Dock morph + shell transition | Put at the top of the root README |

### Capturing

```bash
# Still image
adb exec-out screencap -p > docs/screenshots/home.png

# Screen recording → GIF
adb shell screenrecord --time-limit 6 /sdcard/rec.mp4
adb pull /sdcard/rec.mp4
ffmpeg -i rec.mp4 -vf "fps=20,scale=480:-1" docs/screenshots/dock-morph.gif
```

Keep images ~1080px wide (or 480px for GIFs) so the README stays lightweight.
