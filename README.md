# PanWFS UGens

**PanWFS UGens** is a SuperCollider server-plugin implementation of the PanWFS spatialization tools.  
It moves the main DSP work from SuperCollider pseudo-UGens into compiled C++ UGens.

The package currently includes:

- `PanWFS_Monopole`
- `PanWFS_Cardioid`
- `PanWFS_Headphone`
- `PanWFS_Binaural`
- `PanWFS_Calibrate` — SPL calibration helper (language-side class, not a server UGen)

The WFS UGens are designed for regular linear loudspeaker arrays. The headphone and binaural UGens provide lightweight two-channel monitoring tools for previewing spatial motion over headphones. `PanWFS_Calibrate` ties digital signal level to measured dB SPL so virtual sources can be placed at real-world loudnesses.

> Status: experimental / research version.  
---

## Contents

```text
PanWFS_RealUGens/
├── CMakeLists.txt
├── PanWFS.sc
└── source/
    └── PanWFS.cpp
```

`PanWFS.sc` contains the SuperCollider language classes.

`PanWFS.cpp` contains the C++ server-plugin implementations.

Class documentation is provided as `.schelp` files for `PanWFS_Monopole`, `PanWFS_Cardioid` and `PanWFS_Calibrate`; install them under `HelpSource/Classes/` in the extension folder if you want them in the SuperCollider help browser.

---

## Downloads

Universal MacOS build is provided. File that can run in both Intel and Apple Silicon SuperCollider.

---

## Included UGens

### `PanWFS_Monopole`

A WFS monopole source for a regular linear speaker array.

```supercollider
PanWFS_Monopole.ar(
    input,
    virtual_x,
    virtual_z,
    numChans,
    speaker_distance,
    zRef,
    roomTemp,
    humidity,
    groundElevation,
    max_delay,
    lagTime,
    kFactor,          // SPL calibration constant K (dB) -- see "SPL calibration"
    maxSpl,           // intended level of a full-scale signal (dB SPL @ 1 m)
    maxxOn, maxxFreq, maxxIntensity, maxxHarmDb,
    maxxOrigDb, maxxAttack, maxxRelease, maxxMonitor
)
```

`kFactor` and `maxSpl` both default to `0`, which is unity gain — leave them alone for uncalibrated behaviour. The `maxx*` arguments are the per-source psychoacoustic bass-extension stage and default to bypassed (`maxxOn: 0`).

### `PanWFS_Cardioid`

A WFS cardioid source with source yaw/directivity control.

```supercollider
PanWFS_Cardioid.ar(
    input,
    virtual_x,
    virtual_z,
    yaw,
    numChans,
    speaker_distance,
    zRef,
    roomTemp,
    humidity,
    groundElevation,
    max_delay,
    lagTime,
    kFactor,
    maxSpl,
    maxxOn, maxxFreq, maxxIntensity, maxxHarmDb,
    maxxOrigDb, maxxAttack, maxxRelease, maxxMonitor
)
```

### `PanWFS_Headphone`

A lightweight geometric headphone renderer using interaural delay, distance gain, and simple head-shadow filtering.

```supercollider
PanWFS_Headphone.ar(
    input,
    virtual_x,
    virtual_z,
    listener_x,
    listener_z,
    headyaw,
    headWidth,
    distRef,
    lagTime
)
```

### `PanWFS_Binaural`

A lightweight parametric binaural renderer with simple head shadow, pinna-like peak/notch cues, rear dulling, and near-field low-frequency support.

```supercollider
PanWFS_Binaural.ar(
    input,
    virtual_x,
    virtual_z,
    listener_x,
    listener_z,
    hrtf,
    headyaw
)
```

The headphone and binaural renderers are monitoring tools. They are not calibrated by `kFactor` / `maxSpl`, since they bypass the array driving function entirely.

### `PanWFS_Calibrate`

SPL calibration helper. This is a language-side convenience class, not a server plugin: `*ar` drives the verified `PanWFS_Monopole` renderer with an internally normalised pink-noise source, and `*k` is pure arithmetic.

