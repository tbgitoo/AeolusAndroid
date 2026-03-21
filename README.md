# Aeolus Android – Unofficial Port

## Disclaimer

This work is licensed under **GPL v3.0** and is provided **"as is"**, without any warranty—no guarantee of fitness for a particular purpose and no liability for any damages arising from its use.

This is an independent, unofficial Android port of the open‑source **Aeolus** pipe‑organ software. It is **not affiliated with or endorsed by** the original Aeolus project or any other *AEOLUS* trademark holder.

---
**Why this project exists** — We started this as a family project to play a real MIDI “pedalier” through an Android tablet with a high‑quality, free pipe‑organ sound. Existing Android apps didn’t meet our needs for external MIDI pedalboard support, low‑latency local audio, and a serious organ engine that we could study and adapt. Aeolus already solved the musical side on desktop/Linux, so this project brings Aeolus to Android with the minimal changes required. It’s a specialized instrument, not a consumer app, intended for people who know what manuals, stops, and pedals are—and who don’t mind installing from a release package rather than an app store.

## What is this?

This Android app brings the **Aeolus pipe‑organ synthesizer** to mobile. It can be driven by:

- ✅ the internal (on‑screen) keyboard
- ✅ software MIDI sources (virtual keyboard apps)
- ✅ hardware MIDI sources (externally connected keyboards)

All of these drive the Aeolus synthesizer engine directly.

---

# Quick Start

## Install the APK directly on your device

1. **Download the latest release APK** (for example `AeolusAndroid-v1.0.apk`) from the GitHub Releases page:
   
   👉 https://github.com/tbgitoo/AeolusAndroid/releases/download/v1.0/AeolusAndroid-v1.0.apk

2. On your Android device, **open the APK and follow the prompts**.

   - If Android blocks installation from your browser or file manager, allow **"Install unknown apps"** for that app when prompted.
   - Android may ask to scan the app for threats — you are encouraged to do so; the app should pass.

3. **Open the app**.
   
   - Select some stops for **each of the four divisions**.
   - Go to the **keyboard play page** (keyboard icon in the top‑right corner).

---

# Build from Source

Alternatively — or for development purposes — you can build the app from source.

> **Important**  
> This project uses **git submodules**. You must clone with `--recursive` or initialize submodules after cloning, or the build will be missing dependencies.

## 1. Clone the repository and submodules

```bash
git clone --recursive https://github.com/tbgitoo/AeolusAndroid
```

If you already cloned **without** submodules:

```bash
cd AeolusAndroid
git submodule update --init --recursive
```

Submodules include (among others):

- **oboe** — audio I/O (Apache‑2.0; unmodified submodule)
- **aeolus** — synthesizer core (GPL v3.0; minimal Android portability changes)
- **clthreads** (GPL v2.1) and **libbthread** (GPL v2.1) with small Android tweaks

## 2. Open in Android Studio (recommended)

- Use a **current Android Studio** installation with the **NDK installed**.
- Open the **project root** and let Gradle sync.
- Run → select your device → the **debug build** should install and run.

> **Note**  
> The NDK is currently pinned to **30.0.14904198**. You can change this in the `build.gradle` of the **app** module if needed.

---

# Audio, MIDI, and App Behavior

- **MIDI**  
  The app uses Android’s standard MIDI APIs for both software and hardware sources.  
  Hardware MIDI keyboards should work via **USB‑MIDI**. Virtual keyboard apps can act as software sources.

- **Audio**  
  Audio output is handled via **Google’s Oboe** library for low‑latency audio on Android (included as an unmodified submodule).

- **First‑run defaults**  
  If audio is silent on first run:
  - Make sure you select stops in **all four divisions**.
  - Check device output selection and volume.

  Improved first‑run defaults and a simpler start screen are planned for **v1.1**.

---

# Credits and Upstream References

- **Aeolus synthesizer** (original project; GPL v3.0)  
  https://github.com/SimulPiscator/aeolus  
  This Android port preserves Aeolus’s rank‑wave synthesis, core architecture, and multi‑threading model, with only the portability changes required for Android.

- **General Android wavetable synthesizer** (GPL v3.0)  
  https://github.com/JanWilczek/android-wavetable-synthesizer  
  Included as submodule **SynthesizerBase**, modified and simplified for this project.

- **clthreads** (GPL v2.1)  
  https://github.com/dmeliza/clthreads (Android portability changes)

- **libbthread** (GPL v2.1)  
  https://github.com/tux-mind/libbthread (Android portability changes)

- **Oboe** (Apache‑2.0; unmodified submodule)  
  https://github.com/google/oboe

---

# Documentation

For reuse or further development, **extensive technical documentation** is available here:

👉 https://github.com/tbgitoo/AeolusAndroid/blob/master/doc/Aeolus%20Android%20App%20Documentation.docx

This documentation dives deeply into:

- the internal architecture of the Android app
- the integration with Aeolus
- the original Aeolus codebase

> **Disclaimer**  
> This documentation is provided "as is". No guarantee is made regarding accuracy or fitness for any particular purpose.

---

# Licenses

- This repository: **GPL v3.0**
- Aeolus upstream: **GPL v3.0**
- Wavetable Synthesizer upstream: **GPL v3.0**
- clthreads / libbthread: **GPL v2.1**
- Oboe: **Apache‑2.0** (unmodified submodule)

See the respective `LICENSE` files in the submodules where applicable.

---

# Support / Issues

We have **limited resources** to dedicate to this project — this is our contribution back to the community to make a hobby‑level Android Aeolus‑based synthesizer possible.

If you do open a GitHub issue, please include:

- device model and Android version
- whether you used the APK or built from source
- exact steps to reproduce
- logs if available (for example `adb logcat` around the issue)

We will try to respond, although we cannot guarantee either a response or successful debugging.

Contributions and bug reports are welcome. Thank you for trying **Aeolus Android – Unofficial Port**.
