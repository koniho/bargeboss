package com.bargeboss.game;

import java.util.ArrayList;
import java.util.List;

/** Offline material-authoring geometry; never used by simulation or native hosts. */
public final class WatercraftMesh {
    public static final class Face {
        public final double[][] xyz;
        public final int color;
        Face(double[][] p,int c){xyz=new double[p.length][];for(int i=0;i<p.length;i++)xyz[i]=p[i].clone();color=c;}
    }
    public final List<Face> faces=new ArrayList<Face>();
    public void face(double[][] vertices,int color){faces.add(new Face(vertices,color));}
}