```supercollider
// render the calibration reference through the array
PanWFS_Calibrate.ar(
    numChans,
    speaker_distance,
    zRef,
    sourceDepth,      // cal source depth behind the array (m)
    refRms,           // reference level of the pink noise (dBFS RMS), default -20
    roomTemp,
    humidity,
    groundElevation,
    max_delay,
    lagTime
)

// convert a sound-level meter reading into K (also posted to the post window)
PanWFS_Calibrate.k(
    measuredSpl,      // what the meter shows, dB SPL
    refRms,           // must match the value passed to *ar
    sourceDepth,      // must match *ar
    micDistance       // mic distance in front of the array (m)
)
```

`*ar` renders pink noise normalised to `refRms` as an on-axis (`x = 0`) monopole at `sourceDepth` behind the array, at unity SPL gain, and returns `numChans` channels — route them to the loudspeakers. The array arguments must match the array you will render with.

See **SPL calibration** below for the full procedure.

---

## Requirements

- SuperCollider
- CMake
- A C++ compiler
- Git
- SuperCollider source headers matching your installed SuperCollider version

On macOS:

```bash
xcode-select --install
brew install cmake git
```

---

## Important compatibility note

The compiled plugin must match both:

1. The CPU architecture of the SuperCollider server you are running.
2. The SuperCollider plugin API version of the installed SuperCollider app.

For example, if you are running the Intel version of SuperCollider on Apple Silicon for compatibility with older Quarks, build the plugin as `x86_64`.

If you see an error like:

```text
ERROR: API version mismatch
Plugin's API version: 6. Expected: 3.
```

then the plugin was compiled against the wrong SuperCollider source headers.  
Clone the SuperCollider source version that matches your installed SuperCollider app.

Example:

```bash
git clone --recursive --branch Version-3.13.0 https://github.com/supercollider/supercollider.git
```

Use the tag that matches your installed version.

---

## Installation

Create an extension folder:

```bash
mkdir -p "$HOME/Library/Application Support/SuperCollider/Extensions/PanWFS/classes"
mkdir -p "$HOME/Library/Application Support/SuperCollider/Extensions/PanWFS/plugins"
```

Copy the SuperCollider class file:

```bash
cp ~/PanWFS_RealUGens/PanWFS.sc \
"$HOME/Library/Application Support/SuperCollider/Extensions/PanWFS/classes/"
```

Copy the compiled plugin:

```bash
cp ~/PanWFS_RealUGens/build/PanWFS.scx \
"$HOME/Library/Application Support/SuperCollider/Extensions/PanWFS/plugins/"
```

Remove macOS quarantine if necessary:

```bash
xattr -dr com.apple.quarantine \
"$HOME/Library/Application Support/SuperCollider/Extensions/PanWFS/plugins/PanWFS.scx"
```

Then restart SuperCollider language and server:

```supercollider
thisProcess.recompile;
s.quit;
s.boot;
```

---

## Usage

### Set output channels

For 8 channels:

```supercollider
s.options.numOutputBusChannels = 8;
s.reboot;
```

For 192 channels:

```supercollider
s.options.numOutputBusChannels = 192;
s.reboot;
```

---

## SPL calibration

Digital amplitude is not sound pressure. A signal normalised to 0 dBFS only defines its maximum digital level — it says nothing about how loud the rendered virtual source should be in the room, which depends on the audio interface, amplifier gain, loudspeaker sensitivity, and the array summation itself. Without calibration you cannot author a whisper and a car horn as genuinely different sources rather than two fader positions.

Calibration reduces the whole rig to a single constant:

```text
K = SPL at 1 m produced by a virtual source fed a 0 dBFS-RMS signal
```

Measure `K` once with a sound-level meter. It is then passed as `kFactor` to the renderers, where each source's `maxSpl` places it at a real dB SPL. The applied gain is `10 ** ((maxSpl - kFactor) / 20)`.

Because the synthesis level is made independent of loudspeaker count and spacing, `K` holds across array reconfigurations — assuming identical, equally driven loudspeakers.

### Procedure

**1. Play the calibration reference through the array** and read a sound-level meter on-axis in front of the array. Measure at the reference line (`z = zRef`) for an unambiguous reading, and use a broadband / C-weighted reading since the reference is pink noise.

```supercollider
c = { PanWFS_Calibrate.ar(
    numChans: 16,
    speaker_distance: 0.1,
    zRef: 2,
    sourceDepth: 1.0,
    refRms: -20
) }.play;
```

