package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.*;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class Condition implements Expression<Boolean> {
	static TypedValue<Boolean> TRUE;
	static TypedValue<Boolean> FALSE;

	static {
		try {
			TRUE = new TypedValue<Boolean>(Boolean.getType(), "true");
			FALSE = new TypedValue<Boolean>(Boolean.getType(), "false");
		} catch (MismatchingTypeException e) {
			e.printStackTrace();
		}
	}

	public boolean isSatisfiedBy(Store store) throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException {
		TypedValue value = valueIn(store);
		if(value.getValue().equals(true)){
			return true;
		} else{
			return false;
		}
	}

	public abstract TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException;
	public abstract Expression<Boolean> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, TypeCreationException;

	public static Parser typeParser(TypingContext context){
		return Condition.parser(context);
	}

	public static org.petitparser.parser.Parser parser(TypingContext context) {
		org.petitparser.parser.Parser arithmeticExpression = ArithmeticExpression.typeParser(context);

		SettableParser parser = SettableParser.undefined();
		SettableParser basic = SettableParser.undefined();

		SettableParser generalExpression = SettableParser.undefined();
		generalExpression.set((arithmeticExpression).or(context.variableParser()).or(context.valueParser()));

		org.petitparser.parser.Parser and = And.parser(basic);
		org.petitparser.parser.Parser or = Or.parser(basic);
		org.petitparser.parser.Parser not = Not.parser(basic);

		org.petitparser.parser.Parser isEqualTo = IsEqualTo.parser(generalExpression);
		org.petitparser.parser.Parser isNotEqualTo = IsNotEqualTo.parser(generalExpression);

		org.petitparser.parser.Parser isLessThan = IsLessThan.parser(arithmeticExpression);
		org.petitparser.parser.Parser isLessOrEqualThan = IsLessOrEqualThan.parser(arithmeticExpression);
		org.petitparser.parser.Parser isGreaterOrEqualThan = IsGreaterOrEqualThan.parser(arithmeticExpression);
		org.petitparser.parser.Parser isGreaterThan = IsGreaterThan.parser(arithmeticExpression);

		org.petitparser.parser.Parser comparators =
				isEqualTo
				.or(isNotEqualTo)
				.or(isLessThan)
				.or(isLessOrEqualThan)
				.or(isGreaterOrEqualThan)
				.or(isGreaterThan);

		org.petitparser.parser.Parser value = Boolean.getType().parser();
		org.petitparser.parser.Parser variable = context.getSubContext(Boolean.getType()).variableParser();

		parser.set(and
				.or(or)
				.or(basic));

		basic.set(value
				.or(variable)
				.or(not)
				.or(comparators)
				.or(CharacterParser.of('(').trim().seq(parser).seq(CharacterParser.of(')'))
						.map((List<Object> values) -> {
							return values.get(1);
						}))
				);

		return parser;
	}

	public abstract Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException;

	@Override
	public int hashCode(){
		return Objects.hash(this.toString());
	}
}
