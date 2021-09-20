package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.predicate.Not;
import recipe.lang.expressions.predicate.Or;
import recipe.lang.types.Boolean;

import java.util.*;

public class Choice extends Process{
    public Process a;
    public Process b;

    public Choice(Process a, Process b) {
        this.a = a;
        this.b = b;
    }

    public Expression<Boolean> entryCondition(){
        return new Or(a.entryCondition(), b.entryCondition());
    }

    public void addEntryCondition(Expression<Boolean> condition){
        a.addEntryCondition(condition);
        b.addEntryCondition(condition);
    }

    public Set<Transition> asTransitionSystem(State start, State end){
        Set<Transition> ts = a.asTransitionSystem(start, end);
        ts.addAll(this.b.asTransitionSystem(start, end));

        return ts;
    }

    public static Parser parser(Parser processParser){
        Parser parser =
                processParser.seq((CharacterParser.of('+').trim().seq(processParser).trim()).plus())
                        .map((List<Object> values) -> {
                            Choice choice = null;
                            Process current = (Process) values.get(0);
                            for(int i = 0; i < ((List) values.get(1)).size(); i++){
                                ArrayList val = (ArrayList) ((ArrayList) values.get(1)).get(i);
                                if (Process.class.isAssignableFrom(val.get(1).getClass())) {
                                    choice = new Choice(current, (Process) val.get(1));
                                    current = choice;
                                }
                            }
                            return choice;
                        });

        return parser;
    }
}