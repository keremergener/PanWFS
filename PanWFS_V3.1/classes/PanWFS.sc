// PanWFS SuperCollider class definitions.
//
// SPL CALIBRATION (see PanWFS_Calibrate below):
//   1. Play the calibration tone through the array in WFS render and read an SPL
//      meter on-axis at the reference line:
//        c = { PanWFS_Calibrate.ar(numChans: 32, speaker_distance: 0.1,
//                                  sourceDepth: 1.0, refRms: -20) }.play;
//   2. With the measured value, get K (printed to the post window):
//        k = PanWFS_Calibrate.k(measuredSpl: 78, refRms: -20,
//                               sourceDepth: 1.0, micDistance: 2.0);
//        c.free;
//   3. Drive sources with that K and an intended max SPL (full-scale -> maxSpl):
//        PanWFS_Monopole.ar(sig, x, z, 32, 0.1, 2, kFactor: k, maxSpl: 90);
//
// Generalised source levels (dB SPL @ 1 m, full-scale signal): whisper ~30,
// conversation ~60, raised voice ~75, trumpet (ff) ~90, power tool ~100,
// car horn ~110, jackhammer ~120.

PanWFS_Monopole : MultiOutUGen {
	*ar {
		arg
		input = 0.0,
		virtual_x = 0.0,
		virtual_z = 0.0,
		numChans = 8,
		speaker_distance = 0.125,
		zRef = 2,
		roomTemp = 20,
		humidity = 50,
		groundElevation = 0.0,
		max_delay = 0.5,
		lagTime = 0.02,
		kFactor = 0.0,   // system calibration K (dB SPL @ 1 m for 0 dBFS RMS); from PanWFS_Calibrate.k
		maxSpl = 0.0;    // intended SPL of a full-scale (0 dBFS) signal; kFactor==maxSpl => unity

		input = input.asArray.sum;
		numChans = numChans.asInteger.clip(1, 256);

		^this.multiNew(
			'audio',
			numChans,
			input,
			virtual_x,
			virtual_z,
			speaker_distance,
			zRef,
			roomTemp,
			humidity,
			groundElevation,
			max_delay,
			lagTime,
			kFactor,
			maxSpl
		)
	}

	init {
		arg numChannels ... theInputs;
		inputs = theInputs;
		^this.initOutputs(numChannels, rate)
	}
}

PanWFS_Cardioid : MultiOutUGen {
	*ar {
		arg
		input = 0.0,
		virtual_x = 0.0,
		virtual_z = 0.0,
		yaw = (-pi/2),
		numChans = 8,
		speaker_distance = 0.125,
		zRef = 2,
		roomTemp = 20,
		humidity = 50,
		groundElevation = 0.0,
		max_delay = 0.5,
		lagTime = 0.02,
		kFactor = 0.0,
		maxSpl = 0.0;

		input = input.asArray.sum;
		numChans = numChans.asInteger.clip(1, 256);

		^this.multiNew(
			'audio',
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
			max_delay,
			lagTime,
			kFactor,
			maxSpl
		)
	}

	init {
		arg numChannels ... theInputs;
		inputs = theInputs;
		^this.initOutputs(numChannels, rate)
	}
}

// Calibration helper. *ar renders pink noise (normalised to refRms dBFS) as an
// on-axis monopole through the array so it can be measured; *k turns a meter
// reading into the K factor (and prints it). Not a separate server plugin: it
// reuses the verified PanWFS_Monopole renderer at unity SPL gain.
PanWFS_Calibrate {
	*ar {
		arg
		numChans = 8,
		speaker_distance = 0.125,
		zRef = 2,
		sourceDepth = 1.0,      // cal source depth behind the array (m)
		refRms = -20,           // reference level of the pink noise (dBFS RMS)
		roomTemp = 20,
		humidity = 50,
		groundElevation = 0.0,
		max_delay = 0.5,
		lagTime = 0.02;

		var pink, meanSquare, sig;

		pink = PinkNoise.ar(1.0);
		// Normalise the generator to refRms (RMS) via a slow mean-square estimate.
		meanSquare = LPF.ar(pink * pink, 8.0).max(1e-6);
		sig = pink * (refRms.dbamp / meanSquare.sqrt);

		// Unity SPL gain (kFactor == maxSpl), so the measured SPL reflects the raw
		// rendering chain at the reference RMS.
		^PanWFS_Monopole.ar(
			sig, 0.0, sourceDepth, numChans, speaker_distance, zRef,
			roomTemp, humidity, groundElevation, max_delay, lagTime, 0.0, 0.0
		)
	}

	// K = SPL@1m for a 0 dBFS-RMS source. The cal source sits sourceDepth behind
	// the array and the mic micDistance in front, so source->mic R = depth + mic,
	// and the rendered virtual source follows 1/r: SPL@1m = measured + 20log10(R).
	*k {
		arg measuredSpl, refRms = -20, sourceDepth = 1.0, micDistance = 2.0;
		var r, k;
		r = (sourceDepth + micDistance).max(0.1);
		k = measuredSpl + (20 * r.log10) - refRms;
		("PanWFS_Calibrate: K = " ++ k.round(0.1).asString ++ " dB"
			++ "  (SPL @ 1 m for 0 dBFS RMS; R = " ++ r.round(0.01).asString ++ " m, ref "
			++ refRms.asString ++ " dBFS)").postln;
		^k
	}
}

