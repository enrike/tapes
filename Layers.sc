/* master of layers
*/


Layers{

	var server, <path, <bufs, <sfs, <ps, <howmany=24, <procs;
	var plotwin=nil, plotview, drawview, plotwinrefresh;
	var controlGUI, views;
	var buses, <compressor;
	var volume=1;

	*new {| main=nil, symbol="@" |
		^super.new.initLayers( main, symbol );
	}

	initLayers {| amain, asym |
		~layers = this; // keep me in a global

		procs = Dictionary.new; // stores all tasks

		this.boot;

		if (amain.isNil.not, {this.lang(amain, asym)});// preprocessor
	}


	lang {|main, sym|
		var layervar = "~layers";
		// this is to be able to use the systems using a symbol (like @) instead of ~layers. and symbol2 (eg @2) instead of ~layers.ps[2]
		main.preProcessor = { |code|
			// list with all the commands used by layers
			var mets = [
				"do", "asignbufs", "loadfiles", "bufs", "buf", "curbufs", "all", "one", "some", "info", "verbose", "normalize", "plot", "sch",
				"scratch", "pause", "solo", "fwd", "bwd", "reverse", "volu", "vold", "vol", "fadein", "fadeout", "pan", "rate", "reset", "resume", "shot",
				"lp", "loop", "st", "step", "move", "end", "go", "dur", "len",
				"push", "pop", "save", "load", "control", "search",
				"rbuf", "rrate", "rpan", "rloop", "rdir", "rvol", "rgo", "rst", "rend", "rlen", "rand",
				"bloop", "bpan", "brate", "bvol", "bpan", "bgo",
				"comp", "thr", "slb", "sla",
				"pauseT", "resumeT", "stopT", "noT", "procs"];

			mets.do({|met| // @go --> ~layers.go
				code = code.replace(sym++met, layervar++"."++met);
			});

			100.reverseDo({|num| // reverse to avoid errors with index > 1 digit
				var dest = layervar++".ps["+num.asString+"]";
				code = code.replace(sym++num.asString, dest); // @2 --> ~layers.ps[2]
			});

			code = code.replace("", ""); // THIS MUST BE HERE OTHERWISE THERE IS SOME WEIRD BUG
		};
	}

	boot {
		server = Server.default;
		server.waitForBoot({

			SynthDef( \StPlayer, { arg out=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=0, index=0, trig=0, reset=0, loop=1,
				ampgate=0, ampdur=0, amptarget=1, ampcur=nil,
				rategate=0, ratedur=0, ratetarget=1, ratecur=nil,
				pangate=0, pandur=0, pantarget=1, pancur=nil;

				var length, left, right, phasor, dur, env;

				rate = EnvGen.kr(Env.new(levels: [ rate, ratetarget ], times: [ ratedur ], curve: ratecur), rategate);
				env = EnvGen.kr(Env.new(levels: [ amp, amptarget ], times: [ ampdur ], curve: ampcur), ampgate);
				pan = EnvGen.kr(Env.new(levels: [ pan, pantarget ], times: [ pandur ], curve: pancur), pangate);

				dur = BufFrames.kr(buffer);
				phasor = Phasor.ar( trig, rate * BufRateScale.kr(buffer), start*dur, end*dur, resetPos: reset*dur);
				SendTrig.ar(HPZ1.ar(HPZ1.ar(phasor).sign), index, 1); //loop
				SendTrig.kr( LFPulse.kr(12, 0), index, phasor/dur); //fps 12

				#left, right = BufRd.ar( 2, buffer, phasor, loop:loop ) * amp * env;
				Out.ar(out, Balance2.ar(left, right, pan));
			}).load;


			SynthDef( \ShotPlayer, {|out, buffer=0, amp=1, pan=0, start=0, end=1, rate=1, index=0|
				var left, right;
				#left, right = BufRd.ar(2, buffer,
					Line.ar(start: BufFrames.kr(buffer) * start,
						end: BufFrames.kr(buffer) * end,
						dur: BufDur.kr(buffer) * (end-start) / rate, // change this to set rate
						doneAction: {SendTrig.kr( Impulse.kr(1), index, 1); Done.freeSelf})
				) ;
				Out.ar(out, Balance2.ar(left, right, pan) * amp);
			}).load;


			SynthDef(\rev, {|inbus=0, out=0, mix= 0.33, room= 0.5, damp= 0.5|
				// FreeVerb2.ar(in, in2, mix: 0.33, room: 0.5, damp: 0.5, mul: 1.0, add: 0.0)
				var signal = In.ar(inbus, 2);
				signal = FreeVerb2.ar(signal[0], signal[1], mix, room, damp);
				Out.ar(out, signal);
			}).load;

			buses = Bus.audio(server, 2);
			compressor = Synth(\comp, [\inbus, buses]);
		})
	}

	loadfiles {|apath="~/"|
		path = apath;
		("path is"+apath).postln;

		if (PathName.new(apath).isFile, {
			sfs = List.newUsing( [SoundFile(apath)] );
		}, {
			sfs = List.newUsing( SoundFile.collect( apath++"*") ); // if a folder apply wildcards
		});

		bufs = Array.new(sfs.size);// all files in the dir

		// load ALL buffers
		sfs.size.do({ arg n;
			var buf = Buffer.read(server, sfs.wrapAt(n).path);
			bufs = bufs.add( buf )
		});

		(sfs.size + "buffers available").postln;
		"... loading sound files ...".postln;
	}

	do {|anum=6|
		howmany = anum;
		("creating players:"+howmany.asString).postln;

		views = Array.fill(howmany, {0});

		ps.collect(_.free);
		ps = Array.new(anum);//sfs.size);

		howmany.do({arg index;
			ps = ps.add( Layer.new(index, bufs.wrapAt(index), buses));
		});
	}

	all {^ps}

	search {|st|
		var positives=[];
		ps.do({ |pl|
			if (pl.search(st), {
				positives = positives.add(pl) // append
			});
		})
		^positives
	}

	one {
		^ps.choose;
	}

	some {|howmany|
		if (howmany.isNil, {howmany=ps.size.rand});
		^ps.scramble[0..howmany-1];
	}

	free { bufs.collect(_.free) }

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

	info { ps.collect(_.info) }

	verbose {|flag=true|
		["verbose:", flag].postln;
		ps.do({ |p| p.verbose = flag })
	}

	normalize {
		bufs.collect(_.normalize);
	}

	/*positions {|positions|
	positions.size.do({|index|
	ps[index].pos(positions[index])
	})
	}*/

	buf {|buf, offset=0|
		if (buf.isInteger, {buf = bufs[buf]}); // using the index

		ps.do({ |pl, index|
			{
				pl.buf(buf);
				this.newplotdata(buf, views[index]);
			}.defer(offset.asFloat.rand)
		})
	}

	asignbufs { // asign buffers sequentially if more layers than buffers then wrap
		ps.do({ |pl, index|
			pl.buf( bufs.wrapAt(index))
		})
	}

	newplayer {|asynth| ps.do({ |pl| pl.newplayer(asynth)}) }

	// limits, bounds, points, hoop, ring, rim, roll
	/*	lp {|p, offset=0| // SCLANG DOES NOT LIKE THAT WE USE THE NAME "LOOP" FOR OUR METHOD. change
	if (p.isNil, {p=[0,1]});
	ps.do({ |pl, index|
	{
	pl.loop(p[0], p[1]); // this must change NAME as well
	this.newselection(p[0], p[1], views[index], pl.buf);
	}.defer(offset.asFloat.rand)
	})
	}*/

	step {|gap, offset=0|
		ps.do({ |pl, index|
			{
				pl.loop(pl.st + gap);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	lp {|st, end, shift, grainshift, offset=0| // SCLANG DOES NOT LIKE THAT WE USE THE NAME "LOOP" FOR OUR METHOD. change
		if (st.isNil, {st=0; end=1}); // reset

		ps.do({ |pl, index|
			{
				pl.loop(st, end);
				this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	st {|pos=0, offset=0|
		ps.do({ |pl, index|
			{
				pl.st(pos);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	end {|pos=1, offset=0|
		ps.do({ |pl, index|
			{
				pl.end(pos);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	dur {|val, random=0, offset=0|
		ps.do({ |pl, index|
			{
				pl.dur(val, random);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	len {|ms| ps.do({ |pl| pl.len(ms)}) } // in msecs

	reset { |offset=0|
		ps.do({ |pl|
			{ pl.reset }.defer(offset.asFloat.rand)
		})
	}

	shot { ps.collect(_.shot) } // single play no loop

	resume { ps.collect(_.resume) }

	pause { ps.collect(_.pause) }

	go {|point=0, offset=0|
		ps.do({ |pl|
			{ pl.go(point) }.defer(offset.asFloat.rand)
		})
	}

	move {|pos, offset=0|
		ps.do({ |pl|
			{ pl.move(pos) }.defer(offset.asFloat.rand)
		})
	}

	solo {|id|
		ps.do({ |pl|
			if (pl.id!=id, {
				if (pl.rate!=0, {pl.pause}); // if not already paused, pause.
			}, {
				pl.resume;
			})
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

	//random file, pan, vol, rate, loop (st, end), dir and go
	rand {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			ps.do({ |pl| pl.vol(0)});// mute. necessary?
			this.rbuf(offset);
			this.rvol(time, curve, offset);
			this.rpan(time, curve, offset);
			this.rrate(time, curve, offset);//??
			//this.rdir(time, curve, offset); / not needed
			this.rloop(offset);
			this.rgo;// this should be limited to the current loop
			ps.do({ |pl| pl.vol(pl.vol)}); //restore
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

	rgo {|offset=0|
		ps.do({ |pl|
			{pl.rgo}.defer(offset.asFloat.rand)
		})
	}

	rloop {|offset=0|
		ps.do({ |pl, index|
			{
				pl.rloop;
				this.newselection(pl.st, pl.end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
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

	rate { |rate=1, time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rate(rate, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	reverse {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rate(pl.rate.neg, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	scratch {|target=0, tIn=1, tStay=0.5, tOut=1, curve=\lin, offset=0| // boomerang like pitch change
		ps.do({ |pl|
			{pl.mirror(target, tIn, tStay, tOut, curve)}.defer(offset.asFloat.rand)
		})
	}

	fwd {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.fwd(time, curve)}.defer(offset.asFloat.rand)
		})
	}

	bwd {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.bwd(time, curve)}.defer(offset.asFloat.rand)
		})
	}

	rdir {|curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rdir}.defer(offset.asFloat.rand)
		})
	}

	rrate {|time=0, curve=\lin, offset=0|
		ps.do({ |pl|
			{pl.rrate(time, curve)}.defer(offset.asFloat.rand)
		})
	}

	rbuf {|offset=0|
		ps.do({ |pl, index|
			{
				pl.buf(bufs.choose);
				this.newplotdata(pl.buf, views[index]);
			}.defer(offset.asFloat.rand)
		})
	}

	//

	vol {|avol=1, time=0, curve=\exp, offset=0|
		volume = avol; // remember for the fadein/out
		ps.do({ |pl|
			{pl.vol(volume, time, curve)}.defer(offset.asFloat.rand)
		});
		["set vol", avol].postln
	}

	vold { ps.collect(_.vold) }

	volu { ps.collect(_.volu) }

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

	bloop {|range=0.01, time=0, curve=\lin, offset=0|
		ps.do({|pl, index|
			{
				pl.bloop(range, time, curve);
				this.newselection(pl.st, pl.end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	bgo {|range=0.01, time=0, curve=\lin, offset=0|
		ps.do({|pl|
			{pl.bgo(range, time, curve)}.defer(offset.asFloat.rand)
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

	brate {|range=0.01, time=0, curve=\lin, offset=0|
		ps.do({|pl|
			{pl.brate(range, time, curve)}.defer(offset.asFloat.rand)
		})
	}

	/////// task's stuff ////
	noT {
		procs.collect(_.stop);
		procs = Dictionary.new;
	}

	stopT {|name|
		("-- procs: killing"+name).postln;
		procs[name.asSymbol].stop;
		procs.removeAt(name.asSymbol);
	}
	resumeT {|name| procs[name.asSymbol].resume}
	pauseT {|name| procs[name.asSymbol].pause}

	sch {|name="", function, sleep=5.0, random=0, offset=0| // offset is passed to functions so that local events are not at the same time
		var atask;

		if (name=="", {
			"TASKS MUST HAVE A NAME. Making up one".postln;
			name = ("T"++Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second).asSymbol;
		});

		if (procs[name.asSymbol].isNil.not, { this.stopT(name.asSymbol) }); // kill if already there before rebirth

		atask = Task({
			inf.do({|index|
				var time = ""+Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second;
				function.value(offset:offset); //CHECK: somehow the offsets gets added after the provided args
				if (name != "") {("-- now:"+name++time).postln};
				if (random!=0) {sleep = sleep + (random.rand2)};// +/- rand gets added to sleep

				/*	if (random.isArray, { // it would be nice to be able to use choose and wchoose to decide the sleep
				if (random[0].isArray, //[[values], [weights]]
				{random = random[0].wchoose(random[1])}, //weight rand from two arrays
				{random = random.choose} //rand item in array
				)
				}, { //rand number

				}); //rand number from 0 to off
				*/	//sleep.asFloat.rand.wait

				if (sleep <= 0, {sleep = 0.01}); // force lower limit to task tick resolution
				sleep.wait
			});
		});

		atask.start;
		procs.add(name.asSymbol -> atask);// to keep track of them
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
		plotwin.postln;
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

			drawview = UserView(plotwin, Rect(0, 0, 600, 300))
			.drawFunc_({ arg view; // AND CAN DRAW AS WELL
				ps.do({|ps, index|
					Pen.line( ps.curpos*600 @ 0, ps.curpos*600 @ 300 ); //playhead
				});
				Pen.stroke;
			});

			plotwinrefresh = Task({
				inf.do({|index|
					//plotwin.refresh;
					drawview.refresh;
					0.1.wait;
				})
			}, AppClock);
			plotwinrefresh.start;

			"To zoom in/out: Shift + right-click + mouse-up/down".postln;
			"To scroll: right-click + mouse-left/right".postln;
		});
		this.updateplot( bufs[abuf] ); // draw the data and refresh
	}

	updateplot {|buf| // draw the choosen buffer
		if (plotwin.isNil.not, {
			var f = { |b,v|
				b.loadToFloatArray(action: { |a| { v.setData(a) }.defer });
				//v.gridResolution(b.duration/10); // I would like to divide the window in 10 parts no matter what the sound dur is. Cannot change gridRes on the fly?
			};
			if (buf.isNil.not, {f.(buf, plotview)}); // only if a buf is provided
		});
	}

	control {
		var gap=0, height=0, f;
		if (controlGUI.isNil, {
			controlGUI = Window("All players", Rect(500, 200, 500, 700));
			controlGUI.alwaysOnTop = true;

			controlGUI.front;
			controlGUI.onClose = {
				controlGUI = nil;
				plotwinrefresh.stop;
			}; // needed?
			"OPENING CONTROL GUI".postln;

			height = controlGUI.bounds.height/howmany;

			controlGUI.layout = VLayout();

			// 		"To zoom in/out: Shift + right-click + mouse-up/down".postln;
			// 		"To scroll: right-click + mouse-left/right".postln;
			views.do({|view, index|
				views[index] = SoundFileView().timeCursorOn_(true)//controlGUI, Rect(0, height*index, controlGUI.bounds.width, height));
				.elasticMode_(true)
				.timeCursorColor_(Color.red)
				.drawsWaveForm_(true)
				.gridOn_(true)
				//.gridResolution(10)
				.gridColor_(Color.white)
				.waveColors_([ Color.new255(103, 148, 103), Color.new255(103, 148, 103) ])
				.background_(Color.new255(155, 205, 155))
				.canFocus_(false)
				.setSelectionColor(0, Color.blue)
				.currentSelection_(0)
				.setEditableSelectionStart(0, true)
				.setEditableSelectionSize(0, true)


				//.readFile(SoundFile(ps[index].buf.path), 0, ps[index].buf.numFrames) // file to display
				//.readFile(sfs[index], 0, sfs[index].numFrames) // file to display
				//.setData(sfs[index].data)

				.mouseDownAction_({ |view, x, y, mod, buttonNumber| // update selection loop
					ps[index].st( x.linlin(0, view.bounds.width, 0,1) )
				})
				.mouseUpAction_({ |view, x, y, mod|
					ps[index].end( x.linlin(0, view.bounds.width, 0,1) )
				});

				controlGUI.layout.add(views[index]);

				ps[index].view = views[index];// to update loop point when they change

				this.newplotdata(ps[index].buf, views[index]);
			});

			plotwinrefresh = Task({
				inf.do({|index|
					views.do({|view, index|
						view.timeCursorPosition = ps[index].curpos * sfs[index].numFrames * sfs[index].numChannels; //(buf.numFrames*buf.numChannels);
						0.1.wait;
					});
				})
			}, AppClock);
			plotwinrefresh.start;
		});
	}

	newplotdata {|buf, view|
		if (controlGUI.isNil.not, {
			buf.loadToFloatArray(action: { |a| { view.setData(a) }.defer })
		})
	}

	newselection {|st, end, view, buf|
		if (controlGUI.isNil.not, {
			view.setSelectionStart(0, (buf.numFrames*buf.numChannels) * st); // loop the selection
			view.setSelectionSize(0, (buf.numFrames*buf.numChannels) * (end-st));
		})
	}
}

