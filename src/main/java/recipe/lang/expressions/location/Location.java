package recipe.lang.expressions.location;

import java.util.List;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.types.Boolean;
import recipe.lang.utils.TypingContext;

public abstract class Location {
    public static org.petitparser.parser.Parser parser(TypingContext context) throws Exception {
        Parser predLocParser = PredicateLocation.parser(context);
        
        Parser baseParser = SelfLocation.parser()
            .or(AnyLocation.parser())
            .or(NamedLocation.parser(context));
        
        Parser parser =  predLocParser.or(baseParser).or(
            CharacterParser.of('(')
            .seq(baseParser)
            .seq(CharacterParser.of(')'))
            .map((List<Object> result) -> {return (Location) result.get(1);})
        );
        return parser;
    }

    public abstract Expression<Boolean> getPredicate(TypedValue supplier);
}
