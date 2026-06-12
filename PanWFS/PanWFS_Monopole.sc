// PanWFS
// Copyright (c) 2026 Kerem Ergener. All rights reserved.
// Licensed under the PanWFS Source-Available License.
// See LICENSE for details.

// Disclaimer
// PanWFS is experimental research software for spatial audio and Wave Field Synthesis workflows. Users are responsible for testing patches, gain staging, loudspeaker routing, and output levels before use in studio, installation, or performance contexts. The author is not responsible for speaker damage, hearing damage, data loss, failed performances, incorrect spatial rendering, or any other damages resulting from the use or misuse of this software.

PanWFS_Monopole {
	*ar{
		arg input, virtual_x = 0.0, virtual_z = 0.0, number_of_speakers = 8, speaker_distance = 0.125, zRef = 2, roomTemp = 20, humidity = 50, groundElevation = 0.0 , max_delay = 0.5;

		var sig, amp, speakerX, speakerZ, speaker_dist, time_delay, dxs, dzs, cosPhi, deltaR, zGain,amp2D, amp3D, mode, speedOfSound, pa, offset;

		sig = input;

		virtual_x = virtual_x - (number_of_speakers * speaker_distance * 0.25);


		humidity = humidity/100; //humidity normalizer
		pa =  (101325 * ((1 - (2.25577e-5 * groundElevation)).pow(5.2559))); //barometric pressure calculation

		speedOfSound = ((1.4 * 287.05 * (roomTemp + 273.15) * (1.0 + ((0.61 * 0.62198) * ((humidity * (611.2 * exp(((17.67 * roomTemp) / (roomTemp + 243.5))))) / (pa - (humidity * (611.2 * exp(((17.67 * roomTemp) / (roomTemp + 243.5))))))))))).sqrt;

		virtual_z = virtual_z.neg; //correction of values for Z for positive values to represent values behind the speaker

		speakerX=Array.fill(number_of_speakers, { |i| (i - (number_of_speakers-1)/2) * speaker_distance }); //array of the speaker location on the X axis
		speakerZ = Array.fill(number_of_speakers, { 0.0 });    // speaker line at z=0
		dxs = speakerX.collect { |x| x - virtual_x };
		dzs = speakerZ.collect { |z| z - virtual_z };        // = -zs for all
		speaker_dist = (dxs.squared + dzs.squared).sqrt;    // source to speaker
		speaker_dist = speaker_dist;
		cosPhi = dzs.neg / speaker_dist;                   // geometry of wave propagation - cos φ = (zs - 0)/r = -dz/r
		deltaR = (zRef - speakerZ).abs;                   // speaker to reference line distance

		mode = Select.kr(virtual_z >= 0, [0, 1]);      // select the mode - minus values (hence the .neg non focus mode)

		// zGain = deltaR / (speaker_dist + deltaR) //nonfocusmode;
		// zGain = deltaR / ((deltaR - speaker_dist).abs.max(1e-6)) //focusmode;

		zGain = Select.kr(mode, [deltaR / (speaker_dist + deltaR), deltaR / ((deltaR - speaker_dist).abs.max(1e-6))]); // zGain selector according to distance

		amp = (cosPhi / speaker_dist) * zGain * (1.0 / (2*pi)); //first amp calculation

		time_delay = (speaker_dist / speedOfSound); //time delay calculation
		amp3D = (1.0 / speaker_dist); // ~ -6 dB per doubling
		amp2D = (1.0 / speaker_dist.sqrt);  // ~ -3 dB per doubling

		sig = HPZ1.ar(sig) * (SampleRate.ir / (2pi*speedOfSound)); //jk
		sig = HPF.ar(sig, 30);            // tame DC/infra
		sig = BHiShelf.ar(sig, 1000, 0.5, 0.7); //Compensate real-world HF loss
		sig = DelayL.ar(sig, max_delay, time_delay); // set max delay >= max(r/c)
		sig = sig*amp; //final amplitude calculation before
		sig = sig*amp2D;
		sig = RLPF.ar(sig, (20000/(1 + (0.1 * speaker_dist)))); //frequency roll-off

		^sig;

	}

}
