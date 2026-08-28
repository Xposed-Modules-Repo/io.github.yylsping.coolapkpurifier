import sys
import threading
import frida

SCRIPT = r"""
'use strict';
var OUT = '/data/local/tmp/processing/';
var count = 0;

function sendm(o) { send(o); }

function dumpDirect(name, bb) {
  try {
    var remaining = bb.remaining();
    if (remaining < 0x70) return;
    // direct buffer: read native memory directly
    var DB = Java.use('java.nio.DirectByteBuffer');
    var direct = Java.cast(bb, DB);
    var addr = direct.address();
    if (addr === undefined || addr === null) return;
    var p = ptr(addr).add(bb.position());
    var path = OUT + 'sp_' + count + '_s' + remaining + '.dex';
    var f = new File(path, 'wb');
    var off = 0;
    var CH = 1 << 20;
    while (off < remaining) {
      var n = Math.min(CH, remaining - off);
      f.write(p.add(off).readByteArray(n));
      off += n;
    }
    f.close();
    count++;
    sendm({tag: 'dumped', path: path, size: remaining, src: name});
  } catch (e) {
    sendm({tag: 'err', src: name, e: '' + e});
  }
}

function dumpHeapBuffer(name, bb) {
  try {
    var remaining = bb.remaining();
    if (remaining < 0x70) return;
    var arr = bb.array();  // may throw for direct buffers
    var path = OUT + 'sp_' + count + '_h_s' + remaining + '.dex';
    var FOS = Java.use('java.io.FileOutputStream');
    var fos = FOS.$new(path);
    var off = bb.position();
    var CH = 1 << 20;
    while (off < bb.limit()) {
      var n = Math.min(CH, bb.limit() - off);
      // copy chunk into fresh byte[] to avoid offset issues
      var chunk = Java.array('byte', new Array(n).fill(0));
      // fall back to bulk get through duplicate
      var dup = bb.duplicate();
      dup.position(off);
      dup.limit(off + n);
      dup.get(chunk);
      fos.write(chunk);
      off += n;
    }
    fos.close();
    count++;
    sendm({tag: 'dumped', path: path, size: remaining, src: name + ':heap'});
  } catch (e) {
    sendm({tag: 'err', src: name + ':heap', e: '' + e});
  }
}

function tryBuffer(name, bb) {
  if (bb === null || bb === undefined) return;
  try {
    if (bb.isDirect()) {
      dumpDirect(name, bb);
    } else {
      dumpHeapBuffer(name, bb);
    }
  } catch (e) {
    sendm({tag: 'err', src: name, e: '' + e});
  }
}

function installHooks() {
  try {
    var IMDC = Java.use('dalvik.system.InMemoryDexClassLoader');
    IMDC.$init.overloads.forEach(function (ov) {
      ov.implementation = function () {
        try {
          for (var i = 0; i < arguments.length; i++) {
            var a = arguments[i];
            if (a && a.$className === '[Ljava.nio.ByteBuffer;') {
              var len = a.length;
              for (var k = 0; k < len; k++) {
                tryBuffer('imdc[' + k + ']', a[k]);
              }
            }
          }
        } catch (e) { sendm({tag: 'err', src: 'imdc', e: '' + e}); }
        return ov.apply(this, arguments);
      };
    });
    sendm({tag: 'info', msg: 'InMemoryDexClassLoader hooked'});
  } catch (e) {
    sendm({tag: 'err', src: 'imdc-hook', e: '' + e});
  }

  try {
    var DexFile = Java.use('dalvik.system.DexFile');
    DexFile.$init.overloads.forEach(function (ov) {
      ov.implementation = function () {
        try {
          for (var i = 0; i < arguments.length; i++) {
            var a = arguments[i];
            var cn = a && a.$className ? a.$className : '';
            if (cn === '[B') {
              var path = OUT + 'sp_' + count + '_bytes.dex';
              var FOS = Java.use('java.io.FileOutputStream');
              var fos = FOS.$new(path);
              fos.write(a);
              fos.close();
              count++;
              sendm({tag: 'dumped', path: path, size: a.length, src: 'dexfile-bytes'});
            } else if (cn === 'java.nio.ByteBuffer') {
              tryBuffer('dexfile-bb', a);
            }
          }
        } catch (e) { sendm({tag: 'err', src: 'dexfile', e: '' + e}); }
        return ov.apply(this, arguments);
      };
    });
    sendm({tag: 'info', msg: 'DexFile hooked'});
  } catch (e) {
    sendm({tag: 'err', src: 'dexfile-hook', e: '' + e});
  }
}

function waitJava() {
  try {
    if (typeof Java !== 'undefined' && Java.available) {
      Java.perform(installHooks);
      return;
    }
  } catch (e) {
  }
  setTimeout(waitJava, 50);
}
setTimeout(waitJava, 50);
"""


def main():
    device = frida.get_usb_device(10)
    pid = device.spawn(["com.coolapk.market"])
    print("spawned pid", pid, flush=True)
    session = device.attach(pid)

    def on_message(message, data):
        if message.get("type") == "send":
            print(message["payload"], flush=True)
        elif message.get("type") == "error":
            print("ERROR:", message.get("description"), flush=True)

    script = session.create_script(SCRIPT)
    script.on("message", on_message)
    script.load()
    print("hooks installed, resuming", flush=True)
    device.resume(pid)
    import time

    for _ in range(60):
        time.sleep(1)
    print("done waiting", flush=True)


if __name__ == "__main__":
    main()
