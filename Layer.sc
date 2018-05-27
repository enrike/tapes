/*

*/
Layer{

	var id, buffers, play;
	var <buf, <st=0, <end=1, <vol=1, <rate=0, <panning=0; // state variables
	var memrate=1; // to store rate while paused
	var <ptask, <vtask, <rtask;
	var plotview, plotwin=nil;
	var statesDic;

	*new {| id, buffers = nil |
		^super.new.initLayer( id, buffers );
	}

	initLayer {| aid, abuffers |
		id = aid; // just in case I need to identify later
		buffers = abuffers; // kep a ref to all of them to be able to change later
		buf = buffers.choose; // random

		play.free;
		play = Synth(\StPlayer, [\buffer, buf.bufnum, \rate, rate]);
		this.rpos();
		this.rvol();

		statesDic = Dictionary.new;

		("ready layer"+id).postln;
	}

	push { |which|
		var state = Dictionary.new;
		state.put(\buf, buf);
		state.put(\st, st);
		state.put(\end, end);
		state.put(\vol, vol);
		state.put(\rate, rate);
		state.put(\panning,panning);

		statesDic[which] = state;
	}

	pop {|which|
		var state;
		state = statesDic[which];

		this.setbuf( state[\buf] );
		this.pos( state[\st], state[\end] );
		this.vol( state[\vol] );
		this.rat( state[\rate] );
		this.pan( state[\panning] );
	}

	plot {
		if (plotwin.isNil, {
			// to do: bigger size win and view
			// move playhead as it plays?
			plotwin = Window("Buffer"+id, Rect(200, 200, 400, 200)).alwaysOnTop=true;
			plotwin.front;
			plotwin.onClose = { plotwin = nil }; // needed?

			plotview = SoundFileView(plotwin);
			plotview.mouseUpAction = {
				var cs = plotview.selections[plotview.currentSelection];
				st = cs[0]/buf.numFrames;
				end = (cs[0]+cs[1])/buf.numFrames; //because view wants start and duration
				play.set(\start, st);
				play.set(\end, end);
				["new loop:", st, end].postln;
			};
		});
		this.updateplot; // draw the data and refresh
	}

	updateplot {
		if (plotwin.isNil.not, {
			var f = { |b,v| b.loadToFloatArray(action: { |a| { v.setData(a) }.defer }) };

			plotview.timeCursorOn = true;
			plotview.setSelectionStart(0, buf.numFrames * st);
			plotview.setSelectionSize(0, buf.numFrames * (end-st));
			plotview.readSelection.refresh;

			f.(buf, plotview); //
		});
	}

	info {
		("-- Layer"+id+"--").postln;
		this.file().postln;
		["vol", vol].postln;
		[st, end].postln;
		["rate", rate].postln;
		"---------".postln;
	}

	file {
		^buf.path.split($/).last;
	}

	outb {|bus=0|
		play.set(\out, bus)
	}

	pan {|apan=0|
		panning = apan;
		play.set(\pan, apan)
	}

	volumen {|avol|
		if (avol< 0, {avol=0}); //lower limit
		vol = avol;
		play.set(\amp, vol);
	}

	vold {
		this.volumen(vol-0.02)
	}
	volu {
		this.volumen(vol+0.02)
	}

	rat {|arate|
		rate = arate;
		play.set(\rate, rate);
	}

	len{|ms=100| // IN MILLISECONDS
		var adur= ms / ((buf.numFrames/buf.sampleRate)*1000 ); // from millisecs to 0-1
		this.dur(adur)
	}

	pos {|p1, p2|
		st = p1;
		end = p2;
		play.set(\start, st);
		play.set(\end, end);
		this.updateplot; //only if w open
	}

	dur {|adur|
		end = st + adur;
		play.set(\end, end);
		this.updateplot; //only if w open
	}

	pause {
		memrate = rate; // store
		rate = 0;
		play.set(\rate, rate)
	}

	resume {
		rate = memrate;// retrieve stored value
		play.set(\rate, rate)
	}

	setbuf {|abuf|
		buf = abuf;
		play.set(\buffer, buf.bufnum)
	}

	rvol {
		this.volumen( 1.0.rand );
	}

	rpan {
		this.pan( 1.0.rand2 )
	}

	rbuf {
		buf = buffers.choose;
		play.set(\buffer, buf.bufnum)
	}

	rpos {|st_range=1.0, len_range=0.1|
		st = (st_range.asFloat-len_range.asFloat).rand;
		end = st + (len_range.asFloat.rand);
		this.pos(st, end);
	}

	rst {|range=1.0, keeplen=1|
		var len = end-st;// only used if keeplen
		st = range.asFloat.rand;
		play.set(\start, st);
		if (keeplen.asBoolean, {
			end = st+len;
			play.set(\end, end);// keep the length constant
		});
		this.updateplot; //only if w open
	}

	rend {|range=1.0|
		end = st + range.asFloat.rand;
		play.set(\end, end);
		this.updateplot; //only if w open
	}

	rrate {
		this.rat(1.0.rand2)
	}

	// set start
	// set len

	bpos {|range| this.pos( st+(range.rand2), end+(range.rand2)) }// single step brown variation

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
					this.pos(st, end)
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

}