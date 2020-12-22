/* single layer
*/

Layer{

	var <id, <play, <curpos;
	var buf, st=0, end=1, vol=1, rate=0, pan=0, bus=0, len=0, dur=0, dir=1; // state variables hidden
	var memrate=1; // to store rate while paused
	var >view=nil;
	var <>statesDic, <>verbose=false, loopOSC, playheadOSC;

	*new {| id=0, buffer = nil, bus=0 |
		^super.new.initLayer( id, buffer, bus );
	}

	initLayer {| aid, abuffer, abus |
		id = aid; // just in case I need to identify later
		buf = abuffer;
		bus = abus;

		play.free;

		play = Synth.tail(Server.default, \rPlayer, [\buffer, buf ? buf.bufnum, \rate, rate, \index, id, \out, abus]);

		loopOSC.free;
		loopOSC = OSCdef(\loop++id, {|msg, time, addr, recvPort|
			if (id==msg[2], { this.done });
		}, '/loop');

		playheadOSC.free;
		playheadOSC = OSCdef(\playhead++id, {|msg, time, addr, recvPort|
			if (id==msg[2], { curpos = msg[3] });
		}, '/pos');

		statesDic = Dictionary.new;

		("ready layer @"++id).postln;
	}

	done {} // when loop crossing happens

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

		this.buf( state[\buf] );
		this.loop( state[\st], state[\end] );
		this.vol( state[\vol] );
		this.rate( state[\rate] );
		this.pan( state[pan] );
		this.post("poping state", which);
		if(verbose.asBoolean, {["poping state", which].postln});
	}

	updatelooppoints {
		if (view.isNil.not, {
			{
				view.timeCursorOn = true;
				view.setSelectionStart(0, (buf.numFrames) * st); // loop the selection
				view.setSelectionSize(0, (buf.numFrames) * (end-st));
				view.readSelection.refresh;
			}.defer;
		})
	}

	info {
		("-- Layer"+id+"--").postln;
		this.file().postln;
		["curpos", curpos].postln;
		["volume", vol].postln;
		["loop", st, end].postln;
		["rate", rate].postln;
		["panning", pan].postln;
		["verbose", verbose].postln;
		"--------------".postln;
	}

	post {|action, value|
		if (verbose.asBoolean, {[id, action, value].postln});
	}

