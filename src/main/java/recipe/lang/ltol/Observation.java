package recipe.lang.ltol;

import org.petitparser.parser.Parser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.Config;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.predicate.*;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.RelabellingTypeException;

import java.util.List;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.of;

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

    public static org.petitparser.parser.Parser parser(TypingContext commonVars, TypingContext messageVars, TypingContext agentNames) throws Exception {
        TypingContext cvAndMsg = new TypingContext();
        cvAndMsg.setAll(commonVars);
        cvAndMsg.setAll(messageVars);
        cvAndMsg.set(Config.channelLabel, Enum.getEnum(Config.channelLabel));
        cvAndMsg.set("sender", Config.getAgentType());
        cvAndMsg.addPredicate("exists");
        cvAndMsg.addPredicate("forall");

        Parser primitive = Condition.parser(cvAndMsg).map(Observation::new);

        ExpressionBuilder builder = new ExpressionBuilder();
        builder.group()
                .primitive(primitive)
                .wrapper(of('(').trim(), of(')').trim(),
                        (List<Observation> values) -> {
                    return values.get(1);
                        });

        return builder.build();
    }

    public Observation rename(Function<TypedVariable, TypedVariable> relabelling) throws RelabellingTypeException, MismatchingTypeException {
        return new Observation(observation.relabel((x) -> (relabelling.apply(x))));
    }
}
