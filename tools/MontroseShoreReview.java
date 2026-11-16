package com.bargeboss.game;
/** Actual shared renderer captures for the connected docks and terraced lakefront. */
public final class MontroseShoreReview {
    public static void main(String[] args)throws Exception{
        Game g=new Game();g.challengeId=Challenges.MONTROSE;g.reset();g.configureCanvas(1333,750);
        for(char letter:MontroseShoreArt.NAMED_DOCKS.toCharArray()){float[] a=MontroseShoreArt.dockLabelAnchor(letter);System.out.println(letter+" map="+MontroseHarbor.planX(a[0])+","+MontroseHarbor.planY(a[1]));}
        for(char letter:new char[]{'A','B','M'}){float[] a=MontroseShoreArt.ACCESS[MontroseShoreArt.gateForLetter(letter)];g.layout=Layout.focused(1333,750,a[0],a[1],5f,666,410);MontroseReview.capture(g,"montrose-letter-"+letter+"-v6");}
        float[][] centers={{1050,1220},{1000,190},{600,350}};
        String[] names={"montrose-terraces-v3","montrose-gates-v3","montrose-isolated-stars-v7"};
        for(int i=0;i<centers.length;i++){
            g.layout=Layout.focused(1333,750,MontroseHarbor.mapX(centers[i][0]),MontroseHarbor.mapY(centers[i][1]),i==2?1.3f:2.2f,666,370);
            MontroseReview.capture(g,names[i]);
        }
    }
}
