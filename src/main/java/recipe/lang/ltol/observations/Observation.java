package recipe.lang.ltol.observations;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.Config;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.predicate.*;
import recipe.lang.process.Iterative;
import recipe.lang.process.Process;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.utils.TypingContext;

import java.util.List;

import static org.petitparser.parser.primitive.CharacterParser.of;

public class Observation {
    //TODO we need to be able to talk about the agent who is sending the observation
    public static org.petitparser.parser.Parser parser(TypingContext commonVars, TypingContext messageVars) throws Exception {
        TypingContext cvAndMsg = new TypingContext();
        cvAndMsg.setAll(commonVars);
        cvAndMsg.setAll(messageVars);
        cvAndMsg.set(Config.channelLabel, Enum.getEnum(Config.channelLabel));

        Parser primitive = Condition.parser(cvAndMsg).map(BasicObservation::new);

        ExpressionBuilder builder = new ExpressionBuilder();
        builder.group()
                .primitive(primitive)
                .wrapper(of('(').trim(), of(')').trim(),
                        (List<Observation> values) -> {
                    return values.get(1);
                        });

        // exists is a prefix operator
        builder.group()
                .prefix(StringParser.of("exists").trim(), (List<BasicObservation> values) -> new ObsExists(values.get(1)));

        // forall is a prefix operator
        builder.group()
                .prefix(StringParser.of("forall").trim(), (List<BasicObservation> values) -> new ObsForAll(values.get(1)));

        return builder.build();
    }
}
