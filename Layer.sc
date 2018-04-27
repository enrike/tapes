/*

*/
Layer{

	var id, buffers, play;
	var <>buf, <>st=0, <>end=1, <>vol=1, <>rate=0, memrate=1;
	var ptask, vtask, rtask;

	*new {| aid, abuffers = nil |
		^super.new.initLayer( aid, abuffers );
	}

	initLayer {| aid, abuffers |
		id = aid; // just in case I need to identify later
		buffers = abuffers; // kep a ref to all of them to be able to change later
		buf = buffers.choose; // random
		play.free;
		play = Synth(\StPlayer, [\buffer, buf.bufnum, \rate, rate, \amp, vol]);
		this.rpos();
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
		vol.postln;
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
		play.set(\buffer, buf.bufnum)
	}

	rpos {|st_range=1, len_range=0.1|
		st = (st_range-len_range).rand; // TO DO: adjust length etc... here
		end = st+(len_range.rand);
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
		play.set(\start, p1);
		play.set(\end, p2);
	}

	brownpos {|step=0.01, sleep=5, dsync=0, delta=0|
		ptask.stop; // CORRECT??
		ptask = Task({
			inf.do({ arg i;
				{ this.pos(
					st + step.asFloat.rand2 + delta,
					end + step.asFloat.rand2 + delta
				) }.defer(dsync.asFloat.rand); //out of sync all of them?
				sleep.wait;
			});
		});

		ptask.start;
	}


	brownvol {|step=0.01, sleep=5, dsync=0, delta=0|
		vtask.stop; // is this CORRECT???
		vtask = Task({
			inf.do({ arg i;
				vol = vol + step.asFloat.rand2 + delta;
				if (vol<0, {vol=0});// lower limit
				{ play.set(\amp, vol) }.defer(dsync.asFloat.rand); //out of sync all of them?
				sleep.wait;
			});
		});

		vtask.start;
	}

	brownrate {|step=0.01, sleep=5, dsync=0, delta=0|
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