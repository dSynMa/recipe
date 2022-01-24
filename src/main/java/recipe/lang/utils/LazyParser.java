package recipe.lang.utils;

import org.petitparser.context.Context;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.FailureParser;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class LazyParser<T> extends Parser {
    Function<T, Parser> parser;
    T input;

    public LazyParser(Function<T, Parser> parser, T input){
        this.parser = parser;
        this.input = input;
    }

    @Override
    public Result parseOn(Context context) {
        return parser.apply(input).parseOn(context);
    }

    @Override
    public Parser copy() {
        return new LazyParser(parser, input);
    }

    public static Parser failingParser(AtomicReference<String> label, String message){
        return new LazyParser<AtomicReference<String>>((AtomicReference<String> labell) -> FailureParser.withMessage(label.get() + ": " + message), label);
    }
}
