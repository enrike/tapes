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

	var server, <path, <bufs, <sfs, <ps, <howmany=24, <procs;
	var plotwin=nil, plotview, drawview, plotwinrefresh;

	*new {| server = nil, path = "~/", num = 24 |
		^super.new.initLayers( server, path, num );
	}

	initLayers {| aserver, apath, anum |
		var limit;
		server = aserver;
		path = apath;
		howmany = anum;

		procs = Dictionary.new; // stores all tasks

		("path is"+apath).postln;
		("players:"+howmany.asString).postln;

		SynthDef( \StPlayer, { arg out=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=1, index=0, trig=0, reset=0, gate=1, gdur=1;
			var length, left, right, phasor, dur, env, bus; //offset;

			dur = BufFrames.kr(buffer);
			phasor = Phasor.ar( trig, rate, start*dur, end*dur, resetPos: reset*dur);
			SendTrig.kr( LFPulse.kr(12, 0), index, phasor/dur); //fps 12
			env = EnvGen.kr(Env.asr(gdur,amp,gdur), gate);
			#left, right = BufRd.ar( 2, buffer, phasor, 1 ) * amp * env;
			Out.ar(out, Balance2.ar(left, right, pan));
		}).load;


		/*SynthDef( \StPlayerOD, { arg outbus=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=1, index=0, a=1.4, b=0.3;
			var length, left, right, phasor, dur; //offset;
			dur = BufFrames.kr(buffer);

			rate = Henon2DC.kr(200, 2000, LFNoise2.kr(1, 0.1, 1.3), 0.3);
			phasor = Phasor.ar( 0, rate, start*dur, end*dur);
			#left, right = BufRd.ar( 2, buffer, phasor, 1 ) * amp;
			Out.ar(outbus, Balance2.ar(left, right, pan));
		}).load(server);*/

		SynthDef(\HPF, {|in=0, out=0, cut=100|
			var signal;
			signal = In.ar(in);
			signal  = HPF.ar(signal, cut);
			Out(out, signal);
		}).load(server);

		/*SynthDef(\comp, {
			var lf, rg;
			lf = Compander.ar(10, 10);
			rg = Compander.ar(11, 11);
			Out.ar(0, [lf,rg]);
		});*/

		sfs = List.newUsing( SoundFile.collect( path ) );
		this.free;
		bufs = Array.new(sfs.size);

		// load ALL buffers
		sfs.size.do({ arg n;
			var buf = Buffer.read(server, sfs.wrapAt(n).path);
			bufs = bufs.add( buf )
		});

		(sfs.size + "buffers available").postln;
		"... loading ... please wait ...".postln;

		{
			ps.do({arg pl; pl.free}); // kill everyone first
			ps = Array.new(sfs.size);

			howmany.do({arg index;
				ps = ps.add( Layer.new(index, bufs.wrapAt(index)) ); // just get any
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

	setbuf {|buf| ps.do({ |pl| pl.setbuf(buf)})	}

	asignbufs { // asign buffers sequentially if more layers than buffers then wrap
		ps.do({ |pl, index|
			pl.setbuf( bufs.wrapAt(index))
		})
	}

	newplayer {|asynth| ps.do({ |pl| pl.newplayer(asynth)}) }

	bounds {|st, end|
		ps.do({ |pl| pl.bounds(st,end)})
	}

	len {|ms| ps.do({ |pl| pl.len(ms)}) }

	reset { ps.do({ |pl| pl.reset}) }

	resume { ps.do({ |pl| pl.resume}) }

	pause { ps.do({ |pl| pl.pause}) }

	jump {|point, offset=0|
		ps.do({ |pl|
			{ pl.jump(point) }.defer(offset.asFloat.rand)
		})
	}

	solo {|ly|
		ps.do({ |pl|
			if (pl!=ly, {pl.pause})
		});
	}

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

	/////
	rvol {|offset=0|
		ps.do({ |pl|
			{pl.rvol(1.0/ps.size)}.defer(offset.asFloat.rand)
		})
	}

	rpan {|range=1, offset=0|
		ps.do({ |pl|
			{pl.rpan(range)}.defer(offset.asFloat.rand)
		})
	}

	rjump {|offset=0|
		ps.do({ |pl|
			{pl.rjump}.defer(offset.asFloat.rand)
		})
	}

	rbounds {|offset=0|
		ps.do({ |pl|
			{pl.rbounds}.defer(offset.asFloat.rand)
		})
	}

	rst {|range=1, offset=0|
		ps.do({ |pl|
			{pl.rst(range)}.defer(offset.asFloat.rand)
		})
	}

	rend {|range=1, offset=0|
		ps.do({ |pl|
			{pl.rend(range)}.defer(offset.asFloat.rand)
		})
	}

	rlen {|range=0.5, offset=0|
		ps.do({ |pl|
			{pl.rlen(range)}.defer(offset.asFloat.rand)
		})
	}

	rat { |rate, offset=0|
		ps.do({ |pl|
			{pl.rat(rate)}.defer(offset.asFloat.rand)
		})
	}

	reverse {|offset=0| // TO DO!! apply offset to all functions. system to unsync the changes
		ps.do({ |pl|
			{pl.rat(pl.rate.neg)}.defer(offset.asFloat.rand)
		})
	}

	rdir {|offset=0|
		ps.do({ |pl|
			{pl.rdir}.defer(offset.asFloat.rand)
		})
	}

	rrate {|offset=0|
		ps.do({ |pl|
			{pl.rrate}.defer(offset.asFloat.rand)
		})
	}

	rbuf {|offset=0|
		ps.do({ |pl|
			{pl.setbuf(bufs.choose)}.defer(offset.asFloat.rand)
		})
	}

	//

	vol {|vol, offset=0|
		ps.do({ |pl|
			{pl.volume(vol)}.defer(offset.asFloat.rand)
		})
	}

	vold { ps.do({ |pl| pl.vold}) }

	volu { ps.do({ |pl| pl.volu}) }

	fadeout {|time=1, offset=0|
		ps.do({ |pl|
			{pl.fadeout(time)}.defer(offset.asFloat.rand)
		})
	}

	fadein {|time=1,offset=0|
		ps.do({ |pl|
			{pl.fadein(time)}.defer(offset.asFloat.rand)
		})
	}

	pan { |pan, offset=0|
		ps.do({ |pl|
			{pl.pan(pan)}.defer(offset.asFloat.rand)
		})
	}

	outb {|bus, offset=0|
		ps.do({ |pl|
			{pl.volumen(bus)}.defer(offset.asFloat.rand)
		})
	} // sets synthdef out buf. used for manipulating the signal

	//

	bbounds {|range=0.01, offset=0|
		ps.do({|pl|
			{pl.bbounds(range)}.defer(offset.asFloat.rand)
		})
	}

	bjump {|range=0.01, offset=0|
		ps.do({|pl|
			{pl.bjump(range)}.defer(offset.asFloat.rand)
		})
	}

	bvol {|range=0.01, offset=0|
		ps.do({|pl|
			{pl.bvol(range)}.defer(offset.asFloat.rand)
		})
	}

	brat {|range=0.01, offset=0|
		ps.do({|pl|
			{pl.brat(range)}.defer(offset.asFloat.rand)
		})
	}

	///
	/*
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
	*/
	///




	/////// task's stuff ////
	addTask {|name, task|
		("-- procs: adding"+name).postln;
		procs.add(name.asSymbol -> task);
	}

	stopAll{
		procs.do({|pro| pro.stop});
		procs = Dictionary.new;
	}

	stopT {|name|
		("-- procs: killing"+name).postln;
		procs[name].stop;
		procs.removeAt(name);
	}
	resumeT {|name| procs[name].resume}
	pauseT {|name| procs[name].pause}



	sch {|name="", function, sleep=5.0, offset=0| // off set is passed to functions so that localy the events are not at the same time
		var atask;

		if (name=="", {"TASKS MUST HAVE A NAME".postln; ^false}); // TO DO: must be a string or symbol. sanitize

		if (procs[name].isNil.not, { this.stopT(name) }); // kill if already there before rebirth

		if (sleep <= 0, {sleep = 0.01}); // force lower limit to task tick resolution

		atask = Task({
			inf.do({|index|
				var time = ""+Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second;
				function.value(offset:offset); //CHECK: somehow the offsets gets added after the provided args
				if( (name != ""), {("-- now:"+name++time).postln});
				sleep.wait;
			});
		});

		atask.start;
		this.addTask(name, atask) // to keep track of them
		//^atask;
	}

	gui {|test| // TO DO: a gui to see all layers

	}

	// TO DO: this needs to check for curpos in each layer and draw the position. 12fps
	plot {|abuf|
		if (plotwin.isNil, {
			// to do: bigger size win and view
			// move playhead as it plays?
			plotwin = Window("All players", Rect(100, 200, 600, 300));
			plotwin.alwaysOnTop=true;
			//plotwin.setSelectionColor(0, Color.red);
			plotwin.front;
			plotwin.onClose = {
				plotwinrefresh.stop;
				plotwin = nil;
			}; // needed?

			plotview = SoundFileView(plotwin, Rect(0, 0, 600, 300))
			.elasticMode_(true)
			.timeCursorOn_(true)
			.timeCursorColor_(Color.red)
			.drawsWaveForm_(true)
			.gridOn_(true)
			.gridResolution_(1)
			.gridColor_(Color.white)
			.waveColors_([ Color.new255(103, 148, 103), Color.new255(103, 148, 103) ])
			.background_(Color.new255(155, 205, 155))
			.canFocus_(false)
			.setSelectionColor(0, Color.grey);
			/*plotview.mouseUpAction = {
				var cs = plotview.selections[plotview.currentSelection];
				st = (cs[0]/buf.numFrames)/buf.numChannels;
				end = ((cs[0]+cs[1])/buf.numFrames)/buf.numChannels; //because view wants start and duration
				play.set(\start, st);
				play.set(\end, end);
				["new loop:", st, end].postln;
			};*/

			drawview = UserView(plotwin, Rect(0, 0, 600, 300));
			drawview.drawFunc ={ arg view; // AND CAN DRAW AS WELL
				ps.do({|ps, index|
					//ps.curpos.postln;
					// TO DO. must scale 0-1 to
					Pen.line( ps.curpos*600 @ 0, ps.curpos*600 @ 300 );
				});
				Pen.stroke;
			};

			plotwinrefresh = Task({
				inf.do({|index|
					plotwin.refresh;
					drawview.refresh;
					0.1.wait;
				})
			}, AppClock);
			plotwinrefresh.start;

			"To zoom in/out: Shift + right-click + mouse-up/down".postln;
			"To scroll: right-click + mouse-left/right".postln;
		});
		this.updateplot(abuf); // draw the data and refresh
	}

	updateplot {|buf| // draw the choosen buffer
		if (plotwin.isNil.not, {
			var f = { |b,v| b.loadToFloatArray(action: { |a| { v.setData(a) }.defer }) };

			// TO DO: thiss does not work for some reason. maybe something to do with the supercollider version
			/*{
				plotview.timeCursorOn = true;
				plotview.setSelectionStart(0, (buf.numFrames*buf.numChannels) * st); // loop the selection
				plotview.setSelectionSize(0, (buf.numFrames*buf.numChannels) * (end-st));
				plotview.readSelection.refresh;
			}.defer;*/

			if (buf.isNil.not, {f.(buf, plotview)}); // only if a buf is provided
		});
	}

}

