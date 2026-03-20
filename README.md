This is an Android port of the Aeolus synthesizer.

It can use both software midi sources (i.e. virtual keyboard apps) and 
and hardware midi sources (i.e. externally connected keyboards) to direct
the Aeolus synthesizer.

The Aeolus synthesizer code is from here: 
https://github.com/SimulPiscator/aeolus (GPL v3.0). There is a substantial amount of  
modifications to make this run within Android, but the primary functionality and 
namely the original rank wave synthesis, general architecture and multi-threading approach
is maintained.

The Aeolus synthesizer is a multithread application using the clthread library for
inter-thread communication. The clthread library is from here:
https://github.com/dmeliza/clthreads (GPL v2.1). This library in turn depends
on the libbthread library, which fortunately had already been ported to Android, 
and is available here:
https://github.com/tux-mind/libbthread (GPL v2.1). For both libraries, there are
also minor adaptations to make the entire thing run under Android and with Aeolus.

For midi input, the standard Android mechanisms for software midi and hardware midi.
This is well described in the general Android documentation, in particular at 
https://developer.android.com/reference/android/media/midi/package-summary.html; 
both the section "Writing a MIDI Application" for hardware midi and "Creating a MIDI Virtual Device Service"
were followed here.

For Audio output, the Android oboe library was chosen. In principle, there is 
a prefab, but there were problems with finding headers at compile time with the
prefab library, and so in the end, the oboe library was included as a github 
submodule. The oboe library is licensed under Apache v2.0 license. It is used as 
an unmodified github submodule, included by reference here (i.e. it is linked 
under the main cpp directory, see 
https://github.com/tbgitoo/MidiSynth/tree/master/app/src/main/cpp)

A well-designed combined class structure of an oboe sound player 
and a generic wavetable synthesizer is developped by Jan Wilczek and 
described in worthwhile series of video tutorials (i.e. WolfSound, with the 
Android app part in Kotlin), 
see for example https://youtu.be/Gb4DhIht6_s . There is also a corresponding 
github repository (https://github.com/JanWilczek/android-wavetable-synthesizer, GPL v3.0).
The general class structure for a synthesizer interacting with oboe were imported 
from this github repository and the general overall approach followed from the 
video tutorials.

This work is under a GPL v3.0 license. It is provided "as is", there is no 
guarantee of fitness for any particular purpose, nor responsibility for any 
possible damage resulting from its use from the part of the authors.




