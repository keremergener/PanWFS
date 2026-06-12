# PanWFS UGens

**PanWFS Real UGens** is a SuperCollider server-plugin implementation of the PanWFS spatialization tools.  
It moves the main DSP work from SuperCollider pseudo-UGens into compiled C++ UGens.

The package currently includes:

- `PanWFS_Monopole`
- `PanWFS_Cardioid`
- `PanWFS_Headphone`
- `PanWFS_Binaural`

The WFS UGens are designed for regular linear loudspeaker arrays. The headphone and binaural UGens provide lightweight two-channel monitoring tools for previewing spatial motion over headphones.

> Status: experimental / research version.  
> These UGens are intended for testing, development, and comparison against the original SuperCollider pseudo-UGens.

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

---

## Included UGens

### `PanWFS_Monopole`

A WFS monopole source for a regular linear speaker array.

```supercollider
PanWFS_Monopole.ar(
    numChans,
    input,
    virtual_x,
    virtual_z,
    speaker_distance,
    zRef,
    roomTemp,
    humidity,
    groundElevation,
    max_delay
)
```

### `PanWFS_Cardioid`

A WFS cardioid source with source yaw/directivity control.

```supercollider
PanWFS_Cardioid.ar(
    numChans,
    input,
    virtual_x,
    virtual_z,
    yaw,
    speaker_distance,
    zRef,
    roomTemp,
    humidity,
    groundElevation,
    max_delay
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
    distRef
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

## Examples

### Monopole, 8 channels

```supercollider
(
x = {
    var sig;
    sig = SinOsc.ar(220) * 0.1;

    PanWFS_Monopole.ar(
        8,
        input: sig,
        virtual_x: MouseX.kr(-1.0, 1.0),
        virtual_z: MouseY.kr(0.2, 4.0),
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
        8,
        input: sig,
        virtual_x: MouseX.kr(-1.0, 1.0),
        virtual_z: MouseY.kr(0.2, 4.0),
        yaw: MouseX.kr(-pi, pi),
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

---

## Roadmap

Possible next steps:

- Add `PanWFS_MonopoleBuf` for arbitrary loudspeaker positions from a Buffer.
- Add `PanWFS_CardioidBuf` for irregular arrays.
- Add validation plots comparing pseudo-UGen and C++ UGen output.
- Add help files: `.schelp`.
- Add example SynthDefs for 8-channel, 64-channel, and 192-channel systems.
- Add benchmark comparisons between pseudo-UGen and real UGen versions.

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
