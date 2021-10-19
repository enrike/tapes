/*
Tapes by ixi-audio.net
license GPL

Tapes allows to create and control multiple tapeloop-like sample players

note that it expects stereo samples
*/


//////////////////////////////
// quick "how to use" guide //
Tapes(this, "~/Mahaigaina/audio/00works/"); // prepare the system and load all samples from this folder. must wait until loaded to continue
_add(4); // create 4 players
_play;
//////////////////////////////




_verbose(true) // will post extra log information


// MANAGE PLAYERS //

// _add to instantiate N tape players each playing one of the loaded files.
// if more tape players than sound files it wraps
_add; // adds one new tape player (with rate 0) at current group
_add(5)// adds 5 new tape players
_add(3, _0) // adds 3 tape players copying tape players 0's state
_add(1, 1008) // you can also pass the player's id
_0.copy(_1) // tape player 0 copies tape player 1's state (here you cannot pass the id)

_group // all tape players in current group
_1 // individual tape player _0, _1, _2, _3, etc...
_id(1008) // returns the instance of the tape with that ID
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

// groups. are stacks of tapes that will respond to commands. This allows to have a group doing something and
// create other tapes that are not affected by commmand unless selected with _currentgroup
// by default there is a group called \default
_currentgroup // active group: default is \default
_group // posts current group's content
_groups // all groups' content
_newgroup(\dirty) // creates a new group
_usegroup(\dirty) // switch to this group
_usegroup // if no arg it defaults to \default group
//_useall // use all groups
_removegroup(\dirty) // removes the group and kills all players in it
//_togroup(_1, \dirty) //move _1 from current group to group \a
//_whichgroup(1001) // by ID
_mergegroups // merge all players to group \a and usegroup(\a)

// and example of using groups ///
_add; // creates a tape in \default
_buf; // rand buf tapes in \default
_rate(0.5); // changes rate of tapes in \default
_newgroup(\straight);
_usegroup(\straight); //subsequent calls only affect tapes in \straight
_add;
_buf;
_rate(2);
/////////////////////////////////


_kill(2) // kills tape player at pos 2 in current group. if N is too big it kills the last one
_kill // kills a random tape player at current group
_killall // kills everyone at current group
_killthemall // kills everyone in every group

// experimental.
_plot; // draws the play heads of each tape player the same window
_plot(4) // optional pass the buffer to draw on the background of the window. useful when all tape players share the same buffer


//  BUFFERS // *assumes stereo samples*

_loadfiles("some/directory"); // load all files from this directory into current buffer bank. BEWARE: loading files might take time
_loadfiles("another/directory", overwrite:0); // load and append new buffers to previously existing ones
_loadfiles("some/directory/somefile.wav");// load this file only
_normalize // normalise buffers
_bufs // list with available buffers
_curbufs // posts tape, buffer for each tape. eg: _0, blah.wav, etc...
_bufinfo // posts bank, index and name of available buffers
_buf(5) // asign buffer 5 of current bank to all players in current group
_buf([0,2,5]) // randomly choose between those buffers and assign
_buf // randomly chooses and assigns one

// you could load new files and asign them again after tape players are alive
_loadfiles("another/directory");
_asignbufs // force re-asign them

// banks of buffers. organise your samples into different banks
_newbank(\bells);
_usebank(\bells);
_currentbank; // who's active
_mergebanks; // join all into default bank
_delbank(\bells); // free buffers and delete this bank

// for instance:
_newbank(\bells);
_usebank(\bells);
_loadfiles("some/directory/bells/");
// wait
_newbank(\voices);
_usebank(\voices);
_loadfiles("some/directory/voices/");

///////////////////////////////////////////////////////////////////////////////////
// global controls. control ALL tape players *in current group* at the same time //
///////////////////////////////////////////////////////////////////////////////////

_play // loop play all tapes in current group
_stop
_shot // single shot play. experimental
_solo(0) // pause all but 0 player
_reverse // reverse play direction. +rate to -rate
_dir(1) // 1 forwards, -1 backwards. same as _rate(1) or rate(-1)
_fwd // turn current rate positive. same as _rate(1)
_bwd // turn current rate negative. same as _rate(-1)
_volu // UP by 2/100th
_vold // DOWN by 2/100th
_vol(0.5)
_fadein
_fadeout
_pan(0) //-1 to 1
_out(0) // tapes send output to channel 0,1
_rate(0.8)
_wobble(0.27) // rand fluctuation in rate
_brown(0.7) // brownian fluctuation in rate
_vibrato(value:3, depth:0.04, ratev:0.01, depthv:0.02)

