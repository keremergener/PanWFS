// PanWFS
// Copyright (c) 2026 Kerem Ergener. All rights reserved.
// Licensed under the PanWFS Source-Available License.
// See LICENSE for details.

// Disclaimer
// PanWFS is experimental research software for spatial audio and Wave Field Synthesis workflows. Users are responsible for testing patches, gain staging, loudspeaker routing, and output levels before use in studio, installation, or performance contexts. The author is not responsible for speaker damage, hearing damage, data loss, failed performances, incorrect spatial rendering, or any other damages resulting from the use or misuse of this software.


//ITD + ILD / head-shadow

PanWFS_Headphone {
	*ar { |input,
		virtual_x = 0, virtual_z = 1,
		listener_x = 0, listener_z = -2,
		headyaw = 0,
		headWidth = 0.18,  // meters
		distRef = 1.0 //reference distance

		|
		var speedOfSound, minDistance, shadowNearFc, shadowFarFc, shadowDepth, maxDelay;
		var headRadius;
		var sinYaw, cosYaw, rightX, rightZ;
		var leftEarX, leftEarZ, rightEarX, rightEarZ;
		var vx, vz, localX, localZ, rCenter, lateral;
		var dL, dR, delayL, delayR;
		var shadowL, shadowR, fcL, fcR;
		var gainL, gainR;
		var outL, outR;

		speedOfSound = 343.0;
		minDistance = 0.125; //minimum allowed source-to-ear distance in meters.
		shadowNearFc = 18000; //low-pass cutoff frequency for the ear that is not shadowed.
		shadowFarFc = 1500; // low-pass cutoff frequency for the shadowed ear.
		shadowDepth = 0.35; //extra amplitude reduction on the shadowed ear.
		maxDelay = 0.2;     // seconds; keep > any expected source-ear delay

		headRadius = headWidth * 0.5;

		// Listener basis: yaw = 0 means facing +z
		sinYaw = headyaw.sin;
		cosYaw = headyaw.cos;

		// "Right" vector in x-z plane
		rightX = cosYaw;
		rightZ = 0 - sinYaw;

		// Ear positions
		leftEarX  = listener_x - (rightX * headRadius);
		leftEarZ  = listener_z - (rightZ * headRadius);
		rightEarX = listener_x + (rightX * headRadius);
		rightEarZ = listener_z + (rightZ * headRadius);

		// Source position in listener-centered coordinates
		vx = virtual_x - listener_x;
		vz = virtual_z - listener_z;

		// Rotate world vector into listener-local coordinates
		// localX < 0 => source on listener's left
		// localZ > 0 => source in front
		localX = (vx * cosYaw) - (vz * sinYaw);
		localZ = (vx * sinYaw) + (vz * cosYaw);

		rCenter = ((vx * vx) + (vz * vz)).sqrt.max(minDistance);
		lateral = (localX / rCenter).clip(-1.0, 1.0);

		// Per-ear geometric distance
		dL = (((virtual_x - leftEarX)  * (virtual_x - leftEarX))  + ((virtual_z - leftEarZ)  * (virtual_z - leftEarZ))).sqrt.max(minDistance);
		dR = (((virtual_x - rightEarX) * (virtual_x - rightEarX)) + ((virtual_z - rightEarZ) * (virtual_z - rightEarZ))).sqrt.max(minDistance);

		// Absolute propagation delay to each ear
		delayL = dL / speedOfSound;
		delayR = dR / speedOfSound;

		// Which ear is shadowed?
		// source right  => left ear shadowed
		// source left   => right ear shadowed
		shadowL = lateral.clip(0.0, 1.0);
		shadowR = (0 - lateral).clip(0.0, 1.0);

		// Far ear gets darker
		fcL = shadowFarFc + ((1.0 - shadowL) * (shadowNearFc - shadowFarFc));
		fcR = shadowFarFc + ((1.0 - shadowR) * (shadowNearFc - shadowFarFc));

		// Soft distance attenuation, capped so near sources do not explode
		gainL = distRef / dL.max(distRef);
		gainR = distRef / dR.max(distRef);

		// Delay first
		outL = DelayC.ar(input, maxDelay, delayL);
		outR = DelayC.ar(input, maxDelay, delayR);

		// Then distance gain + simple head-shadow gain trim
		outL = outL * gainL * (1.0 - (shadowDepth * shadowL));
		outR = outR * gainR * (1.0 - (shadowDepth * shadowR));

		// Distance based frequency roll-off
		outL = RLPF.ar(outL, (20000/(1 + (0.1 * dL.max(distRef)))));
		outR = RLPF.ar(outR, (20000/(1 + (0.1 * dR.max(distRef)))));

		// ~ -3 dB per doubling
		outL = outL * (1.0 / dL.max(distRef));  // ~ -6 dB per doubling
		outR = outR * (1.0 / dR.max(distRef));  // ~ -6 dB per doubling

		// Simple spectral shadowing
		outL = LPF.ar(outL, fcL);
		outR = LPF.ar(outR, fcR);

		^[outL, outR];
	}
}

