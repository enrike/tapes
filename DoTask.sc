/*
to do:
- gui to control with timeline to see when is going to fire and fields/sliders to control parameters
- tasks start stopped option
- run the function manually when task is stopped and increase loopcount
- when recreating keep loop count

*/

DoTask {
	var task, itercount, sttime;
	var <name, <>function, <>sleep, <>random, <defer, <iter, <when, <then, <clock, <>verbose;

	*new {| name="", function, sleep=1, random=0, defer=0, iter=inf, when=true, then, clock=0, verbose=1|  // systemdir
		^super.new.initDo(name, function, sleep, random, defer, iter, when, then, clock, verbose);
	}

/*	update {| name="", function, sleep=1, random=0, defer=0, iter=inf, when=true, then, clock=0, verbose=1|
		this.initDo(name, function, sleep, random, defer, iter, when, then, clock, verbose);
	}*/

	stop { task.stop }
	pause { task.stop }
	start { task.start }
	resume { task.resume }
	//reset { task.reset}
	shutup {|flag=false|verbose = flag.asInteger}
	isPlaying { ^task.isPlaying }

	once {
		this.function.value(0, Process.elapsedTime - sttime)
	}

	reset {
		("reset" + name).postln;
		task.reset
		/*task.stop;
		{task.reset}.defer(0.05);
		{task.start}.defer(0.1);*/ // needed?
	}

	initDo {|aname, afunction, asleep, arandom, adefer, aiter, awhen, athen, aclock, averbose|
		name = aname;
		function = afunction;
		sleep = asleep;
		random = arandom;
		defer = adefer;
		iter = aiter;
		when = awhen;
		then = athen;
		clock = aclock;
		verbose = averbose;

		iter=iter.max(0);

		if (name=="", {
			"TASKS MUST HAVE A NAME. Making one up".postln;
			name = ("T"++Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second).asSymbol;
			name.postln;
		});

		("creating _do"+name).postln;

		/*if (procs[name.asSymbol].notNil, {// kill before rebirth if already there
		procs[name.asSymbol][0].stop;
		procs.removeAt(name.asSymbol)
		});*/
		if (task.notNil, {task.stop});

		clock ?? clock = TempoClock; // default

		task = Task({
			block {|break| // this block runs on task.start
				inf.do{
					if (when.value.asBoolean, { //go
						iter.do{|index|
							var wait = sleep.asString.asFloat; // reset each time
							var time = ""+Date.getDate.hour++":"++Date.getDate.minute++":"++Date.getDate.second;
							var elapsed = Process.elapsedTime -sttime;// procs[name.asSymbol][5];

							itercount = index; // keep track
							function.value(index.asInteger, elapsed); // only run if {when} is true. pass index and elapsed

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

							if (verbose.asInteger==1, {//procs[name.asSymbol][4]==1, { // verbose
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
			task.stop; //procs[name.asSymbol][0].stop;
			//procs.removeAt(name.asSymbol)

		}, clock); // end definition of task

		//procs.add(name.asSymbol -> [atask, function, sleep, random, verbose, 0]);// to keep track of them
		sleep = sleep;
		random = random;
		verbose = verbose;

		{
			task.start; // now run it
			//procs[name.asSymbol][5] = Process.elapsedTime
			sttime = Process.elapsedTime
		}.defer(defer);
	}


}