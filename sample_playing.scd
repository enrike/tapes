/*
Layers by ixi-audio.net
license GPL

Layers allows to create and control multiple layers of samplers

For instance:
l = Layers.new("~/mysamples", 12); // this creates 12 players and loads all files in "~/mysamples"
// now wait until the layers are ready. check the post window
l.resume; // all players start playing now
l.rrate // all get random rate now
*/



s.boot;// BOOT SERVER FIRST



// CREATE LAYERS //
// l = Layers.new("pathtofileorfolder", NUMOFLAYERS);

// you can pass the path to a folder with many sounds files
// each player gets a buffer. if more players than buffers it
// wraps like (p1->b1, p2->b2, p3->b1 and so on)




//a = "/home/r2d2/Mahaigaina/audio/00works/";
//a = "/home/r2d2/Mahaigaina/audio/00_mbv/isntanything/";
//a = "/home/r2d2/Mahaigaina/audio/00songs/";
a = "/home/r2d2/Mahaigaina/audio/beat_happening_cuts/";
// you can pass a single soundfile too and all layers play that one
//a = "/home/r2d2/Mahaigaina/audio/00songs/03_Pinebox_Derby.flac";

l = Layers.new(a); // this takes a few seconds... please wait...
l.boot(2)
// you can have more than one set of Layers at the same time
l = Layers.new("/some/sounds/", 3);
f = Layers.new("/other/sounds/", 6);


// to create a single independent layer
n = Buffer.new(s, "~/tm-2018-06-01T14_40_59.flac") // load a buffer
m = Layer.new(1000, n); //1000 is just any unique ID
m.resume;
// or to control a single layer from Layers
l.ps[0].pause



( //initial situation. for instance
l.normalize;
l.vol(0.3);
l.resume; // go play
)



/////////////////////////////////////
// global controls. for all layers //
/////////////////////////////////////

l.allbufs // posts buffers available
l.curbufs // posts buffers actually being used by which players
l.ps // list containing all players
l.info // post internal states for all available layers
l.verbose(true)
l.setbuf( l.bufs[34] ); // set this buffer to all layers
l.asignbufs // asign the loaded buffers to layers, it happens by default on Layers.new
l.normalize
l.plot; // draws the play heads of each layer in a window
l.plot(l.bufs[4]) // optional pass the buffer to draw on the background of the window. useful when all layers share the same buffer

l.resume
l.pause
l.solo(0) // pause all but this layer
l.reverse // reverse play direction. +rate to -rate and - to +
l.gofwd // turn current rate positive
l.gobwd // turn current rate negative
l.volu // UP
l.vold // DOWN
l.vol(0.5)
l.fadein
l.fadeout
l.pan(0) //-1 to 1
l.rat(1) // rate
l.bounds(0, 0.01) // st, end. in 0 to 1 range
l.jump(0.5, offset:0.3) // jump there
l.len(600) // length, **in milliseconds** useful if they are using different buffers and you want the same loop duration


// RESET: calling some functions with no args should reset its corresponding state for instance...
l.reset // reset bounds to 0-1, play from 0, pitch to 1
l.bounds // resets bounds to 0-1
l.jump // resets rate to 0
// AND THE SAME with l.pan, l.rat, l.vol


// store and retrieve named states. they dont get deleted from memory until the session ends
l.push(\dut)
l.pop(\dut)
l.save // save current states from memblahblahory into a file.
l.save("caos") // optionally provide a name. if no name is provided it uses the timestamp
l.load // open a dialogue to search for a file containg states

// search. identify which layers are playing which sound file
p = l.search("Roll") //return a list of layers whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.volume(0.3)}) // do whatever with the search results

//random
l.some // returns a list with a random number of random players
l.some(3) // returns a list with N random items from all players

l.rbuf // anyone gets a new buffer randomly
l.rpan
l.rrat
l.rdir // + or - rate
l.rvol
l.rbounds // 0 to 1
l.rbounds(0.2, 0.1) // set start and LENGTH limits for the random. in this case start will be rrange(0,0.2) and length start+(0.1.rand)
l.rst // start point
l.rend // end point (not length)
l.rlen(0.1) // length. in range 0-1
l.rjump
l.rand // random file, pan, vol, rate, dir, bounds (st, end) and jump

