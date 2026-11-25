package com.bargeboss.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Every surveyed level goes through SurveyLevel and the shared pipeline, so new levels
 * inherit shared renderer optimizations (issue #26). */
public final class LevelContractTest {
    /** Files allowed to name individual stages: level rules (voyage, berths, moorings, docks,
     * mission hooks) and the predicates themselves. Shared rendering/geography must not. */
    static final Set<String> STAGE_CHECK_ALLOWLIST=new HashSet<String>(Arrays.asList(
            "Game.java","BargeAbilities.java","CargoTransfer.java","DockView.java","DockWorkers.java",
            "HarborView.java","Mooring.java","MooringView.java","OverviewMap.java"));
    private static final Pattern STAGE_CHECK=Pattern.compile("\\b(calumet|indiana|chicago)Stage\\(\\)");
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
    public static void run(){
        int levels=0,checks=0;Set<Integer> seen=new HashSet<Integer>();
        String scenarios=read(Paths.get("developer/shared/com/bargeboss/game/ProfileScenario.java"));
        for(int id=0;id<Challenges.COUNT;id++){
            Game g=new Game();g.challengeId=id;g.reset();SurveyLevel level=g.level();
            if(level==null)continue;
            check(g.surveyStage(),"surveyStage follows level()");
            check(level.shoreline().length>0 && level.chart()!=null && level.entrances()!=null,level.key()+" prepares shoreline, chart and entrances");
            check(level.inBounds(g.tug.x,g.tug.y) && level.contains(g.tug.x,g.tug.y),level.key()+" starts its tug in its own water");
            g.layout=Layout.focused(1280,720,g.tug.x,g.tug.y,1,640,360);
            int before=level.ground().cachedCells();
            Renderer.draw(g,new Parity());
            check(level.ground().cachedCells()>before || before>0,level.key()+" draws ground through the shared survey ground cache");
            float[] box=new float[4];
            for(int i=0;i<level.landmarkCount(g);i++){
                check(level.landmarkBox(g,i,box),level.key()+" landmark "+i+" has a screen box");
                check(box[0]<=box[2] && box[1]<=box[3] && !Float.isNaN(box[0]+box[1]+box[2]+box[3]),level.key()+" landmark "+i+" box is ordered and finite");
                checks++;
            }
            if(seen.add(level.id)){levels++;
                check(scenarios.contains("\""+level.key()+"-"),level.key()+" has a ProfileScenario fixture named \""+level.key()+"-...\"");}
            checks+=5;
        }
        check(levels==SurveyLevel.COUNT,"every SurveyLevel is reachable from a challenge");
        // Source guard: no new shared file may branch on a stage name.
        try(DirectoryStream<Path> files=Files.newDirectoryStream(Paths.get("core/com/bargeboss/game"),"*.java")){
            for(Path f:files){String name=f.getFileName().toString();
                if(STAGE_CHECK.matcher(read(f)).find())check(STAGE_CHECK_ALLOWLIST.contains(name),
                        name+" branches on a stage name; dispatch through Game.level() / SurveyLevel instead");
                checks++;}
        }catch(IOException e){throw new AssertionError(e);}
        System.out.println(checks+" survey level contract assertions passed across "+levels+" levels");
    }
    private static String read(Path p){try{return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);}catch(IOException e){throw new AssertionError(e);}}
    public static void main(String[] args){run();}
}
