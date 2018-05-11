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
//a = "/home/r2d2/Mahaigaina/audio/00cuts/*";
a = "/home/r2d2/Mahaigaina/audio/00songs/*"; // Folder to load samples from
s.waitForBoot({
	if (l.isNil.not, {l.free});
	l = Layers.new(s, a, 24); //HOW MANY players
	{ l.players }.defer(3)
})
)


// global. all layers //
l.allbufs
l.curbufs
l.normalize
l.ps // list of players

l.resume
l.pause
l.rat(0.9)
l.vol(0.4)
l.volu //UP
l.vold //DOWN

//random
l.rvol
l.rbuf
l.rpos
l.rrate
l.rst
l.rlen

// tasks. schedule random prop set
l.brownpos(0.1, 2, 1, 0.05)
l.brownvol(0.01, 5, 1, 0)
l.brownrate(0.01, 1, 1, 0)
l.stopptask // pos // TO DO this is not stopping the tasks
l.stopvtask // vol
l.stoprtask // rate




// control individual layers //

// GET
l.ps[0].vol
l.ps[0].info
l.ps[0].file
l.ps[0].plot
l.ps[0].end // get
l.ps[0].st //get

// SET
l.ps[0].setbuf( l.bufs.choose )
l.ps[0].volumen( 1 )
l.ps[0].rat(1)
l.ps[0].pos(0.1, 0.12) // st and end

// ACTIONS
l.ps[0].resume
l.ps[0].pause

l.ps[0].rpos
l.ps[0].rst
l.ps[0].rvol
l.ps[0].rrate

l.ps[0].brownpos(0.1, 1, 1, 0)
l.ps[0].brownvol(0.9, 1, 1, 0)
l.ps[0].brownrate(0.1, 1, 1, 0)
l.ps[0].ptask.stop;
l.ps[0].vtask.stop;
l.ps[0].rtask.stop;

