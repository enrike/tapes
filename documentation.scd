/*
Layers by ixi-audio.net
license GPL

Layers allows to create and control multiple layers of samplers
*/





/*
use @? this collides with 5@6 points. # colllides with #a,b=[1,2]. maybe use regexp in preprocesor?

loop, rloop and bloop decide whether they should be one or two args or 4@4 or some other solution

len / rlen: if len is in msecs rlen should be in millisecs

@r -> rate
@rr -> rrate
@br -> @brate
etc...?

*/


// 1st: set up the system
Layers(this);

// 2nd: load all samples from this folder
@loadfiles( "~/Mahaigaina/audio/00works/" );
//@loadfiles( "~/Mahaigaina/audio/00_mbv/isntanything/" );
//@loadfiles( "~/Mahaigaina/audio/00songs/" );
//@loadfiles( "~/Mahaigaina/audio/beat_happening_cuts/");

// 3rd: instantiate N layers each playing one of the loaded files.
// if more layers than sound files it wraps
@do(4);

@resume; // everything is ready, go play


// you could load new files and asign them again
@loadfiles("some/another/directory"); // must wait until files are loaded
@asignbufs // force re-asigns them
//you could do it manually as well
@2.buf( @bufs[5] ) // etc...


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
@buf(1); // OR do it by index
@asignbufs // asign the loaded buffers to layers, it happens by default on Layers.new
@normalize // normalise buffers
@plot; // draws the play heads of each layer the same window
@plot(4) // optional pass the buffer to draw on the background of the window. useful when all layers share the same buffer

@resume // loop play
@pause
@shot // single shot play
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
@rate()

@lp(0, 0.01) // st, end. in 0 to 1 range
@st(0.5) //loop start
@end(0.55) // loop end
@dur(0.006) // loop duration in 0-1 range from st
@len(600) // length, **in milliseconds** useful if they are using different buffers and you want the same loop duration
@step(0.01) // move st and end by that step from the current st position. maintains the loop duration
@move(0.5) // move playhead to that position, set st to that position and calculate the new end maintaning the loop duration
@go(0.5) // send playhead to point in sample (only works if >st and <end)

@scratch // experimental. |target=0, tIn=1, tStay=0.5, tOut=1, curve=\lin, offset=0| // boomerang like pitch change

// RESET: calling some functions with no args should reset its corresponding state for instance...
@reset // reset loop to 0-1, play from 0, pitch to 1
@lp // resets loop to 0-1
@go // resets pos to 0
@gost // to to loop start point
@goend // go to loops end point
// SAME happens with @pan, @rate, @vol


// store and retrieve named states. they dont get deleted from memory until the session ends
@push(\dut)
@pop(\dut)
@save // save current states from memory into a file.
@save("caos") // optionally provide a name. if no name is provided it uses the timestamp
@load // open a dialogue to search for a file containg states

@control // opens a window with all layers. you can control de loop selection
@control( 300, 200 )

// search. identify which layers are playing which sound file
p = @search("Roll") //return a list of layers whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.vol(0.3)}) // do whatever with the search results

//random
@one // choose and return a random layer
@some // returns a list with a random number of random players
@some(3) // returns a list with N random items from all players
// for instance
@one.rrate // one gets random rate
@some.collect(_.rrate) // some of them get random rate

@rbuf // anyone gets a new buffer randomly
@rpan
@rrate
@rdir // + or - rate
@rvol
@rloop // 0 to 1
@rloop([0.2, 0.1]) // set start and LENGTH limits for the random. in this case start will be rrange(0,0.2) and length start+(0.1.rand)
@rst // start point
@rend // end point (not length)
@rlen(0.1) // length. in range 0-1
@rgo // go to a random point in the sound
@rand // random file, pan, vol, rate, dir, loop (st, end) and go

// random walks
@bloop // small range go. range defaults to 0.01
@bloop(0.05) // single step small random
@bvol // small range go. range defaults to 0.01
@bvol(0.05) // single step small random
@bpan
@bgo(0.9)
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

