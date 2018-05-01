/*

*/
Layer{

	var id, buffers, play, blen;
	var <>buf, <>st=0, <>end=1, <>vol=1, <>rate=0, memrate=1;
	var <ptask, <vtask, <rtask;
	var plotview, plotwin=nil;

	*new {| id, buffers = nil |
		^super.new.initLayer( id, buffers );
	}

	initLayer {| aid, abuffers |
		id = aid; // just in case I need to identify later
		buffers = abuffers; // kep a ref to all of them to be able to change later
		buf = buffers.choose; // random

		try{
			blen = buf.numFrames / buf.numChannels.asFloat; // to get length in frames of one chanel
		}{|error| error.postln};

		play.free;
		play = Synth(\StPlayer, [\buffer, buf.bufnum, \rate, rate]);
		this.rpos();
		this.rvol();
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
				st = cs[0]/blen;
				end = (cs[0]+cs[1])/blen; //because view wants start and duration
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
			plotview.setSelectionStart(0, blen * st);
			plotview.setSelectionSize(0, blen * (end-st));
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

	volumen {|avol|
		vol = avol;
		play.set(\amp, vol);
	}

	vold {
		this.volumen(vol-0.02)
	}
	volu {
		this.volumen(vol+0.02)
	}

	rvol {
		this.volumen( 1.0.rand );
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

	dur {|adur|
		end = st + adur;
	}

	rbuf {
		buf = buffers.choose;
		blen = buf.numFrames/buf.numChannels;
		play.set(\buffer, buf.bufnum)
	}

	setbuf {|abuf|
		buf = abuf;
		blen = buf.numFrames/buf.numChannels;
		play.set(\buffer, buf.bufnum)
	}

	rpos {|st_range=1, len_range=0.1|
		st = (st_range-len_range).rand; // TO DO: adjust length etc... here
		end = st + (len_range.rand);
		this.pos(st, end);
	}

	rst {|range=1|
		st = range.rand;
		play.set(\start, st);
	}

	rlen {|range=1|
		end = st + range.rand;
		play.set(\end, end);
	}

	rrate {
		this.rat(1.0.rand2)
	}

	rat {|arate|
		rate = arate;
		play.set(\rate, rate);
	}

	pos {|p1, p2|
		st = p1;
		end = p2;
		play.set(\start, st);
		play.set(\end, end);
		this.updateplot; //only if w open
	}

	// set start
	// set len

	brownpos {|step=0.01, sleep=5, dsync=0, delta=0|
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
					[st, end].postln;
					this.pos(st, end)
				}.defer(dsync.asFloat.rand); //out of sync all of them?
				sleep.wait;
			});
		});

		ptask.start;
	}


	brownvol {|step=0.01, sleep=5, dsync=0, delta=0|
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

	brownrate {|step=0.01, sleep=5, dsync=0, delta=0|
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