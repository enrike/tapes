/*
Layers by ixi-audio.net
license GPL
*/





s.boot;// BOOT SERVER FIRST

// CREATE LAYERS instantiating Layers class
// l = Layers.new("pathtofileorfolder", NUMOFLAYERS);
// you can pass the path to a folder with many sounds files or the path to a single soundfile
// when a folder is passed each player gets a buffer. if more players than buffers it
// wraps like (p1->b1, p2->b2, p3->b1 and so on)
// if a single file all layers play the same file

// for instance...
//a = "/home/r2d2/Mahaigaina/audio/00works/";
a = "/home/r2d2/Mahaigaina/audio/00songs/03_Pinebox_Derby.flac";
//a = "/home/r2d2/Mahaigaina/audio/00_mbv/isntanything/";
//a = "/home/r2d2/Mahaigaina/audio/00songs/"; // Folder to load samples from

l.free
l = Layers.new(a, 12); // this takes a few seconds... please wait...


// to create a single independent layer
n = Buffer.new(s, "~/tm-2018-06-01T14_40_59.flac")
m = Layer.new(1000, n); //1000 is just any unique ID
m.resume;
// or to control a single layer from all Layers
l.ps[0].pause



( //initial situation
l.normalize;
l.vol(0.3);
l.resume; // go play
)




/////////////////////////////////////
// global controls. for all layers //
/////////////////////////////////////

l.allbufs // buffers available
l.curbufs // buffers actually being used by whom
l.ps // list containing all players
l.info // post internal states for all available layers
l.verbose(true)
l.setbuf( l.bufs[3] ); // set this buffer to all layers
l.asignbufs // asign the loaded buffers to layers, it happens by default on Layers.new
l.normalize
l.plot; // draws the play heads of each layer in a window
l.plot(l.bufs[2]) // optional pass the buffer to draw on the background of the window. useful when all layers share the same buffer

l.resume
l.pause
l.solo( l.ps[0] ) // pause all but this layer
l.reverse // change play direction. +rate to -rate
l.gofwd // force positive rate
l.gobwd // force negative rate
l.volu // UP
l.vold // DOWN
l.vol(0.5)
l.fadein
l.fadeout
l.pan(0) //-1 to 1
l.rat(1)
l.bounds(0, 0.01) // st, end. in 0 to 1 range
l.jump(0.5) // go there
l.len(600) // length, **in milliseconds** useful if they are using different buffers and you want same loop duration
l.reset // reset bounds to 0-1 and play from 0

// store and retrieve named states. they dont get deleted from memory until the session ends
l.push(\dut)
l.pop(\dut)
l.save // save current states from memory into a file.
l.save("blahblah") // optionally provide a name. if no name is provided it uses the timestamp
l.load // open a dialogue to search for a file containg states

// search. identify which layers are playing which sound file
p = l.search("Roll") //return a list of layers whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.volume(0.3)}) // do whatever with the search results

//random
l.rbuf // anyone gets a new buffer randomly
l.rbounds // 0 to 1
l.rbounds(0.2, 0.1) // set start and LENGTH limits for the random. in this case start will be rrange(0,0.2) and length start+(0.1.rand)
l.rpan
l.rrat
l.rvol
l.rst // start point
l.rend // end point (not length)
l.rlen(0.1) // length. in range 0-1
l.rdir // + or - rate
l.rjump

// random walks
l.bbounds // small range jump. range defaults to 0.01
l.bbounds(0.05) // single step small random
l.bvol // small range jump. range defaults to 0.01
l.bvol(0.05) // single step small random
l.bpan
l.bjump(0.9)
l.brat


// offsets: functions also have offsets so that layers dont run the function at the same time
l.rrat(offset:8) // all layers random rate within 8 secs. each one at a different time within the next 8 secs
l.rlen(offset:20) // and so on...


// transitions: some functions (the ones related to panning, amplitude and pitch) can perform a transition between the current state and the target state in N secs
//go to a value in N secs describing a shape: \lin, \exp, \sin, \wel, \cub, \sqr
l.rat(0.1, 10, \lin)// check this out, the sound is self explanatory :)
l.brat(0.1, 20, \sin) // this will start a shift to a random rate from -0.1 to 0.1 of current rate that will take 20 secs and describing a sinusoidal ramp during the change
l.brat(0.1, 20, \sqr, offset:8) // offset can be use here too


// scheduling functions
l.sch(\randpan, {l.rpan}, 15); //schedule a function every N secs. args: NAME, FUNCTION, TICK TIME, OFFSET
l.sch(\randpan, {l.rpan}, 15, offset:5); // ... optional offset delay so that each layer fires at N+(offset.rand)
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

l.sch(\rlen, {l.rlen(0.7, 0.9)}, 8, 4)
l.stopT(\rlen)

l.sch(\brat, {l.brat(0.01)}, 10, offset:4) // rate
l.stopT(\brat)

l.sch(\bbounds, {l.bbounds}, 4)
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
( //inital situation
l.verbose(true);
l.normalize;
l.noT;
l.vol(0.1); // lower vol because all layers are in synch
l.setbuf( l.bufs[2] ); // all layers use the same buffer
l.jump(0);
l.rat(1);
l.thr(0.2);// compressor threshold
l.slb(0.7); // increase to expand low
l.sla(0.3); // decrease to compress top
l.plot(l.bufs[4]);
l.resume;
)

(
l.jump(0.8); //
l.bounds(0.885, 0.97)
)



l.reset
l.rjump
l.jump(0.1);
l.rat(1, 20)

l.rat(1, 7)
l.brat(0.01, 10, offset:5);

l.jump(0.5, offset:0.1);

l.vol(0.15, 120)

l.sch(\brat, {l.brat(0.001)}, 10)
l.stopT(\brat)

( //MVB
l.setbuf( l.bufs[9] );
l.rjump;
l.sch(\rpan, {l.rpan(5)}, 15);
l.sch(\bvol, {l.bvol(0.01, 5)}, 6);
l.thr(0.08);// compressor threshold
l.slb(1.1); // increase to expand low
l.sla(0.01); // decrease to compress top

l.sch(\rpan, {l.rpan(10)}, 10)

l.sch(\one, {
	var play, old;
	play=l.ps.choose;
	old=play.volume;
	play.vol(l.ps.choose.volume+0.07, 10);
	{play.vol(old, 10)}.defer(10)
}, 10)
)

l.vol(0)
l.vol(0.15, 360, \exp)
l.pan(0, 10)
l.nocomp; l.noT

l.bjump(0.02)

l.rdir(offset:5)

l.reverse(offset:3)
l.reverse(1)
l.gofwd(5)
l.gobwd

(
l.brat(0.01);
l.jump(0.8);
)





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

