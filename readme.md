Tapes by www.ixi-audio.net

[tape](https://github.com/enrike/tapes/blob/master/tape.png?raw=true)

Supercollider based sample loop system. I use it to control many layers of tape-loop-like machines at the same time

check the documentation.scd file for usage example.

for instance, to create and play 4 tapes:

Tapes(this)

_loadfiles(/path/to/sound/files/)

_add(4)

_play


To install just run:

Quarks.install("https://github.com/enrike/tapes.git");

or download https://github.com/enrike/tapes/archive/refs/heads/master.zip and uncompress into the Supercollider's Extensions folder.


License : GNU GPL

This application is free software; you can redistribute it and/or modify it under the terms of the GNU, General Public License as published by the Free Software Foundation.

This aplication is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

