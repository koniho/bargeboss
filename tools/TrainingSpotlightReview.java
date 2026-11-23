package com.bargeboss.game;
public final class TrainingSpotlightReview {
 public static void main(String[] args)throws Exception {
  Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
  g.tutorial.lesson=Tutorial.MOVE;g.tutorial.setup(g);g.tutorial.entering=false;g.dialogue.close(g);
  g.tug.x=-1500;g.tug.y=600;g.tug.angle=-(float)Math.PI/2;g.spotlight.aim(g,g.tug.angle);
  g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,2.4f,700,340);g.abilities.reveal(g,g.tug);
  for(int i=0;i<20;i++)g.abilities.step(g);DialogueReview.capture(root,"training-spotlight-downward");
  g.settings.waterDetail=0;DialogueReview.capture(root,"training-spotlight-low-detail");
  SpotlightWaterTest.run();
 }
}