//lightweight parametric-HRTF
PanWFS_Binaural {
	*ar { |input,
		virtual_x = 0, virtual_z = 1,
		listener_x = 0, listener_z = -2,
		hrtf = nil,          // Event / IdentityDictionary of params
		headyaw = 0
		|
		var p;

		var headWidth, speedOfSound, maxDelay, lagTime, minDistance, distRef;
		var shadowFc, shadowRs, shadowDb;
		var lowShelfFc, lowShelfRs, nearLowBoostDb, nearRefDistance;
		var pinnaPeakFcFront, pinnaPeakFcSide, pinnaPeakRq, pinnaPeakDb;
		var notchFcFront, notchFcRear, notchRq, notchDb;
		var rearHiShelfFc, rearHiShelfRs, rearHiShelfDb;
		var outputGainDb;

		var headRadius;
		var sinYaw, cosYaw, rightX, rightZ;
		var leftEarX, leftEarZ, rightEarX, rightEarZ;

		var vx, vz, localX, localZ, rCenter;
		var lateral, frontness, leftness, rightness, frontBlend, rearBlend;

		var dL, dR, delayL, delayR, gainL, gainR, nearBoostDb;

		var shadowAmtL, shadowAmtR;
		var peakFcL, peakFcR;
		var peakDbL, peakDbR;
		var notchFc;
		var rearShelfDb;
		var outL, outR;

		p = hrtf ? ();

		// Geometry / transport
		headWidth      = p[\headWidth]      ? 0.18;
		speedOfSound   = p[\speedOfSound]   ? 343.0;
		maxDelay       = p[\maxDelay]       ? 0.05;
		lagTime        = p[\lagTime]        ? 0.02;
		minDistance    = p[\minDistance]    ? 0.125;
		distRef        = p[\distRef]        ? 1.0;

		// Head shadow section
		shadowFc       = p[\shadowFc]       ? 3500;
		shadowRs       = p[\shadowRs]       ? 1.0;
		shadowDb       = p[\shadowDb]       ? (-10.0); // applied on far ear as a high-shelf cut

		// Near-field low-frequency weight
		lowShelfFc     = p[\lowShelfFc]     ? 250;
		lowShelfRs     = p[\lowShelfRs]     ? 1.0;
		nearLowBoostDb = p[\nearLowBoostDb] ? 2.0;
		nearRefDistance= p[\nearRefDistance]? 0.6;

		// Simple pinna-like peak
		pinnaPeakFcFront = p[\pinnaPeakFcFront] ? 3200;
		pinnaPeakFcSide  = p[\pinnaPeakFcSide]  ? 4200;
		pinnaPeakRq      = p[\pinnaPeakRq]      ? 0.8;
		pinnaPeakDb      = p[\pinnaPeakDb]      ? 4.0;

		// Simple pinna-like notch
		notchFcFront   = p[\notchFcFront]   ? 8500;
		notchFcRear    = p[\notchFcRear]    ? 6000;
		notchRq        = p[\notchRq]        ? 0.45;
		notchDb        = p[\notchDb]        ? (-8.0);

		// Rear dulling cue
		rearHiShelfFc  = p[\rearHiShelfFc]  ? 5000;
		rearHiShelfRs  = p[\rearHiShelfRs]  ? 1.0;
		rearHiShelfDb  = p[\rearHiShelfDb]  ? (-4.0);

		// global trim
		outputGainDb   = p[\outputGainDb]   ? 0.0;

		headRadius = headWidth * 0.5;

		// Listener basis: yaw = 0 means facing +z
		sinYaw = headyaw.sin;
		cosYaw = headyaw.cos;

		// Horizontal "right" vector in x-z plane
		rightX = cosYaw;
		rightZ = 0 - sinYaw;

		// Ear positions
		leftEarX  = listener_x - (rightX * headRadius);
		leftEarZ  = listener_z - (rightZ * headRadius);
		rightEarX = listener_x + (rightX * headRadius);
		rightEarZ = listener_z + (rightZ * headRadius);

		// Source in listener-centered coordinates
		vx = virtual_x - listener_x;
		vz = virtual_z - listener_z;

		// Rotate world vector into listener-local coordinates
		// localX < 0 => source left
		// localZ > 0 => source front
		localX = (vx * cosYaw) - (vz * sinYaw);
		localZ = (vx * sinYaw) + (vz * cosYaw);

		rCenter = ((vx * vx) + (vz * vz)).sqrt.max(minDistance);

		lateral   = (localX / rCenter).clip(-1.0, 1.0);
		frontness = (localZ / rCenter).clip(-1.0, 1.0);

		leftness   = (0 - lateral).clip(0.0, 1.0);
		rightness  = lateral.clip(0.0, 1.0);
		frontBlend = frontness.linlin(-1.0, 1.0, 0.0, 1.0);
		rearBlend  = 1.0 - frontBlend;

		// Per-ear geometric distance
		dL = (((virtual_x - leftEarX)  * (virtual_x - leftEarX))  + ((virtual_z - leftEarZ)  * (virtual_z - leftEarZ))).sqrt.max(minDistance);
		dR = (((virtual_x - rightEarX) * (virtual_x - rightEarX)) + ((virtual_z - rightEarZ) * (virtual_z - rightEarZ))).sqrt.max(minDistance);

		delayL = Lag.kr((dL / speedOfSound).min(maxDelay), lagTime);
		delayR = Lag.kr((dR / speedOfSound).min(maxDelay), lagTime);

		// soft distance attenuation
		gainL = distRef / dL.max(distRef);
		gainR = distRef / dR.max(distRef);

		// near-field LF support on both ears
		nearBoostDb = (((nearRefDistance / rCenter).clip(0.0, 1.0)) * nearLowBoostDb);

		// far-ear shadow amounts
		// source on right => left ear is shadowed
		// source on left  => right ear is shadowed
		shadowAmtL = rightness;
		shadowAmtR = leftness;

		// pinna peak shifts a bit with laterality
		peakFcL = pinnaPeakFcFront + ((1.0 - shadowAmtL) * (pinnaPeakFcSide - pinnaPeakFcFront));
		peakFcR = pinnaPeakFcFront + ((1.0 - shadowAmtR) * (pinnaPeakFcSide - pinnaPeakFcFront));

		// strongest pinna peak when source is in front, weaker when behind
		peakDbL = pinnaPeakDb * frontBlend * (1.0 - (0.5 * shadowAmtL));
		peakDbR = pinnaPeakDb * frontBlend * (1.0 - (0.5 * shadowAmtR));

		// notch moves lower toward rear
		notchFc = notchFcRear + (frontBlend * (notchFcFront - notchFcRear));

		// rear gets darker
		rearShelfDb = rearHiShelfDb * rearBlend;

		// start with monaural delay + distance
		outL = DelayC.ar(input, maxDelay, delayL) * gainL;
		outR = DelayC.ar(input, maxDelay, delayR) * gainR;

		// 1) near-field low shelf
		outL = BLowShelf.ar(outL, lowShelfFc, lowShelfRs, nearBoostDb);
		outR = BLowShelf.ar(outR, lowShelfFc, lowShelfRs, nearBoostDb);

		// 2) far-ear head shadow: high-shelf cut
		outL = BHiShelf.ar(outL, shadowFc, shadowRs, shadowDb * shadowAmtL);
		outR = BHiShelf.ar(outR, shadowFc, shadowRs, shadowDb * shadowAmtR);

		// 3) simple front-biased pinna peak
		outL = BPeakEQ.ar(outL, peakFcL, pinnaPeakRq, peakDbL);
		outR = BPeakEQ.ar(outR, peakFcR, pinnaPeakRq, peakDbR);

		// 4) simple front/back notch cue
		outL = BPeakEQ.ar(outL, notchFc, notchRq, notchDb);
		outR = BPeakEQ.ar(outR, notchFc, notchRq, notchDb);

		// 5) rear dulling
		outL = BHiShelf.ar(outL, rearHiShelfFc, rearHiShelfRs, rearShelfDb);
		outR = BHiShelf.ar(outR, rearHiShelfFc, rearHiShelfRs, rearShelfDb);

		// Distance based frequency roll-off
		outL = RLPF.ar(outL, (20000/(1 + (0.1 * dL.max(distRef)))));
		outR = RLPF.ar(outR, (20000/(1 + (0.1 * dR.max(distRef)))));

		// ~ -3 dB per doubling
		outL = outL * (1.0 / dL.max(distRef));  // ~ -3 dB per doubling
		outR = outR * (1.0 / dR.max(distRef));  // ~ -3 dB per doubling


		// global trim
		outL = outL * outputGainDb.dbamp;
		outR = outR * outputGainDb.dbamp;

		^[outL, outR];
	}
}
