/*
Layers by ixi-audio.net
license GPL

Layers allows to create and control multiple layers of samplers
*/

/* use @? this collides with 5@6 points. # colllides with #a,b=[1,2]
@N.plot not implemened
bounds, rbounds and bbounds decide whether they should be one or two args or 4@4 or some other solution

len / rlen: if len is in msecs rlen should be in millisecs
*/


(
//a = "~/Mahaigaina/audio/00works/";
a = "~/Mahaigaina/audio/00_mbv/isntanything/";
//a = "~/Mahaigaina/audio/00songs/";
//a = "~/Mahaigaina/audio/beat_happening_cuts/";
// you can pass a single soundfile too and all layers play that one
//a = "~/Mahaigaina/audio/00songs/03_Pinebox_Derby.flac";
Layers(a, this);
)



@boot(4); // starts audio and instantiates 4 layers each playing one sample





// if you wanted to create a single independent layer out of the main control
n = Buffer.new(s, "~/tm-2018-06-01T14_40_59.flac") // load a buffer
m = Layer.new(1000, n); //1000 is just any unique ID
m.resume;



( //initial situation. for instance
@normalize;
@vol(0.3);
@resume; // go play
)


//////////////////////////////////////////////////////////
// global controls. control all layers at the same time //
//////////////////////////////////////////////////////////

@bufs // posts buffers available
@curbufs // posts buffers actually being used by which players
@all // list containing all players
@some // returns a list with randomly chosen layers
@info // post internal states for all available layers
@verbose(true)
@buf( @bufs[1] ); // set this buffer to all layers
@asignbufs // asign the loaded buffers to layers, it happens by default on Layers.new
@normalize // normalise buffers
@plot; // draws the play heads of each layer the same window
@plot(@bufs[4]) // optional pass the buffer to draw on the background of the window. useful when all layers share the same buffer

@resume
@pause
@solo(0) // pause all but this layer
@reverse // reverse play direction. +rate to -rate
@fwd // turn current rate positive
@bwd // turn current rate negative
@volu // UP by 2/100th
@vold // DOWN by 2/100th
@vol(0.5)
@fadein
@fadeout
@pan(0) //-1 to 1
@rate(1)
@bounds([0, 0.01]) // st, end. in 0 to 1 range
@st(0.5) //bounds start
@end(0.55) // bounds end
@jump(0.5) // jump to point in sample
@len(600) // length, **in milliseconds** useful if they are using different buffers and you want the same loop duration

@mirror // experimental

// RESET: calling some functions with no args should reset its corresponding state for instance...
@reset // reset bounds to 0-1, play from 0, pitch to 1
@bounds // resets bounds to 0-1
@jump // resets pos to 0
// SAME happens with @pan, @rat, @vol


// store and retrieve named states. they dont get deleted from memory until the session ends
@push(\dut)
@pop(\dut)
@save // save current states from memblahblahory into a file.
@save("caos") // optionally provide a name. if no name is provided it uses the timestamp
@load // open a dialogue to search for a file containg states

// search. identify which layers are playing which sound file
p = @search("Roll") //return a list of layers whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.volume(0.3)}) // do whatever with the search results

//random
@one
@some // returns a list with a random number of random players
@some(3) // returns a list with N random items from all players

@rbuf // anyone gets a new buffer randomly
@rpan
@rrate
@rdir // + or - rate
@rvol
@rbounds // 0 to 1
@rbounds([0.2, 0.1]) // set start and LENGTH limits for the random. in this case start will be rrange(0,0.2) and length start+(0.1.rand)
@rst // start point
@rend // end point (not length)
@rlen(0.1) // length. in range 0-1
@rjump
@rand // random file, pan, vol, rate, dir, bounds (st, end) and jump

// random walks
@bbounds // small range jump. range defaults to 0.01
@bbounds(0.05) // single step small random
@bvol // small range jump. range defaults to 0.01
@bvol(0.05) // single step small random
@bpan
@bjump(0.9)
@brate


// OFFSET:
// functions also have offsets so that layers dont run the function at the same time
@rrate(5, offset:8) // all layers random rate within 8 secs. each one at a different time within the next 8 secs
@rlen(offset:20) // and so on...