**2. Convert the meter reading into `K`.** The calibration source sits `sourceDepth` behind the array and the microphone `micDistance` in front, so the source-to-microphone distance is `R = sourceDepth + micDistance`. Since the rendered virtual source follows a 1/r law:

```text
K = measuredSpl + 20 * log10(sourceDepth + micDistance) - refRms
```

Suppose the meter reads 78 dB with the mic 2 m in front:

```supercollider
~k = PanWFS_Calibrate.k(
    measuredSpl: 78,
    refRms: -20,
    sourceDepth: 1.0,
    micDistance: 2.0
);   // prints and returns K
c.free;
```

**3. Render with `kFactor` and per-source `maxSpl`.** Both signals below are fed at full scale, but they reproduce 80 dB apart:

```supercollider
(
x = {
    var whisper, horn;
    whisper = PinkNoise.ar(0.2);
    horn    = SinOsc.ar(440) * 0.2;

    PanWFS_Monopole.ar(whisper, -1.5, 3, 16, 0.1, 2, kFactor: ~k, maxSpl: 30)
    + PanWFS_Monopole.ar(horn,   1.5, 3, 16, 0.1, 2, kFactor: ~k, maxSpl: 110);
}.play;
)
```

### Reference levels

`maxSpl` values for common sources (dB SPL @ 1 m, full-scale signal):

| source | dB SPL @ 1 m |
|---|---|
| whisper | 30 |
| conversation | 60 |
| raised voice | 75 |
| trumpet (ff) | 90 |
| power tool | 100 |
| car horn | 110 |
| jackhammer | 120 |

### Notes

- Re-measure `K` whenever the interface, amplifier gain, or loudspeakers change. Array size and spacing do not require re-measurement.
- `K` is a single constant for the whole rig, so it assumes identical, equally driven loudspeakers; per-driver sensitivity differences are not compensated.
- `K` describes the plain rendering chain. The bass-extension stage sits upstream of the SPL anchor and is not represented in `K`.
- `PanWFS_Headphone` and `PanWFS_Binaural` are not calibrated — they bypass the array driving function.

---

## Examples

### Monopole, 8 channels

```supercollider
(
x = {
    var sig;
    sig = SinOsc.ar(220) * 0.1;

    PanWFS_Monopole.ar(
        input: sig,
        virtual_x: MouseX.kr(-1.0, 1.0),
        virtual_z: MouseY.kr(0.2, 4.0),
        8,
        speaker_distance: 0.125,
        zRef: 2,
        roomTemp: 20,
        humidity: 50,
        groundElevation: 0,
        max_delay: 0.5
    )
}.play;
)
```

Stop:

```supercollider
x.free;
```

---

### Cardioid, 8 channels

```supercollider
(
x = {
    var sig;
    sig = Saw.ar(120) * 0.05;

    PanWFS_Cardioid.ar(
        input: sig,
        virtual_x: MouseX.kr(-1.0, 1.0),
        virtual_z: MouseY.kr(0.2, 4.0),
        yaw: MouseX.kr(-pi, pi),
        8,
        speaker_distance: 0.125,
        zRef: 2,
        roomTemp: 20,
        humidity: 50,
        groundElevation: 0,
        max_delay: 0.5
    )
}.play;
)
```

---

### Headphone preview

```supercollider
(
x = {
    var sig;
    sig = PinkNoise.ar(0.05);

    PanWFS_Headphone.ar(
        input: sig,
        virtual_x: MouseX.kr(-2.0, 2.0),
        virtual_z: MouseY.kr(-1.0, 4.0),
        listener_x: 0,
        listener_z: -2,
        headyaw: 0,
        headWidth: 0.18,
        distRef: 1.0
    )
}.play;
)
```

---

### Parametric binaural preview

