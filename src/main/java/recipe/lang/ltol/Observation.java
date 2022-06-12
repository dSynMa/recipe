package recipe.lang.ltol;

import org.petitparser.parser.Parser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.Config;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.*;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.utils.TypingContext;

import java.util.List;

import static org.petitparser.parser.primitive.CharacterParser.of;

public class Observation {
    public Expression<Boolean> getObservation() {
        return observation;
    }

    Expression<recipe.lang.types.Boolean> observation;

    public Observation(Expression<Boolean> observation) {
        this.observation = observation;
    }

    public static org.petitparser.parser.Parser parser(TypingContext commonVars, TypingContext messageVars, TypingContext agentNames) throws Exception {
        TypingContext cvAndMsg = new TypingContext();
        cvAndMsg.setAll(commonVars);
        cvAndMsg.setAll(messageVars);
        cvAndMsg.set(Config.channelLabel, Enum.getEnum(Config.channelLabel));
        cvAndMsg.set("sender", Enum.getEnum(Config.agentEnumType));
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

//        // exists is a prefix operator
//        builder.group()
//                .prefix(StringParser.of("exists").trim(), (List<BasicObservation> values) -> new ObsExists(values.get(1)));
//
//        // forall is a prefix operator
//        builder.group()
//                .prefix(StringParser.of("forall").trim(), (List<BasicObservation> values) -> new ObsForAll(values.get(1)));

        return builder.build();
    }
}
