/* single ḉtape
*/

Tape{

	var <id, <player, <curpos, <loops= -1, out=0, sttime, markers, markerindex=0, fps=12;
	var buf, st=0, end=1, vol=0, rate=0, pan=0, len=0, dur=0, dir=1, wobble=0, brown=0, vib; // state variables hidden
	var memrate=0; // to store rate while stopped
	var <>del=0.02; //time in between two consecutive p.set. required by gates
	var <>view=nil;
	var <>statesDic, <>verbose=false, loopOSC, playheadOSC, xloopF, xdoneF, targetloops=inf;

	*new {| buffer=nil |
		^super.new.initTape( buffer );
	}

	initTape {| abuffer |
		buf = abuffer;
		//	delta = 0.005;

		vib = [1,1,0,0,0,0,0];

		id = UniqueID.next;
		this.redoplayer;

		sttime = Process.elapsedTime;

		markers = Dictionary.new;
		//markerFunc = {};

		xloopF = {};
		xdoneF = {};
		loopOSC.free;
		loopOSC = OSCdef(\xloop++id, {|msg, time, addr, recvPort|
			if (id==msg[2], {
				//(\xloop++id).postln;
				if (loops >= (targetloops-1), { this.done }); // done
				if (rate!=0, {loops = loops + 1}); // not when stopped
				if (loops > 1, { // not the first round
					xloopF.value(this, loops)
				});
			});
		}, '/xloop'); // this might be not the most efficient way to do it. better /xloop++id ??

		playheadOSC.free;
		playheadOSC = OSCdef(\playhead++id, {|msg, time, addr, recvPort|
			if (id==msg[2], {
				var nextmark = markers.keys.asArray.sort.wrapAt(markerindex);
				curpos = msg[3];
				if (nextmark.notNil, {
					if (curpos.equalWithPrecision(nextmark.asFloat, 0.5/(buf.duration*(fps)) ), {
						markers[nextmark].value(curpos);
						markerindex = markerindex + 1;
					});
				});
			});
		}, '/pos'); // this might be not the most efficient way to do it. better /pos++id ??

		statesDic = Dictionary.new;
		("-----------").postln;
		("ready tape ID"+id).postln;
	}

	fps {|value|
		fps = value;
		player.set(\fps, fps);
	}

	markers {|items, func|
		/*if (func.isNil && items.isNil, {
		^markers
		});*/
		if (func.isNil, { //clear
			if (items.isNil, {
				markers = Dictionary.new;
			},{
				items.asArray.do{|marker|
					markers.removeAt(marker.asSymbol);
				}
			})
		}, {
			items.asArray.do{|marker|
				markers.add(marker.asSymbol -> func);
			}
		});
		if (markers.size>0, {this.fps(30)},{this.fps(12)})
	}

	time {
		^(Process.elapsedTime-sttime)
	}

	done {
		//["done", targetloops].postln;
		player.set(\reset, st, \trig, 0); //reset to st
		this.stop;
		{ player.set(\trig, 1) }.defer(del);
		xdoneF.value(this);
	}

	xdone {|func| xdoneF = func }
	xloop {|func| xloopF = func }

	redoplayer {
		if (player.isNil, {
			Routine.run { // INSIDE A ONE SHOT ROUTINE TO BE ABLE TO SYNC
				try{player.free}; // silently
				if (buf.isNil, {buf=Buffer()}); // just give the player something to chew
				player = Synth.tail(Server.default, \rPlayerLoop,
					[\buffer, buf, \start, st, \end, end, \amp, vol, \rate, memrate,
						\pan, pan, \index, id, \out, out, \fps, fps,
						\dir, dir, \wobble, wobble, \brown, brown, \vib, vib ]);
				Server.default.sync;// wait til is allocated
			}
		})
	}

	kill {
		player.free;
		loopOSC.free;
		playheadOSC.free;
		statesDic=nil;
	}

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
		this.state( tape.state() );
		this.xloopF = tape.xloopF;
		this.xdoneF = tape.xdoneF;
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

	updateframepoints {
		if (view.notNil, {
			{
				view.timeCursorOn = true;
				view.setSelectionStart(0, (buf.numFrames) * st); // frame the selection
				view.setSelectionSize(0, (buf.numFrames) * (end-st)); // len
				view.readSelection.refresh;
			}.defer;
		})
	}

	info {
		var data = Dictionary.new;
		data[\id] = id;
		data[\file] = this.file(); //warning. if nil key does not get created
		data[\vol] = vol;
		data[\pos] = curpos;
		data[\frame] = [st,end];
		data[\rate] = rate;
		data[\dir] = dir;
		data[\targetloops] = targetloops;
		data[\loops] = loops;
		data[\pan] = pan;
		data[\wobble] = wobble;
		data[\brown] = brown;
		data[\vibrato] = vib;
		data[\verbose] = verbose;
		data[\out] = out;
		^data;
	}

	post {|action, value|
		if (verbose.asBoolean, {[id, action, value].postln});
	}

	go {|value=0, random=0|
		value = value + random.asFloat.rand2;
		player.set(\amp, 0, \reset, value.clip(st,end), \trig, 0, \amp, vol);
		{ player.set(\trig, 1) }.defer(del);
	}

	jump {|value=0, random=0|
		value = curpos + value + random.asFloat.rand2;
		value = value.clip(st,end);
		this.go(value);
	}

	gost {this.go(st)}

	goend {this.go(end)}

	// these should be called moveframe and moveframeby
	move {|value=0, random=0| // moves the frame to another position maintaing the duration
		value = value + random.asFloat.rand2;
		this.frame(value, value+(end-st)); //keep len
	}

	moveby {|value=0, random=0| // moves the frame to another position maintaing the duration
		value = value + random.asFloat.rand2;
		this.frame(st+value, st+value+(end-st)); //keep len
	}

	file { //MST FIND ANOTHER WAY TO RETURN SOMETHING WHEN IS A BUF IN MEMORY
		var res="";
		try { res = PathName(buf.path).fileName }{"buffer in memory?".postln; res = buf.bufnum};
		^res
	}

	pan {|value=nil, time=0|
		if (value.notNil, {
			pan = value;
			player.set(\panlag, time, \pan, value);
			this.post("pan", pan);
		}, {
			^pan
		})
	}

	out {|value=0|
		out = value;
		player.set(\out, value);
	}

	vol {|value=nil, random=0, time=0|
		if (value.notNil, {
			value = value + random.asFloat.rand2;

			vol = value.clip(0,5); //limits
			player.set(\amplag, time, \amp, vol, \gate, 1);

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

	/*	env {| attackTime= 0.01, decayTime= 0.3, sustainLevel= 0.5, releaseTime= 1.0,
	peakLevel= 1.0, curve= -4.0, bias=0 |
	player.set(\gate, 1, \attackTime, attackTime, \decayTime, decayTime, \sustainLevel, sustainLevel, \releaseTime, releaseTime, \peakLevel, peakLevel, \curve, curve, \bias, bias)
	}*/

	wobble {|value=0, random=0, time=0|
		value = value + random.asFloat.rand2;
		wobble = value;
		player.set(\wobblelag, time, \wobble, value);
	}

	brown {|value=0, random=0, time=0|
		value = value + random.asFloat.rand2;
		brown = value;
		player.set(\brownlag, time, \brown, value.max(0));
	}

	vibrato { |value=1, depth=0, ratev=0, depthv=0, time=0|
		vib = [1, value.max(0.01), depth, time, 0, ratev, depthv];
		player.set(\viblag, time, \vib, vib)
	}

	dir {|value=1, time=0|
		dir = value;
		player.set(\dir, value)
	}

	rate {|value=nil, random=0, time=0|
		if (value.notNil, {
			value = value + random.asFloat.rand2;
			//["rate / memrate", value, memrate].postln;
			player.set(\ratelag, time, \rate, value);

			memrate = rate;
			rate = value;
			//if (rate==0, {this.vol(0, time:time)}, {this.vol(vol, time:time)});
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
		this.rate(target, time:tIn);
		{this.rate(restore, time:tOut)}.defer(tIn+tStay);
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
		this.frame(0,1);
		this.go(0);
		this.rate(1);
	}

	frame {|...args|
		if (args.size==0, {
			^[st, end]
		});

		st = args[0].clip(0,1);//.clip(0, 1-(end-st));
		end = args[1].clip(st,1);

		if (st>=end,{
			(id+"warning: start >= end! reseting...").postln;
			end=st+0.01;
		});

		player.set(\start, st, \end, end);
		//[st, end].postln;
		this.post("frame", st.asString+"-"+end.asString);
		this.updateframepoints; //only if w open
	}

	st {|value, random=0|
		if (value.notNil, {
			st = value + random.asFloat.rand2;
			player.set(\start, st);
			this.updateframepoints; //only if w open
		}, {
			^st
		});
	}

	end {|value, random=0|
		if (value.notNil, {
			end = value + random.asFloat.rand2;
			player.set(\end, end);
			this.updateframepoints; //only if w open
		}, {
			^end
		})
	}

	dur {|value, random=0|
		if (value.notNil, {
			end = st + value + random.asFloat.rand2;
			player.set(\end, end);
			this.post("end", end);
			this.updateframepoints; //only if w open
		}, {
			^dur
		})
	}

	len {|value| // IN MILLISECONDS
		if (value.notNil, {
			var adur= value / ((buf.numFrames/buf.sampleRate)*1000 ); // from millisecs to 0-1
			this.dur(adur)
		}, {
			^len
		})
	}

	stop {
		memrate = rate; // store
		//"stop".postln;
		//this.vol(0);
		this.rate(0)
	}

	play {|value=inf|
		loops = 0; // reset count
		targetloops = value;
		//targetloops.postln;
		this.redoplayer;
		if(memrate==0, {memrate=rate}); //otherwise it wont play
		if(memrate==0, {memrate=1});
		this.rate(memrate); // retrieve stored value
	}

	dcoffset {|value=1|
		value = value.clip(-1, 1);
		player.set(\dcoffset, value);
	}

	buf {|value|
		if (value.notNil, {
			buf = value;
			player.set(\buffer, buf.bufnum);
			this.post("buffer", this.file());
			this.updateframepoints; //only if w open
		}, {
			^buf
		})
	}

	rvol {|value=1.0, time=0 |
		this.vol( value.rand, time:time );
	}

	rpan {|time=0| // -1 to 1
		this.pan( 1.asFloat.rand2, time:time )
	}

	rgo {
		var target = rrand(st.asFloat, end.asFloat);
		this.go(target)
	}

	rframe {|st_range=1, len_range=1|
		st = st_range.asFloat.rand;
		end = st + len_range.asFloat.rand;
		if (end>1, {end=1}); //limit. maybe not needed
		this.frame(st, end);
	}

	rmove {|range=1|
		var pos = range.rand;
		this.frame(pos, pos+(end-st)); //keep len
	}

	rst {|range=1.0|
		//st = range.asFloat.rand;
		this.st(range.asFloat.rand);
		//player.set(\start, st);
		//this.updateframepoints; //only if w open
	}

	rend {|range=1.0|
		this.end(range.asFloat.rand)
		//end = range.asFloat.rand; // total rand
		//player.set(\end, end);
		//this.updateframepoints; //only if w open
	}

	rlen {|range=0.5|
		this.end(st + range.asFloat.rand)
		//end = st + range.asFloat.rand; // rand from st point
		//player.set(\end, end);
		//this.updateframepoints; //only if w open
	}

	rdir {|time=0|
		this.dir([1,-1].choose, time:time)
	}

	rrate {|time=0|
		this.rate(1.0.rand2, time:time)
	}

	rand {|time=0|
		this.rpan(time:time);
		this.rrate(time:time);//??
		this.rdir(time:time);
		this.rframe;
		this.rgo;// this should be limited to the current frame
		this.rvol(time:time); // why?
	}

	bframe {|range=0.01|
		this.frame(this.st+(range.asFloat.rand2), this.end+(range.asFloat.rand2))
	}

	bgo {|range=0.01| this.go( curpos+(range.asFloat.rand2)) }// single step brown variation

	bvol {|range=0.05, time=0|
		this.vol( vol+(range.asFloat.rand2), time:time)
	}// single step brown variation

	bpan {|range=0.1, time=0|
		this.pan( pan+(range.asFloat.rand2), time:time)
	}

	brate {|range=0.05, time=0|
		this.rate( rate+(range.asFloat.rand2), time:time )
	}// single step brown variation

	bmove {|range=0.05|
		var pos = st+(range.asFloat.rand2);
		this.frame(pos, pos+(end-st)); //keep len
	}
}
