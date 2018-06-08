/*
Layers by ixi-audio
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
// each player gets a random buffer to play

s.boot;
a = "/home/r2d2/Mahaigaina/audio/00cuts/*";
//a = "/home/r2d2/Mahaigaina/audio/00songs/*"; // Folder to load samples from
s.waitForBoot({
	s.meter;
	if (l.isNil.not, {l.free});
	l = Layers.new(s, a, 24); //HOW MANY players
})
)


( // INITIAL SITUATION
l.rvol;
l.resume;
)



l.newplayer(\StPlayerOD) // chaotic player
l.newplayer(\StPlayer) // normal player



// global. all layers //
l.allbufs // available
l.curbufs // actualy being used by whom
l.setbuf( l.bufs[27] ); // all layers use the same buffer
l.normalize
l.ps // list containing players


l.resume
l.pause
l.reverse
l.volu //UP
l.vold //DOWN
l.vol(0.4)
l.pan(0) //-1 to 1
l.rat(0.8)
l.pos(0, 0.1) // st, end in 0 to 1 range
l.len(1000) // in milliseconds!

// store and retrieve named states. they dont get deleted until the session ends
l.push(\dut)
l.pop(\blah2)
l.save // l.save("blahblah") if no name is provided it uses the timestamp
l.load // open a dialogue to search for the state file and load the state into the system


p=l.search("dut") //return a list of layers whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.volume(0.2)}) // do whatever with the search results

o = l.sch(5, {l.rpan}); //schedule a function every N secs
o.pause; o.resume; o.stop


//random
l.rbuf
l.rpos
l.rpos(0.9, 0.1) // start and length
l.rpan
l.rrate
l.rvol
l.rst
l.rend
l.rlen(0.1)

l.bpos // small range jump defaults to 0.01
l.bpos(0.05) // single step small random
l.bvol // small range jump defaults to 0.01
l.bvol(0.05) // single step small random



// tasks. schedule random prop set
l.brownpos(0.1, 2, 0, 0.05) // step, sleep, dsync, delta
l.brownvol(0.01, 5, 1, 0)
l.brownrate(0.01, 1, 1, 0)
// l.brownpan(0.01, 1, 1, 0) // TO DO
l.stopptask // pos // TO DO this is not stopping the tasks
l.stopvtask // vol
l.stoprtask // rate
// TO DO: brown pan



(
t.stop;
t = l.sch(2.8, {l.reverse})
)

(
y.stop;
y = l.sch(4, {l.rlen(0.3)})
)

(
u.stop;
u = l.sch(4, {l.bpos})
)

(
q.stop;
q = l.sch(60, {l.pos(0.1,0.3)}) // back to origin
)

(
v.stop;
v = l.sch(5, {l.rpos})
)





// control individual layers //

// GET
l.ps[0].vol
l.ps[0].info
l.ps[0].file
// displays the buffer and the current loop selection. it allows to change the selection by click
l.ps[0].plot
l.ps[0].end // get
l.ps[0].st //get

// SET
l.ps[0].setbuf( l.bufs[26] )
l.ps[0].volume( 1 )
l.ps[0].pan( 0 ) // -1 to 1
l.ps[0].rat(1)
l.ps[0].pos(0.1, 0.12) // st and end
l.ps[0].len(1000) // in millisecs!!!

// ACTIONS
l.ps[0].resume
l.ps[0].pause
l.ps[0].bpos(0.01) // single step small random

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
l.ps[0].brownpos(0.1, 1, 1, 0)
l.ps[0].brownvol(0.9, 1, 1, 0)
l.ps[0].brownrate(0.1, 1, 1, 0)
l.ps[0].ptask.stop;
l.ps[0].vtask.stop;
l.ps[0].rtask.stop;

