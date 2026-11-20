package recipe.lang.types;

import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.expressions.TypedValue;
import org.petitparser.context.Result;
import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;

import java.util.*;

public class BoundedInteger extends Integer {
    private int min;
    private int max;

    public BoundedInteger(int min, int max){
        if (min > max) {
            throw new IllegalArgumentException("Invalid integer domain " + min + ".." + max
                + ": lower bound exceeds upper bound");
        }
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundedInteger that = (BoundedInteger) o;
        return min == that.min && max == that.max;
    }

    @Override
    public Parser valueParser() {
        Parser integerLiteral = CharacterParser.of('-').optional()
                .seq(CharacterParser.digit().plus())
                .flatten()
                .seq(CharacterParser.word().not())
                .pick(0);

        return integerLiteral.callCC((parse, context) -> {
            Result result = parse.apply(context);
            if (result.isFailure()) {
                return result;
            }

            String literal = result.get();
            try {
                return result.success(new TypedValue<>(this, literal));
            } catch (MismatchingTypeException e) {
                return context.failure(e.getMessage());
            }
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public java.lang.Number interpret(String value) throws MismatchingTypeException {
        try{
            int val = java.lang.Integer.parseInt(value);
            if(val >= min && val <= max){
                return val;
            } else{
                throw new MismatchingTypeException(value + " is not of type " + name());
            }

        } catch (Exception e) {
            throw new MismatchingTypeException(value + " is not of type " + name());
        }
    }
    public Set<TypedValue> getAllValues() throws MismatchingTypeException {
        Set<TypedValue> values = new HashSet<>();
        for(long i = min; i <= max; i++){
            values.add(new TypedValue<BoundedInteger>(this, "" + i));
        }
        return values;
    }
    @Override
    public String name() {
        return min + ".." + max;
    }
}
