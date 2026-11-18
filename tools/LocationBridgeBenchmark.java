package com.bargeboss.game;
/** JVM preparation evidence only; not a native frame-rate benchmark. */
public final class LocationBridgeBenchmark {
 public static void main(String[] args){
  Runtime rt=Runtime.getRuntime();System.gc();long before=rt.totalMemory()-rt.freeMemory(),start=System.nanoTime();
  for(LandmarkBridge[] group:new LandmarkBridge[][]{LocationBridges.calumet(),LocationBridges.indiana()}){
   LocationBridges.prepare(group);int faces=0,fragments=0;
   for(LandmarkBridge b:group){faces+=b.fixed.sourceFaceCount()+b.moving.sourceFaceCount();fragments+=b.motionFragments();}
   System.out.println(group[0].id+": "+group.length+" bridges, "+fragments+" motion fragments, "+faces+" source faces");
  }
  long elapsed=System.nanoTime()-start;System.gc();long after=rt.totalMemory()-rt.freeMemory();
  System.out.println("Cold preparation "+elapsed/1000000+" ms; approximate retained JVM heap delta "+(after-before)/1024+" KiB. Includes water geometry/class initialization; excludes native rendering/texture memory.");
 }
}
