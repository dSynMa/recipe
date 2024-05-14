package recipe.lang.expressions.location;

import java.util.List;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;

import recipe.lang.agents.AgentInstance;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.types.Boolean;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.utils.TypingContext;

public abstract class Location {
    public static org.petitparser.parser.Parser parser(TypingContext context) throws Exception {
        Parser predLocParser = PredicateLocation.parser(context);
        Parser baseParser =
            CharacterParser.noneOf("()[],;").plus().trim().flatten().map(
                (String word) -> {
                    if (word.equals(SelfLocation.selfToken)) return new SelfLocation();
                    else if (word.equals(AnyLocation.anyToken)) return new AnyLocation();
                    else return new NamedLocation(word);
            });
        
        Parser parser =  predLocParser.or(baseParser).or(
            CharacterParser.of('(')
            .seq(baseParser)
            .seq(CharacterParser.of(')'))
            .map((List<Object> result) -> {return (NamedLocation) result.get(1);})
        );
        return parser;
    }

    public abstract Expression<Boolean> getPredicate(TypedValue supplier);
}
