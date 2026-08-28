#!/system/bin/sh
mkdir -p /data/local/tmp/processing/d23641
cd /data/local/tmp/processing/d23641
dd if=/proc/23641/mem bs=4096 skip=129099628 count=1829 of=d_7b1e76c000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129101458 count=2361 of=d_7b1ee92000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129103820 count=2383 of=d_7b1f7cc000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129106204 count=2107 of=d_7b2011c000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129108312 count=3051 of=d_7b20958000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129119556 count=4546 of=d_7b23544000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129132133 count=1841 of=d_7b26665000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129133975 count=2462 of=d_7b26d97000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129136566 count=1919 of=d_7b277b6000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=129138486 count=2301 of=d_7b27f36000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=131165071 count=32 of=d_7d16b8f000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=131223758 count=32 of=d_7d250ce000.bin 2>/dev/null || true
dd if=/proc/23641/mem bs=4096 skip=131309665 count=64 of=d_7d3a061000.bin 2>/dev/null || true
echo DONE $(ls | wc -l)
total: 13 bytes: 102105088
