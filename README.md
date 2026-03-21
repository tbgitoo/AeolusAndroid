QUICK START 

===========



Install the APK directly on your device

\---------------------------------------



1\) Download the latest release APK (e.g., "AeolusAndroid-v1.0.apk") from the

&#x20;  Releases page (https://github.com/tbgitoo/AeolusAndroid/releases/download/v1.0/AeolusAndroid-v1.0.apk)



2\) On your Android device, open the APK and follow the prompts.

&#x20;  • If Android blocks installation from your browser or files app, allow

&#x20;    "Install unknown apps" for that app (you will get a prompt; you can

&#x20;    toggle it off again later in Settings).



&#x20;  • Android may also ask to scan the app for threats. By all means, do so, it should pass.



3\) Open the app. To test it, you select some stops for each of the 4 divisions, and go to the keyboard play page (icon in the top-right corner) 

&#x20;  



BUILD FROM SOURCE

=================



Alternatively, or for development purposes, you can also build this app from source.



Important: this project uses git submodules. Clone with --recursive or initialize

submodules after cloning; otherwise your build will miss dependencies.



1\) Clone the repository and submodules

\--------------------------------------

&#x20;  git clone --recursive https://github.com/tbgitoo/AeolusAndroid



&#x20;  If you already cloned without submodules:

&#x20;  cd AeolusAndroid

&#x20;  git submodule update --init --recursive



&#x20;  Submodules include (among others):

&#x20;  • oboe (audio I/O; Apache‑2.0; unmodified submodule)

&#x20;  • aeolus (synth; GPL v3.0; minimal portability changes for Android)

&#x20;  • clthreads (GPL v2.1) and libbthread (GPL v2.1) with small Android tweaks



2\) Open in Android Studio (recommended)

\--------------------------------------

&#x20;  • Use a current Android Studio with the NDK installed.

&#x20;  • Open the project root and let Gradle sync.

&#x20;  • Run → choose your device → the debug build should install and run.

&#x20;  • Note that right now, the ndk is pinned to 30.0.14904198. You cange this in build.gradle of the app module.





AUDIO, MIDI, AND APP BEHAVIOR

=============================

• MIDI: The app relies on Android’s standard MIDI APIs for software and hardware

&#x20; sources. Hardware MIDI keyboards should work via USB‑MIDI; on‑screen/virtual

&#x20; keyboards can act as software sources.

• Audio: The app uses Google’s Oboe library for low‑latency audio on Android

&#x20; (kept as an unmodified submodule).

• First‑run defaults: If audio is silent on first run, make sure you select stops, to start with in all four divisions. Also check device output

&#x20; selection/volume. First‑run audio defaults and a simpler start screen are

&#x20; planned for v1.1.





CREDITS AND UPSTREAM REFERENCES

===============================



• Aeolus synthesizer (original project; GPL v3.0)

&#x20; https://github.com/SimulPiscator/aeolus

&#x20; This Android port preserves Aeolus’s rank‑wave synthesis, core architecture,

&#x20; and multi‑threading model with only portability changes needed for Android.



• General Android wavetable synthesizer (original project; GPL v3.0)

&#x20; https://github.com/JanWilczek/android-wavetable-synthesizer

&#x20; As submodule "SynthesizerBase", modified and simplified for the current project.



• clthreads (GPL v2.1)

&#x20; https://github.com/dmeliza/clthreads (portability changes for Android)



• libbthread (GPL v2.1)

&#x20; https://github.com/tux-mind/libbthread (portability changes for Android)



• Oboe (Apache‑2.0; unmodified submodule)

&#x20; https://github.com/google/oboe



DOCUMENTATION

=============



For the purpose of reusing parts of this app or developing it further, extensive technical documentation is 

available here: https://github.com/tbgitoo/AeolusAndroid/blob/master/doc/Aeolus%20Android%20App%20Documentation.docx





LICENSES

========

• This repository: GPL v3.0

• Aeolus upstream: GPL v3.0

• Wavetable Synthesizer upstream: GPL v3.0

• clthreads / libbthread: GPL v2.1

• Oboe: Apache‑2.0 (submodule, unmodified)



See the respective LICENSE files in the submodules where applicable.





SUPPORT / ISSUES

================



We have limited ressources to dedicate to this project, this is really our return to the community for making an hobby-level Android Aeolus-based synthesizer port possible.



But if you do open a GitHub issue and include:

• device model and Android version,

• whether you used the APK or built from source,

• exact steps to reproduce, and

• logs if available (e.g., `adb logcat` around the problem).



we will try to respond, although we can't guarantee neither a response nor success of debugging.



Contributions and bug reports are also welcome. Thank you for trying

"Aeolus Android – Unofficial Port".



