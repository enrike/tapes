/* single tape
*/

Tape{

	var <id, <player, <curpos;
	var buf, st=0, end=1, vol=1, rate=0, pan=0, bus=0, len=0, dur=0, dir=1, wobble=0, brown=0, vib; // state variables hidden
	var memrate=1; // to store rate while stopped
	var <>view=nil;
	var <>statesDic, <>verbose=false, loopOSC, playheadOSC;

	*new {| buffer=nil, bus=0 |
		^super.new.initTape( buffer, bus );
	}

	initTape {| abuffer, abus |
		buf = abuffer;
		bus = abus;

		vib = [1,0,0,0,0,0,0];

		id = UniqueID.next;

		player.free;
		player = Synth.tail(Server.default, \rPlayer, [\buffer, buf ? buf.bufnum, \rate, rate, \index, id, \out, abus]);

		loopOSC.free;
		loopOSC = OSCdef(\loop++id, {|msg, time, addr, recvPort|
			if (id==msg[2], { this.done });
		}, '/loop');

		playheadOSC.free;
		playheadOSC = OSCdef(\playhead++id, {|msg, time, addr, recvPort|
			if (id==msg[2], { curpos = msg[3] });
		}, '/pos');

		statesDic = Dictionary.new;
		("-----------").postln;
		("ready tape ID"+id).postln;
		("using buffer"+this.file).postln;
	}

	kill {
		player.free;
		loopOSC.free;
		playheadOSC.free;
		statesDic=nil;
	}

	done {} // when loop crossing happens

	/*	loadbuf {|server, path|
	var abuf = Buffer.read(server, path, action:{ this.setbuf(abuf) });
	}*/

	search {|st|
		^this.file().containsi(st) // no case sensitive
	}

	duration { // buffer len in msecs
		^((buf.numFrames/buf.sampleRate)*1000).asInt // buf.numChannels/
	}

	state {|state|
		if (state.isNil, {
			var state = Dictionary.new;
			state.put(\buf, buf);
			state.put(\st, st);
			state.put(\end, end);
			state.put(\vol, vol);
			state.put(\rate, rate);
			state.put(\dir, dir);
			state.put(\pos, curpos);
			state.put(\wobble, wobble);
			state.put(\brown, brown);
			state.put(\vibrato, vib);
			state.put(\panning, pan);
			^state
		}, {
			this.buf(state[\buf]);
			this.st(state[\st]);
			this.end(state[\end]);
			this.vol(state[\vol]);
			this.go(state[\pos]);
			this.rate(state[\rate]);
			this.wobble(state[\wobble]);
			this.brown(state[\brown]);
			this.vibrato(state[\vibrato][1], state[\vibrato][2], state[\vibrato][5], state[\vibrato][6]); //rate=1, depth=0, ratev=0, depthv=0
			this.dir(state[\dir]);
			this.pan(state[\panning]);
		});
	}

	copy {|tape|
		this.state( tape.state() )
	}

	push { |which|
		statesDic[which] = this.state;
		this.post("pushing state", which);
		if(verbose.asBoolean, {["pushing state", which].postln});
	}

	pop {|which|
		statesDic.postln;
		this.state( statesDic[which] );
		this.post("poping state", which);
		if(verbose.asBoolean, {["poping state", which].postln});
	}

	updatelooppoints {
		if (view.notNil, {
			{
				view.timeCursorOn = true;
				view.setSelectionStart(0, (buf.numFrames) * st); // loop the selection
				view.setSelectionSize(0, (buf.numFrames) * (end-st)); // len
				view.readSelection.refresh;
			}.defer;
		})
	}

	info {
		("-- Tape ID"+id+"--").postln;
		this.file().postln;
		["curpos", curpos].postln;
		["volume", vol].postln;
		["loop", st, end].postln;
		["rate", rate].postln;
		["direction", dir].postln;
		["panning", pan].postln;
		["wobble", wobble].postln;
		["brown", brown].postln;
		["vibrato", vib].postln;
		["verbose", verbose].postln;
		"--------------".postln;
	}

	post {|action, value|
		if (verbose.asBoolean, {[id, action, value].postln});
	}

	go {|pos=0|
		player.set(\reset, pos.clip(st,end));
		player.set(\trig, 0);
		{ player.set(\trig, 1) }.defer(0.04);
	}

	gost {this.go(st)}

	goend {this.go(end)}

	move {|pos=0, random=0| // moves the loop to another position maintaing the duration
		pos = pos + random.asFloat.rand2;
		this.loop(pos, pos+(end-st)); //keep len
	}

	moveby {|delta=0, random=0| // moves the loop to another position maintaing the duration
		delta = delta + random.asFloat.rand2;
		this.loop(st+delta, st+delta+(end-st)); //keep len
	}

	file {
		^PathName(buf.path).fileName
	}

	outb {|abus=nil|
		if (abus.notNil, {
			bus = abus;
			player.set(\out, bus)
		}, {
			^bus
		})
	}

	pan {|apan=nil, time=0|
		if (apan.notNil, {
			pan = apan;
			player.set(\panlag, time);
			player.set(\pan, apan);
			this.post("pan", pan);
		}, {
			^pan
		})
	}

	out {|ch=0|
		player.set(\out, ch);
	}

	vol {|avol=nil, time=0, random=0|
		if (avol.notNil, {
			avol = avol + random.asFloat.rand2;

			vol = avol.clip(0,1); //limits

			player.set(\amplag, time);
			player.set(\amp, avol);

			{player.set(\ampgate, 1)}.defer(0.05);

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
		wobble = arate;
		player.set(\wobblelag, time);
		player.set(\wobble, arate);
	}

	brown {|level=0, time=0, random=0|
		level = level + random.asFloat.rand2;
		brown = level;
		player.set(\brownlag, time);
		player.set(\brown, level.max(0));
	}

	vibrato { |rate=1, depth=0, ratev=0, depthv=0, time=0|
		vib = [1, rate, depth, 0, 0, ratev, depthv];
		player.set(\viblag, time);
		player.setn(\vib, vib)
	}

	dir {|adir=1, time=0|
		dir = adir;
		player.set(\dir, adir)
	}

	rate {|arate=nil, time=0, random=0|
		if (arate.notNil, {
			arate = arate + random.asFloat.rand2;
			//if (rate != 0, { // only update if playing
			player.set(\ratelag, time);
			player.set(\rate, arate);
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
		});

		st = args[0].clip(0,1);//.clip(0, 1-(end-st));
		end = args[1].clip(st,1);

		if (st==end,{(id+"warning. start and end point are equal!").postln});

		player.set(\start, st);
		player.set(\end, end);
		//[st, end].postln;
		this.post("loop", st.asString+"-"+end.asString);
		this.updatelooppoints; //only if w open
	}

	st {|p, random=0|
		if (p.notNil, {
			st = p + random.asFloat.rand2;
			player.set(\start, st);
			this.updatelooppoints; //only if w open
		}, {
			^st
		});
	}

	end {|p, random=0|
		if (p.notNil, {
			end = p + random.asFloat.rand2;
			player.set(\end, end);
			this.updatelooppoints; //only if w open
		}, {
			^end
		})
	}

	dur {|adur, random=0|
		if (adur.notNil, {
			end = st + adur + random.asFloat.rand2;
			player.set(\end, end);
			this.post("end", end);
			this.updatelooppoints; //only if w open
		}, {
			^dur
		})
	}

	len {|ms| // IN MILLISECONDS
		if (ms.notNil, {
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

	stop {
		memrate = rate; // store
		this.rate(0)
	}

	play {
		this.rate(memrate); // retrieve stored value
	}

	shot { // this should play the sound only once but using all the properties. maybe use another synthdef to do it.
		//if (rate!=0, { this.pause });
		Synth(\ShotPlayer, [\buffer, buf, \start, st, \end, end, \amp, vol, \rate, memrate,
			\pan, pan, \index, id, \out, bus]);
	}

	buf {|abuf|
		if (abuf.notNil, {
			buf = abuf;
			player.set(\buffer, buf.bufnum);
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
	player.set(\buffer, buf.bufnum)
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
		var pos = range.rand;
		this.loop(pos, pos+(end-st)); //keep len
	}

	rst {|range=1.0|
		//st = range.asFloat.rand;
		this.st(range.asFloat.rand);
		//player.set(\start, st);
		//this.updatelooppoints; //only if w open
	}

	rend {|range=1.0|
		this.end(range.asFloat.rand)
		//end = range.asFloat.rand; // total rand
		//player.set(\end, end);
		//this.updatelooppoints; //only if w open
	}

	rlen {|range=0.5|
		this.end(st + range.asFloat.rand)
		//end = st + range.asFloat.rand; // rand from st point
		//player.set(\end, end);
		//this.updatelooppoints; //only if w open
	}

	rdir {|time=0|
		this.dir([1,-1].choose, time)
	}

	rrate {|time=0|
		this.rate(1.0.rand2, time)
	}

	rand {|time=0|
		this.rpan(time);
		this.rrate(time);//??
		this.rdir(time);
		this.rloop;
		this.rgo;// this should be limited to the current loop
		this.rvol(time:time); // why?
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