// TRANSITION TIME:
// some functions (the ones related to panning, amplitude and pitch) can perform a transition between the current state and the target state in N secs
// go to a value in N secs describing a shape: \lin, \exp, \sin, \wel, \cub, \sqr
@rate(0.1, 10, \lin)// check this out, the sound is self explanatory :)
@brate(0.1, 20, \sin) // this will start a shift to a random rate ranging -0.1 to 0.1 from the current rate that will take 20 secs and will describe a sinusoidal ramp during the change
@brate(0.1, 20, \sqr, offset:8) // offset can be use here too


// compressor. yes, there is a built in compressor/expander. inactive by default
@comp(0.2, 0.6, 0.2) //threshold, slopeBelow, slopeAbove
@thr(0.2); // threshold
@slb(0.6); // slope below thr <1 for expansion
@sla(0.2); //slope above thr <1 for compression >1 for expansion


// scheduling functions
@sch(\randpan, {@rpan}, 15); //schedule a function every N secs. args: NAME, FUNCTION, TICK TIME, OFFSET, RANDOM
@sch(\randpan, {@rpan}, 15, offset:5); // ... optional offset delay so that each layer fires at N+(offset.rand)
@sch(\randpan, {@rpan}, 15, random:2); // rand swing: the next sch will happen after 15 + rrand(-2,+2) secs

@pauseT(\randpan)
@resumeT(\randpan)
@stopT(\randpan)
@procs // dictionary containing all Tasks alive
@noT; // kills all tasks

// some examples of scheduling tasks
@sch(\backtoinit, {@reset}, 10)
@stopT(\backtoinit)

@sch(\rev, {@reverse}, 10, offset:5)
@stopT(\rev)

@sch(\rlen, {@rlen(1, 2)}, 8, 4)
@stopT(\rlen)

@sch(\brat, {@brate(0.01)}, 10, offset:4)
@stopT(\brat)

@sch(\bbounds, {@bbounds}, 4, offset:3)
@stopT(\bbounds)

@sch(\bvol, {@bvol}, 4)
@stopT(\bvol)

@sch(\bjump, {@bjump(0.01)}, 1, offset:1)
@stopT(\bjump)

@sch(\rst, {@rst}, 15, offset:10)
@stopT(\rst)

@sch(\rbufs, {@one.buf(@bufs.choose)}, 15) //a single random player gets a random sound every 15s
@stopT(\rbufs)

@sch(\inoutrand, { //every 30 sec: 9 sec fade in- after 15 secs - 9 secs fade out
	@fadein(9);
	{@fadeout(9)}.defer(15)
}, 30)
@stopT(\inoutrand)

@sch(\ramps, {// this will move every 10 secs all the pitchs to a random value ranging -4 to +4 from the current value in a 5 sec transition
	@brate(4, 5, \sin)
}, 10, 3)
@stopT(\ramps)


(
var len=10, gap=5, fade=0.5;
@sch(\rum, {
	@rbuf;
	@rdir; @rrate; @rbounds;
	@fadein(fade);
	{@fadeout(fade)}.defer(len-gap);
}, len)
)



///////////////////////////////
// control individual layers //
///////////////////////////////

// you can also control each layer individually

// GET
@0.vol
@0.info
@0.file
@0.buf
@5.plot // opens a window that displays the buffer and the current loop selection. it allows to change the selection by click
@0.end // get
@0.st //get
@0.curpos
@0.verbose

// SET
@0.buf( @bufs[1] )
@0.vol(1)
@0.pan(0) // -1 to 1
@0.rate(1)
@0.bounds([0.1, 0.12]) // st and end. range 0-1
@0.len(1000) // in millisecs!!!
// and so on...

// ACTIONS
@0.resume
@0.fadein(100)
@0.fadeout(5)
@0.pause
@0.jump(0.7) // go there now
// and so on...

// memory
@0.push
@0.pop

// random
@0.rbounds //
@0.rst
@0.rend
@0.rvol
@0.rpan
@0.rrate
@0.rdir
@0.bbounds(0.01) // single step small random
// and so on...

