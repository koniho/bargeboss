#!/usr/bin/env python3
"""Approximate exclusive/inclusive thread CPU attribution from ART dual-clock sampled traces.

Use for hotspot ranking only. Sampled transitions and missing initial stacks are
not exact invocation timings. Trace-enabled frame timings include profiler cost.
"""
import collections, struct, sys
from pathlib import Path
raw=Path(sys.argv[1]).read_bytes();split=raw.index(b'*end\n')+5
header=raw[:split].decode();methods={};threads={};section=''
for line in header.splitlines():
    if line.startswith('*'):section=line
    elif section=='*methods':
        fields=line.split('\t');methods[int(fields[0],16)]=fields[1]+'.'+fields[2]
    elif section=='*threads':
        fields=line.split('\t');threads[int(fields[0])]=fields[1]
magic,version,offset=struct.unpack_from('<4sHH',raw,split)
if magic!=b'SLOW' or version!=3 or 'clock=dual' not in header:raise SystemExit('Expected ART v3 dual-clock trace')
size=struct.unpack_from('<H',raw,split+16)[0]
stacks=collections.defaultdict(list);last={};self_us=collections.Counter();inclusive=collections.Counter();total=0;mismatch=0
for pos in range(split+offset,len(raw)-size+1,size):
    tid,packed,cpu,wall=struct.unpack_from('<HIII',raw,pos)
    if threads.get(tid)!='main':continue
    stack=stacks[tid];elapsed=cpu-last.get(tid,cpu);last[tid]=cpu
    if elapsed<0:raise SystemExit('Nonmonotonic CPU timestamps: trace transfer may be corrupt')
    if stack and elapsed>=0:
        self_us[stack[-1]]+=elapsed;total+=elapsed
        for method in set(stack):inclusive[method]+=elapsed
    method,action=packed&~3,packed&3
    if action==0:stack.append(method)
    elif stack and stack[-1]==method:stack.pop()
    else:
        mismatch+=1
        if method in stack:del stack[stack.index(method):]
print('Main-thread sampled CPU %.1f ms; unmatched transitions %d; overflow=%s' % (total/1000,mismatch,'data-file-overflow=true' in header))
for label,counts in [('EXCLUSIVE',self_us),('INCLUSIVE GAME',inclusive)]:
    print(label)
    displayed=0
    for method,value in counts.most_common():
        name=methods.get(method,hex(method))
        if label=='INCLUSIVE GAME' and not name.startswith('com.bargeboss.'):continue
        print('%6.2f%% %9.1f ms %s' % (100*value/max(1,total),value/1000,name));displayed+=1
        if displayed==25:break
