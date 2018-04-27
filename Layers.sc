/* master of layers
*/

/*

( // to do: retrieve parameters from layers and store into a sc prefs file
var data = Dictionary.new;

//Array.fill(24, {[]});

p.size.do({arg index;
	data.put(index, pl.buf);

	pl.buf;
	pl.st;
	pl.end;
});

data.writeArchive(basepath ++ "/presets/" ++ filename);

)

//data = Object.readArchive(basepath ++ "/presets/" ++ menu.item);


*/

Layers{

	var server, <path, <bufs, <sfs, <pls, <howmany=24;

	*new {| aserver = nil, apath = nil, anum = 24 |
		^super.new.initLayers( aserver, apath, anum );
	}

	initLayers {| aserver, apath, anum |
		server = aserver;
		path = apath;
		howmany = anum;

		apath.postln;
		howmany.postln;

		SynthDef( \StPlayer, {
			arg outbus=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=1, index=0;
			var length, left, right, phasor, dur; //offset;

			dur = BufFrames.kr(buffer);
			phasor = Phasor.ar( 0, rate, start*dur, end*dur);
			SendTrig.kr( LFPulse.kr(12, 0), index, phasor/dur); //fps 12
			#left, right = BufRd.ar( 2, buffer, phasor, 1 ) * amp;
			Out.ar(outbus, Balance2.ar(left, right, pan));
		}).load(aserver);


		sfs = List.newUsing( SoundFile.collect( path ) );
		(sfs.size + "files imported").postln;

		bufs.do({arg buf; buf.free}); // clear all first
		bufs = Array.new(howmany);

		// load buffers
		howmany.do({ arg n;
			bufs.add( Buffer.read(server, sfs[n].path) )
		});

	}

	players {
		pls.do({arg pl; pl.free}); // kill everyone first
		pls = Array.new(sfs.size);

		howmany.do({arg index;
			pls.add( Layer.new(index, bufs) );
		});
	}

	resume { pls.do({ |pl| pl.resume}) }

	pause { pls.do({ |pl| pl.pause}) }

	rvol { pls.do({ |pl| pl.rvol}) }

	rpos { pls.do({ |pl| pl.rpos}) }

	rat { |rate| pls.do({ |pl|	pl.rat(rate)}) }

	rrate { pls.do({ |pl| pl.rrate}) }

	rbuf { pls.do({ |pl| pl.rbuf}) }

	vol { |vol| pls.do({ |pl|	pl.vol(vol)}) }

	vold { pls.do({ |pl| pl.vold}) }

	volu { pls.do({ |pl| pl.volu}) }

	brownpos {|step=0.01, sleep=5, dsync=0, delta=0|
		["brown POS", step, sleep, dsync, delta].postln;
		pls.do({ |pl| pl.brownpos(step, sleep, dsync, delta) })
	}

	brownvol {|step=0.01, sleep=5, dsync=0, delta=0|
		["brown VOL", step, sleep, dsync, delta].postln;
		pls.do({ |pl| pl.brownvol(step, sleep, dsync, delta) })
	}

	brownrate {|step=0.01, sleep=5, dsync=0, delta=0|
		["brown RATE", step, sleep, dsync, delta].postln;
		pls.do({ |pl| pl.brownrate(step, sleep, dsync, delta) })
	}
}