_lp(0, 0.01) // set loop start and end. in 0 to 1 range
_st(0.5) // loop start
_end(0.55) // loop end
_dur(0.006) // loop duration in 0-1 range from st
_len(600) // loop length **in milliseconds** useful if they are using different buffers and you want the same loop duration
_move(0.5) // move loop start point to that position and try to maintain the loop duration
_moveby(-0.01) // move loop start and end point by that amount
_go(0.5) // send playhead to point in sample (only works if >st and <end)

_env(vol:1, fadein:0.01, len:0.5, fadeout:0.1); // fadein time to vol, stay len, fadeout time
_trans(-2)// transpose pitch by n semitones

// slicer-like behaviour (http://github.com/enrike/slicer)
//  good idea to use together with _control
_slice(sttime:0.5, shift:2, grain:0.001, grainshift:1)
_slicegui(300,600); // 2x 2D GUI with sliders to control _slice parameters

_scratch // experimental. |target=0, tIn=1, tStay=0.5, tOut=1, offset=0| // boomerang like pitch change

// RESET
_reset // reset loop to 0-1, play from 0, pitch to 1
_lp // resets loop to 0-1
_gost // to to loop start point
_goend // go to loops end point
_go // resets pos to 0 --> *SAME* happens with _pan, _rate, _vol. They reset to default value


// store and retrieve named states. they dont get deleted from memory until the session ends
_push(\dut) // store current state as \dut in memory
_save // save current states from memory into a file in home directory ( Platform.userHomeDir ). it posts the path
_save("caos") // optionally provide a name. if no name is provided it uses the timestamp
_load // open a dialogue to search for a file containg states
_load("/home/xxx/caos.states") // tries to load states from the file
_pop(\dut) // load \dut from current states

_control // opens a GUI window with displays of all tape players waveform. you can click+drag to control de loop selection as well
_control( 300, 200 ) // open with this size

// search. identify which tape players are playing which sound file
p = _search("Roll") //returns a list of tape players whose buffer contains this string in the file name. no case sensitive
p.do({|lay| lay.vol(0.3)}) // do whatever with the search results



// RANDOM //

_rbuf(0) // random buffer. all get the same one. default behaviour
_rbuf(1) // each one gets a different buffer randomly
_rpan
_rrate
_rdir // + or - rate
_rvol
_rloop // 0 to 1
_rloop([0.2, 0.1]) // set start and *LENGTH* limits for the random. in this case start will be rrange(0,0.2) and length start+(0.1.rand)
_rmove; // move current loop to a random position maintaing the length
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
// etc...

// rand select some players to apply commands to only those but not to all
_one.rrate // one gets random rate
_some // returns a list of random players
_some(3) // 3 random players
_some.collect(_.rrate) // some of them get random rate
_some(3).collect(_.rrate) // 3 get random rate


// DEFER
// functions can be deferred in time
_rate(defer:4) // same as {_rate}.defer(4)
_rate(d:4) // shortcut

// OFFSET //
// functions also have offsets so that tape players dont run the function at the same time
_rrate(offset:8) // all players get random rate at a different random times within the next 8 secs
_rate(o:4) // shortcut


// TRANSITION TIME:
// some functions (the ones related to panning, amplitude and pitch) can perform a transition between the current state and the target state in N secs
_rate(0.1, time:10)// go to 0.1 rate in 10secs
_rate(1, t:4) // shortcut
_rate(0.1, time:20, offset:8) // offset can be use here too

//local randoms
_rate(1, random:0.1); // each of them will get a random rate ranging -0.1 to 0.1 around 1
_rate(r:4) // shortcut
//etc...

// ALL OPTIONS TOGETHER
_rate(2, d:5, o:10, t:1, r:0.1)

/*// compressor. yes, there is a built in compressor/expander. inactive by default
_comp(0.2, 0.6, 0.2) //threshold, slopeBelow, slopeAbove
_thr(0.2); // threshold
_slb(0.6); // slope below thr <1 for expansion
_sla(0.2); //slope above thr <1 for compression >1 for expansion*/


// Loop crossing event. SCHEDULE actions at loop crossing points
// beware that if you have 20 tapes there will be 20 xloops, one for each tape
_xloop({ _rrate }) // everytime one single tape crosses the loop point, all off them change their rate
_xloop({|me| me.rrate})  // every individual xloop each changes ITS own rate. Others are not affected
_xloop // this clears the xloop functions for everyone
_one.xloop = {me.id.postln} // individually too
_0.xloop = {me.id.postln} // too

// MIDI
_midion
_midioff
_ccin(\test, 41, {|v|v.postln}) //bind a midi CC message with a function