//local randoms
@rate(1, random:0.1); // each of them will apply a small random variation around 1 ranging -0.1 to 0.1
@end(0.5, random:0.01);
@dur(0.1, random:0.05)

// compressor. yes, there is a built in compressor/expander. inactive by default
@comp(0.2, 0.6, 0.2) //threshold, slopeBelow, slopeAbove
@thr(0.2); // threshold
@slb(0.6); // slope below thr <1 for expansion
@sla(0.2); //slope above thr <1 for compression >1 for expansion


// scheduling functions
@sch(\randpan, {@rpan}, 15); //schedule a function every N secs. args: NAME, FUNCTION, TICK TIME, OFFSET, RANDOM

// random periods
@sch(\randpan, {@rpan}, 15, offset:5); // ... optional offset delay so that each layer fires at N+(offset.rand)
@sch(\randpan, {@rpan}, 15, random:2); // rand swing: the next sch will happen after 15 + rrand(-2,+2) secs
@sch(\wrand, {
	"two options".postln;
}, random:[[1,3],[0.9, 0.1]])// run every every 1 sec (90% chances) or 3 secs (10%chances)



@pauseT(\randpan)
@resumeT(\randpan)
@stopT(\randpan) // STOPS the task
@procs // dictionary containing all Tasks alive
@stopT // kills all tasks
@noT; // kills all tasks

// some examples of scheduling tasks
@sch(\backtoinit, {@reset}, 10) // resets every 10s
@stopT(\backtoinit)

@sch(\rev, {@reverse}, 10, offset:5)
@stopT(\rev)

@sch(\rlen, {@rlen(1, 2)}, 8, 4)
@stopT(\rlen)

@sch(\brat, {@brate(0.01)}, 10, offset:4)
@stopT(\brat)

@sch(\bloop, {@bloop}, 4, offset:3)
@stopT(\bloop)

@sch(\bvol, {@bvol}, 4)
@stopT(\bvol)

@sch(\bgo, {@bgo(0.01)}, 1, offset:1)
@stopT(\bgo)

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


( //keeps mirror playing while slowly moving around the same area
@rdir;
@sch(\rev, {
	@bgo(0.01);
	@reverse
}, 1, random:0.2)
)

( // moves forwards going
var st=0, end;
@sch(\go, {
	st = st+(0.001.rand);
	@loop([st, st+0.001])
}, 0.8)
)

(
var len=10, gap=5, fade=0.5;
@sch(\rum, {
	@rbuf;
	@rdir; @rrate; @rloop;
	@fadein(fade);
	{@fadeout(fade)}.defer(len-gap);
}, len)
)

(
var st=0, end=0, speed=0.001, len=0.01, time=3;
@sch(\go, {
	st = st+(speed.rand);
	if ( (st+len)>1, {st=0});
	st.postln;
	@bpan(0.5, time:time);
	@rdir;
	@brate(0.0005);
	@loop([st, st+len])
}, time, random:time/3.0)
)



///////////////////////////////
// control individual layers //
///////////////////////////////

// you can also control each layer individually with @0, @1 etc...

// GET
@0.vol
@0.info
@0.file
@0.buf
@0.end
@0.st
@0.curpos // 0-1 playhead position
@0.verbose

// SET
@0.buf( @bufs[1] )
@0.vol(1)
@0.pan(0) // -1 to 1
@0.rate(1)
@0.loop([0.1, 0.12]) // st and end. range 0-1
@0.len(1000) // in millisecs!!!
// and so on...

// ACTIONS
@0.resume
@0.fadein(100)
@0.fadeout(5)
@0.pause
@0.go(0.7)
// and so on...

// memory
@0.push
@0.pop

// random
@0.rloop //
@0.rst
@0.rend
@0.rvol
@0.rpan
@0.rrate
@0.rdir
@0.bloop(0.01) // single step small random
// and so on...

