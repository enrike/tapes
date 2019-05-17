/* single layer
*/

Layer{

	var <id, <play, <curpos;
	var buf, st=0, end=1, vol=1, rate=0, pan=0, bus=0, len=0, dur=0, bounds=0; // state variables
	var memrate=1; // to store rate while paused
	//var <ptask, <vtask, <rtask;
	//var plotview, plotwin=nil;
	var <>statesDic, <>verbose=false;
//	var test;

	*new {| id=0, buffer = nil, bus |
		^super.new.initLayer( id, buffer, bus );
	}

/*	test{|val|
		if (val.isNil.not, {
			test = val;
		}, {
			^test;
		})
	}*/

	initLayer {| aid, abuffer, abus |
		var initbuf;
		id = aid; // just in case I need to identify later
		buf = abuffer;

		bounds = [0,1];

		if(buf.isNil.not, { initbuf = buf.bufnum }); // only if specified. otherwise nil

		play.free;
		play = Synth(\StPlayer, [\buffer, initbuf, \rate, rate, \index, id, \out, abus]);

		OSCdef(\playhead++id).clear;
		OSCdef(\playhead++id).free;
		OSCdef(\playhead++id, {|msg, time, addr, recvPort|
			if (id==msg[2], { curpos = msg[3] });
		}, '/tr', NetAddr("127.0.0.1", 57110));

		statesDic = Dictionary.new;

		("ready layer @"++id).postln;
	}

	/*newplayer {|asynth| // experimental. not used.
		play.free; // get rid of the old one
		play = Synth(asynth, [\buffer, buf.bufnum, \rate, rate]);
	}*/

	loadbuf {|server, path| // actually loads a file from disk into the server and sets it as current buffer used by this player
		var abuf = Buffer.read(server, path);
		{ this.setbuf(abuf) }.defer(0.1); // make sure it has been loaded
		this.post("loading", path);
		if(verbose.asBoolean, {["loading", path].postln});
	}

	search {|st|
		^this.file().containsi(st) // no case sensitive
	}

	duration { // buffer len in msecs
		^((buf.numFrames/buf.sampleRate)*1000).asInt // buf.numChannels/
	}

	push { |which|
		var state = Dictionary.new;
		which.postln;
		state.put(\buf, buf);
		state.put(\st, st);
		state.put(\end, end);
		state.put(\vol, vol);
		state.put(\rate, rate);
		state.put(\panning, pan);

		statesDic[which] = state;

		this.post("pushing state", which);
		if(verbose.asBoolean, {["pushing state", which].postln});
	}

	pop {|which|
		var state;
		statesDic.postln;
		state = statesDic[which];

		//statesDic.postln;
		this.buf( state[\buf] );
		this.bounds( state[\st], state[\end] );
		this.vol( state[\vol] );
		this.rate( state[\rate] );
		this.pan( state[pan] );
		this.post("poping state", which);
		if(verbose.asBoolean, {["poping state", which].postln});
	}

/*	plot {
		if (plotwin.isNil, {
			// to do: bigger size win and view
			// move playhead as it plays?
			plotwin = Window("Buffer"+id, Rect(100, 200, 600, 300));
			plotwin.alwaysOnTop=true;
			//plotwin.setSelectionColor(0, Color.red);
			plotwin.front;
			plotwin.onClose = { plotwin = nil }; // needed?

			plotview = SoundFileView(plotwin, Rect(0, 0, 600, 300))
			.elasticMode(true)
			.timeCursorOn(true)
			.timeCursorColor(Color.red)
			.drawsWaveForm(true)
			.gridOn(true)
			.gridResolution(1)
			.gridColor(Color.white)
			.waveColors([ Color.new255(103, 148, 103), Color.new255(103, 148, 103) ])
			.background(Color.new255(155, 205, 155))
			.canFocus(false)
			.setSelectionColor(0, Color.grey);
			plotview.mouseUpAction = {
				var cs = plotview.selections[plotview.currentSelection];
				st = (cs[0]/buf.numFrames)/buf.numChannels;
				end = ((cs[0]+cs[1])/buf.numFrames)/buf.numChannels; //because view wants start and duration
				play.set(\start, st);
				play.set(\end, end);
				["new loop:", st, end].postln;
			};
			"To zoom in/out: Shift + right-click + mouse-up/down".postln;
			"To scroll: right-click + mouse-left/right".postln;
		});
		this.updateplot; // draw the data and refresh
	}

	updateplot {
		if (plotwin.isNil.not, {
			var f = { |b,v| b.loadToFloatArray(action: { |a| { v.setData(a) }.defer }) };

			// TO DO: thiss does not work for some reason. maybe something to do with the supercollider version
			{
				plotview.timeCursorOn = true;
				plotview.setSelectionStart(0, (buf.numFrames*buf.numChannels) * st); // loop the selection
				plotview.setSelectionSize(0, (buf.numFrames*buf.numChannels) * (end-st));
				plotview.readSelection.refresh;
			}.defer;

			f.(buf, plotview); //
		});
	}*/

	info {
		("-- Layer"+id+"--").postln;
		this.file().postln;
		["volume", vol].postln;
		["bounds", st, end].postln;
		["rate", rate].postln;
		["panning", pan].postln;
		["verbose", verbose].postln;
		"--------------".postln;
	}

	post {|action, value|
		if (verbose, {[id, action, value].postln});
	}

	jump {|pos=0|
		if (pos<st, {pos=st}); // limits
		if (pos>end, {pos=end});
		play.set(\reset, pos);
		play.set(\trig, 0);
		{ play.set(\trig, 1) }.defer(0.05);
	}

	file {
		^buf.path.split($/).last;
	}

	outb {|abus=nil|
		if (abus.isNil.not, {
			bus = abus;
			play.set(\out, bus)
		}, {
			^bus
		})
	}

	pan {|apan=nil, time=0, curve=\lin|
		if (pan.isNil.not, {
			pan = apan;
			play.set(\pancur, curve);
			play.set(\pangate, 0);

			play.set(\pantarget, pan);
			play.set(\pandur, time);

			{play.set(\pangate, 1)}.defer(0.01);

			this.post("pan", pan);
		}, {
			^pan
		})
	}

	vol {|avol=nil, time=0, curve=\exp|
		if (avol.isNil.not, {
			if (avol< 0, {avol=0}); //lower limit
			vol = avol;

			play.set(\ampcur, curve);
			play.set(\ampgate, 0);

			play.set(\amptarget, vol);
			play.set(\ampdur, time);

			{play.set(\ampgate, 1)}.defer(0.01);

			this.post("volume", (vol.asString + time.asString + curve.asString) );
		}, {
			^vol
		})
	}

	vold {
		this.vol(vol-0.02)
	}
	volu {
		this.vol(vol+0.02)
	}

	rate {|arate=nil, time=0, curve=\lin|
		if (arate.isNil.not, {
			rate = arate;
			play.set(\ratecur, curve);
			play.set(\rategate, 0);

			play.set(\ratetarget, rate);
			play.set(\ratedur, time);

			{play.set(\rategate, 1)}.defer(0.01);

			this.post("rate", rate);
		}, {
			"return rate".postln;
			^rate
		})
	}

	reverse {
		this.rate(rate.neg)
	}

	boom {|target=0, tIn=1, tStay=0.5, tOut=1, curve=\lin| // boomerang like pitch change
		var restore = rate;//
		this.rate(target, tIn, curve);
		{this.rate(restore, tOut, curve)}.defer(tIn+tStay);
	}

	mir { |time=0| // mirror
		// this should change play mode to mirror <>
	}

	fwd {
		if (rate<0, {this.reverse})
	}
	bwd {
		if (rate>0, {this.reverse})
	}

	reset {
		this.bounds(0,1);
		this.jump(0);
		this.rate(1);
	}

	bounds {|...args|
		if (args.size==0, {
			^bounds
		}, {
			if (args.size==1, {
				st = args[0][0]; // st and end are variables in this class. they must be updated as well
				end = args[0][1]
			});
			if (args.size==2, {
				st = args[0]; // st and end are variables in this class. they must be updated as well
				end = args[1]
			});

			play.set(\start, st);
			play.set(\end, end);
			this.post("bounds", st.asString+"-"+end.asString);
			//this.updateplot; //only if w open
		})
	}

	st {|p|
		if (p.isNil.not, {
			st=p;
			play.set(\start, st)
		}, {
			^st
		})
	}

	end {|p=0|
		if (p.isNil.not, {
			end=p;
			play.set(\end, end)
		}, {
			^end
		})
	}

	dur {|adur|
		if (adur.isNil.not, {
			end = st + adur;
			play.set(\end, end);
			this.post("end", end);
			//this.updateplot; //only if w open
		}, {
			^dur
		})
	}

	len {|ms| // IN MILLISECONDS
		if (ms.isNil.not, {
			var adur= ms / ((buf.numFrames/buf.sampleRate)*1000 ); // from millisecs to 0-1
			this.dur(adur)
		}, {
			^len
		})
	}

	pause {
		memrate = rate; // store
		this.rate(0)
	}

	resume {
		this.rate(memrate); // retrieve stored value
	}

	buf {|abuf|
		if (abuf.isNil.not, {
			buf = abuf;
			play.set(\buffer, buf.bufnum);
			this.post("buffer", this.file());
			//this.updateplot; //only if w open
		}, {
			^buf
		})
	}

	rvol {|limit=1.0, time=0, curve=\lin |
		this.vol( limit.rand, time, curve );
	}

	rpan {|time=0, curve=\lin| // -1 to 1
		this.pan( 1.asFloat.rand2, time, curve )
	}

	/*rbuf {
		buf = buffers.choose;
		play.set(\buffer, buf.bufnum)
	}*/

	rjump {|range=1|
		var target = rrand(st.asFloat, end.asFloat);
		this.jump(target)
	}

	rbounds {|st_range=1, len_range=1|
		st = st_range.asFloat.rand;
		end = st + len_range.asFloat.rand;
		if (end>1, {end=1}); //limit. maybe not needed
		this.bounds(st, end);
	}

	rst {|range=1.0|
		st = range.asFloat.rand;
		play.set(\start, st);
		this.updateplot; //only if w open
	}

	rend {|range=1.0|
		end = range.asFloat.rand; // total rand
		play.set(\end, end);
		this.updateplot; //only if w open
	}

	rlen {|range=0.5|
		end = st + range.asFloat.rand; // rand from st point
		play.set(\end, end);
		this.updateplot; //only if w open
	}

	rdir {|time=0, curve=\lin|
		this.rate(rate * [1,-1].choose, time, curve)
	}

	rrate {|time=0, curve=\lin|
		this.rate(1.0.rand2, time, curve)
	}

	// set start
	// set len

	bbounds {|range=0.01| this.bounds() }// **** NOT WORKING ***** single step brown variation

	bjump {|range=0.01| this.jump( curpos+(range.rand2)) }// single step brown variation

	bvol {|range=0.05, time=0, curve=\lin|
		this.vol( vol+(range.rand2), time, curve)
	}// single step brown variation

	bpan {|range=0.1, time=0, curve=\lin|
		this.pan( pan+(range.rand2), time, curve)
	}

	brate {|range=0.05, time=0, curve=\lin|
		this.rate( rate+(range.rand2), time, curve )
	}// single step brown variation
/*
	brownpos {|step=0.01, sleep=5.0, dsync=0, delta=0|
		if (sleep <= 0, {sleep = 0.01}); // limit
		ptask.stop; // CORRECT??
		ptask = Task({
			inf.do({ arg i;
				{
					var len;
					st = st + step.asFloat.rand2 + delta;
					end = end + step.asFloat.rand2;
					if (end<st, {end=st+0.005}); //correct this!!
					len = end-st;
					if(st<0, {st=0});// limits
					if(end>1, {end=1});
					if(st>(1-len), {st=(1-len)});// limits
					//if(end>(1-len), {end=(1-len)});
					this.bounds(st, end)
				}.defer(dsync.asFloat.rand); //out of sync all of them?
				sleep.wait;
			});
		});

		ptask.start;
	}


	brownvol {|step=0.01, sleep=5.0, dsync=0, delta=0|
		if (sleep <= 0, {sleep = 0.01}); // limit
		vtask.stop; // is this CORRECT???
		vtask = Task({
			inf.do({ arg i;
				vol = vol + step.asFloat.rand2 + delta;
				if (vol<0, {vol=0});// no negative volume values
				{ play.set(\amp, vol) }.defer(dsync.asFloat.rand); //out of sync all of them?
				sleep.wait;
			});
		});

		vtask.start;
	}

	brownrate {|step=0.01, sleep=5.0, dsync=0, delta=0|
		if (sleep <= 0, {sleep = 0.01}); // limit
		rtask.stop; // is this CORRECT???
		rtask = Task({
			inf.do({ arg i;
				var lag=0;
				rate = rate + step.asFloat.rand2 + delta;
				{ play.set(\rate, rate) }.defer(dsync.asFloat.rand); //out of sync all of them?
				sleep.wait;
			});
		});

		rtask.start;
	}
	*/
}
