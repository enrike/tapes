/*
Tapes by ixi-audio.net
license GPL

Tapes allows to create and control multiple tapeloop-like sample players
*/



// "how to use" guide in there lines
Tapes(this, "~/Mahaigaina/audio/00works/"); // prepare the system and load all samples from this folder. must wait until loaded to continue
_add(4); // create 4 players. wait until created to continue
_play;


_verbose(true) // will post extra log information


// MANAGE PLAYERS //

// _add to instantiate N tape players each playing one of the loaded files.
// if more tape players than sound files it wraps
_add; // adds one new tape player (with rate 0) at current group
_add(5)// adds 5 new tape players
_add(3, _0) // adds 3 tape players copying tape players 0's state
_add(1, 1008) // you can also pass the player's id
_0.copy(_1) // tape player 0 copies tape player 1's state (here you cannot pass the id)

_group // all tape player in current group
_1 // individual tape player _0, _1, _2, _3, etc...
_id(1008) // returns the tape with that ID
_where(1008)// returns the position of tape with that id in current group 0,1,2...

_info // post internal states for all available tape players
_2.state // returns a dictionary with the state of tape player 2, etc...

_all // array with all players
_hm // how many players exist

_some // returns a list with randomly chosen tape players
_some(3) // returns a list with 3 randomly chosen tape players
_them // last ones choosen by _some
_one // choose one rand
_it // last one choosen by _one

// groups
_currentgroup // active group: default is \a
_group // posts current group's content
_groups // all groups' content
_newgroup(\dirty) // creates a new group
_usegroup(\dirty) // switch to this roup
//_useall // use all groups
_removegroup(\dirty) // removes the group and kills all players in it
//_movegroup(_1, \dirty) //move _1 from current group to group \a
//_whichgroup(1001) // by ID
_mergegroups // merge all players to group \a and usegroup(\a)

_kill(2) // kills tape player at pos 2 in current group. if N is too big it kills the last one
_kill // kills a random tape player at current group
_killall // kills everyone at current group
_killthemall // kills everyone in every group

// experimental
_plot; // draws the play heads of each tape player the same window
_plot(4) // optional pass the buffer to draw on the background of the window. useful when all tape players share the same buffer


//  BUFFERS //

_loadfiles("some/directory"); // load all files from this directory. BEWARE: loading files takes time
_normalize // normalise buffers
_bufs // posts buffers available
_curbufs // posts which buffers actually being used by which tape players (_0, filename)
_bufinfo // posts index and name of laoded buffers
_buf(5) // asign buffer 5 to all players in current group

// you could load new files and asign them again after tape players are alive
_loadfiles("another/directory");
_asignbufs // force re-asign them



//////////////////////////////////////////////////////////
// global controls. control all tape players at the same time //
//////////////////////////////////////////////////////////

_play // loop play
_stop
_shot // single shot play. experimental
_solo(0) // pause all but 0 player
_reverse // reverse play direction. +rate to -rate
_dir(1) // 1 forwards, -1 backwards
_fwd // turn current rate positive
_bwd // turn current rate negative
_volu // UP by 2/100th
_vold // DOWN by 2/100th
_vol(0.5)
_fadein
_fadeout
_pan(0) //-1 to 1
_rate(0.8)
_wobble(0.07) // rand fluctuation in rate
_brown(0.5) // brownian fluctuation in rate
_vibrato(rate:3, depth:0.04, ratev:0.01, depthv:0.02)

_lp(0, 0.01) // st, end. in 0 to 1 range
_st(0.5) //loop start
_end(0.55) // loop end
_dur(0.006) // loop duration in 0-1 range from st
_len(600) // length, **in milliseconds** useful if they are using different buffers and you want the same loop duration
_move(0.5) // move loop start point to that position and try to maintain the loop duration
_moveby(-0.01) // move loop start and end point by that
_go(0.5) // send playhead to point in sample (only works if >st and <end)

// slicer-like behaviour (http://github.com/enrike/slicer)
_slice(sttime:0.5, shift:2, grain:0.001, grainshift:1)
_slicegui(300,600); // 2x 2D sliders to control _slice parameters

_scratch // experimental. |target=0, tIn=1, tStay=0.5, tOut=1, offset=0| // boomerang like pitch change

// RESET: calling some functions with no args should reset its corresponding state for instance...
_reset // reset loop to 0-1, play from 0, pitch to 1
_lp // resets loop to 0-1
_gost // to to loop start point
_goend // go to loops end poi

_go // resets pos to 0 --> SAME happens with _pan, _rate, _vol. They reset to default value


// store and retrieve named states. they dont get deleted from memory until the session ends
_push(\dut) // store current state as \dut in memory
_save // save current states from memory into a file in home directory ( Platform.userHomeDir ). it posts the path
_save("caos") // optionally provide a name. if no name is provided it uses the timestamp
_load // open a dialogue to search for a file containg states
_load("/home/xxx/caos.states") // tries to load states from the file
_pop(\dut) // load \dut from current states

_control // opens a window with all tape players. you can control de loop selection
_control( 300, 200 )

// search. identify which tape players are playing which sound file
p = _search("Roll") //return a list of tape players whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.vol(0.3)}) // do whatever with the search results



// RANDOM //

_rbuf // anyone gets a new buffer randomly
_rpan
_rrate
_rdir // + or - rate
_rvol
_rloop // 0 to 1
_rloop([0.2, 0.1]) // set start and LENGTH limits for the random. in this case start will be rrange(0,0.2) and length start+(0.1.rand)
_rmove; // move current st-end to a random position
_rst // start point
_rend // end point (not length)
_rlen(0.1) // length. in range 0-1
_rgo // go to a random point in the sound
_rand // random file, pan, vol, rate, dir, loop (st, end) and go