// random walks
l.bbounds // small range jump. range defaults to 0.01
l.bbounds(0.05) // single step small random
l.bvol // small range jump. range defaults to 0.01
l.bvol(0.05) // single step small random
l.bpan
l.bjump(0.9)
l.brat


// OFFSET:
// functions also have offsets so that layers dont run the function at the same time
l.rrat(5, offset:8) // all layers random rate within 8 secs. each one at a different time within the next 8 secs
l.rlen(offset:20) // and so on...


// TRANSITION TIME:
// some functions (the ones related to panning, amplitude and pitch) can perform a transition between the current state and the target state in N secs
// go to a value in N secs describing a shape: \lin, \exp, \sin, \wel, \cub, \sqr
l.rat(0.1, 10, \lin)// check this out, the sound is self explanatory :)
l.brat(0.1, 20, \sin) // this will start a shift to a random rate from -0.1 to 0.1 of current rate that will take 20 secs and describing a sinusoidal ramp during the change
l.brat(0.1, 20, \sqr, offset:8) // offset can be use here too


// scheduling functions
l.sch(\randpan, {l.rpan}, 15); //schedule a function every N secs. args: NAME, FUNCTION, TICK TIME, OFFSET
l.sch(\randpan, {l.rpan}, 15, offset:5); // ... optional offset delay so that each layer fires at N+(offset.rand)
l.sch(\randpan, {l.rpan}, 15, random:1); // rand swing: the next sch will happen after 15 + rrand(-1,+1) secs

l.pauseT(\randpan)
l.resumeT(\randpan)
l.stopT(\randpan)
l.procs // dictionary containing all Tasks alive
l.noT; // kills all tasks


// compressor. yes, there is a built in compressor/expander. inactive by default
l.comp(0.2, 0.6, 0.2) //threshold, slopeBelow, slopeAbove
l.thr(0.2); // threshold
l.slb(0.6); // slope below thr <1 for expansion
l.sla(0.2); //slope above thr <1 for compression >1 for expansion



// some examples of scheduling tasks
l.sch(\backtoinit, {l.reset; l.rat(1)}, 10)
l.stopT(\backtoinit)

l.sch(\rev, {l.reverse}, 10, offset:5)
l.stopT(\rev)

l.sch(\rlen, {l.rlen(1, 2)}, 8, 4)
l.stopT(\rlen)

l.sch(\brat, {l.brat(0.01)}, 10, offset:4) // rate
l.stopT(\brat)

l.sch(\bbounds, {l.bbounds}, 4, offset:3)
l.stopT(\bbounds)

l.sch(\bvol, {l.bvol}, 4)
l.stopT(\bvol)

l.sch(\bjump, {l.bjump(0.01)}, 1, offset:1)
l.stopT(\bjump)

l.sch(\rst, {l.rst}, 15, offset:10)
l.stopT(\rst)

l.sch(\rbufs, {l.ps.choose.setbuf(l.bufs.choose)}, 15)
l.stopT(\rbufs)

l.sch(\inoutrand, {
	//l.rjump;
	//l.rrat;
	//l.brat(0.01, 6);
	l.fadein(9);
	{l.fadeout(9)}.defer(15)
}, 30)
l.stopT(\inoutrand)

l.sch(\ramps, {// this will move every 10 secs all the pitchs to a random value ranging -4 to +4 from the current value in a 5 sec transition
	l.brat(4, 5, \sin)
}, 10, 3)
l.stopT(\ramps)







/////////////////////
// more examples

( //inital situation
l.verbose(true);
l.normalize;
l.noT;
l.vol(0.1); // lower vol because all layers are in synch
l.setbuf( l.bufs[32] ); // all layers use the same buffer
l.jump(0);
l.rat(1);
l.thr(0.2);// compressor threshold
l.slb(0.7); // increase to expand low
l.sla(0.3); // decrease to compress top
l.plot(l.bufs[2]);
l.resume;
)

