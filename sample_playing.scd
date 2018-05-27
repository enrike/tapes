/*
Layers by ixi-audio
license GPL
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
	if (l.isNil.not, {l.free});
	l = Layers.new(s, a, 24); //HOW MANY players
})
)





s.meter(12, 2)




// global. all layers //
l.allbufs
l.curbufs
l.normalize
l.ps // list of players

l.resume
l.pause
l.rat(1)
l.vol(0.4)
l.pos(0, 0.1) // st, end
l.len(1000) // in milliseconds!
l.volu //UP
l.vold //DOWN

//l.outb(10) // outbus for all players
//f = Synth(\HPF, [\cut, 1000])
//f.set(\cut, 2000)

// store and retrieve named states. they dont get deleted until the session ends
l.push(\blah)
l.pop(\blah)


//random
l.rbuf
l.rpos
l.rrate
l.rvol
l.rst
l.rend

l.bpos // small range jump defaults to 0.01
l.bpos(0.05)

// tasks. schedule random prop set
l.brownpos(0.1, 2, 0, 0.05)
l.brownvol(0.01, 5, 1, 0)
l.brownrate(0.01, 1, 1, 0)
l.stopptask // pos // TO DO this is not stopping the tasks
l.stopvtask // vol
l.stoprtask // rate

l.bpos(0.05) // single step small random



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
l.ps[0].setbuf( l.bufs.choose )
l.ps[0].volumen( 1 )
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