/*	actualsynthrate {
		play.get(\rate, {|val| rate=val})
	}*/

	go {|pos=0|
		//if (pos<st, {pos=st}); // limits
		//if (pos>end, {pos=end});
		play.set(\reset, pos);
		play.set(\trig, 0);
		{ play.set(\trig, 1) }.defer(0.04);
	}

	gost {this.go(st)}

	goend {this.go(end)}

	move {|pos=0, random=0|
		pos = pos + random.asFloat.rand2;
		this.loop(pos, pos+(end-st)); //keep len
		//this.go(pos);
	}

	step {|gap=0, random=0|
		var pos = st + gap + random.asFloat.rand2;
		this.loop(pos, pos+(end-st))
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

	pan {|apan=nil, time=0|
		if (apan.isNil.not, {
			pan = apan;
			play.set(\panlag, time);
			play.set(\pan, apan);
			this.post("pan", pan);
		}, {
			^pan
		})
	}

	out {|ch=0|
		play.set(\out, ch);
	}

	vol {|avol=nil, time=0, random=0|
		if (avol.isNil.not, {
			//if (random>0, {avol = random.asFloat.rand2});
			avol = avol + random.asFloat.rand2;

			vol = avol.clip(0,1); //limits

			play.set(\amplag, time);
			play.set(\amp, avol);

			{play.set(\ampgate, 1)}.defer(0.05);

			this.post("volume", (vol.asString + time.asString  + random.asString) );
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

	fadeout {|time=1|
		this.vol(0, time)
	}

	fadein {|time=1|
		this.vol(vol, time)
	}

	wobble {|arate=0, time=0, random=0|
		arate = arate + random.asFloat.rand2;
		play.set(\wobblelag, time);
		play.set(\wobble, arate);
	}

	dir {|adir=1, time=0|
		dir = adir;
		play.set(\dir, adir)
	}

	rate {|arate=nil, time=0, random=0|
		if (arate.isNil.not, {
			arate = arate + random.asFloat.rand2;
			//if (rate != 0, { // only update if playing
			play.set(\ratelag, time);
			play.set(\rate, arate);
			//});
			memrate = rate;
			rate = arate;
			this.post("rate", rate);
		}, {
			^rate
		})
	}

	reverse {
		this.dir(dir.neg)
	}

	// mirror? boomerang??
	scratch {|target=0, tIn=1, tStay=0.5, tOut=1| // boomerang like pitch change
		var restore = rate;//
		this.rate(target, tIn);
		{this.rate(restore, tOut)}.defer(tIn+tStay);
	}

	mir { |time=0| // mirror
		// this should change play mode to mirror <>
	}

	fwd {
		if (dir<0, {this.reverse})
	}
	bwd {
		if (dir>0, {this.reverse})
	}

	reset {
		this.loop(0,1);
		this.go(0);
		this.rate(1);
	}

	loop {|...args|
		if (args.size==0, {
			^[st, end]
		}, {
			/*			if (args[1].isNil,
			{end = args[0] + (end-st)}, // keep the len
			{end = args[1]}
			);*/

			end = args[1];
			st = args[0];

			play.set(\start, st);
			play.set(\end, end);
			//[st, end].postln;
			this.post("loop", st.asString+"-"+end.asString);
			this.updatelooppoints; //only if w open
		})
	}

	st {|p, random=0|
		if (p.isNil.not, {
			st = p + random.asFloat.rand2;
			play.set(\start, st);
			this.updatelooppoints; //only if w open
		}, {
			^st
		});
	}

	end {|p, random=0|
		if (p.isNil.not, {
			end = p + random.asFloat.rand2;
			play.set(\end, end);
			this.updatelooppoints; //only if w open
		}, {
			^end
		})
	}

	dur {|adur, random=0|
		if (adur.isNil.not, {
			end = st + adur + random.asFloat.rand2;
			play.set(\end, end);
			this.post("end", end);
			this.updatelooppoints; //only if w open
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

	shot { // this should play the sound only once but using all the properties. maybe use another synthdef to do it.
		//if (rate!=0, { this.pause });
		Synth(\ShotPlayer, [\buffer, buf, \start, st, \end, end, \amp, vol, \rate, memrate,
			\pan, pan, \index, id, \out, bus]);
	}

	buf {|abuf|
		if (abuf.isNil.not, {
			//if (buf.isInteger, {buf = bufs[buf]}); // using the index

			buf = abuf;
			play.set(\buffer, buf.bufnum);
			this.post("buffer", this.file());
			this.updatelooppoints; //only if w open
		}, {
			^buf
		})
	}

	rvol {|limit=1.0, time=0 |
		this.vol( limit.rand, time );
	}

	rpan {|time=0| // -1 to 1
		this.pan( 1.asFloat.rand2, time )
	}

	/*rbuf {
	buf = buffers.choose;
	play.set(\buffer, buf.bufnum)
	}*/

	rgo {|range=1|
		var target = rrand(st.asFloat, end.asFloat);
		this.go(target)
	}

	rloop {|st_range=1, len_range=1|
		st = st_range.asFloat.rand;
		end = st + len_range.asFloat.rand;
		if (end>1, {end=1}); //limit. maybe not needed
		this.loop(st, end);
	}

	rmove {|range=1|
		// go to random point update the loop points but maintan the length
		//this.loop()
	}

	rst {|range=1.0|
		//st = range.asFloat.rand;
		this.st(range.asFloat.rand);
		//play.set(\start, st);
		//this.updatelooppoints; //only if w open
	}

	rend {|range=1.0|
		this.end(range.asFloat.rand)
		//end = range.asFloat.rand; // total rand
		//play.set(\end, end);
		//this.updatelooppoints; //only if w open
	}

	rlen {|range=0.5|
		this.end(st + range.asFloat.rand)
		//end = st + range.asFloat.rand; // rand from st point
		//play.set(\end, end);
		//this.updatelooppoints; //only if w open
	}

	rdir {|time=0|
		this.dir([1,-1].choose, time)
	}

	rrate {|time=0|
		this.rate(1.0.rand2, time)
	}

	// set start
	// set len

	bloop {|range=0.01|
		this.loop(this.st+range.rand2, this.end+range.rand2)
	}

	bgo {|range=0.01| this.go( curpos+(range.rand2)) }// single step brown variation

	bvol {|range=0.05, time=0|
		this.vol( vol+(range.asFloat.rand2), time)
	}// single step brown variation

	bpan {|range=0.1, time=0|
		this.pan( pan+(range.asFloat.rand2), time)
	}

	brate {|range=0.05, time=0|
		this.rate( rate+(range.rand2), time )
	}// single step brown variation
}