```supercollider
(
~hrtf = (
    headWidth: 0.18,
    speedOfSound: 343.0,
    maxDelay: 0.05,
    lagTime: 0.02,
    minDistance: 0.125,
    distRef: 1.0,

    shadowFc: 3500,
    shadowRs: 1.0,
    shadowDb: -10.0,

    lowShelfFc: 250,
    lowShelfRs: 1.0,
    nearLowBoostDb: 2.0,
    nearRefDistance: 0.6,

    pinnaPeakFcFront: 3200,
    pinnaPeakFcSide: 4200,
    pinnaPeakRq: 0.8,
    pinnaPeakDb: 4.0,

    notchFcFront: 8500,
    notchFcRear: 6000,
    notchRq: 0.45,
    notchDb: -8.0,

    rearHiShelfFc: 5000,
    rearHiShelfRs: 1.0,
    rearHiShelfDb: -4.0,

    outputGainDb: -3.0
);

x = {
    var sig;
    sig = Dust.ar(8) * 0.2;

    PanWFS_Binaural.ar(
        input: sig,
        virtual_x: MouseX.kr(-2.0, 2.0),
        virtual_z: MouseY.kr(-1.0, 4.0),
        listener_x: 0,
        listener_z: -2,
        hrtf: ~hrtf,
        headyaw: 0
    )
}.play;
)
```

---

## Notes on DSP equivalence

These are C++ implementations of the PanWFS pseudo-UGen structures, but they are not guaranteed to be bit-identical to the original pseudo-UGens.

The original SuperCollider pseudo-UGens used combinations of:

```supercollider
HPZ1.ar
HPF.ar
BHiShelf.ar
DelayL.ar
DelayC.ar
RLPF.ar
LPF.ar
BLowShelf.ar
BPeakEQ.ar
```

The C++ versions implement equivalent internal DSP sections:

- one-sample difference for `HPZ1`
- simple high-pass filtering
- biquad shelf and peak filters
- linear interpolation for `DelayL`
- cubic interpolation for `DelayC`
- distance-dependent low-pass filtering
- per-speaker delay and gain calculation

For research or release use, compare the C++ versions against the original pseudo-UGens with controlled test signals.

---

## Troubleshooting

### `UGen 'PanWFS_Monopole' not installed`

The SuperCollider language found the class, but the server did not load the plugin.

Check that the plugin exists:

```bash
ls -lah "$HOME/Library/Application Support/SuperCollider/Extensions/PanWFS/plugins"
```

Restart the server:

```supercollider
s.quit;
s.boot;
```

---

### Wrong architecture

Check the plugin architecture:

```bash
file "$HOME/Library/Application Support/SuperCollider/Extensions/PanWFS/plugins/PanWFS.scx"
```

For Intel SuperCollider, you need:

```text
x86_64
```

For Apple Silicon-native SuperCollider, you need:

```text
arm64
```

---

### API version mismatch

If you see:

```text
Plugin's API version: 6. Expected: 3.
```

you compiled with the wrong SuperCollider headers.

Fix: clone the matching SuperCollider source version and rebuild.

---

### Calibrated sources are far too loud or too quiet

`kFactor` and `maxSpl` default to `0`. If only one of them is set, the gain is `10 ** ((maxSpl - kFactor) / 20)` with the other at zero — e.g. passing `maxSpl: 90` without `kFactor` applies +90 dB. Pass both, or neither.

Also confirm that `refRms` matched between `PanWFS_Calibrate.ar` and `.k`, and that `numChans` / `speaker_distance` / `zRef` were the same during calibration as during rendering.

---

## Development notes

The current package is organized as one server plugin containing multiple UGens:

```cpp
PluginLoad(PanWFS)
{
    ft = inTable;
    DefineDtorUnit(PanWFS_Monopole);
    DefineDtorUnit(PanWFS_Cardioid);
    DefineDtorUnit(PanWFS_Headphone);
    DefineDtorUnit(PanWFS_Binaural);
}
```

This allows all classes to live in one `.sc` file and all compiled DSP to live in one `.scx` plugin.

`PanWFS_Calibrate` does not appear here: it is implemented entirely in `PanWFS.sc` and delegates its rendering to `PanWFS_Monopole`, so there is no separate server plugin to define.

---

## Roadmap

Possible next steps:

- Add `PanWFS_MonopoleBuf` for arbitrary loudspeaker positions from a Buffer.
- Add `PanWFS_CardioidBuf` for irregular arrays.
- Add `PanWFS_MonopoleSquare` for square arrays.
- Add reverb algorythms. 

---

## License

Add your preferred license here.

Recommended options:

- MIT License for a permissive open-source release.
- GPL-3.0 if you want stricter copyleft terms.
- Custom research-use license if the project is not yet ready for general release.

---

## Author

Kerem Ergener  
PanWFS / Wave Field Synthesis research and SuperCollider development
keremergener.com
theaura.keremergener.com
