/* master of layers
*/


Tapes {

	var server, <path, <bufs, <sfs, <ps, <procs, <onsets;
	var plotwin=nil, plotview, drawview, plotwinrefresh;
	var controlGUI, <views;
	//var buses, <compressor;
	var volume=1, sttime;
	var it, them; // to remember @one and @some
	var <grouplists, <currentgroup;
	var <currentbank;
	var <slicestate=#[ 0.624, 7.156, 0.05, 0.0 ];
	var <rbuffer, rsynth; // to record

	*new {| main=nil, dir, symbol="_" | // systemdir
		^super.new.initTapes( main, dir, symbol );
	}

	initTapes {| amain, adir, asym |
		~tapes = this; // keep me in a global

		//Platform.case(\linux, { Server.supernova });

		OSCdef.freeAll;

		procs = Dictionary.new; // stores all tasks

		controlGUI = Dictionary.new;

		currentgroup = \default;
		grouplists = Dictionary[currentgroup -> List.new];

		views =  Dictionary[currentgroup -> List.new];

		bufs = Dictionary[currentbank -> List.new];
		currentbank = \default;

		onsets = Dictionary.new;

		this.boot(adir);

		if (amain.notNil, {
			this.lang(amain, asym)
		});

		sttime = Process.elapsedTime;
	}


	lang {|main, sym|
		var globalvar = "~tapes";
		// this is to be able to use the systems using a symbol (like _) instead of ~tapes. and symbol2 (eg _2) instead of ~tapes.grouplists[\current][2]
		main.preProcessor = { |code|
			// list with all the commands defined by Tapes
			var keywords = [
				"add", "new", "kill", "killall", "asignbufs", "loadfiles", "removebuf",
				"bufs", "buf", "curbufs", "bufinfo", "normalize",
				"one", "it", "some", "them", "info", "verbose", "plot", "control", "hm",
				"scratch", "pause", "solo", "fwd", "bwd", "dir", "reverse", "volu", "vold", "vol",
				"fadein", "fadeout", "mute", "rwd", "trans", "xloop", "xdone",
				"pan", "rate", "wobble", "brown", "vibrato", "reset", "shot", "out", "stop", "play",
				"frame", "st", "move", "moveby", "jump", "end", "go", "gost", "goend", "dur", "len", "env",
				"push", "pop", "save", "load", "search", "id", "where",
				"rbuf", "rrate", "rpan", "rframe", "rdir", "rvol", "rgo", "rst", "rend", "rlen", "rmove", "rand",
				"bframe", "bmove", "bpan", "brate", "bvol", "bpan", "bgo", "spread",
				//"comp", "thr", "slb", "sla",
				"do", "undo", "does", "dogui", "pause", "resume", "shutup",
				"slice", "slicegui",
				"group", "groups", "mergegroups", "usegroup", "ug", "currentgroup", "newgroup",
				"killgroup", "all", "ggui",
				"bank", "banks", "mergebanks", "usebank", "currentbank", "newbank", "delbank",
				"loadonsetanalysis", "onsets", //experimental
				"midion", "midioff", "ccin",
				"rec", "preparerec", "bufrec", "zerorecbuf", "recstate",
				"time", "secs", "mins"
			];

			keywords.do({|met| // eg: _go --> ~tapes.go
				code = code.replace(sym++met, globalvar++"."++met);
			});
			100.reverseDo({|num| // reverse to avoid errors with index > 1 digit
				var dest = globalvar++".grouplists[\\"++currentgroup++"]"++"["++num.asString++"]";
				code = code.replace(sym++num.asString, dest); // _2 --> ~tapes.grouplists[\whatever][2]
			});

			code = code.replace("", ""); // THIS MUST BE HERE OTHERWISE THERE IS SOME WEIRD BUG
		};
	}

	boot {|dir|
		OSCdef.freeAll;

		server = Server.default;
		server.waitForBoot({
			SynthDef(\recb, { |in=0, out = 0, bufnum = 0, loop = 0|
				var signal = In.ar(in, 2);
				signal = Limiter.ar(signal); // avoid bursts
				RecordBuf.ar(signal, bufnum, doneAction: Done.freeSelf, loop: loop);
			}).load;

			SynthDef( \rPlayerLoop, { arg out=0, buffer=0, amp=1, pan=0, start=0, end=1, rate=0, dir=1, index=0,
				trig=0, reset=0, wobble=0, amplag=0, ratelag=0, panlag=0, wobblelag=0,
				brown=0, brownlag=0,
				vib = #[1,1,0,0,0,0,0], viblag=0;

				var left, right, phasor, dur = BufFrames.kr(buffer);

				rate = rate.lag(ratelag) + wobble.lag(wobblelag).rand2;
				rate = rate * dir;
				rate = rate + BrownNoise.ar(brown.lag(brownlag));
				rate = rate * Vibrato.ar(*vib.lag(viblag));

				amp = VarLag.kr(amp,amplag, Env.shapeNumber(\lin));
				pan = pan.lag(panlag);

				phasor = Phasor.ar( trig, rate * BufRateScale.kr(buffer), start*dur, end*dur, resetPos: reset*dur);

				SendReply.ar( Trig.ar(phasor >= ( (end*dur) - 1)), '/xloop', 1, index); // loop point
				SendReply.kr( LFPulse.kr(12, 0), '/pos', phasor/dur, index); //fps 12

				#left, right = BufRd.ar( 2, buffer, phasor ) * amp;
				Out.ar(out, Balance2.ar(left, right, pan));
			}).load;

			if (dir.notNil, {this.loadfiles(dir)})
		})
	}

	secs {|who| ^this.time(who)}
	mins {|who| ^this.time(who)/60}
	time {|who|
		var t;
		if (who.isNil, {t = Process.elapsedTime-sttime}, {
			if (who.isSymbolWS, t = Process.elapsedTime - procs[who][5]);
			if (who.class.name==\Tape, t=who.time);
		});
		^t
	}
	// REC ///////
	preparerec {|len|
		Buffer.alloc(Server.default,
			len*Server.default.sampleRate, 2); // must wait for alloc before next line!!!!!!!!!
	}
	zerorecbuf{
		rbuffer.zero;
	}
	bufrec {
		^rbuffer
	}
	rec {|in=0, len=1, loop=0|
		rsynth.free;
		rbuffer.free;
		"start sampling into _bufrec".postln;
		Routine.run { // INSIDE A ONE SHOT ROUTINE TO BE ABLE TO SYNC
			rbuffer = Buffer.alloc(Server.default, len*Server.default.sampleRate, 2);
			Server.default.sync;// wait til is allocated
			rsynth = Synth.tail(Server.default, \recb, [\in, in, \bufnum, rbuffer, \loop, loop]);
			if (loop==0, { {"done sampling!".postln}.defer(len) });
		}
	}
	stopr { // this should kill the \recb synth if loop
		rsynth.free;
	}
	recstate { //recording?
		^rsynth.notNil
	}


	loadfiles {|apath, overwrite=1| // overwrite will remove the previous one if any
		var files=List.new;
		var already = sfs.size;
		var target = currentbank;

		server.waitForBoot({
			//path = apath;
			("path is"+apath).postln;

			try {
				if (PathName.new(apath).isFile, { // string with path to a single file
					files = List.newUsing( SoundFile.collect(apath) );
				}, {
					if (PathName.new(apath).isFolder, { // if a folder apply wildcards
						files = List.newUsing( SoundFile.collect( apath++Platform.pathSeparator++"*") );
					})
				});
			} {|er|
				apath.do{|pa, i|
					if (PathName.new(pa).isFile, {
						files.add( SoundFile.collect(pa)[0] ); // for some reason collect() processes paths different than new()
					})
				}
			};

			if (files.size < 1, {
				"no files found!".postln;
			}, {

				if (overwrite==1, {
					sfs=files;
					bufs[target] = List.new;//(files.size);// all files in the dir
				},{
					sfs=sfs++files; // append but later do not load all of them! just the new ones
					//bufs = Array.new(files.size);// all files in the dir
				});

				files.postln;

				files.size.do({ arg n; // load ALL buffers
					var buf = Buffer.read(server, files.wrapAt(n).path,
						action:{
							("... loaded"+PathName(files.wrapAt(n).path).fileName).postln;
							if (n>=(files.size-1), {
								".-------------------------".postln;
								"... DONE LOADING FILES!".postln;
								"--------------------------".postln
							})
						}
					);
					bufs[target] = bufs[target].add( buf )
				});

				("..." + sfs.size + "files available").postln;
				"... loading sounds into buffers".postln;
				"... PLEASE WAIT ...".postln;
			});
		})
	}


	// groups of tapes
	group {
		^grouplists[currentgroup]
	}
	groups {
		^grouplists
	}
	all {
		^grouplists.values.flat
	}
	hm {
		^grouplists.values.flat.size
	}
	mergegroups{|defer=0, d|
		var target = currentgroup;
		var players;
		defer = d?defer;
		{
			players = grouplists.values.flat;
			grouplists = Dictionary.new.add(\default -> players);
			views = Dictionary.new.add(\default -> views.values.flat);
			currentgroup = \default;
			this.usegroup(currentgroup)
		}.defer(defer)
	}
	usegroup {|name=\default|
		this.ug(name)
	}
	ug {|name=\default|
		if (grouplists.keys.includes(name).not, {
			name = \default;
		});
		currentgroup=name;
	}
	newgroup {|name|
		grouplists.add(name -> List.new);
		views.add(name -> List.new);
		("created group"+name).postln;
	}
	removegroup {|name|
		this.killall(name);
		grouplists.removeAt(name);
		views.removeAt(name);
		("removed group"+name).postln;
	}
	id {|id| // return the tape whose ID==id
		^this.all.select { |item| item.id==id }[0];
	}
	where {|id| // return position in group by id
		var p;
		grouplists[currentgroup].size.do{|i|
			if (grouplists[currentgroup][i].id==id, {p=i})
		}
		^p
	}



	// buffer BANKS
	bank {
		^bufs[currentbank]
	}
	/*	banks {
	^bufs
	}*/
	mergebanks{
		var bs = bufs.values.flat;
		bufs = Dictionary.new.add(\default -> bs);
		currentbank = \default;
		this.usebank(currentbank)
	}
	usebank {|name|
		if (bufs.keys.includes(name).not, {
			("bank"+name+"does NOT exist").postln;
			name = \default;
		});
		currentbank=name;
		("currentbank is"+name).postln;
	}
	newbank {|name|
		bufs.add(name -> List.new);
		("created bank"+name).postln;
	}
	removebank {|name|
		bufs[currentbank].collect(_.free);
		bufs.removeAt(name);
		("removed bank"+name).postln;
	}

	// MIDI ////////////////

	midion {
		"Initialising MIDI... wait...".postln;
		MIDIClient.init;
		MIDIIn.connectAll;
		MIDIdef.freeAll
	}

	midioff {MIDIIn.disconnectAll}

	ccin {|name="cc", cc=0, action|
		if (action.isNil, {action={|i| ("no valid action defined for cc" +cc).postln} });
		MIDIdef.cc(name, {arg ...args;
			action.value(args[0])
		}, cc);
	}


	//////////////////
	//change new keyword to something else. new is the constructor of classes in SC
	// shortcut do add n tapes to a group and create the group if does not exist
	new {|howmany=1, buffer, group=\default, defer=0, d|
		defer = d?defer;
		if (grouplists[group].isNil, {this.newgroup(group)});
		{
			this.usegroup(group);
			this.add(howmany);
			if (buffer.notNil, {this.buf(buffer)})
		}.defer(defer)
	}

	add {|howmany=1, copythis, defer=0, d|
		var target = currentgroup;
		defer = d?defer;
		{
			("creating players:"+howmany.asString).postln;
			howmany.do({
				var thebuffer, lay;
				if (bufs[currentbank].size>0, {
					thebuffer = bufs[currentbank].wrapAt( grouplists[target].size );
					lay = Tape.new(thebuffer);
					("at group"+currentgroup+"in position @"++grouplists[target].size).postln;
					("-----------").postln;
					grouplists[target].add(lay); // check if = is needed

					if (copythis.isNumber, { // by id. get the instance
						copythis = this.id(copythis)
					});
					if (copythis.notNil, { lay.copy(copythis) });

					views[target].add( SoundFileView() );
				}, {
					"error: no buffers available!! run _loadfiles".postln;
				})
			})
		}.defer(defer)
	}

	kill {|index, agroup, defer=0, d|
		var target = currentgroup;
		defer = d?defer;
		{
			index ?? index = grouplists[target].size.rand;
			agroup !? agroup = currentgroup;
			try { grouplists[agroup].removeAt(index).kill;
				views[agroup].pop;
				("free"+index+"at group"+agroup).postln;}
			{("warning: nothing left to kill in group"+agroup).postln}
		}.defer(defer)
	}

	killall {|agroup|
		agroup !? agroup = currentgroup;
		grouplists[agroup].do{|pla|
			pla.kill;
			views[agroup].pop
		};
		grouplists[agroup] = List.new;
		views[agroup] = List.new;
		("free group"+agroup).postln;
	}

	killthemall{|defer=0, d|
		defer = d?defer;
		{
			grouplists.values.flat.collect(_.kill);
			grouplists = Dictionary.new.add(\default -> List.new);
			"killall in all groups".postln;
		}.defer(defer)
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

	free { bufs[currentbank].collect(_.free) }
	freeall { bufs.values.flat.collect(_.free) }

	/*	allbufs{
	"-- Available buffers --".postln;
	sfs.size.do({|i|
	(i.asString++": ").post;
	sfs[i].path.split($/).last.postln;
	})
	}*/

	curbufs {
		"-- index of player, filename --".postln;
		grouplists[currentgroup].size.do({ |i|
			("_"++i++":" + grouplists[currentgroup][i].file).postln
		})
	}

	info {|post|
		var data = List.new;
		//if (this.hm==0, "no players!".postln);
		grouplists[currentgroup].do{|pl, i|
			data.add(pl.info);
		};
		if (post==1, {
			("---------------------").postln;
			("-- Group:" + currentgroup + "with" + grouplists[currentgroup].size.asString).postln;
			data.do{|tap|
				("-- Tape --").postln;
				tap.associationsDo{|assoc| assoc.postln };
				("---------------------").postln;
			};
			("--------------").postln;
		});
		^data
	}

	verbose {|flag=true|
		["verbose:", flag].postln;
		grouplists[currentgroup].do({ |p| p.verbose = flag })
	}

	normalize {
		bufs[currentbank].collect(_.normalize);
	}

	buf {|value, offset=0, defer=0, o=nil, d=nil| // value can be a Buffer, an array of ints
		var target = currentgroup;
		#offset, defer = [o?offset, d?defer];

		if (value.isArray, {value=value.choose}); // choose between given integer values

		// finally if valus is an Integer we need the actual buffer
		if (value.isInteger, {value = bufs[currentbank][value]}); // using the index

		if (value.isNil, { value=bufs[currentbank].choose }); // get an actual buffer

		{
			grouplists[target].do({ |pl, index|
				{
					pl.buf(value);
					this.newplotdata(value, views[target][index], target); // if control is open then update display
				}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	removebuf {|value, defer=0, d=nil|
		var target = currentbank;
		defer = d?defer;
		{
			if (value.isInteger, {bufs[target].removeAt(value)}); // using the index
		}.defer(defer)
	}

	asignbufs { // asign buffers sequentially if more tapes than buffers then wrap
		grouplists[currentgroup].do({ |pl, index|
			pl.buf( bufs[currentbank].wrapAt(index))
		})
	}

	bufinfo {
		"-- buffer bank index, filename --".postln;
		bufs[currentbank].do{|b, i|
			("bank:" ++ currentbank.asString + "/ i:" ++ i.asString +"/"+ PathName(b.path).fileName).postln;
		}
	}

	newplayer {|asynth| grouplists[currentgroup].do({ |pl| pl.newplayer(asynth)}) }

	slice {|sttime, shift, grain, grainshift, offset=0, defer=0, o=nil, d=nil| // SLICER like behaviour
		var target = currentgroup;
		{
			offset=o?offset;defer?d?defer;
			slicestate = [sttime, shift, grain, grainshift];

			grouplists[target].do({ |pl, index|
				var mysttime = sttime + (index * (shift/100.0));
				var myendtime;// = mysttime + grain + (index * (grainshift/100.0));

				if ( (mysttime<0) || (mysttime>1), { //st left, right
					mysttime = mysttime % 1;
				});

				myendtime = mysttime + grain + (index * (grainshift/100.0));

				if ( (myendtime<0) || (myendtime>1), { //end left, right
					mysttime = mysttime % 1;
					myendtime = mysttime + grain + (index * (grainshift/100.0));
				});

				if (myendtime<mysttime, { // reverse
					var temp1=mysttime;
					var temp2=myendtime;
					mysttime = temp2;
					myendtime = temp1;
				});

				{
					pl.frame(mysttime, myendtime);
					this.newselection(mysttime, myendtime, views[target][index], pl.buf, target);
				}.defer(offset.asFloat.rand);
			})
		}.defer(defer)
	}

	slicegui2d {|w=250,h=500, target=\default|
		var label;
		var doslice = this;
		var delta = 15;
		var slicerw = Window("Slicer 2D", w@h).alwaysOnTop_(1);
		slicerw.layout = VLayout();

		Slider2D(slicerw, (w-10)@(h-10))
		.x_(0) // initial location of x
		.y_(0.5)   // initial location of y
		.action_({|sl|
			slicestate[0] = sl.x.asFloat;
			slicestate[1] = sl.y.linlin(0,1, delta.neg, delta).asFloat;
			this.usegroup(target);
			doslice.slice(*slicestate);
			label.string = format("% % % %", slicestate[0].asStringPrec(2), slicestate[1].asStringPrec(2),
				slicestate[2].asStringPrec(2), slicestate[3].asStringPrec(2));
			this.usegroup;
		});
		Slider2D(slicerw, (w-10)@(h-10))
		.x_(0) // initial location of x
		.y_(0.5)   // initial location of y
		.action_({|sl|
			slicestate[2] = sl.x.asFloat;
			slicestate[3] = sl.y.linlin(0,1,  delta.neg, delta).asFloat;
			doslice.slice(*slicestate);
			this.usegroup(target);
			label.string = format("% % % %", slicestate[0].asStringPrec(2), slicestate[1].asStringPrec(2),
				slicestate[2].asStringPrec(2), slicestate[3].asStringPrec(2));
			this.usegroup;
		});
		label = StaticText(slicerw, 140@20);
		slicerw.front;
	}

	// add a reset button? use autogui?
	slicegui {|w=450|
		var label;
		var doslice = this;
		var delta = 15;
		var slicerw = Window("Slicer 4x", w@175).alwaysOnTop_(true);
		var cols = [Color.grey,Color.white, Color.grey(0.7),Color.grey,Color.white, Color.yellow,nil,nil, Color.grey(0.7)];
		var controls = [];
		var target = currentgroup;
		slicerw.view.decorator = FlowLayout(slicerw.view.bounds);
		slicerw.view.decorator.gap=2@2;
		/*
		Button(slicerw, 50@20) // OPEN
		.states_([["open", Color.black, Color.red]])
		.action_({ |butt|
		FileDialog({ |apath|
		var	data = Object.readArchive(apath);
		("reading preset"+apath).postln;

		[\bounds, data[\bounds]].postln; //make sure it first deals with ON
		{ w.bounds = data[\bounds] }.defer; // wait for QT
		data.removeAt(\bounds); // we are done with this

		data.keysValuesDo{ |key, value|
		[key, value].postln; // we must first run ON button to trigger the synth. then do the rest.
		/*					try {
		{controls[key].valueAction = value}.defer // wait for QT
		}{|er| er.postln; "XXXXX".postln}*/
		};
		},
		fileMode: 0,
		stripResult: true,
		path: Platform.userHomeDir); // not defined!
		});
		//////////////////////////////
		Button(slicerw, 50@20) // SAVE
		.states_([["save", Color.black, Color.red]])
		.action_({ |butt|
		var data = Dictionary.new, name="slicer", filename;
		filename = name++"_"++Date.getDate.stamp++".preset";

		data.put(\bounds, slicerw.bounds);

		controls.do { |widget|
		data.put(widget.label, widget.value)
		};

		("saving preset into" + Platform.userHomeDir ++ Platform.pathSeparator ++ "presets" ++ Platform.pathSeparator ++ filename).postln;

		data.writeArchive(Platform.userHomeDir ++ Platform.pathSeparator ++ "presets" ++ Platform.pathSeparator ++ filename);
		});
		*/
		controls.add( EZSlider(slicerw, (w-10)@40, "start",
			ControlSpec(0, 1, \lin, 0.0001, 0),
			{|sl|
				this.usegroup(target);
				slicestate[0] = sl.value.asFloat;
				doslice.slice(*slicestate);
		}, slicestate[0], layout:\line2, labelHeight:15).setColors(*cols).numberView.maxDecimals = 5 ;);

		controls.add( EZSlider(slicerw, (w-10)@40, "shift",
			ControlSpec(delta.neg, delta, \lin, 0.0001, 0),
			{|sl|
				this.usegroup(target);
				slicestate[1] = sl.value.asFloat;
				doslice.slice(*slicestate);
		}, slicestate[1], layout:\line2, labelHeight:15).setColors(*cols).numberView.maxDecimals = 5 ;);

		controls.add( EZSlider(slicerw, (w-10)@40, "grain",
			ControlSpec(0,1, \lin, 0.0001, 0),
			{|sl|
				this.usegroup(target);
				slicestate[2] = sl.value.asFloat;
				doslice.slice(*slicestate);
		}, slicestate[2], layout:\line2, labelHeight:15).setColors(*cols).numberView.maxDecimals = 5 ;);

		controls.add( EZSlider(slicerw, (w-10)@40, "grain shift",
			ControlSpec(delta.neg, delta, \lin, 0.0001, 0),
			{|sl|
				this.usegroup(target);
				slicestate[3] = sl.value.asFloat;
				doslice.slice(*slicestate);
		}, slicestate[3], layout:\line2, labelHeight:15).setColors(*cols).numberView.maxDecimals = 5;);

		slicerw.front;//!!!!!
	}

	frame {|st, end, offset=0, defer=0, o=nil, d=nil|
		var target = currentgroup;
		if (st.isNil, {st=0; end=1}); // reset
		#offset, defer = [o?offset, d?defer];
		{grouplists[target].do({ |pl, index|
			{
				pl.frame(st, end);
				this.newselection(st, end, views[target][index], pl.buf, target);
			}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	st {|value, random=0, offset=0, defer=0, r=nil, o=nil, d=nil|
		this.action(\st, value, random, 0, offset, defer, r, nil, o, d);
	}

	end {|value, random=0, offset=0, defer=0, r=nil, o=nil, d=nil|
		this.action(\end, value, random, 0, offset, defer, r, nil, o, d);
	}

	dur {|value, random=0, offset=0, defer=0, r=nil, o=nil, d=nil|
		this.action(\dur, value, random, 0, offset, defer, r, nil, o, d);
	}

	len {|ms| grouplists[currentgroup].do({ |pl| pl.len(ms)}) } // in msecs

	reset { |offset=0, defer=0, o=nil, d=nil|
		this.action(\reset, 0, 0, 0, offset, defer, nil, nil, o, d);
	}

	play { |value=inf, offset=0, defer=0, o=nil, d=nil|
		this.action(\play, value, 0, 0, offset, defer, nil, nil, o, d);
	}

	stop { |offset=0, defer=0, o=nil, d=nil|
		this.action(\stop, 0, 0, 0, offset, defer, nil, nil, o, d);
	}

	go {|value=1, offset=0, defer=0, o=nil, d=nil|
		this.action(\go, value, 0, 0, offset, defer, nil, nil, o, d);
	}

	gost {|offset=0, defer=0, o=nil, d=nil|
		this.action(\gost, 0, 0, offset, defer, nil, nil, o, d);
	}

	goend {|offset=0, defer=0, o=nil, d=nil|
		this.action(\goend, 0, 0, offset, defer, nil, nil, o, d);
	}

	jump {|value, random=0, offset=0, defer=0, r=nil, o=nil, d=nil|
		this.action(\jump, value, random, 0, offset, defer, r, nil, o, d);
	}

	move {|value, random=0, offset=0, defer=0, r=nil, o=nil, d=nil|
		this.action(\move, value, random, 0, offset, defer, r, nil, o, d);
	}

	moveby {|value, random=0, offset=0, defer=0, r=nil, o=nil, d=nil|
		this.action(\moveby, value, random, 0, offset, defer, r, nil, o, d);
	}

	solo {|index, defer=0, d=nil| // add offset and defer?
		index.postln;
		grouplists[currentgroup][index].id.postln;
		defer = d?defer;
		{
			grouplists[currentgroup].do({ |pl|
				//if (pl.id!=id, {
				if (pl!=grouplists[currentgroup][index], {
					if (pl.rate!=0, {pl.pause}); // if not already paused, pause.
				}, {
					pl.resume;
				})
			});
		}.defer(defer)
	}


	// STATES SAVE/LOAD
	states {
		grouplists[currentgroup][0].statesDic.postln;
	}

	push {|which| grouplists[currentgroup].do({ |pl| pl.push(which)}) } // if no which it appends to stack

	pop {|which| grouplists[currentgroup].do({ |pl| pl.pop(which)}) } // if no which it pops last one

	save { |filename| // save to a file current state dictionary. in the folder where the samples are
		var data, path;
		if (filename.isNil, {
			filename = Date.getDate.stamp++".states";
		}, {
			filename = filename.asString++".states"}
		);

		data = Dictionary.new;

		grouplists[currentgroup].do({ |pl, index|
			data.put(\tape++index, pl.statesDic)
		});

		path = thisProcess.nowExecutingPath.dirname ++ Platform.pathSeparator ++ "states" ++ Platform.pathSeparator ++ filename;
		("saving" + path).postln;
		data.writeArchive(path);
		//("saving" + Platform.userHomeDir ++ Platform.pathSeparator ++ filename).postln;
		//data.writeArchive(Platform.userHomeDir ++ Platform.pathSeparator ++ filename);
	}

	load {|filepath|

		if (filepath.isNil, {// opn dialogue to load file with state dictionary
			FileDialog({ |path|
				this.readstates(path[0])
			}, fileMode:1,
			path: Platform.userHomeDir
			)
		},{
			var path =  thisProcess.nowExecutingPath.dirname ++ Platform.pathSeparator ++ "states" ++ Platform.pathSeparator ++ filepath;
			this.readstates(filepath)
		})

	}

	readstates {|path|
		var data = Object.readArchive(path);
		if (data.notNil, {
			grouplists[currentgroup].do({ |pl, index|
				pl.statesDic = data[\tape++index];
				if (index==0, {
					"available states: ".postln;
					data[\tape++index].keys.do({|key, pos| [pos, key].postln})
				});
			});
		})
	}


	// RANDOM
	//random file, pan, vol, rate, loop (st, end), dir and go
	rand {|time=0, offset=0|
		{this.rbuf}.defer(offset.asFloat.rand);
		grouplists[currentgroup].do({ |pl|
			{pl.rand(time)}.defer(offset.asFloat.rand)
		})
	}

	rvol {|time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\rvol, 1.0/this.hm, 0, time, offset, defer, nil, t, o, d);
	}

	rpan {|time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\rpan, 0, 0, time, offset, defer, nil, t, o, d);
	}

	rgo {|range=0, offset=0, defer=0, o=nil, d=nil|
		this.action(\rgo, 0, 0, 0, offset, defer, nil, nil, o, d);
	}

	rframe {|offset=0, defer=0, o=nil, d=nil|
		var target = currentgroup;
		#offset, defer = [o?offset, d?defer];
		{grouplists[target].do({ |pl, index|
			{
				pl.rframe;
				this.newselection(pl.st, pl.end, views[target][index], pl.buf, target);
			}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	rmove {|offset=0, defer=0, o=nil, d=nil|
		this.action(\rmove, 0, 0, 0, offset, defer, nil, nil, o, d);
	}

	rst {|range=1, offset=0, defer=0, o=nil, d=nil|
		this.action(\rst, range, 0, 0, offset, defer, nil, nil, o, d);
	}

	rend {|range=1, offset=0, defer=0, o=nil, d=nil|
		this.action(\rend, range, 0, 0, offset, defer, nil, nil, o, d);
	}

	rlen {|range=1, offset=0, defer=0, o=nil, d=nil|
		this.action(\rlen, range, 0, 0, offset, defer, nil, nil, o, d);
	}
	rdir {|offset=0, defer=0, o=0, d=0|
		this.action(\rdir, 0, 0, 0, offset, defer, nil, nil, o, d);
	}

	rrate {|time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\rrate, 0, 0, time, offset, defer, nil, t, o, d);
	}

	rbuf {|mode=0, offset=0, defer=0, o=0, d=0|
		var buffer = bufs[currentbank].choose; // defaulto to all the same
		var target = currentgroup; // freeze target in case of defer
		#offset, defer = [o?offset, d?defer];
		{grouplists[target].do({ |pl, index|
			{
				if (mode==1, {buffer=bufs[currentbank].choose}); // each one different
				pl.buf(buffer);
				this.newplotdata(pl.buf, views[target][index], target);
			}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	////


	action { |act=nil, value=1, random=0, time=0, offset=0, defer=0, r=nil, t=nil, o=nil, d=nil|
		var target = currentgroup; // freeze target in case of defer
		#random, time, offset, defer = [r?random, t?time, o?offset, d?defer];
		{
			grouplists[target].do({ |pl|
				{pl.performKeyValuePairs(act,
					[\value, value, \random, random, \time, time])}.defer(offset.asFloat.rand)
			})
		}.defer(defer)
	}

	trans { |value=1, random=0, time=0, offset=0, defer=0, r=nil, t=nil, o=nil, d=nil|
		//var result = (value * 2.log / 12).exp;
		this.action(\rate, value.midiratio, random, time, offset, defer, r, t, o, d);
	}

	rate { |value=1, random=0, time=0, offset=0, defer=0, r=nil, t=nil, o=nil, d=nil|
		this.action(\rate, value, random, time, offset, defer, r, t, o, d);
	}

	wobble {|value=0, random=0, time=0, offset=0, defer=0, r=nil, t=nil, o=nil, d=nil|
		this.action(\wobble, value, random, time, offset, defer, r, t, o, d);
	}

	brown {|value=0, random=0, time=0, offset=0, defer=0, r=nil, t=nil, o=nil, d=nil|
		this.action(\brown, value, random, time, offset, defer, r, t, o, d);
	}
	//(freq: 440.0, rate: 6, depth: 0.02, delay: 0.0, onset: 0.0, rateVariation: 0.04, depthVariation: 0.1, iphase: 0.0, trig: 0.0)
	vibrato {|value=1, depth=0, ratev=0, depthv=0, time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		var target = currentgroup; // freeze target in case of defer
		#time, offset, defer = [t?time, o?offset, d?defer];
		{grouplists[target].do({ |pl|
			{pl.vibrato(value,depth,ratev,depthv, time)}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	reverse {|time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		var target = currentgroup; // freeze target in case of defer
		#time, offset, defer = [t?time, o?offset, d?defer];
		{grouplists[target].do({ |pl|
			{pl.rate(pl.rate.neg, time:time)}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	//fwd {

	//}

	rwd {|time=2, offset=0, defer=0, t=nil, o=nil, d=nil|
		var target = currentgroup; // freeze target in case of defer
		#time, offset, defer = [t?time, o?offset, d?defer];
		{grouplists[target].do({ |pl|
			{
				var olrd=pl.rate;
				pl.rate(-1.8, time:time/2.0); //back accelerate
				{pl.rate(olrd, time:time/2.0)}.defer(time); // wait and finally slow down
			}.defer(offset.asFloat.rand)
		})}.defer(defer)

	}

	scratch {|target=0, tIn=1, tStay=0.5, tOut=1, offset=0, defer=0, o=nil, d=nil| // boomerang like pitch change
		var gtarget = currentgroup; // freeze target in case of defer
		#offset, defer = [o?offset, d?defer];
		{ grouplists[gtarget].do({ |pl|
			{pl.scratch(target, tIn, tStay, tOut)}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	dir {|value=1, time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\dir, value, 0, time, offset, defer, nil, t, o, d);
	}

	fwd {|time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\fwd, 0, 0, time, offset, defer, nil, t, o, d);
	}

	bwd {|time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\bwd, 0, 0, time, offset, defer, nil, t, o, d);
	}

	out { |ch=0|
		var target = currentgroup; // freeze target in case of defer
		grouplists[target].collect(_.out(ch))
	}

	spread { // each tape takes one out chanel
		grouplists.values.flat.do{|tap, n|
			tap.out(n)
		}
	}

	vol {|value=0, random=0, time=0, offset=0, defer=0, r=nil, t=nil, o=nil, d=nil|
		volume = value; // remember for the fadein/out
		this.action(\vol, value, random, time, offset, defer, r, t, o, d);
	}

	vold {
		var target = currentgroup; // freeze target in case of defer
		grouplists[target].collect(_.vold)
	}

	volu {
		var target = currentgroup; // freeze target in case of defer
		grouplists[target].collect(_.volu)
	}

	fadeout {|time=1, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\vol, 0, 0, time, offset, defer, nil, t, o, d);
	}

	fadein {|time=1, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\vol, volume, 0, time, offset, defer, nil, t, o, d); // volume is a var that stores the prev volume value
	}

	mute {|time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		volume = 0; // remember for the fadein/out
		this.action(\vol, volume, 0, time, offset, defer, nil, t, o, d);
	}

	pan { |value=0, random=0, time=0, offset=0, defer=0, r=nil, t=nil, o=nil, d=nil|
		this.action(\pan, value, random, time, offset, defer, r, t, o, d);
	}

	outb {|value, offset=0, defer=0, o=0, d=0|
		this.action(\bgo, value, 0, 0, offset, defer, nil, nil, o, d);
	}

	// BROWN RAND
	bframe {|range=0.01, offset=0, defer=0, o=0, d=0|
		var target = currentgroup; // freeze target in case of defer
		#offset, defer = [o?offset, d?defer];
		{
			grouplists[target].do({|pl, index|
				{
					pl.bframe(range);
					this.newselection(pl.st, pl.end, views[target][index], pl.buf, target);
				}.defer(offset.asFloat.rand)
			})
		}.defer(defer)
	}

	bmove {|range=0.1, offset=0, defer=0, o=nil, d=nil|
		this.action(\bmove, range, 0, 0, offset, defer, nil, nil, o, d);
	}

	bgo {|range=0.01, time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\bgo, range, 0, time, offset, defer, nil, t, o, d);
	}

	bvol {|range=0.01, time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\bvol, range, 0, time, offset, defer, nil, t, o, d);
	}

	bpan {|range=0.01, time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\bpan, range, 0, time, offset, defer, nil, t, o, d);
	}

	brate {|range=0.01, time=0, offset=0, defer=0, t=nil, o=nil, d=nil|
		this.action(\brate, range, 0, time, offset, defer, nil, t, o, d);
	}

	env {|vol=1, fadein=0.01, len=0.5, fadeout=0.1, offset=0, defer=0, o=nil, d=nil|
		var target = currentgroup;
		#offset, defer = [o?offset, d?defer];
		{grouplists[target].do({ |pl, index|
			{
				pl.vol(vol, time:fadein); //rump up
				{pl.vol(0,  time:fadeout)}.defer(fadein+len); // ramp down / fade len
			}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	xloop {|func, offset=0, defer=0, o=0, d=0|
		var target = currentgroup; // freeze target in case of defer
		#offset, defer = [o?offset, d?defer];
		func=func?{};
		{grouplists[target].do({ |pl, index|
			{
				pl.xloop = func;
			}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}

	xdone {|func, offset=0, defer=0, o=0, d=0|
		var target = currentgroup; // freeze target in case of defer
		#offset, defer = [o?offset, d?defer];
		func=func?{};
		{grouplists[target].do({ |pl, index|
			{
				pl.xdone = func;
			}.defer(offset.asFloat.rand)
		})}.defer(defer)
	}


	// TASKS
	does { procs.keys.postln; ^procs }
	pause {|name, defer=0, d=nil|
		defer = d?defer;
		{
			if (name.isNil, {
				"-- pause all _do".postln;
				procs.do{|p|p[0].pause};
			},{
				("-- _do: pause"+name+procs[name.asSymbol]).postln;
				procs[name.asSymbol][0].pause;
			})
		}.defer(defer)
	}
	resume {|name, defer=0, d=nil|
		defer = d?defer;
		{
			if (name.isNil, {
				"-- resume all _do".postln;
				procs.do{|p|p[0].resume};
			},{
				("-- _do: resume"+name+procs[name.asSymbol]).postln;
				procs[name.asSymbol][0].resume;
			})
		}.defer(defer)
	}
	undo {|name, defer=0, d=nil|
		defer = d?defer;
		{
			if (name.isNil, {
				"-- kill all _do".postln;
				try {
					procs.do{|p|p[0].stop};
					procs = Dictionary.new;
				}
			},{
				("-- _do: killing"+name+procs[name.asSymbol]).postln;
				try {
					procs[name.asSymbol][0].stop;
					procs.removeAt(name.asSymbol)
				}{ (name+"does not exist").postln};
			})
		}.defer(defer)
	}

	shutup {|flag=0|
		procs.do{|proc| proc[4]=flag}
	}

	// TO DO: sleep as bpm argument?
	do {|name="", function, sleep=1, random=0, defer=0, iter=inf, when=true, then,
		clock=0, verbose=1, s, r, d, i, w, t, c, v|
		var atask, target;
		sleep = s?sleep; defer=d?defer; iter=i?iter; when=w?when; then=t?then;
		random=r?random; clock=c?clock; verbose=v?verbose;

		iter=iter.max(0);

		if (name=="", {
			"TASKS MUST HAVE A NAME. Making one up".postln;
			name = ("T"++Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second).asSymbol;
			name.postln;
		});

		("creating _do"+name).postln;

		if (procs[name.asSymbol].notNil, {// kill before rebirth if already there
			procs[name.asSymbol][0].stop;
			procs.removeAt(name.asSymbol)
		});

		clock ?? clock = TempoClock; // default

		target = currentgroup;

		atask = Task({
			block {|break| // this block runs on task.start
				inf.do{
					if (when.value.asBoolean, { //go
						iter.do{|index|
							var wait = sleep.asString.asFloat; // reset each time
							var time = ""+Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second;
							var elapsed = Process.elapsedTime - procs[name.asSymbol][5];

							this.usegroup(target);
							function.value(index.asInteger); // only run if {when} is true. pass index

							if (sleep.isArray, {
								wait = sleep.wrapAt(index).asString.asFloat; //cycle //USE Pseq instead?
							});

							if (random.isArray,{
								if (random[1].isArray, {
									wait = random[0].wchoose(random[1]) ; // {values}, {chances}
								}, {
									wait = random.choose; // {n1, n2, n3}
								})
							},{
								wait = wait + random.asFloat.rand2
							});// rand gets added to sleep

							if (procs[name.asSymbol][4]==1, {
								("_do:"+name++time+elapsed+(index.asInteger+1)++":"++iter+"wait"+wait).postln});

							wait.max(0.005).wait;
						};
						break.value(999); // done iter. this will never happen when iter is inf
					});
					0.01.wait // needed for when
				}
			};

			then.value; // last will
			("-- done with"+name).postln;
			procs[name.asSymbol][0].stop;
			procs.removeAt(name.asSymbol)

		}, clock); // end definition of task

		procs.add(name.asSymbol -> [atask, function, sleep, random, verbose, 0]);// to keep track of them

		{
			atask.start; // now run it
			procs[name.asSymbol][5] = Process.elapsedTime
		}.defer(defer);
	}

	ggui { //controls groups
		var w=Window.new("tape groups", 500@(30+(grouplists.size*25))).front;
		var string;
		var clean={|data=""| // removes [ and ] from array.asString for displaying it in a statictext
			var res=data;
			if (data.isArray, {res=data.asString[2..data.asString.size-2]}, {res.asString});
			res;
		};
		w.alwaysOnTop = true;

		w.view.decorator = FlowLayout(w.view.bounds);

		grouplists.keys.do{|key|
			var bu = Button(w, 80@20).states_([
				[key, Color.red, Color.grey],
				[key, Color.black, Color.grey]
			])
			.action_({|bu|
				this.usegroup(key);
				if (bu.value==1, {
					this.stop
				}, {
					this.play
				})
			});
			EZSlider( w,         // parent
				200@20,    // bounds
				"amp",  // label
				ControlSpec(0, 2, \lin, 0.001, 1),     // controlSpec
				{ |ez|
					this.usegroup(key);
					this.vol(ez.value)
				}, // action
				labelWidth:30
			); //.numberView.maxDecimals = 3 ;
			EZSlider( w,         // parent
				200@20,    // bounds
				"freq",  // label
				ControlSpec(-2, 2, \lin, 0.001, 1),     // controlSpec
				{ |ez|
					this.usegroup(key);
					this.rate(ez.value)
				}, // action
				labelWidth:30
			); //.numberView.maxDecimals = 3 ;
			w.view.decorator.nextLine;
		};
		w.front;
	}

	dogui { // control do processes
		var w=Window.new("Current tasks", 275@(30+(procs.size*25))).front;
		var string;
		var clean={|data=""| // removes [ and ] from array.asString for displaying it in a statictext
			var res=data;
			if (data.isArray, {res=data.asString[2..data.asString.size-2]}, {res.asString});
			res;
		};
		w.alwaysOnTop = true;
		//w.layout = VLayout();
		w.view.decorator = FlowLayout(w.view.bounds);

		StaticText(w, 90@18).align_(\left).string_("Do").resize_(7);
		StaticText(w, 75@18).align_(\left).string_("Sleep").resize_(7);
		StaticText(w, 45@18).align_(\left).string_("Rand").resize_(7);
		StaticText(w, 45@18).align_(\left).string_("Trig").resize_(7);
		w.view.decorator.nextLine;

		procs.keys.do{|key|
			var bu = Button(w, 90@20).states_([
				[key, Color.red, Color.grey],
				[key, Color.black, Color.grey]
			])
			.action_({|bu|
				if (bu.value==1, {
					procs[key][0].pause
				}, {
					procs[key][0].resume
				})
			});

			string = clean.value(procs[key][2]);

			TextField(w, 75@20) // sleep
			.string_(string)
			.action_({|txt|
				this.do(key, procs[key][1], txt.value.asArray, procs[key][3]); //rebirth task w new sleep
				bu.value = 0;// display it
			});
			TextField(w, 45@20) // random
			.string_(procs[key][3].asString)
			.action_({|txt|
				this.do(key, procs[key][1], procs[key][2], txt.value.asFloat); //rebirth task w new rand
				bu.value = 0;// display it
			});
			Button(w, 30@20).states_([ // once
				["once", Color.black, Color.grey]
			])
			.action_({|bu|
				procs[key][1].value
			});
			w.view.decorator.nextLine;
		};


	}
	////////////////////////////

	/*	// compressor/expander ///
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
	*/

	loadonsetanalysis {|buffer, filepath| // onset analisys file from Segmentation(s)
		var	data = Object.readArchive(filepath);
		data.do{|onset, i|
			data[i][0] = data[i][0] / buffer.numFrames
		};
		onsets.put(PathName(buffer.path).fileName, data)
	}

	updateplot {|buf, aview, agroup| // draw the choosen buffer
		if (controlGUI[agroup].notNil, {
			var f = { |b,v|
				b.loadToFloatArray(action: { |a| { v.setData(a) }.defer });
				//v.gridResolution(b.duration/10); // I would like to divide the window in 10 parts no matter what the sound dur is. Cannot change gridRes on the fly?
			};
			buf !? f.(buf, aview)
		});
	}

	control {|group, cwidth, cheight, defer=0, d|
		var gap=0, height=0;
		var target = group?currentgroup; // freeze target in case of defer
		if (grouplists[target].isNil, {target=\default; "group does not exist!".postln}); // default if not there
		defer=d?defer;
		{
			if (controlGUI[target].isNil, {
				controlGUI[target] = Window( ("Tapes from group"+target), Rect(500, 200, cwidth?500, cheight?700));
				controlGUI[target].alwaysOnTop = true;
				controlGUI[target].front;
				controlGUI[target].onClose = {
					controlGUI[target] = nil;
					grouplists[target].do({|play| play.view = nil });
					plotwinrefresh.stop;
				};
				("OPENING CONTROL GUI: group"+target).postln;

				//height = controlGUI.bounds.height/howmany;

				controlGUI[target].layout = VLayout();
				// 		"To zoom in/out: Shift + right-click + mouse-up/down".postln;
				// 		"To scroll: right-click + mouse-left/right".postln;
				views[target].size.do({|index|
					views[target][index] = SoundFileView().timeCursorOn_(true)
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
						grouplists[target][index].st( x.linlin(0, thisview.bounds.width, 0,1) ) // what about when zoomed in?
					})
					.mouseUpAction_({ |thisview, x, y, mod|
						grouplists[target][index].end( x.linlin(0, thisview.bounds.width, 0,1) )
					});
					controlGUI[target].layout.add(views[target][index]);

					grouplists[target][index].view = views[target][index];
					grouplists[target][index].updateframepoints();
					this.newplotdata(grouplists[target][index].buf, views[target][index], target);

					//grouplists.values.flat[index].view = views[target][index];// to update loop point when they change
					//grouplists.values.flat[index].updatelooppoints();

					//this.newplotdata(grouplists.values.flat[index].buf, views[target][index]);
				});

				plotwinrefresh = Task({
					inf.do({|index|
						views[target].do({|view, index|
							view.timeCursorPosition = grouplists.values.flat[index].curpos * (grouplists.values.flat[index].buf.numFrames);
							0.1.wait;
						});
						//"----".postln;
					})
				}, AppClock);
				plotwinrefresh.start;
			});
		}.defer(defer)
	}

	newplotdata {|buf, view, group=\default|
		if (controlGUI[group].notNil, {
			if (buf.path.notNil, {
				var sf = SoundFile.new;
				sf.openRead(buf.path);
				view.soundfile = sf;
				view.read(0, sf.numFrames);     // read in the entire file.
			}, {
				buf.loadToFloatArray(action: { |a| { view.setData(a, channels:2) }.defer })
			});

			view.refresh;
		})
	}

	newselection {|st, end, view, buf, group=\default|
		if (controlGUI[group].notNil, {
			view.setSelectionStart(0, (buf.numFrames) * st); // loop the selection
			view.setSelectionSize(0, (buf.numFrames) * (end-st));
		})
	}
}

