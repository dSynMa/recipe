package recipe.analysis;

import java.util.List;
import org.junit.Test;
import static recipe.analysis.TranslationTestModels.*;

public class TranslationFiniteObservationMatrixTest {
    private String numeric(String selection,String observation) throws Exception {
        return source("observation-forall-constant")
            .replace("ready: boolean","rank: 0..2").replace("ready <- TRUE","rank <- 1")
            .replace("ready <- !received","rank <- 1").replace("(@ready)","("+selection+")")
            .replace("c!","*!").replace("c?","*?").replace("forall(TRUE)",observation);
    }
    @Test(timeout=60000)
    public void finiteNumericAtomsMatchExplicitScopeAndIndependentTruthTable() throws Exception {
        String[] selections={"@rank == 1","@rank != 1","TRUE","FALSE"};
        String[] atoms={"rank == 1","rank != 1","rank < 2","!(rank < 2)","rank + 1 == 2"};
        String[] explicit={"forall(rank == 1)","exists(rank != 1)","forall(rank < 2)","exists(!(rank < 2))","forall(rank + 1 == 2)"};
        boolean[][] expected={{true,false,true,false,true},{false,true,false,true,false},{false,true,false,true,false},{true,false,true,false,true}};
        for(int selection=0;selection<selections.length;selection++) for(int atom=0;atom<atoms.length;atom++) {
            for(String observation:List.of(atoms[atom],explicit[atom])) {
                assertCtl(ToNuXmv.transform(parse(numeric(selections[selection],observation))),"EX obs0",expected[selection][atom]);
            }
        }
    }
    @Test(timeout=60000)
    public void finiteEnumEqualityAndItsDualMatchExplicitQuantifiers() throws Exception {
        String[] selections={"@role == c","@role != c","TRUE","FALSE"};
        boolean[][] expected={{true,false},{false,true},{false,true},{true,false}};
        for(int selection=0;selection<selections.length;selection++) for(int inequality=0;inequality<2;inequality++) {
            String atom=inequality==0?"role == c":"role != c";
            String explicit=(inequality==0?"forall(":"exists(")+atom+")";
            for(String observation:List.of(atom,explicit)) {
                String text=source("observation-forall-constant").replace("channels: c","channels: c,d")
                    .replace("ready: boolean","role: channel").replace("ready <- TRUE","role <- c")
                    .replace("ready <- !received","role <- d").replace("(@ready)","("+selections[selection]+")")
                    .replace("c!","*!").replace("c?","*?").replace("forall(TRUE)",observation);
                assertCtl(ToNuXmv.transform(parse(text)),"EX obs0",expected[selection][inequality]);
            }
        }
    }
}
