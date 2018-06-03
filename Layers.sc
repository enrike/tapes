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

	var server, <path, <bufs, <sfs, <ps, <howmany=24;

	*new {| server = nil, path = "~/", num = 24 |
		^super.new.initLayers( server, path, num );
	}

	initLayers {| aserver, apath, anum |
		server = aserver;
		path = apath;
		howmany = anum;

		("path is"+apath).postln;
		("players:"+howmany.asString).postln;

		SynthDef( \StPlayer, { arg outbus=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=1, index=0;
			var length, left, right, phasor, dur; //offset;

			dur = BufFrames.kr(buffer);
			phasor = Phasor.ar( 0, rate, start*dur, end*dur);
			SendTrig.kr( LFPulse.kr(12, 0), index, phasor/dur); //fps 12
			#left, right = BufRd.ar( 2, buffer, phasor, 1 ) * amp;
			Out.ar(outbus, Balance2.ar(left, right, pan));
		}).load(server);


		SynthDef( \StPlayerOD, { arg outbus=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=1, index=0, a=1.4, b=0.3;
			var length, left, right, phasor, dur; //offset;
			dur = BufFrames.kr(buffer);

			rate = Henon2DC.kr(200, 2000, LFNoise2.kr(1, 0.1, 1.3), 0.3);
			phasor = Phasor.ar( 0, rate, start*dur, end*dur);
			#left, right = BufRd.ar( 2, buffer, phasor, 1 ) * amp;
			Out.ar(outbus, Balance2.ar(left, right, pan));
		}).load(server);

		SynthDef(\HPF, {|in=10, out=0, cut=100|
			var signal;
			signal = In.ar(in);
			signal  = HPF.ar(signal, cut);
			Out(out, signal);
		}).load(server);

		sfs = List.newUsing( SoundFile.collect( path ) );
		this.free;
		bufs = Array.new(howmany);

		// load buffers
		howmany.do({ arg n;
			var buf = Buffer.read(server, sfs.wrapAt(n).path);
			bufs = bufs.add( buf )
		});

		(sfs.size + "buffers available").postln;
		"...loading...".postln;

		{
			ps.do({arg pl; pl.free}); // kill everyone first
			ps = Array.new(sfs.size);

			howmany.do({arg index;
				ps = ps.add( Layer.new(index, bufs) );
			});
		}.defer(4)
	}

	search {|st|
		var positives=[];
		ps.do({ |pl|
			if (pl.search(st), {
				positives = positives.add(pl) // append
			});
		})
		^positives
	}

	free {
		bufs.do({arg buf; buf.free}); // clear all first
	}

	allbufs{
		"-- Available buffers --".postln;
		sfs.size.do({|i|
			(i.asString++": ").post;
			sfs[i].path.split($/).last.postln;
		})
	}

	curbufs {
		ps.size.do({ |i|
			(i.asString++":" + ps[i].file).postln
		})
	}

	normalize {
		bufs.do({arg buf; buf.normalize});
	}

	/*positions {|positions|
		positions.size.do({|index|
			ps[index].pos(positions[index])
		})
	}*/

	newplayer {|asynth| ps.do({ |pl| pl.newplayer(asynth)}) }

	pos {|st, end|
		ps.do({ |pl| pl.pos(st,end)})
	}

	len {|ms| ps.do({ |pl| pl.len(ms)}) }

	resume { ps.do({ |pl| pl.resume}) }

	pause { ps.do({ |pl| pl.pause}) }

	push {|which| ps.do({ |pl| pl.push(which)}) } // if no which it appends to stack

	pop {|which| ps.do({ |pl| pl.pop(which)}) } // if no which it pops last one

	save { |filename| // save to a file current state dictionary. in the folder where the samples are
		var data;
		if (filename.isNil, {
			filename = Date.getDate.stamp++".states";
		}, {
			filename = filename.string++".states"}
		);

		data = Dictionary.new;

		ps.do({ |pl, index|
			data.put(\layer++index, pl.statesDic)
		});

		// open dialogue if no file path is provided
		//(path ++ "/" ++ filename).postln;
		data.writeArchive(path[..path.size-2] ++ "/" ++ filename);

	}

	load { // opn dialogue to load file with state dictionary
		FileDialog({ |path|
			var data = Object.readArchive(path[0]);
			if (data.isNil.not, {
				ps.do({ |pl, index|
					pl.statesDic = data[\layer++index];
					if (index==0, {
						"available states: ".postln;
						data[\layer++index].keys.do({|key, pos| [pos, key].postln})
					});
				});
			})
		}, fileMode:1)
	}


	rvol { ps.do({ |pl| pl.rvol}) }

	rpan { ps.do({ |pl| pl.rpan}) }

	rpos { ps.do({ |pl| pl.rpos}) }

	rst {|range=1, keeplen=1|
		ps.do({ |pl| pl.rst(range, keeplen)})
	}

	rend { ps.do({ |pl| pl.rend}) }

	rat { |rate| ps.do({ |pl|	pl.rat(rate)}) }

	rrate { ps.do({ |pl| pl.rrate}) }

	rbuf { ps.do({ |pl| pl.rbuf}) }

	vol { |vol| ps.do({ |pl| pl.volumen(vol) }) }

	vold { ps.do({ |pl| pl.vold}) }

	volu { ps.do({ |pl| pl.volu}) }

	outb {|bus| ps.do({ |pl| pl.volumen(bus) })} // sets synthdef out buf. used for manipulating the signal

	bpos {|range=0.01| ps.do({|pl| pl.bpos(range) }) }

	bvol {|range=0.01| ps.do({|pl| pl.bvol(range) }) }

	stopptask { ps.do({ |p| p.ptask.stop }) }
	stoprtask { ps.do({ |p| p.rtask.stop }) }
	stopvtask { ps.do({ |p| p.vtask.stop }) }

	brownpos {|step=0.01, sleep=5, dsync=0, delta=0|
		["brown POS", step, sleep, dsync, delta].postln;
		ps.do({ |pl| pl.brownpos(step, sleep, dsync, delta) })
	}

	brownvol {|step=0.01, sleep=5, dsync=0, delta=0|
		["brown VOL", step, sleep, dsync, delta].postln;
		ps.do({ |pl| pl.brownvol(step, sleep, dsync, delta) })
	}

	brownrate {|step=0.01, sleep=5, dsync=0, delta=0|
		["brown RATE", step, sleep, dsync, delta].postln;
		ps.do({ |pl| pl.brownrate(step, sleep, dsync, delta) })
	}

	sched {|sleep=5.0, function|
		var atask;
		if (sleep <= 0, {sleep = 0.01}); // limit

		atask = Task({
			inf.do({
				function.value();
				sleep.wait;
			});
		});

		atask.start;
		^atask;
	}
}
