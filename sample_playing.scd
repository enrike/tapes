

(
s.boot;
s.waitForBoot({
	l = Layers.new(s, "/home/r2d2/Mahaigaina/audio/00cuts/*", 24);
	{ l.players }.defer(2)
})
)

l.rvol
l.resume
l.pause
l.rbuf
l.rpos
l.volu
l.vold
l.vol(1)
l.rat(0.5)
l.rrate

l.brownpos(5, 0.3, 1, 0.05)
l.brownvol(1, 1, 1, -1)
l.brownrate(0.01, 3, 1, 0)

l.pls[0].file
l.pls[0].vol
l.pls[0].info



1.asBoolean