PanWFS_Headphone : MultiOutUGen {
	*ar {
		arg input = 0.0,
		virtual_x = 0.0,
		virtual_z = 1.0,
		listener_x = 0.0,
		listener_z = -2.0,
		headyaw = 0.0,
		headWidth = 0.18,
		distRef = 1.0,
		lagTime = 0.02;

		input = input.asArray.sum;

		^this.multiNew(
			'audio',
			2,
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
	}

	init {
		arg numChannels ... theInputs;
		inputs = theInputs;
		^this.initOutputs(numChannels, rate)
	}
}

PanWFS_Binaural : MultiOutUGen {
	*ar {
		arg input = 0.0,
		virtual_x = 0.0,
		virtual_z = 1.0,
		listener_x = 0.0,
		listener_z = -2.0,
		hrtf = nil,
		headyaw = 0.0;

		var p;

		var headWidth, speedOfSound, maxDelay, lagTime, minDistance, distRef;
		var shadowFc, shadowRs, shadowDb;
		var lowShelfFc, lowShelfRs, nearLowBoostDb, nearRefDistance;
		var pinnaPeakFcFront, pinnaPeakFcSide, pinnaPeakRq, pinnaPeakDb;
		var notchFcFront, notchFcRear, notchRq, notchDb;
		var rearHiShelfFc, rearHiShelfRs, rearHiShelfDb;
		var outputGainDb;

		input = input.asArray.sum;

		p = hrtf ? ();

		headWidth      = p[\headWidth]      ? 0.18;
		speedOfSound   = p[\speedOfSound]   ? 343.0;
		maxDelay       = p[\maxDelay]       ? 0.05;
		lagTime        = p[\lagTime]        ? 0.02;
		minDistance    = p[\minDistance]    ? 0.125;
		distRef        = p[\distRef]        ? 1.0;

		shadowFc       = p[\shadowFc]       ? 3500;
		shadowRs       = p[\shadowRs]       ? 1.0;
		shadowDb       = p[\shadowDb]       ? (-10.0);

		lowShelfFc     = p[\lowShelfFc]     ? 250;
		lowShelfRs     = p[\lowShelfRs]     ? 1.0;
		nearLowBoostDb = p[\nearLowBoostDb] ? 2.0;
		nearRefDistance= p[\nearRefDistance]? 0.6;

		pinnaPeakFcFront = p[\pinnaPeakFcFront] ? 3200;
		pinnaPeakFcSide  = p[\pinnaPeakFcSide]  ? 4200;
		pinnaPeakRq      = p[\pinnaPeakRq]      ? 0.8;
		pinnaPeakDb      = p[\pinnaPeakDb]      ? 4.0;

		notchFcFront   = p[\notchFcFront]   ? 8500;
		notchFcRear    = p[\notchFcRear]    ? 6000;
		notchRq        = p[\notchRq]        ? 0.45;
		notchDb        = p[\notchDb]        ? (-8.0);

		rearHiShelfFc  = p[\rearHiShelfFc]  ? 5000;
		rearHiShelfRs  = p[\rearHiShelfRs]  ? 1.0;
		rearHiShelfDb  = p[\rearHiShelfDb]  ? (-4.0);

		outputGainDb   = p[\outputGainDb]   ? 0.0;

		^this.multiNew(
			'audio',
			2,
			input,
			virtual_x,
			virtual_z,
			listener_x,
			listener_z,
			headyaw,
			headWidth,
			speedOfSound,
			maxDelay,
			lagTime,
			minDistance,
			distRef,
			shadowFc,
			shadowRs,
			shadowDb,
			lowShelfFc,
			lowShelfRs,
			nearLowBoostDb,
			nearRefDistance,
			pinnaPeakFcFront,
			pinnaPeakFcSide,
			pinnaPeakRq,
			pinnaPeakDb,
			notchFcFront,
			notchFcRear,
			notchRq,
			notchDb,
			rearHiShelfFc,
			rearHiShelfRs,
			rearHiShelfDb,
			outputGainDb
		)
	}

	init {
		arg numChannels ... theInputs;
		inputs = theInputs;
		^this.initOutputs(numChannels, rate)
	}
}
