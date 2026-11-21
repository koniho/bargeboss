package com.bargeboss.game;

public final class BargeAbilitiesReview {
    public static void main(String[] args)throws Exception{
        Game g=BargeAbilitiesTest.fixture();for(int i=0;i<80;i++)g.step();
        g.abilities.reveal(g,g.cargo[0]);for(int i=0;i<80;i++)g.step();DialogueReview.capture(g,"barge-ability-controls-v1");
        ReplayInput.tap(g,g.abilities.buttonX(g,0),g.abilities.buttonY(g));for(int i=0;i<90;i++)g.step();DialogueReview.capture(g,"fireworks-barge-v1");
        g.abilities.reveal(g,g.cargo[2]);g.pointerDown(77,g.abilities.buttonX(g,0),g.abilities.buttonY(g));
        Body b=g.cargo[2];g.pointerMove(77,g.layout.camera.screenX(b.x,b.y-140),g.layout.camera.screenY(b.x,b.y-140));for(int i=0;i<80;i++)g.step();DialogueReview.capture(g,"fire-barge-spray-v1");
    }
}
