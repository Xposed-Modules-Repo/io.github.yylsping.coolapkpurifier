# Attach to the running Coolapk main process by PID and dump every DEX found
# in anonymous readable memory. Dedup by (checksum, file_size).
import sys
import frida

PID = int(sys.argv[1])

SCRIPT = r"""
'use strict';
var OUT = '/data/local/tmp/processing/';
var dumped = {};
var count = 0;
var errors = 0;
var ranges = Process.enumerateRanges('r--');
send({tag: 'info', msg: 'ranges=' + ranges.length});

function tryDump(addr, regionEnd) {
  try {
    var fileSize = addr.add(32).readU32();
    if (fileSize < 0x70 || fileSize > 0x6000000) return;
    var magic8 = addr.readByteArray(8);
    var m = new Uint8Array(magic8);
    var isCdex = m[0] === 0x63 && m[1] === 0x64 && m[2] === 0x65 && m[3] === 0x78;
    if (!isCdex) {
      var mapOff = addr.add(52).readU32();
      if (mapOff < 112 || mapOff >= fileSize) return;
    }
    if (addr.add(fileSize).compare(regionEnd) > 0) return;
    var checksum = addr.add(8).readU32();
    var key = checksum + ':' + fileSize;
    if (dumped[key]) return;
    dumped[key] = true;
    var path = OUT + 'ckdex_' + count + '_s' + fileSize + '_c' + checksum.toString(16) + '.dex';
    var f = new File(path, 'wb');
    var off = 0;
    var CH = 1 << 20;
    while (off < fileSize) {
      var n = Math.min(CH, fileSize - off);
      var buf = addr.add(off).readByteArray(n);
      if (!buf) throw new Error('null buf at ' + off);
      f.write(buf);
      off += n;
    }
    f.close();
    count++;
    send({tag: 'dumped', path: path, size: fileSize});
  } catch (e) {
    errors++;
  }
}

var interesting = 0;
for (var i = 0; i < ranges.length; i++) {
  var r = ranges[i];
  var p = r.file ? (r.file.path || '') : '';
  if (p !== ''
      && p.indexOf('/memfd') !== 0
      && p.indexOf('/dev/ashmem') !== 0
      && p.indexOf('/data/') !== 0
      && p.toLowerCase().indexOf('dex') === -1
      && p.toLowerCase().indexOf('.jar') === -1
      && p.toLowerCase().indexOf('.apk') === -1) {
    continue;
  }
  interesting++;
  if (r.size < 0x70) continue;
  var pats = ['64 65 78 0a 30 33 3? 00', '63 64 65 78 30 30 ?? 00'];
  var end = r.base.add(r.size);
  for (var k = 0; k < pats.length; k++) {
    try {
      var results = Memory.scanSync(r.base, r.size, pats[k]);
      for (var j = 0; j < results.length; j++) {
        tryDump(results[j].address, end);
      }
    } catch (e) {
      errors++;
    }
  }
}
send({tag: 'done', dumped: count, errors: errors, interesting: interesting});
"""


import threading

done = threading.Event()


def on_message(message, data):
    if message.get("type") == "send":
        print(message["payload"], flush=True)
        if message["payload"].get("tag") == "done":
            done.set()
    elif message.get("type") == "error":
        print("ERROR:", message.get("description"), flush=True)


device = frida.get_usb_device(10)
session = device.attach(PID)
script = session.create_script(SCRIPT)
script.on("message", on_message)
script.load()
print("script loaded; waiting for scan to finish", flush=True)
done.wait(600)
print("finished", flush=True)
