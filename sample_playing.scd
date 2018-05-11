/*
*/


(
s.boot;
//a = "/home/r2d2/Mahaigaina/audio/00cuts/*";
a = "/home/r2d2/Mahaigaina/audio/00songs/*";
s.waitForBoot({
	if (l.isNil.not, {l.free}); // just in case
	l = Layers.new(s, a, 24);
	{ l.players }.defer(2)
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

// tasks. schecdule random prop set
l.brownpos(0.1, 2, 1, 0.05)
l.brownvol(0.01, 5, 1, 0)
l.brownrate(0.01, 1, 1, 0)
l.stopptask
l.stopvtask
l.stoprtask




// individual layers //
l.ps[0].setbuf( l.bufs.choose )
l.ps[0].volumen( 1 )
l.ps[0].rat(1)

l.ps[0].vol
l.ps[0].info
l.ps[0].file
l.ps[0].plot

l.ps[0].resume
l.ps[0].pause

l.ps[0].end
l.ps[0].st

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

l.ps.do({|p| p.rtask.stop})