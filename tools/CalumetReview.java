package com.bargeboss.game;
/** Captures the permanent Calumet shore and machinery with the production renderer. */
public final class CalumetReview {
 public static void main(String[] args)throws Exception {
  Game g=new Game();g.reset();g.configureCanvas(1333,750);
  float[][] centers={{300,350},{550,450},{300,-100},{-450,100}};
  for(int i=0;i<centers.length;i++){g.layout=Layout.focused(1333,750,centers[i][0],centers[i][1],1.1f,666,375);MontroseReview.capture(g,"calumet-shore-v1-"+i);}
 }
}
