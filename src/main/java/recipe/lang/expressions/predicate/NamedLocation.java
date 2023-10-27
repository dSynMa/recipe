package recipe.lang.expressions.predicate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;

import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.utils.exceptions.AttributeNotInStoreException;
import recipe.lang.utils.exceptions.AttributeTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.exceptions.NotImplementedYetException;
import recipe.lang.utils.exceptions.RelabellingTypeException;
import recipe.lang.utils.exceptions.TypeCreationException;

public class NamedLocation extends Condition {
    private String name;
    private boolean _isSelf = false;

    public NamedLocation() { _isSelf = true; }

    public NamedLocation(String name) { this.name = name; }

    @Override
    public String toString() { return _isSelf ? "SELF" : name; }

    public boolean isSelf() { return _isSelf; }

    public static org.petitparser.parser.Parser parser() throws Exception {
        Parser baseParser =
            CharacterParser.noneOf("()[],;").plus().trim().flatten().map(
                (String word) -> {
                    if (word.equals("SELF")) return new NamedLocation();
                    else return new NamedLocation(word);
            });
        Parser parser = baseParser.or(
            CharacterParser.of('(')
            .seq(baseParser)
            .seq(CharacterParser.of(')'))
            .map((List<Object> result) -> {return (NamedLocation) result.get(1);})
        );
        return parser;
    }

    @Override
    public Set<Expression<Boolean>> subformulas() {
        return new HashSet<>();
    }

    @Override
    public Expression<Boolean> replace(Predicate<Expression<Boolean>> cond,
            Function<Expression<Boolean>, Expression<Boolean>> act) {
        return this;
    }

    @Override
    public Expression<Boolean> removePreds() {
        return this;
    }

    @Override
    public TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException,
            MismatchingTypeException, NotImplementedYetException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'valueIn'");
    }

    @Override
    public Expression<Boolean> simplify() throws AttributeNotInStoreException, AttributeTypeException,
            MismatchingTypeException, TypeCreationException, RelabellingTypeException {
        return this;
    }

    @Override
    public Expression<Boolean> relabel(Function<TypedVariable, Expression> relabelling)
            throws RelabellingTypeException, MismatchingTypeException {
        return this;
    }
    
}
