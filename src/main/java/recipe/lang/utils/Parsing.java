package recipe.lang.utils;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.arithmetic.NumberVariable;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.channels.ChannelValue;
import recipe.lang.expressions.channels.ChannelVariable;
import recipe.lang.expressions.predicate.BooleanVariable;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.expressions.strings.StringExpression;
import recipe.lang.expressions.strings.StringVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.word;

public class Parsing {
    public static org.petitparser.parser.Parser disjunctiveWordParser(Set<String> allowed, Function<String, Expression> transformation) {
        return disjunctiveWordParser(new ArrayList<>(allowed), transformation);
    }

    public static org.petitparser.parser.Parser disjunctiveWordParser(List<String> allowed, Function<String, Expression> transformation) {
        if (allowed.size() == 0) {
            return StringParser.of("").not();
        }

        org.petitparser.parser.Parser parser = StringParser.of(allowed.get(0)).map(transformation);
        for (int i = 1; i < allowed.size(); i++) {
            parser = parser.or(StringParser.of(allowed.get(i)).map(transformation));
        }

        return parser;
    }

    public static org.petitparser.parser.Parser expressionParser(TypingContext context) {
        return Condition.parser(context)
                .or(ArithmeticExpression.parser(context))
                .or(StringExpression.parser(context))
                .or(ChannelExpression.parser(context));
    }

    public static org.petitparser.parser.Parser variableParser(TypingContext context) {
        return BooleanVariable.parser(context)
                .or(NumberVariable.parser(context))
                .or(StringVariable.parser(context))
                .or(ChannelVariable.parser(context));
    }

    public static org.petitparser.parser.Parser assignmentParser(TypingContext variableContext,
                                                                 TypingContext expressionContext) {
        Parser assignment =
                variableParser(variableContext)
                        .seq(StringParser.of(":=").trim())
                        .seq(expressionParser(expressionContext))
                .map((List<Object> values) -> {
                    return new Pair(values.get(0).toString(), values.get(2));
                });

        return assignment;
    }

    public static org.petitparser.parser.Parser assignmentListParser(TypingContext variableContext,
                                                                 TypingContext expressionContext) {
        Parser assignment =
                assignmentParser(variableContext, expressionContext)
                        .delimitedBy(CharacterParser.of(',').trim())
                .map((List<Pair> values) -> {
                    HashMap<String, Expression> map = new HashMap();
                    for(Pair<String, Expression> pair : values){
                        map.put(pair.getLeft(), pair.getRight());
                    }

                    return map;
                });

        return assignment;
    }
}