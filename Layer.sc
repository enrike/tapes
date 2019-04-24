/* single layer
*/

Layer{

	var <id, <play, <curpos;
	var <buf, <st=0, <end=1, <volume=1, <rate=0, <panning=0; // state variables
	var memrate=1; // to store rate while paused
	//var <ptask, <vtask, <rtask;
	//var plotview, plotwin=nil;
	var <>statesDic, <>verbose=false;

	*new {| id=0, buffer = nil, bus |
		^super.new.initLayer( id, buffer, bus );
	}

	initLayer {| aid, abuffer, abus |
		var initbuf;
		id = aid; // just in case I need to identify later
		buf = abuffer;

		if(buf.isNil.not, { initbuf = buf.bufnum }); // only if specified. otherwise nil

		play.free;
		play = Synth(\StPlayer, [\buffer, initbuf, \rate, rate, \index, id, \out, abus]);

		OSCdef(\playhead++id).clear;
		OSCdef(\playhead++id).free;
		OSCdef(\playhead++id, {|msg, time, addr, recvPort|
			if (id==msg[2], { curpos = msg[3] });
		}, '/tr', NetAddr("127.0.0.1", 57110));

		statesDic = Dictionary.new;

		("ready layer"+id).postln;
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
		state.put(\vol, volume);
		state.put(\rate, rate);
		state.put(\panning, panning);

		statesDic[which] = state;

		this.post("pushing state", which);
		if(verbose.asBoolean, {["pushing state", which].postln});
	}

	pop {|which|
		var state;
		statesDic.postln;
		state = statesDic[which];

		//statesDic.postln;
		this.setbuf( state[\buf] );
		this.bounds( state[\st], state[\end] );
		this.vol( state[\vol] );
		this.rat( state[\rate] );
		this.pan( state[\panning] );
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
		["volume", volume].postln;
		["bounds", st, end].postln;
		["rate", rate].postln;
		["panning", panning].postln;
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

	outb {|bus=0|
		play.set(\out, bus)
	}

	pan {|apan=0, time=0, curve=\lin|
		panning = apan;
		play.set(\pancur, curve);
		play.set(\pangate, 0);

		play.set(\pantarget, panning);
		play.set(\pandur, time);

		{play.set(\pangate, 1)}.defer(0.01);

		this.post("pan", panning);
	}

	vol {|avol=1, time=0, curve=\exp|
		if (avol< 0, {avol=0}); //lower limit
		volume = avol;

		play.set(\ampcur, curve);
		play.set(\ampgate, 0);

		play.set(\amptarget, volume);
		play.set(\ampdur, time);

		{play.set(\ampgate, 1)}.defer(0.01);

		this.post("volume", (volume.asString + time.asString + curve.asString) );
	}

	vold {
		this.vol(volume-0.02)
	}
	volu {
		this.vol(volume+0.02)
	}

	rat {|arate=1, time=0, curve=\lin|
		rate = arate;
		play.set(\ratecur, curve);
		play.set(\rategate, 0);

		play.set(\ratetarget, rate);
		play.set(\ratedur, time);

		{play.set(\rategate, 1)}.defer(0.01);

		this.post("rate", rate);
	}

	reverse {
		this.rat(rate.neg)
	}

	boom {|target=0, tIn=1, tStay=0.5, tOut=1, curve=\lin| // boomerang like pitch change
		var restore = rate;//
		this.rat(target, tIn, curve);
		{this.rat(restore, tOut, curve)}.defer(tIn+tStay);
	}

	mir{ |time=0| // mirror
		// this should change play mode to mirror <>
	}

	gofwd {
		if (rate<0, {this.reverse})
	}
	gobwd {
		if (rate>0, {this.reverse})
	}

	reset {
		this.bounds(0,1);
		this.jump(0);
		this.rat(1);
	}

	bounds {|p1=0, p2=1|
		st = p1; // st and end are variables in this class. they must be updated as well
		end = p2;
		play.set(\start, st);
		play.set(\end, end);
		this.post("bounds", st.asString+"-"+end.asString);
		//this.updateplot; //only if w open
	}

	boundsA {|p=0| st=p; play.set(\start, st)}
	boundsB {|p=0| end=p; play.set(\end, end)}

	dur {|adur|
		end = st + adur;
		play.set(\end, end);
		this.post("end", end);
		//this.updateplot; //only if w open
	}

	len {|ms=100| // IN MILLISECONDS
		var adur= ms / ((buf.numFrames/buf.sampleRate)*1000 ); // from millisecs to 0-1
		this.dur(adur)
	}

	pause {
		memrate = rate; // store
		this.rat(0)
	}

	resume {
		this.rat(memrate); // retrieve stored value
	}

	setbuf {|abuf|
		buf = abuf;
		play.set(\buffer, buf.bufnum);
		this.post("buffer", this.file());
		//this.updateplot; //only if w open
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

	rdir{|time=0, curve=\lin|
		this.rat(rate * [1,-1].choose, time, curve)
	}

	rrat {|time=0, curve=\lin|
		this.rat(1.0.rand2, time, curve)
	}

	// set start
	// set len

	bbounds {|range=0.01| this.bounds( st+(range.rand2), end+(range.rand2)) }// single step brown variation

	bjump {|range=0.01| this.jump( curpos+(range.rand2)) }// single step brown variation

	bvol {|range=0.05, time=0, curve=\lin|
		this.vol( volume+(range.rand2), time, curve)
	}// single step brown variation

	bpan {|range=0.1, time=0, curve=\lin|
		this.pan( panning+(range.rand2), time, curve)
	}

	brat {|range=0.05, time=0, curve=\lin|
		this.rat( rate+(range.rand2), time, curve )
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
