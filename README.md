# SuperCollider Psedo UGen for Wave Field Synthesis

PanWFS is an open-source SuperCollider extension developed alongside THE AURA and compatible with any 2.5D equidistant linear WFS array. It treats WFS authoring as another panning algorithm: define a virtual source position (and, for directional sources, an orientation) and PanWFS synthesises the wavefront by computing per-loudspeaker delay and amplitude from the Rayleigh/Huygens formulation. Doppler shift emerges naturally from changing propagation delays — no separate algorithm is required.


PanWFS
Copyright (c) 2026 Kerem Ergener. All rights reserved.
Licensed under the PanWFS Source-Available License.
See LICENSE for details.
keremergener.com

More information of The AURA WFS system: theaura.keremergener.com

Disclaimer
PanWFS is experimental research software for spatial audio and Wave Field Synthesis workflows. Users are responsible for testing patches, gain staging, loudspeaker routing, and output levels before use in studio, installation, or performance contexts. The author is not responsible for speaker damage, hearing damage, data loss, failed performances, incorrect spatial rendering, or any other damages resulting from the use or misuse of this software.