// SCHEDULING FUNCTIONS: _do //
// _do args are: name, function, sleep, random, defer, iter, when, then, verbose
_do(\randpan, {_rpan}, 2); //loop run a function named \randpan every 15 secs sleep.
_undo(\randpan) // kill \randpan
_undo // kill all scheduled functions
_undo(\randpan, d:10) // it can be deferred too

_do(\num, {|i| i.postln}, 1) // get access to num of loops

// you can overwrite a function by changing it and running it again as long as the name is the same
_do(\blah, {"hi".postln}, 2) // run this first. it posts "hi" every 2 secs
_do(\blah, {"hi ya!".postln}, 1) // now run this. it overwrites the previous \blah

// playing with sleeping times //

// cycle an array of sleep times
_do(\cycle, {"sequence".postln}, [1,2,3])
_do(\cycle, {"sequence".postln}, [0.1, 3, 0.2].mirror) // you can use all the sclang commands related to arrays

// random: introducing swing. irregular sleeps
_do(\randpan, {_rrate}, 15, random:2); // rand swing: the next sch will happen after 15 + rrand(-2,+2) secs

_do(\randpan, {_rrate}, 15, random:[2,4,6]); // rand choose betwen given values

_do(\wrand, { // weight random
	"two options".postln;
}, random:[[1,3],[0.9, 0.1]])// run every every 1 sec (90% chances) or 3 secs (10% chances)

// defer
_do(\spawn, {_rrate}, 3, defer:20); //wait 20 secs, then run every 3secs

// num of iterations
_do(\spawn, {_add(1,_0)}, 3, iter:10); //run every 3secs for 10 times then die

// conditions: when
z = 0;
_do(\check, {"run!!!".postln}, 3, when:{z>0}) // will start running once z>1
z = 1; // run this line to start running _do

// when + then
_do(\check, {"run!!!".postln}, 3, when:{z>0}, then:0) // run once when z>0 then die

// iter + then
_do(\check, {|i| i.postln}, 0.5, iter:5, then:{"goodbye!!!!!".postln})

// postln or not every run
_do(\silent, { // wont print its state every run. useful with quick and repetitive tasks
	_rpan
}, 0.2, verbose:false);

// which group of tapes to apply the commands to. it defaults to \default
_newgroup(\noises);
_usegroup(\noises);
_add;
_do(\rand, {_rrand}, 1); // will only affect to tapes in noises group
// this line above is the same as this code below
_do(\rand, {
	_usegroup(\noises);
	_rrand
}, 1)
// you can always switch groups within the function
_do(\mix, {
	_usegroup; // default
	_rrate;
	_usegroup(\noises);
	_rvol;
}, 10)

// shorcuts work here too. beware that in this context t means THEN and not time
_do(\test, {_rvol}, s:3, r:3, d:1, i:2, w:{z>1}, t:0, v:true, g:\noises)




// some examples of scheduling tasks
_do(\backtoinit, {_reset}, 10) // resets every 10s
_do(\rev, {_reverse(offset:5)}, 10)
_do(\rlen, {_rlen(1, 2)}, 8, random:4)
_do(\bgo, {_bgo(0.01, offset:1)}, 1)

// recursive _do: every 60s trigger 20 times _rdir very quickly
_do(\sch, {
	_do(\rd, {_rdir}, 0.2, iter:20)
}, 60);

_do(\inoutrand, { //every 30 sec: 9 sec fade in- after 15 secs - 9 secs fade out
	_fadein(9);
	_fadeout(9, d:15)
}, 30)
_undo(\inoutrand)

_do(\ramps, {// this will move every 10 secs all the pitchs to a random value ranging -3 to +3 from the current value in a 5 sec transition
	_brate(4, 5)
}, 10, random:3)


( // keeps mirror-playing while slowly moving around the same area
_rdir;
_do(\rev, {
	_bgo(0.01);
	_reverse
}, 1, random:0.2)
)

( // moves forwards in sample
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
	_fadeout(fade, d:(len-gap));
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





////////////////////////////////
// control individual players //
////////////////////////////////

// you can also control each player individually with _0, _1 etc...
// I hardly use this so documentation might not be totally up to date
// some functions might work slightly different from the global ones for technical reasons
// if in doubt check the source code at Tape.sc

// GET
_0.id;
_1.id; // etc... _2, _3, ...

_0.vol
_0.info
_0.file
_0.buf
_0.end
_0.st
_0.curpos // 0-1 playhead position
_0.verbose

// SET
_0.buf( _bufs[1] ) // must pass the buffer like this in this case
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

// x loop events
_0.xloop = {me.id.postln}

// and so on...
