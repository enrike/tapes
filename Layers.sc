/* master of layers
*/


Layers{

	var server, <path, <bufs, <sfs, <ps, <howmany=24, <procs;
	var plotwin=nil, plotview, drawview, plotwinrefresh;
	var buses, <compressor;
	var volume=1;

	*new {| path = "~/", num = 24 |
		^super.new.initLayers( path, num );
	}

	initLayers {| apath, anum |
		var limit;
		server = Server.default;
		path = apath;
		howmany = anum;

		procs = Dictionary.new; // stores all tasks

		("path is"+apath).postln;
		("players:"+howmany.asString).postln;

		SynthDef( \StPlayer, { arg out=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=0, index=0, trig=0, reset=0,
			ampgate=0, ampdur=0, amptarget=1, ampcur=nil,
			rategate=0, ratedur=0, ratetarget=1, ratecur=nil,
			pangate=0, pandur=0, pantarget=1, pancur=nil;

			var length, left, right, phasor, dur, env;

			rate = EnvGen.kr(Env.new(levels: [ rate, ratetarget ], times: [ ratedur ], curve: ratecur), rategate);
			env = EnvGen.kr(Env.new(levels: [ amp, amptarget ], times: [ ampdur ], curve: ampcur), ampgate);
			pan = EnvGen.kr(Env.new(levels: [ pan, pantarget ], times: [ pandur ], curve: pancur), pangate);

			dur = BufFrames.kr(buffer);
			phasor = Phasor.ar( trig, rate * BufRateScale.kr(buffer), start*dur, end*dur, resetPos: reset*dur);
			SendTrig.kr( LFPulse.kr(12, 0), index, phasor/dur); //fps 12

			#left, right = BufRd.ar( 2, buffer, phasor, 1 ) * amp * env;
			Out.ar(out, Balance2.ar(left, right, pan));
		}).load;

		//Compander.ar(in: 0.0, control: 0.0, thresh: 0.5, slopeBelow: 1.0, slopeAbove: 1.0, clampTime: 0.01, relaxTime: 0.1, mul: 1.0, add: 0.0)
		SynthDef(\comp, {|inbus=0, thr=0.5, slb=0.9, sla=0.5|
			var signal = Compander.ar(In.ar(inbus, 2), In.ar(inbus, 2), thr, slopeBelow:slb, slopeAbove:sla);
			Out.ar(0, signal);
		}).load(server);

		this.free;

		if (PathName.new(path).isFile, {
			sfs = List.newUsing( [SoundFile(path)] );
		}, {
			sfs = List.newUsing( SoundFile.collect( path++"*") ); // if a folder apply wildcards
		});

		bufs = Array.new(sfs.size);
		// load ALL buffers
		sfs.size.do({ arg n;
			var buf = Buffer.read(server, sfs.wrapAt(n).path);
			bufs = bufs.add( buf )
		});

		(sfs.size + "buffers available").postln;
		"... loading ... please wait ...".postln;

		buses = Bus.audio(server, 2);
		compressor = Synth(\comp, [\inbus, buses]);

		{
			ps.do({arg pl; pl.free}); // kill everyone first
			ps = Array.new(sfs.size);

			howmany.do({arg index;
				ps = ps.add( Layer.new(index, bufs.wrapAt(index), buses));
			});

		}.defer(4)
	}

	// TODO: a function to load all buffers from a directory
	loadall {|path| // flag to overwrite or add to existing array???
		sfs = List.newUsing( SoundFile.collect( path ) );
		//sfs.clear; // delete all items
		this.free;
		bufs = Array.new(sfs.size);

		// load ALL buffers
		sfs.size.do({ arg n;
			var buf = Buffer.read(server, sfs.wrapAt(n).path);
			bufs = bufs.add( buf )
		});

		(sfs.size + "buffers available").postln;
		"... loading ... please wait ...".postln;
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

	some {|howmany|
		if (howmany.isNil, {howmany=ps.size.rand});
		^ps.scramble[0..howmany-1];
	}

	free {
		bufs.do({|buf| buf.free}); // clear all first
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

	info {
		ps.do({ |p| p.info })
	}

	verbose {|flag=true|
		["verbose:", flag].postln;
		ps.do({ |p| p.verbose = flag })
	}

	normalize {
		bufs.do({|buf| buf.normalize});
	}

	/*positions {|positions|
		positions.size.do({|index|
			ps[index].pos(positions[index])
		})
	}*/

	setbuf {|buf, offset=0|
		ps.do({ |pl|
			{ pl.setbuf(buf) }.defer(offset.asFloat.rand)
		})
	}

	asignbufs { // asign buffers sequentially if more layers than buffers then wrap
		ps.do({ |pl, index|
			pl.setbuf( bufs.wrapAt(index))
		})
	}

	newplayer {|asynth| ps.do({ |pl| pl.newplayer(asynth)}) }

	bounds {|st=0, end=1, offset=0|
		ps.do({ |pl|
			{ pl.bounds(st,end) }.defer(offset.asFloat.rand)
		})
	}

	len {|ms| ps.do({ |pl| pl.len(ms)}) }

	reset { |offset=0|
		ps.do({ |pl|
			{ pl.reset }.defer(offset.asFloat.rand)

		})
	}

	resume { ps.do({ |pl| pl.resume}) }

	pause { ps.do({ |pl| pl.pause}) }

	jump {|point=0, offset=0|
		ps.do({ |pl|
			{ pl.jump(point) }.defer(offset.asFloat.rand)
		})
	}

	solo {|ly|
		ps.do({ |pl|
			if (pl!=ly, {pl.pause}) // pause if not me
		});
	}

	push {|which| ps.do({ |pl| pl.push(which)}) } // if no which it appends to stack

	pop {|which| ps.do({ |pl| pl.pop(which)}) } // if no which it pops last one

	save { |filename| // save to a file current state dictionary. in the folder where the samples are
		var data;
		if (filename.isNil, {
			filename = Date.getDate.stamp++".states";
		}, {
			filename = filename.asString++".states"}
		);

		data = Dictionary.new;

		ps.do({ |pl, index|
			data.put(\layer++index, pl.statesDic)
		});

		// open dialogue if no file path is provided
		("saving" + path ++ filename).postln;
		data.writeArchive(path ++ filename);
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

	//random file, pan, vol, rate, bounds (st, end), dir and jump
	rand {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			ps.do({ |pl| pl.vol(0)});// mute. necessary?
			this.rbuf(offset);
			this.rvol(time, curve, offset);
			this.rpan(time, curve, offset);
			this.rrat(time, curve, offset);//??
			//this.rdir(time, curve, offset); / not needed
			this.rbounds(offset);
			this.rjump;// this should be limited to the current bounds
			ps.do({ |pl| pl.vol(pl.volume)}); //restore
		})
	}


	rvol {|time=0, curve=\exp, offset=0|
		ps.do({ |pl|
			{pl.rvol(1.0/ps.size)}.defer(offset.asFloat.rand)
		})
	}

	rpan {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rpan(time, curve)}.defer(offset.asFloat.rand)
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

	rat { |rate, time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rat(rate, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	reverse {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rat(pl.rate.neg, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	gofwd {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.gofwd(time, curve)}.defer(offset.asFloat.rand)
		})
	}

	gobwd {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.gobwd(time, curve)}.defer(offset.asFloat.rand)
		})
	}

	rdir {|curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rdir}.defer(offset.asFloat.rand)
		})
	}

	rrat {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rrat(time, curve)}.defer(offset.asFloat.rand)
		})
	}

	rbuf {|offset=0|
		ps.do({ |pl|
			{pl.setbuf(bufs.choose)}.defer(offset.asFloat.rand)
		})
	}

	//

	vol {|avol=1, time=0, curve=\exp, offset=0|
		volume = avol; // remember for the fadein/out
		ps.do({ |pl|
			{pl.vol(volume, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	vold { ps.do({ |pl| pl.vold}) }

	volu { ps.do({ |pl| pl.volu}) }

	fadeout {|time=1, curve=\exp, offset=0|
		ps.do({ |pl|
			{pl.vol(0, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	fadein {|time=1, curve=\exp, offset=0|
		ps.do({ |pl|
			{pl.vol(volume, time, curve)}.defer(offset.asFloat.rand) // fade in to volume. not to 1
		})
	}

	pan { |pan=0, time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.pan(pan, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	outb {|bus, offset=0|
		ps.do({ |pl|
			{pl.outb(bus)}.defer(offset.asFloat.rand)
		})
	} // sets synthdef out buf. used for manipulating the signal w effects

	//

	bbounds {|range=0.01, time=0, curve=\lin, offset=0|
		ps.do({|pl|
			{pl.bbounds(range, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	bjump {|range=0.01, time=0, curve=\lin, offset=0|
		ps.do({|pl|
			{pl.bjump(range, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	bvol {|range=0.01, time=0, curve=\lin, offset=0|
		ps.do({|pl|
			{pl.bvol(range, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	bpan {|range=0.1, time=0, curve=\lin, offset=0|
		ps.do({|pl|
			{pl.bpan(range, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	brat {|range=0.01, time=0, curve=\lin, offset=0|
		ps.do({|pl|
			{pl.brat(range, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	/////// task's stuff ////
	addTask {|name, task|
		("-- procs: adding"+name).postln;
		procs.add(name.asSymbol -> task);
	}

	noT {
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

	sch {|name="", function, sleep=5.0, offset=0| // offset is passed to functions so that local events are not at the same time
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
	}

	gui {|test| // TO DO: a gui to see all layers

	}

	////////////////////////////



	// compressor/expander ///
	comp{|thr=0.5, sla=1, slb=1| // threshold, slopeBelow, slopeAbove
		compressor.set(\thr, thr);
		compressor.set(\sla, sla);
		compressor.set(\slb, slb)
	}
	thr{|val=0.5| compressor.set(\thr, val)}
	sla{|val=1| compressor.set(\sla, val)}
	slb{|val=1| compressor.set(\slb, val)}
	nocomp{this.comp(0.5,1,1)} // reset
	/////////////////


	plot {|abuf|
		//if (plotwin.isNil.not, {plotwin.close});
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
			.gridResolution_(10)
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
			var f = { |b,v|
				b.loadToFloatArray(action: { |a| { v.setData(a) }.defer });
				v.gridResolution(b.duration/10); // I would like to divide the window in 10 parts no matter what the sound dur is. Cannot change gridRes on the fly?
			};

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

