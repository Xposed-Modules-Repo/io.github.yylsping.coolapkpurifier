#!/system/bin/sh
n1=$(dd if=/proc/19925/mem bs=4096 skip=127829811 count=2208 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=127829811 count=2208 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=127829811 count=2208 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 79e8733000 size 9043968 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=127836389 count=2423 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=127836389 count=2423 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=127836389 count=2423 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 79ea0e5000 size 9924608 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=127884922 count=2722 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=127884922 count=2722 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=127884922 count=2722 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 79f5e7a000 size 11149312 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=127897904 count=3060 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=127897904 count=3060 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=127897904 count=3060 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 79f9130000 size 12533760 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=127925370 count=2690 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=127925370 count=2690 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=127925370 count=2690 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 79ffc7a000 size 11018240 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=128011566 count=2730 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=128011566 count=2730 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=128011566 count=2730 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7a14d2e000 size 11182080 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=128125892 count=1064 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=128125892 count=1064 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=128125892 count=1064 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7a30bc4000 size 4358144 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=128136520 count=2783 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=128136520 count=2783 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=128136520 count=2783 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7a33548000 size 11399168 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=128319403 count=3127 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=128319403 count=3127 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=128319403 count=3127 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7a5ffab000 size 12808192 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=128366824 count=1848 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=128366824 count=1848 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=128366824 count=1848 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7a6b8e8000 size 7569408 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=128592970 count=2336 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=128592970 count=2336 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=128592970 count=2336 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7aa2c4a000 size 9568256 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=128686818 count=748 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=128686818 count=748 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=128686818 count=748 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7ab9ae2000 size 3063808 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
n1=$(dd if=/proc/19925/mem bs=4096 skip=130466112 count=129 2>/dev/null | grep -a -c "x-app-device" || true)
n2=$(dd if=/proc/19925/mem bs=4096 skip=130466112 count=129 2>/dev/null | grep -a -c "JNIFactory" || true)
n3=$(dd if=/proc/19925/mem bs=4096 skip=130466112 count=129 2>/dev/null | grep -a -c "aebd1811194e82d9" || true)
echo "region 7c6c140000 size 528384 x-app-device=$n1 JNIFactory=$n2 aebd=$n3"
