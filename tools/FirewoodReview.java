package com.bargeboss.game;
/** Fixed camera comparison of intact, burning and extinguished timber plus tow context. */
public final class FirewoodReview {
    public static void main(String[] args)throws Exception{
        Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);
        Game g=root.activeGame();g.tutorial.lesson=Tutorial.SHOW;g.tutorial.setup(g);
        for(int i=0;i<100;i++)g.step();
        g.layout=Layout.focused(1333,600,FirewoodPile.X,FirewoodPile.Y,2.2f,650,420);
        DialogueReview.capture(g,"firewood-intact");
        g.tutorial.fireworks.ignited=true;g.tutorial.fireworks.heat=FireworksLesson.HEAT;
        DialogueReview.capture(g,"firewood-burning");
        g.tutorial.fireworks.heat=0;g.tutorial.fireworks.done=true;g.tutorial.fireworks.steam=0;
        DialogueReview.capture(g,"firewood-charred");
        g.tutorial.lesson=Tutorial.LINK;g.tutorial.setup(g);
        for(int i=0;i<120;i++)g.step();DialogueReview.capture(g,"tow-narrow-passage-context");
    }
}
