package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.security.MessageDigest;
import java.util.HashSet;
import javax.imageio.ImageIO;

/** Geometric source exports retain matching alpha, fixed pivots and 64 unique rotations. */
public final class BargeEquipmentAssetCheck {
    public static void main(String[] args)throws Exception{
        for(int atlas:new int[]{SpriteAtlas.FIREWORKS,SpriteAtlas.FIRE_BARGE}){
            String name=atlas==SpriteAtlas.FIREWORKS?"fireworks":"fire";
            BufferedImage base=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(atlas)));
            BufferedImage normal=ImageIO.read(new File("docs/art-studies/barge-equipment-v1/"+name+"-normal-height.png"));
            BufferedImage material=ImageIO.read(new File("docs/art-studies/barge-equipment-v1/"+name+"-material.png"));
            if(base.getWidth()!=2048 || base.getHeight()!=2048 || normal.getWidth()!=2048 || normal.getHeight()!=2048 || material.getWidth()!=2048 || material.getHeight()!=2048)throw new AssertionError("Registered 8x8 equipment sheets required");
            HashSet<String> poses=new HashSet<String>();
            for(int frame=0;frame<64;frame++){
                MessageDigest hash=MessageDigest.getInstance("SHA-256");int pixels=0;
                for(int y=0;y<256;y++)for(int x=0;x<256;x++){
                    int xx=frame%8*256+x,yy=frame/8*256+y,p=base.getRGB(xx,yy),a=p>>>24;
                    if(a!=(normal.getRGB(xx,yy)>>>24) || a!=(material.getRGB(xx,yy)>>>24))throw new AssertionError("Material coverage mismatch");
                    if((x==0 || x==255 || y==0 || y==255) && a!=0)throw new AssertionError("Missing transparent gutter");
                    if(a>0)pixels++;for(int shift=0;shift<32;shift+=8)hash.update((byte)(p>>>shift));
                }
                if(pixels<100)throw new AssertionError("Empty equipment pose");
                poses.add(java.util.Arrays.toString(hash.digest()));
            }
            if(poses.size()!=64)throw new AssertionError("Duplicated equipment headings");
        }
        System.out.println("128 distinct rigid equipment poses: matching material coverage, fixed registration, transparent gutters");
    }
}
