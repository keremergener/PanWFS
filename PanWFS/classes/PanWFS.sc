PanWFS_Monopole : MultiOutUGen {
	*ar {
		arg input = 0.0,
		virtual_x = 0.0,
		virtual_z = 0.0,
		numChans = 8,
		speaker_distance = 0.125,
		zRef = 2,
		roomTemp = 20,
		humidity = 50,
		groundElevation = 0.0,
		max_delay = 0.5;

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
			max_delay
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
		arg input = 0.0,
		virtual_x = 0.0,
		virtual_z = 0.0,
		yaw = (-pi/2),
		numChans = 8,
		speaker_distance = 0.125,
		zRef = 2,
		roomTemp = 20,
		humidity = 50,
		groundElevation = 0.0,
		max_delay = 0.5;

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
			max_delay
		)
	}

	init {
		arg numChannels ... theInputs;
		inputs = theInputs;
		^this.initOutputs(numChannels, rate)
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
		distRef = 1.0;

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
			distRef
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
