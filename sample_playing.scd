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
	s.meter;
	if (l.isNil.not, {l.free});
	l = Layers.new(s, a, 12); //HOW MANY players
})
)


( // INITIAL SITUATION
l.normalize;
//l.rvol;
l.vol(0.4);
l.resume;
)

//n= Buffer.new(s, "/home/r2d2/ownCloud/audio/tm-2018-06-01T14_40_59.flac")
//n.play
//m = Layer.new(1000, n); // independent single layer



l.newplayer(\StPlayerOD) // chaotic player. experimental.
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
l.pos(0, 0.1) // st, end in 0 to 1 range
l.jump(0) // go there
l.len(600) // in milliseconds!
l.reset // reset pos and play from 0

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
l.rpos // 0 to 1
l.rpos(0.9, 0.1) // set start and length limits
l.rpan
l.rrate
l.rvol
l.rst // start point
l.rend
l.rlen(0.1) // range 0-1

// random walks
l.bpos // small range jump. range defaults to 0.01
l.bpos(0.05) // single step small random
l.bvol // small range jump. range defaults to 0.01
l.bvol(0.05) // single step small random
l.bjump
l.brat


// schedule functions
l.sch(\randpan, {l.rpan}, 15); //schedule a function every N secs. args: NAME, FUNCTION, TICK TIME, RAND RANGE OFFSET
l.sch(\randpan, {l.rpan}, 15, offset:5); // ... optional offset delay so that each layer fires at N+(offset.rand)
l.pauseT(\randpan)
l.resumeT(\randpan)
l.stopT(\randpan)
l.procs // dictionary containing all Tasks alive


// some examples of scheduling tasks. not rocket science
l.sch(\backtoinit, {l.reset; l.rat(1)}, 10)
l.stopT(\backtoinit)

l.sch(\rev, {l.reverse}, 10, offset:2.5)
l.stopT(\rev)

l.sch(\rlen, {l.rlen(0.7, 0.9)}, 8, 4)
l.stopT(\rlen)

l.sch(\brat, {l.brat(0.01)}, 8, offset:4)
l.stopT(\brat)

l.sch(\bpos, {l.bpos}, 4)
l.stopT(\bpos)

l.sch(\bvol, {l.bvol}, 4)
l.stopT(\bvol)

l.sch(\bjump, {l.bjump(0.1)}, 2)
l.stopT(\bjump)

l.sch(\rst, {l.rst}, 15, offset:3)
l.stopT(\rst)

l.sch(\rbufs, {l.ps.choose.setbuf(l.bufs.choose)}, 15)
l.stopT(\rbufs)


(
l.reset;
l.rat(1)
)







///////////////////////////////
// control individual layers //
///////////////////////////////


// GET
l.ps[0].vol
l.ps[0].info
l.ps[0].file
l.ps[0].plot // opens a window that displays the buffer and the current loop selection. it allows to change the selection by click
l.ps[0].end // get
l.ps[0].st //get

// SET
l.ps[0].setbuf( l.bufs[1] )
l.ps[0].volume( 1 )
l.ps[0].pan( 0 ) // -1 to 1
l.ps[0].rat(1)
l.ps[0].pos(0.1, 0.12) // st and end. range 0-1
l.ps[0].len(1000) // in millisecs!!!

// ACTIONS
l.ps[0].resume
l.ps[0].fadein(8)
l.ps[0].fadeout(5)
l.ps[0].pause
l.ps[0].bpos(0.01) // single step small random
l.ps[0].jump(0.7) // go there now

// memory
l.ps[0].push
l.ps[0].pop

// random
l.ps[0].rpos
l.ps[0].rst
l.ps[0].rend
l.ps[0].rvol
l.ps[0].rpan
l.ps[0].rrate

// tasks
/*
l.ps[0].brownpos(0.1, 1, 1, 0)
l.ps[0].brownvol(0.9, 1, 1, 0)
l.ps[0].brownrate(0.1, 1, 1, 0)
l.ps[0].ptask.stop;
l.ps[0].vtask.stop;
l.ps[0].rtask.stop;
*/
