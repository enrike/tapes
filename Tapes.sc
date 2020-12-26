/* master of layers
*/


Tapes{

	var server, <path, <bufs, <sfs, <ps, procs;
	var plotwin=nil, plotview, drawview, plotwinrefresh;
	var controlGUI, views;
	var buses, <compressor;
	var volume=1;
	var it, them; // to remember @one and @some
	var <grouplists, <currentgroup;

	*new {| main=nil, symbol="_" |
		^super.new.initTapes( main, symbol );
	}

	initTapes {| amain, asym |
		~tapes = this; // keep me in a global

		procs = Dictionary.new; // stores all tasks

		currentgroup = \a;
		grouplists = Dictionary[currentgroup -> List.new];

		views = List.new;

		this.boot;

		amain !? this.lang(amain, asym)
	}


	lang {|main, sym|
		var globalvar = "~tapes";
		// this is to be able to use the systems using a symbol (like _) instead of ~tapes. and symbol2 (eg _2) instead of ~tapes.grouplists[\current][2]
		main.preProcessor = { |code|
			// list with all the commands used by Tapes
			var mets = [
				"add", "kill", "killall", "asignbufs", "loadfiles", "bufs", "buf", "curbufs", "one", "it", "some",
				"them", "info", "verbose", "normalize", "plot",
				"scratch", "pause", "solo", "fwd", "bwd", "dir", "reverse", "volu", "vold", "vol", "fadein", "fadeout",
				"pan", "rate", "wobble", "reset", "resume", "shot", "out", "stop", "play",
				"lp", "loop", "st", "step", "move", "end", "go", "gost", "goend", "dur", "len",
				"push", "pop", "save", "load", "control", "search",
				"rbuf", "rrate", "rpan", "rloop", "rdir", "rvol", "rgo", "rst", "rend", "rlen", "rand",
				"bloop", "bpan", "brate", "bvol", "bpan", "bgo",
				"comp", "thr", "slb", "sla",
				"do", "undo",
				"slice",
				"group", "groups", "mergegroups", "usegroup", "currentgroup", "newgroup", "killgroup", "all"
			];

			mets.do({|met| // @go --> ~tapes.go
				code = code.replace(sym++met, globalvar++"."++met);
			});

			100.reverseDo({|num| // reverse to avoid errors with index > 1 digit
				var dest = globalvar++".grouplists[\\"++currentgroup++"]"++"["++num.asString++"]";
				code = code.replace(sym++num.asString, dest); // _2 --> ~tapes.grouplists[\whatever][2]
			});

			code = code.replace("", ""); // THIS MUST BE HERE OTHERWISE THERE IS SOME WEIRD BUG
		};
	}

	boot {
		OSCdef.freeAll;

		server = Server.default;
		server.waitForBoot({

			SynthDef( \rPlayer, { arg out=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=0, dir=1, index=0, trig=0, reset=0, loop=1, wobble=0, amplag=0, ratelag=0, panlag=0, wobblelag=0;

				var left, right, phasor, dur;

				rate = (rate.lag(ratelag) + wobble.lag(wobblelag).rand2) * dir;
				amp = amp.lag(amplag);
				pan = pan.lag(panlag);

				dur = BufFrames.kr(buffer);
				phasor = Phasor.ar( trig, rate * BufRateScale.kr(buffer), start*dur, end*dur, resetPos: reset*dur);

				SendReply.ar( HPZ1.ar(HPZ1.ar(phasor).sign), '/loop', 1, index); //loop point
				SendReply.kr( LFPulse.kr(12, 0), '/pos', phasor/dur, index); //fps 12

				#left, right = BufRd.ar( 2, buffer, phasor, loop:loop ) * amp;
				Out.ar(out, Balance2.ar(left, right, pan));
			}).load;


			SynthDef( \ShotPlayer, {|out=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=1, index=0|
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
		server.waitForBoot({
			path = apath;
			("path is"+apath).postln;

			if (PathName.new(apath).isFile, {
				sfs = List.newUsing( [SoundFile(apath)] );
			}, {
				sfs = List.newUsing( SoundFile.collect( apath++"*") ); // if a folder apply wildcards
			});

			if (sfs.size < 1, {
				"no files found!".postln;
			}, {
				bufs = Array.new(sfs.size);// all files in the dir


				// load ALL buffers
				sfs.size.do({ arg n;
					var buf = Buffer.read(server, sfs.wrapAt(n).path,
						action:{
							("...loaded"+PathName(sfs.wrapAt(n).path).fileName).postln;
							if (n>=(sfs.size-1), {"DONE LOADING FILES".postln})
						}
					);
					bufs = bufs.add( buf )
				});

				(sfs.size + "files available").postln;
				"loading sounds into buffers".postln;
				"please wait ...".postln;
			});
		})
	}

	// groups
	group {
		^grouplists[currentgroup]
	}
	groups {
		^grouplists
	}
	all {
		^grouplists.values.flat
	}
	mergegroups{
		var players = grouplists.values.flat;
		grouplists = Dictionary.new.add(\a -> players);
		currentgroup = \a;
		this.usegroup(currentgroup)
	}
	usegroup {|name|
		if (grouplists.keys.includes(name), {
			currentgroup=name;
		}, {
			("group"+name+"does NOT exist").postln
		});
	}
	newgroup {|name|
		grouplists.add(name -> List.new);
		("created group"+name).postln;
	}
	removegroup {|name|
		this.killall(name);
		grouplists.removeAt(name);
		("removed group"+name).postln;
	}
	/////////

	add {|howmany=1, copythis|
		("creating players:"+howmany.asString).postln;
		howmany.do({
			var thebuffer, lay;
			if (bufs.size>0, {
				thebuffer = bufs.wrapAt( grouplists[currentgroup].size );
				lay = Tape.new(thebuffer);
				("at group"+currentgroup+"in position @"++grouplists[currentgroup].size).postln;
				grouplists[currentgroup].add(lay); // check if = is needed
				//copythis !? lay.copy(copythis) // state
				if (copythis.notNil, { lay.copy(copythis) });
				views.add(0);
			}, {
				"error: no buffers available!! run loadfiles()".postln;
			})
		})
	}

	kill {|index, agroup|
		index ?? index = grouplists[currentgroup].size.rand;
		agroup !? agroup = currentgroup;
		grouplists[agroup].removeAt(index).kill;
		views.pop;
		("free"+index+"at group"+agroup).postln;
	}

	killall {|agroup|
		agroup !? agroup = currentgroup;
		grouplists[agroup].do{|pla| pla.kill};
		grouplists[agroup] = List.new;
		("free group"+agroup).postln;

	}

	killthemall{
		grouplists.values.flat.collect(_.kill);
		grouplists = Dictionary.new.add(\a -> List.new);
		"killall in all groups".postln;
	}

	search {|st|
		var positives=List.new;
		grouplists[currentgroup].do({ |pl|
			if (pl.search(st), {
				positives = positives.add(pl) // append
			});
		})
		^positives
	}

	one {
		it = grouplists[currentgroup].choose; // keep it in a var
		^it;
	}

	it {^it} // retrieve it

	some {|howmany=1|
		howmany ?? howmany = grouplists[currentgroup].size.rand;
		them = grouplists[currentgroup].scramble[0..howmany-1];
		^them;
	}

	them {^them;}

	free { bufs.collect(_.free) }

	allbufs{
		"-- Available buffers --".postln;
		sfs.size.do({|i|
			(i.asString++": ").post;
			sfs[i].path.split($/).last.postln;
		})
	}

	curbufs {
		grouplists[currentgroup].size.do({ |i|
			(i.asString++":" + grouplists[currentgroup][i].file).postln
		})
	}

	info { grouplists[currentgroup].collect(_.info) }

	verbose {|flag=true|
		["verbose:", flag].postln;
		grouplists[currentgroup].do({ |p| p.verbose = flag })
	}

	normalize {
		bufs.collect(_.normalize);
	}

	buf {|buf, offset=0|
		if (buf.isInteger, {buf = bufs[buf]}); // using the index

		grouplists[currentgroup].do({ |pl, index|
			{
				pl.buf(buf);
				this.newplotdata(buf, views[index]);
			}.defer(offset.asFloat.rand)
		})
	}

	asignbufs { // asign buffers sequentially if more tapes than buffers then wrap
		grouplists[currentgroup].do({ |pl, index|
			pl.buf( bufs.wrapAt(index))
		})
	}

	newplayer {|asynth| grouplists[currentgroup].do({ |pl| pl.newplayer(asynth)}) }

	slice {|sttime, shift, grain, grainshift, offset=0| // SLICER like behaviour
		grouplists[currentgroup].do({ |pl, index|
			var mysttime = sttime + (index * (shift/100.0));
			var myendtime = mysttime + grain + (index * (grainshift/100.0));
			//	myendtime = myendtime.clip(0,1);
			//	mysttime = mysttime.clip(0,1);
			{
				pl.loop(mysttime, myendtime);
				this.newselection(mysttime, myendtime, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	step {|gap, offset=0|
		grouplists[currentgroup].do({ |pl, index|
			{
				pl.step(gap, offset);
				//pl.loop(pl.st + gap);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	lp {|st, end, offset=0| // SCLANG DOES NOT LIKE THAT WE USE THE NAME "LOOP" FOR OUR METHOD. change
		if (st.isNil, {st=0; end=1}); // reset

		grouplists[currentgroup].do({ |pl, index|
			{
				pl.loop(st, end);
				this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	st {|pos=0, random=0, offset=0|
		grouplists[currentgroup].do({ |pl, index|
			{
				pl.st(pos);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	end {|pos=1, random=0, offset=0|
		grouplists[currentgroup].do({ |pl, index|
			{
				pl.end(pos);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	dur {|val, random=0, offset=0|
		grouplists[currentgroup].do({ |pl, index|
			{
				pl.dur(val, random);
				//this.newselection(st, end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	len {|ms| grouplists[currentgroup].do({ |pl| pl.len(ms)}) } // in msecs

	reset { |offset=0|
		grouplists[currentgroup].do({ |pl|
			{ pl.reset }.defer(offset.asFloat.rand)
		})
	}

	shot { grouplists[currentgroup].collect(_.shot) } // single play no loop

	resume { grouplists[currentgroup].collect(_.resume) }

	pause { grouplists[currentgroup].collect(_.pause) }

	play { grouplists[currentgroup].collect(_.play) }

	stop { grouplists[currentgroup].collect(_.stop) }

	go {|point=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{ pl.go(point) }.defer(offset.asFloat.rand)
		})
	}

	gost {|offset=0|
		grouplists[currentgroup].do({ |pl|
			{ pl.gost }.defer(offset.asFloat.rand)
		})
	}

	goend {|offset=0|
		grouplists[currentgroup].do({ |pl|
			{ pl.goend }.defer(offset.asFloat.rand)
		})
	}

	move {|pos, random=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{ pl.move(pos, random) }.defer(offset.asFloat.rand)
		})
	}

	solo {|id|
		grouplists[currentgroup].do({ |pl|
			if (pl.id!=id, {
				if (pl.rate!=0, {pl.pause}); // if not already paused, pause.
			}, {
				pl.resume;
			})
		});
	}

	push {|which| grouplists[currentgroup].do({ |pl| pl.push(which)}) } // if no which it appends to stack

	pop {|which| grouplists[currentgroup].do({ |pl| pl.pop(which)}) } // if no which it pops last one

	save { |filename| // save to a file current state dictionary. in the folder where the samples are
		var data;
		if (filename.isNil, {
			filename = Date.getDate.stamp++".states";
		}, {
			filename = filename.asString++".states"}
		);

		data = Dictionary.new;

		grouplists[currentgroup].do({ |pl, index|
			data.put(\tape++index, pl.statesDic)
		});

		// open dialogue if no file path is provided
		("saving" + path ++ filename).postln;
		data.writeArchive(path ++ filename);
	}

	load { // opn dialogue to load file with state dictionary
		FileDialog({ |path|
			var data = Object.readArchive(path[0]);
			if (data.isNil.not, {
				grouplists[currentgroup].do({ |pl, index|
					pl.statesDic = data[\tape++index];
					if (index==0, {
						"available states: ".postln;
						data[\tape++index].keys.do({|key, pos| [pos, key].postln})
					});
				});
			})
		}, fileMode:1)
	}

	//random file, pan, vol, rate, loop (st, end), dir and go
	rand {|time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			grouplists[currentgroup].do({ |pl| pl.vol(0)});// mute. necessary?
			this.rbuf(offset);
			this.rvol(time, offset);
			this.rpan(time, offset);
			this.rrate(time, offset);//??
			//this.rdir(time, offset); / not needed
			this.rloop(offset);
			this.rgo;// this should be limited to the current loop
			grouplists[currentgroup].do({ |pl| pl.vol(pl.vol)}); //restore
		})
	}


	rvol {|time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rvol(1.0/grouplists[currentgroup].size)}.defer(offset.asFloat.rand)
		})
	}

	rpan {|time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rpan(time)}.defer(offset.asFloat.rand)
		})
	}

	rgo {|offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rgo}.defer(offset.asFloat.rand)
		})
	}

	rloop {|offset=0|
		grouplists[currentgroup].do({ |pl, index|
			{
				pl.rloop;
				this.newselection(pl.st, pl.end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	rst {|range=1, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rst(range)}.defer(offset.asFloat.rand)
		})
	}

	rend {|range=1, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rend(range)}.defer(offset.asFloat.rand)
		})
	}

	rlen {|range=0.5, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rlen(range)}.defer(offset.asFloat.rand)
		})
	}

	rate { |rate=1, time=0, random=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rate(rate, time)}.defer(offset.asFloat.rand)
		})
	}

	wobble {|arate=0, time=0, random=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.wobble(arate, time, random)}.defer(offset.asFloat.rand)
		})
	}

	reverse {|time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rate(pl.rate.neg, time)}.defer(offset.asFloat.rand)
		})
	}

	scratch {|target=0, tIn=1, tStay=0.5, tOut=1, offset=0| // boomerang like pitch change
		grouplists[currentgroup].do({ |pl|
			{pl.scratch(target, tIn, tStay, tOut)}.defer(offset.asFloat.rand)
		})
	}

	dir {|to=1, time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.dir(to, time)}.defer(offset.asFloat.rand)
		})

	}

	fwd {|time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.fwd(time)}.defer(offset.asFloat.rand)
		})
	}

	bwd {|time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.bwd(time)}.defer(offset.asFloat.rand)
		})
	}

	rdir {|curve=\lin, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rdir}.defer(offset.asFloat.rand)
		})
	}

	rrate {|time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.rrate(time)}.defer(offset.asFloat.rand)
		})
	}

	rbuf {|offset=0|
		grouplists[currentgroup].do({ |pl, index|
			{
				pl.buf(bufs.choose);
				this.newplotdata(pl.buf, views[index]);
			}.defer(offset.asFloat.rand)
		})
	}

	//
	out { |ch=0| grouplists[currentgroup].collect(_.out(ch)) }
	/*	out {|ch=0|
	grouplists[currentgroup].do({ |pl|
	pl.out(ch);
	})
	}*/

	vol {|avol=1, time=0, offset=0|
		volume = avol; // remember for the fadein/out
		grouplists[currentgroup].do({ |pl|
			{pl.vol(volume, time)}.defer(offset.asFloat.rand)
		});
		["set vol", avol].postln
	}

	vold { grouplists[currentgroup].collect(_.vold) }

	volu { grouplists[currentgroup].collect(_.volu) }

	fadeout {|time=1, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.vol(0, time)}.defer(offset.asFloat.rand)
		})
	}

	fadein {|time=1, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.vol(volume, time)}.defer(offset.asFloat.rand) // fade in to volume. not to 1
		})
	}

	pan { |pan=0, time=0, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.pan(pan, time)}.defer(offset.asFloat.rand)
		})
	}

	outb {|bus, offset=0|
		grouplists[currentgroup].do({ |pl|
			{pl.outb(bus)}.defer(offset.asFloat.rand)
		})
	} // sets synthdef out buf. used for manipulating the signal w effects

	//

	bloop {|range=0.01, time=0, offset=0|
		grouplists[currentgroup].do({|pl, index|
			{
				pl.bloop(range, time);
				this.newselection(pl.st, pl.end, views[index], pl.buf);
			}.defer(offset.asFloat.rand)
		})
	}

	bgo {|range=0.01, time=0, offset=0|
		grouplists[currentgroup].do({|pl|
			{pl.bgo(range, time)}.defer(offset.asFloat.rand)
		})
	}

	bvol {|range=0.01, time=0, offset=0|
		grouplists[currentgroup].do({|pl|
			{pl.bvol(range, time)}.defer(offset.asFloat.rand)
		})
	}

	bpan {|range=0.01, time=0, offset=0|
		grouplists[currentgroup].do({|pl|
			{pl.bpan(range, time)}.defer(offset.asFloat.rand)
		})
	}

	brate {|range=0.01, time=0, offset=0|
		grouplists[currentgroup].do({|pl|
			{pl.brate(range, time)}.defer(offset.asFloat.rand)
		})
	}

	/////// task's stuff ////
	undo {|name|
		if (name.isNil, {
			"-- kill all procs".postln;
			procs.collect(_.stop);
			procs = Dictionary.new;
		},{
			("-- procs: killing"+name).postln;
			procs[name.asSymbol].stop;
			procs.removeAt(name.asSymbol);
		})
	}

	do {|name="", function, sleep=5.0, defer=0, iter=inf, when=true, then=1, random=0, offset=0, clock=0, verbose=true| // offset is passed to functions so that local events are not at the same time
		var atask;

		if (name=="", {
			"TASKS MUST HAVE A NAME. Making up one".postln;
			name = ("T"++Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second).asSymbol;
			name.postln;
		});

		if (procs[name.asSymbol].notNil, { this.undo(name.asSymbol) }); // kill before rebirth if already there

		clock ?? clock = TempoClock; // default

		atask = Task({
			block {|break|
				iter.do {|index|
					var time = ""+Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second;

					if (verbose==true, {("-- now:"+name++time).postln});

					if (when.value, {
						function.value(offset:offset);
						if (then==0, {break.value(999)})
					});

					if ((random.isArray),
						{sleep = random[0].wchoose(random[1])} ,
						{sleep = sleep + (random.rand2)}
					);// +/- rand gets added to sleep
					if (sleep <= 0, {sleep = 0.01}); // force lower limit to task tick resolution
					sleep.wait
				};
			};
			("-- done with"+name).postln;
			this.undo(name.asSymbol)
		}, clock);

		{ atask.start }.defer(defer);

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
			plotwin = Window("All tapes", Rect(100, 200, 600, 300));
			plotwin.alwaysOnTop=true;
			//plotwin.setSelectionColor(0, Color.red);
			plotwin.front;
			plotwin.onClose = {
				"control window closed".postln;
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
				grouplists[currentgroup].do({|pl, index|
					Pen.line( pl.curpos*600 @ 0, pl.curpos*600 @ 300 ); //playhead
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
		if (plotwin.notNil, {
			var f = { |b,v|
				b.loadToFloatArray(action: { |a| { v.setData(a) }.defer });
				//v.gridResolution(b.duration/10); // I would like to divide the window in 10 parts no matter what the sound dur is. Cannot change gridRes on the fly?
			};
			//if (buf.notNil, {f.(buf, plotview)}); // only if a buf is provided
			buf !? f.(buf, plotview)
		});
	}

	control {|cwidth, cheight|
		var gap=0, height=0;
		if (controlGUI.isNil, {
			controlGUI = Window("All tapes", Rect(500, 200, cwidth?500, cheight?700));
			controlGUI.alwaysOnTop = true;
			controlGUI.front;
			controlGUI.onClose = {
				controlGUI = nil;
				grouplists[currentgroup].do({|play| play.view = nil });
				plotwinrefresh.stop;
			};
			"OPENING CONTROL GUI".postln;

			//height = controlGUI.bounds.height/howmany;

			controlGUI.layout = VLayout();

			// 		"To zoom in/out: Shift + right-click + mouse-up/down".postln;
			// 		"To scroll: right-click + mouse-left/right".postln;
			views.size.do({|index|
				views[index] = SoundFileView().timeCursorOn_(true)
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

				.mouseDownAction_({ |thisview, x, y, mod, buttonNumber| // update selection loop
					grouplists[currentgroup][index].st( x.linlin(0, thisview.bounds.width, 0,1) ) // what about when zoomed in?
				})
				.mouseUpAction_({ |thisview, x, y, mod|
					grouplists[currentgroup][index].end( x.linlin(0, thisview.bounds.width, 0,1) )
				});
				controlGUI.layout.add(views[index]);

				grouplists.values.flat[index].view = views[index];// to update loop point when they change
				grouplists.values.flat[index].updatelooppoints();

				this.newplotdata(grouplists.values.flat[index].buf, views[index]);
			});

			plotwinrefresh = Task({
				inf.do({|index|
					views.do({|view, index|
						view.timeCursorPosition = grouplists.values.flat[index].curpos * (grouplists.values.flat[index].buf.numFrames);
						0.1.wait;
					});
					//"----".postln;
				})
			}, AppClock);
			plotwinrefresh.start;
		});
	}

	newplotdata {|buf, view|
		if (controlGUI.notNil, {
			var sf = SoundFile.new;
			sf.openRead(buf.path);
			view.postln;
			view.soundfile = sf;            // set soundfile
			view.read(0, sf.numFrames);     // read in the entire file.
			view.refresh;
			//buf.loadToFloatArray(action: { |a| { view.setData(a) }.defer })
		})
	}

	newselection {|st, end, view, buf|
		if (controlGUI.notNil, {
			view.setSelectionStart(0, (buf.numFrames) * st); // loop the selection
			view.setSelectionSize(0, (buf.numFrames) * (end-st));
		})
	}
}

