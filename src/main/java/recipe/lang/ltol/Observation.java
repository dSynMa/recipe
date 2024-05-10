package recipe.lang.ltol;

import org.petitparser.parser.Parser;
import recipe.Config;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.*;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.Map;
import java.util.function.Function;

public class Observation {
    public Expression<Boolean> getObservation() {
        return observation;
    }

    Expression<recipe.lang.types.Boolean> observation;

    @Override
    public String toString(){
        return observation.toString();
    }

    public Observation(Expression<Boolean> observation) {
        this.observation = observation;
    }

    public static org.petitparser.parser.Parser parser(TypingContext commonVars, TypingContext messageVars, TypingContext agentParameters) throws Exception {
        TypingContext cvAndMsg = new TypingContext();
        
        // For p2p observations about the supplier's cvs
        for(Map.Entry<String, Type> en : commonVars.getVarType().entrySet()) {
            cvAndMsg.set("supplier-" + en.getKey(), en.getValue());
        }
        
        cvAndMsg.setAll(commonVars);
        cvAndMsg.setAll(messageVars);
        cvAndMsg.set(Config.channelLabel, Enum.getEnum(Config.channelLabel));
        cvAndMsg.set("supplier", Config.getAgentType());
        cvAndMsg.set("getter", Config.getAgentType());
        cvAndMsg.set("initiator", Config.getAgentType());
        cvAndMsg.set("producer", Config.getAgentType());
        cvAndMsg.set("sender", Config.getAgentType());
        cvAndMsg.set("p2p", Boolean.getType());
        cvAndMsg.setAll(agentParameters);
        cvAndMsg.addPredicate("exists");
        cvAndMsg.addPredicate("forall");

        Parser primitive = Condition.parser(cvAndMsg).map(Observation::new);

        return primitive;
    }

    public Observation rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Observation(observation.relabel((x) -> (relabelling.apply(x))));
    }
}
