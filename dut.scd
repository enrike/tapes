(
s.boot;
a = "/home/r2d2/Mahaigaina/audio/00cuts/*";
s.waitForBoot({
	if (l.isNil.not, {l.free});
	l = Layers.new(s, a, 3);
})
)

l.allbufs // available
l.curbufs // actualy being used by whom

l.setbuf( l.bufs[27] ) // set buffer

p = l.ps[0]
p.resume
p.volume(1);
p.plot

p.info
p.file

p.rpos
p.rst
p.rend // is this working ok

(
t.stop;
t = p.sch(1, {p.reverse})
)

(
y.stop;
y = p.sch(4, {p.rpan})
)

(
u.stop;
u = p.sch(4, {p.rpos})
)

(
q.stop;
q = p.sch(60, {p.pos(0.1,1)}) // back to origin
)

(
v.stop;
v = p.sch(5, {p.rpos})
)


p.len(100)
p.reverse
p.bpos(0.01)
p.bvol(0.01)

p.rat(1)
p.info
p.file


// memory
p.push
p.pop

// random
p.rvol
p.rpan
p.rrate

// tasks
p.brownpos(0.1, 1, 1, 0)
p.brownvol(0.9, 1, 1, 0)
p.brownrate(0.1, 1, 1, 0)
p.ptask.stop;
p.vtask.stop;
p.rtask.stop;


////////////////////////////
////////////////////////////


(
s.boot;
{
	p = Layer.new;
	p.loadbuf(s, "/home/r2d2/Mahaigaina/audio/00cuts/dut.wav");
	p.resume;
	//p.plot
}.defer(2)
)


