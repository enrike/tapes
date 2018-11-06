/*
Layers by ixi-audio.net
license GPL
*/


/*
TO DO: create a GUI where there is a representation of each layer and its buffer and individual volumes can be controlled ?

filter an array of objects that meet a condition
*/


(
// load all sound files in a folder
// instantiate as many players as you wish
// wait for everything to be loaded and ready
// each player gets a buffer. if more players than buffers it wraps (p1->b1, p2->b2, p3->b1 and so on)

s.boot;
//a = "/home/r2d2/Mahaigaina/audio/00cuts/*";
a = "/home/r2d2/Mahaigaina/audio/00works/*";
//a = "/home/r2d2/Mahaigaina/audio/00songs/*"; // Folder to load samples from
//a = "/home/r2d2/Mahaigaina/audio/sounds/*";
//a = "/home/r2d2/Mahaigaina/audio/00works2/*";
s.waitForBoot({
	s.meter(2,2);
	if (l.isNil.not, {l.free});
	l = Layers.new(s, a, 12); //HOW MANY players
})
)


( // INITIAL SITUATION
l.normalize;
//l.rvol;
l.vol(0.3);
l.resume;
)

//n= Buffer.new(s, "/home/r2d2/ownCloud/audio/tm-2018-06-01T14_40_59.flac")
//n.play
//m = Layer.new(1000, n); // independent single layer



//l.newplayer(\StPlayerOD) // chaotic player. experimental.
l.newplayer(\StPlayer) // normal player. default






/////////////////////////////////////
// global controls. for all layers //
/////////////////////////////////////

l.allbufs // available
l.curbufs // actualy being used by whom
l.setbuf( l.bufs[2] ); // all layers use the same buffer
l.asignbufs // asign buffers to layers, it happens by default on Layers.new
l.normalize
l.ps // list containing players
l.plot(l.bufs[2]) // optional pass the buffer to draw


l.resume
l.solo( l.ps[0] ) // pause all but this layer
l.pause
l.reverse
l.volu // UP
l.vold // DOWN
l.vol(0.4)
l.fadein(8)
l.fadeout(250, 10)
l.pan(0) //-1 to 1
l.rat(0.92)
l.bounds(0, 0.1) // st, end in 0 to 1 range
l.jump(0.5) // go there
l.len(600) // in milliseconds!
l.reset // reset bounds and play from 0

// store and retrieve named states. they dont get deleted until the session ends
l.push(\dut)
l.pop(\chulo)
l.save // l.save("blahblah") // save current states from memory into a file. if no name is provided it uses the timestamp
l.load // open a dialogue to search for a file containg stated

// identify which layers are using which sound
p = l.search("Roll") //return a list of layers whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.volume(0.3)}) // do whatever with the search results

//random
l.rbuf // anyone gets a new buffer randomly
l.rbounds // 0 to 1
l.rbounds(0.9, 0.1) // set start and length limits
l.rpan
l.rrate
l.rvol
l.rst // start point
l.rend
l.rlen(0.1) // range 0-1
l.rdir // + or - pitch
l.rjump

// random walks
// TODO CHECK IF THIS IS BOUNDS OR JUMP
l.bbounds // small range jump. range defaults to 0.01
l.bbounds(0.05) // single step small random
l.bvol // small range jump. range defaults to 0.01
l.bvol(0.05) // single step small random
l.bjump(0.9)
l.brat


// functions also have offsets so that layers dont run the function at the same time
l.rrate(offset:8) // all shoud get a random rate after 8 secs. each one at a different time


// schedule functions
l.sch(\randpan, {l.rpan}, 15); //schedule a function every N secs. args: NAME, FUNCTION, TICK TIME, RAND RANGE OFFSET
l.sch(\randpan, {l.rpan}, 15, offset:5); // ... optional offset delay so that each layer fires at N+(offset.rand)
l.pauseT(\randpan)
l.resumeT(\randpan)
l.stopT(\randpan)
l.procs // dictionary containing all Tasks alive
l.stopAll; // kills all tasks




// some examples of scheduling tasks. not rocket science
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







/////////////////////
( //inital situation
l.vol(0.2); // lower vol because all layers are in synch
l.setbuf( l.bufs[2] ); // all layers use the same buffer
l.resume;
)

( // reset to init
l.stopAll;
l.vol(0.2);
l.jump(0);
l.rat(1);
)

l.rjump

// TODO; a single window with the sample to see all playheads at thesame time

l.brat(0.001, offset:20);
l.sch(\brat, {l.brat(0.001)}, 10)
l.stopT(\brat)


l.bjump(0.01)

l.rdir(offset:10);

l.rat(1)

l.reverse(offset:3)
l.reverse

( // all within 20 secs
l.bjump(0.01, offset:20);
l.vol(0.3, offset:20)
)


( // if rate
l.vol(0.15);
l.brat(0.01);
l.jump(0.8, offset:0); //0.8
)


/////////////////////////











///////////////////////////////
// control individual layers //
///////////////////////////////


// GET
l.ps[0].vol
l.ps[0].info
l.ps[0].file
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

// ACTIONS
l.ps[0].resume
l.ps[0].fadein(8)
l.ps[0].fadeout(5)
l.ps[0].pause
l.ps[0].jump(0.7) // go there now

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

// etc...

// tasks
/*
l.ps[0].brownpos(0.1, 1, 1, 0)
l.ps[0].brownvol(0.9, 1, 1, 0)
l.ps[0].brownrate(0.1, 1, 1, 0)
l.ps[0].ptask.stop;
l.ps[0].vtask.stop;
l.ps[0].rtask.stop;
*/