// random walks
_bloop // small range go. range defaults to 0.01
_bloop(0.05) // single step small random
_bvol // small range go. range defaults to 0.01
_bvol(0.05) // single step small random
_bpan
_bgo(0.9)
_brate

// apply commands to some players but not to all
_one.rrate // one gets random rate
_some // returns a random list of N players
_some(3) // 3 random players
_some.collect(_.rrate) // some of them get random rate
_some(3).collect(_.rrate) // 3 get random rate

// OFFSET //
// functions also have offsets so that tape players dont run the function at the same time
_rrate(5, offset:8) // all players random rate at a different timing within the next 8 secs
_rlen(offset:20) // and so on...


// TRANSITION TIME:
// some functions (the ones related to panning, amplitude and pitch) can perform a transition between the current state and the target state in N secs
// go to a value in N secs
_rate(0.1, 10)// go to 0.1 rate in 10secs
_brate(0.1, time:20) // this will start a shift to a random rate ranging -0.1 to 0.1 from the current rate that will take 20s
_brate(0.1, time:20, offset:8) // offset can be use here too

//local randoms
_rate(1, random:0.1); // each of them will apply a small random variation around 1 ranging -0.1 to 0.1
_end(0.5, random:0.01);
_dur(0.1, random:0.05)

// compressor. yes, there is a built in compressor/expander. inactive by default
_comp(0.2, 0.6, 0.2) //threshold, slopeBelow, slopeAbove
_thr(0.2); // threshold
_slb(0.6); // slope below thr <1 for expansion
_sla(0.2); //slope above thr <1 for compression >1 for expansion


// SCHEDULE FUNCTIONS //
// _do args: NAME, FUNCTION, SLEEP, DEFER, ITER, WHEN, OFFSET, RANDOM, TALK
_do(\randpan, {_rpan}, 15); //schedule a function every N secs.
_undo(\randpan) // stops this one
_undo // stops all

// defer
_do(\spawn, {_rrate}, 3, defer:20); //wait 20 secs, then run every 3secs

// num of iterations
_do(\spawn, {_add(1,_0)}, 3, iter:10); //run every 3secs for 10 times then die

// random: introducing swing
_do(\randpan, {_rrate}, 15, random:2); // rand swing: the next sch will happen after 15 + rrand(-2,+2) secs

_do(\wrand, {
	"two options".postln;
}, random:[[1,3],[0.9, 0.1]])// run every every 1 sec (90% chances) or 3 secs (10% chances)

// conditions
z = 0;
_do(\check, {"run".postln}, 3, when:{z>0}) // will start running once z>1
_do(\check, {"run".postln}, 3, when:{z>0}, then:0) // will only run once, then die

// postln or not
_do(\silent, { // wont print every run. useful with quick and repetitive tasks
	_rpan
}, 0.5, verbose:false);



// some examples of scheduling tasks
_do(\backtoinit, {_reset}, 10) // resets every 10s
_do(\rev, {_reverse}, 10, offset:5)
_do(\rlen, {_rlen(1, 2)}, 8, random:4)
_do(\bgo, {_bgo(0.01)}, 1, offset:1)

_do(\inoutrand, { //every 30 sec: 9 sec fade in- after 15 secs - 9 secs fade out
	_fadein(9);
	{_fadeout(9)}.defer(15)
}, 30)
_undo(\inoutrand)

_do(\ramps, {// this will move every 10 secs all the pitchs to a random value ranging -3 to +3 from the current value in a 5 sec transition
	_brate(4, 5, \sin)
}, 10, random:3)


( // keeps mirror playing while slowly moving around the same area
_rdir;
_do(\rev, {
	_bgo(0.01);
	_reverse
}, 1, random:0.2)
)

( // moves forwards
var st=0, end;
_do(\go, {
	st = st+(0.001.rand);
	_loop(st, st+0.001)
}, 0.8)
)

(
var len=10, gap=5, fade=0.5;
_do(\rum, {
	_rbuf;
	_rdir; _rrate; _rloop;
	_fadein(fade);
	{_fadeout(fade)}.defer(len-gap);
}, len)
)

(
var st=0, end=0, speed=0.001, len=0.01, time=3;
_do(\go, {
	st = st+(speed.rand);
	if ( (st+len)>1, {st=0});
	st.postln;
	_bpan(0.5, time:time);
	_rdir;
	_brate(0.0005);
	_loop([st, st+len])
}, time, random:time/3.0)
)





///////////////////////////////
// control individual players //
///////////////////////////////

// you can also control each player individually with _0, _1 etc...

// GET
_0.vol
_0.info
_0.file
_0.buf
_0.end
_0.st
_0.curpos // 0-1 playhead position
_0.verbose

// SET
_0.buf( _bufs[1] )
_0.vol(1)
_0.pan(0) // -1 to 1
_0.rate(1)
_0.loop(0.1, 0.12) // st and end. range 0-1
_0.len(1000) // in millisecs!!!
// and so on...

// ACTIONS
_0.play
_0.fadein(100)
_0.fadeout(5)
_0.stop
_0.go(0.7)
// and so on...

// memory
_0.push
_0.pop

// random
_0.rloop //
_0.rst
_0.rend
_0.rvol
_0.rpan
_0.rrate
_0.rdir
_0.bloop(0.01) // single step small random
// and so on...

