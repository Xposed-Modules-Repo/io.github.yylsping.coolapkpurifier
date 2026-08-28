import sys
import threading
import frida

PID = int(sys.argv[1])

SCRIPT = r"""
'use strict';
var OUT = '/data/local/tmp/processing/';
var dumped = {};
var count = 0;
var errors = 0;
var scanned = 0;
var skipped = 0;
var ranges = Process.enumerateRanges('r--');
send({tag: 'info', msg: 'ranges=' + ranges.length});

function tryDump(addr, regionEnd, ext) {
  try {
    var fileSize = addr.add(32).readU32();
    if (fileSize < 0x70 || fileSize > 0x8000000) return;
    var magic8 = new Uint8Array(addr.readByteArray(8));
    var isCdex = magic8[0] === 0x63 && magic8[1] === 0x64 && magic8[2] === 0x65 && magic8[3] === 0x78;
    if (!isCdex) {
      var mapOff = addr.add(52).readU32();
      if (mapOff < 112 || mapOff >= fileSize) return;
    }
    if (addr.add(fileSize).compare(regionEnd) > 0) return;
    var checksum = addr.add(8).readU32();
    var key = checksum + ':' + fileSize;
    if (dumped[key]) return;
    dumped[key] = true;
    var path = OUT + 'ck_' + count + '_s' + fileSize + '_c' + checksum.toString(16) + '.' + ext;
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

function scanAll() {
  for (var i = 0; i < ranges.length; i++) {
  var r = ranges[i];
  var p = r.file ? (r.file.path || '') : '';
  if (p !== '') {
    var lp = p.toLowerCase();
    var ok = p.indexOf('/memfd') === 0
        || p.indexOf('/dev/ashmem') === 0;
    if (!ok) { skipped++; continue; }
  } else {
    var size = r.size;
    if (size > 0x10000000) { skipped++; continue; }  // >256MB single region, skip
  }
  if (r.size < 0x70) continue;
  scanned++;
  var end = r.base.add(r.size);
  var pats = ['64 65 78 0a 30 33 3? 00', '63 64 65 78 30 30 ?? 00'];
  for (var k = 0; k < pats.length; k++) {
    try {
      var results = Memory.scanSync(r.base, r.size, pats[k]);
      for (var j = 0; j < results.length; j++) {
        tryDump(results[j].address, end, k === 0 ? 'dex' : 'cdex');
      }
    } catch (e) {
      errors++;
    }
  }
}
send({tag: 'done', dumped: count, errors: errors, scanned: scanned, skipped: skipped});
}
setTimeout(scanAll, 0);
"""


def main():
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
    print("script loaded; scanning", flush=True)
    done.wait(300)
    print("finished", flush=True)


if __name__ == "__main__":
    main()
