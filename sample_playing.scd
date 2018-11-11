/*
Layers by ixi-audio.net
license GPL
*/


/*
TO DO: create a GUI where there is a representation of each layer and its buffer and individual volumes can be controlled ?

*/


( // TO START THE SYSTEM
// this loads all sound files in a folder
// instantiates as many players as you wish
// waits for everything to be loaded and ready
// each player gets a buffer. if more players than buffers it wraps (p1->b1, p2->b2, p3->b1 and so on)

s.boot;
//a = "/home/r2d2/Mahaigaina/audio/00cuts/*";
a = "/home/r2d2/Mahaigaina/audio/00works/*";
//a = "/home/r2d2/Mahaigaina/audio/00songs/*"; // Folder to load samples from
//a = "/home/r2d2/Mahaigaina/audio/sounds/*";
s.waitForBoot({
	s.meter(2,2);
	if (l.isNil.not, {l.free});
	l = Layers.new(s, a, 12); //HOW MANY players
})
)


( // INITIAL SITUATION, for instance
l.normalize;
//l.rvol;
l.vol(0.3);
l.resume;
)

// to create a single layer
//n= Buffer.new(s, "/home/r2d2/ownCloud/audio/tm-2018-06-01T14_40_59.flac")
//m = Layer.new(1000, n);
//m.resume;



/////////////////////////////////////
// global controls. for all layers //
/////////////////////////////////////

l.allbufs // available
l.curbufs // actually being used by whom
l.info // post internal states for all available layers
l.verbose(true)
l.setbuf( l.bufs[3] ); // all layers to use the same buffer
l.asignbufs // asign loaded buffers to layers, it happens by default on Layers.new
l.normalize
l.ps // list containing players
l.plot; // draws the play heads of each layer in a window
l.plot(l.bufs[2]) // optional pass the buffer to draw on the background of the window. useful when all layers share the same buffer

l.resume
l.pause
l.solo( l.ps[0] ) // pause all but this layer
l.reverse
l.gofwd // postive rate
l.gobwd // negative rate
l.volu // UP
l.vold // DOWN
l.vol(0.1)
l.fadein(5)
l.fadeout(5)
l.pan(0) //-1 to 1
l.rat(1)
l.bounds(0, 0.1) // st, end in 0 to 1 range
l.jump(0.5) // go there
l.len(600) // length, in milliseconds!
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
l.rbounds(0.2, 0.1) // set start and LENGTH limits for the random
l.rpan
l.rrate
l.rvol
l.rst // start point
l.rend // end point (not length)
l.rlen(0.1) // lengthin range 0-1
l.rdir // + or - pitch
l.rjump

// random walks
l.bbounds // small range jump. range defaults to 0.01
l.bbounds(0.05) // single step small random
l.bvol // small range jump. range defaults to 0.01
l.bvol(0.05) // single step small random
l.bjump(0.9)
l.brat


// offsets: functions also have offsets so that layers dont run the function at the same time
l.rrate(offset:8) // all layers shoud get a random rate after 8 secs. each one at a different time within the next 8 secs
l.rlen(offset:20) // and so on...


// transitions: some functions (the ones related to panning, amplitude and pitch) perform a transition between the current state and the target state
//go to a value in N secs describing a shape: \lin, \exp, \sin, \wel, \cub, \sqr
l.rat(0.1, 10, \lin)// check this out, it is self explanatory :)
l.brat(0.1, 20, \sin) // this will move to a random rate from -0.1 to 0.1 of current state in 20 secs applying a sinusoidal ramp to the change
l.brat(0.1, 20, \sqr, offset:8) // offset can be use here too


// scheduling functions
l.sch(\randpan, {l.rpan}, 15); //schedule a function every N secs. args: NAME, FUNCTION, TICK TIME, OFFSET
l.sch(\randpan, {l.rpan}, 15, offset:5); // ... optional offset delay so that each layer fires at N+(offset.rand)
l.pauseT(\randpan)
l.resumeT(\randpan)
l.stopT(\randpan)
l.procs // dictionary containing all Tasks alive
l.noT; // kills all tasks


// compressor. yes, there is a built in compressor/expander inactive by default
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
	l.rjump;
	[l.rrate, l.rrate(offset:20)].choose;
	l.fadein(10);
	{l.fadeout(10)}.defer(30)
}, 50)
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
l.setbuf( l.bufs[3] ); // all layers use the same buffer
l.jump(0);
l.rat(1);
l.thr(0.08);// compressor threshold
l.slb(0.7); // increase to expand low
l.sla(0.3); // decrease to compress top
l.plot(l.bufs[1]);
l.resume;
)

(
l.setbuf( l.bufs[5] );
l.jump(0.885); //
l.bounds(0.885, 0.97)
)



l.reset
l.rjump
l.rat(1)

l.rat(1, 20)
l.brat(0.01, 10, offset:0);

l.jump(0.5, offset:0.1);


l.sch(\brat, {l.brat(0.001)}, 10)
l.stopT(\brat)

l.bjump(0.02)

l.rdir(offset:5)

l.reverse(offset:3)
l.reverse
l.gofwd
l.gobwd

(
l.brat(0.01);
l.jump(0.6);
)





///////////////////////////////
// control individual layers //
///////////////////////////////


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
l.ps[0].rrate
l.ps[0].rdir
l.ps[0].bbounds(0.01) // single step small random
// and so on...