(
l.jump(0.8); //
l.bounds(0.979, 1)
)

l.bounds(0,1, offset:10)



l.gofwd
l.noT
l.sch(\jump, {l.jump(1.0.rand)}, 3)
l.rat(0.5)

l.info

l.reset
l.rjump(offset:10)
l.jump(0.9, 10);

l.rat(0, 5)

l.brat(0.1, 2, offset:3);///

l.sch(\ku, {l.bjump(0.01)}, 8, 2);

l.fadeout(20, offset:7)
l.vol(0.15, 120)

l.sch(\brat, {l.brat(0.02, 6, offset:5)}, 10)
l.stopT(\jump)

l.volu

l.vol(0.4)
l.jump(0.03, offset:8)
l.resume
l.rrat(10, offset:5)
l.pause
l.reverse
l.rdir(10, offset:10)

(
l.sch(\files, {
	//l.rjump;
//	l.setbuf( l.bufs.choose );
	l.rst;
	l.len(rrand(10, 20));
}, 1.5)
)

l.stopT(\files)
l.noT



//
l = Layers.new("/home/r2d2/Mahaigaina/audio/00songs/", 12); // this takes a few seconds... please wait...
l.save

l.rbuf
l.setbuf(l.bufs[2])
l.curbufs

(
l.vol(0.35);
l.rbuf;
l.rbounds;
l.rat(0.1, 60*2, offset:60*1.5);
l.rdir(offset:10);
)

l.sch(\bo, {l.reverse}, 4)

(
var len=50, gap=5, fade=0.5;
l.sch(\rum, {
	l.rbuf;
	l.rdir; l.rrat; l.rbounds;
	l.fadein(fade);
	{l.fadeout(fade)}.defer(len-gap);
}, len)
)

l.rvol
l.vol(0.3, offset:10)
l.rdir
l.gobwd
l.gofwd
l.ps.choose.rdir
//l.some(2).rbounds // it would be great to be able to do this. with more than one item in the array how to call that function to all items in the array?
// would be great too to be able to add a pause after between the loops l.loop(0.1)?
l.reverse;
l.fadeout(60*2)
l.rbounds(offset:10)
l.reset(offset:0.9)
l.rjump(offset:0.6)

(
l.sch(\rbounds, {
	l.ps.choose.rbounds;
}, 20)
)
(
l.sch(\rvol, {
	l.ps.choose.bvol;
}, 5.5)
)
(
l.sch(\reset, {
	l.reset(offset:0.9)
}, 60)
)
l.noT

l.rvol

l.stopT(\rum)

///////////////////////////////
// control individual layers //
///////////////////////////////

// you can do almost the same to all layers and to individual ones

// GET
l.ps[0].vol
l.ps[0].info
l.ps[0].file
l.ps[0].buf
l.ps[10].plot // opens a window that displays the buffer and the current loop selection. it allows to change the selection by click
l.ps[0].end // get
l.ps[0].st //get
l.ps[0].curpos
l.ps[0].verbose = 1 // then the layer will post events

// SET
l.ps[0].setbuf( l.bufs[1] )
l.ps[0].volume( 1 )
l.ps[0].pan( 0 ) // -1 to 1
l.ps[0].rat(1)
l.ps[0].bounds(0.1, 0.12) // st and end. range 0-1
l.ps[0].len(1000) // in millisecs!!!
// and so on...

// ACTIONS
l.ps[0].resume
l.ps[0].fadein(100)
l.ps[0].fadeout(5)
l.ps[0].pause
l.ps[0].jump(0.7) // go there now
// and so on...

// memory
l.ps[0].push
l.ps[0].pop

// random
l.ps[0].rbounds //
l.ps[0].rst
l.ps[0].rend
l.ps[0].rvol
l.ps[0].rpan
l.ps[0].rrat
l.ps[0].rdir
l.ps[0].bbounds(0.01) // single step small random
// and so on...

