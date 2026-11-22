package com.bargeboss.game;
/** Shared-renderer review of the separate rescue basin and winding retrieval route. */
public final class FireBasinReview {public static void main(String[] args)throws Exception{
Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);Game g=root.activeGame();g.tutorial.lesson=Tutorial.FIRE_TOW;g.tutorial.setup(g);
for(int i=0;i<450;i++){ReplayInput.tutorial(g);g.step();}
DialogueReview.capture(g,"fire-basin-retrieval");
g.layout=Layout.focused(1333,600,-25,-1175,.9f,666,300);DialogueReview.capture(g,"fire-basin-overview");
}}
