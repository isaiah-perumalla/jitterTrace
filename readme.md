
## Check setup

* check freq `cpupower frequency-info`

set required frequecy 
`sudo cpupower frequency-set -d 2.75`

### Isolated cpu

check if cpu is isolated
`dcat /sys/devices/system/cpu/isolate`


### set affinity 
* manually use taskset `taskset -pc 1,2 <tid>`

```C
#define _GNU_SOURCE
#include <sched.h>

cpu_set_t  mask;
CPU_ZERO(&mask);
CPU_SET(0, &mask);
CPU_SET(2, &mask);
int result = sched_setaffinity(0, sizeof(mask), &mask);
```

### Results
#### No Isolation
```declarative
P50:107 ns
P90:108 ns
P99:109 ns
P999:109 ns
P9999:109 ns
Min:73, Max:2077695, Mean:154.926116, stdDev:1003.531762 ns
[39, 79] -> 393
[79, 159] -> 76431504
[159, 319] -> 22986431
[319, 639] -> 57502
[639, 1279] -> 15165
[1279, 2559] -> 2882
[2559, 5119] -> 14181
[5119, 10239] -> 1421
[10239, 20479] -> 2848
[20479, 40959] -> 1535
[40959, 81919] -> 35
[81919, 163839] -> 118
[163839, 327679] -> 50
[327679, 655359] -> 72
[655359, 1310719] -> 80
[1310719, 2621439] -> 5

```


#### Isolated pinned cpus
```
P50:70 ns
P90:70 ns
P99:71 ns
P999:71 ns
P9999:71 ns
Min:60, Max:11796479, Mean:89.775517, stdDev:1727.213675 ns
[39, 79] -> 24073690
[79, 159] -> 75155066
[159, 319] -> 301462
[319, 639] -> 19515
[639, 1279] -> 9264
[1279, 2559] -> 638
[2559, 5119] -> 9183
[5119, 10239] -> 492
[10239, 20479] -> 4293
[20479, 40959] -> 1743
[40959, 81919] -> 3
[81919, 163839] -> 94
[163839, 327679] -> 8
[327679, 655359] -> 84
[655359, 1310719] -> 101
[1310719, 2621439] -> 19
[10485759, 20971519] -> 1
```

jitterTrace master  ? ❯ mv memoryPingPo