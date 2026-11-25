package com.bargeboss.game;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.*;

/** Bounded PCM streaming on one daemon; no soundtrack-sized allocation or work on the UI thread. */
final class MusicStream {
    private volatile float gain;
    private volatile boolean closed;
    private volatile SourceDataLine line;
    private final Thread worker;
    MusicStream(){worker=MusicAudio.available()?new Thread(this::play,"BargeBoss music"):null;if(worker!=null){worker.setDaemon(true);worker.start();}}
    synchronized void gain(float value){gain=value;notifyAll();}
    private AudioInputStream open()throws Exception{
        InputStream input=MusicStream.class.getResourceAsStream("/assets/"+MusicAudio.asset());
        if(input==null)input=new java.io.FileInputStream("assets/"+MusicAudio.asset());
        BufferedInputStream buffered=new BufferedInputStream(input);
        try{return AudioSystem.getAudioInputStream(buffered);}catch(Exception e){buffered.close();throw e;}
    }
    private void play(){
        byte[] bytes=new byte[8192];float previous=0;
        try{
            while(!closed){
                try(AudioInputStream input=open()){
                    AudioFormat format=input.getFormat();
                    if(format.getSampleSizeInBits()!=16 || format.isBigEndian() || !format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED))throw new IllegalArgumentException("Music must be little-endian 16-bit PCM");
                    if(line==null){
                        SourceDataLine next=AudioSystem.getSourceDataLine(format);next.open(format,16384);
                        synchronized(this){if(closed){next.close();return;}line=next;}
                    }
                    while(!closed){
                        synchronized(this){
                            if(gain<=0){line.stop();line.flush();previous=0;}
                            while(gain<=0 && !closed)wait();
                        }
                        if(closed)break;
                        int count=input.read(bytes,0,bytes.length);if(count<0)break;
                        float target=gain;int samples=count/2;
                        for(int i=0;i<samples;i++){
                            int value=(short)((bytes[i*2]&255)|(bytes[i*2+1]<<8));
                            float scale=previous+(target-previous)*(i+1)/samples;
                            int scaled=Math.round(value*scale);bytes[i*2]=(byte)scaled;bytes[i*2+1]=(byte)(scaled>>8);
                        }
                        previous=target;line.start();
                        for(int at=0;at<count && !closed;)at+=line.write(bytes,at,count-at);
                    }
                }
            }
        }catch(Exception e){if(!closed)System.err.println("Music unavailable: "+e.getMessage());}
        finally{if(line!=null)line.close();}
    }
    void close(){synchronized(this){closed=true;notifyAll();}if(worker!=null)worker.interrupt();if(line!=null)line.close();}
}
