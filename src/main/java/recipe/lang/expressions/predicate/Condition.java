package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.*;
import recipe.lang.expressions.channels.ChannelExpression;
import recipe.lang.expressions.strings.StringExpression;
import recipe.lang.store.Store;
import recipe.lang.utils.Parsing;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

//TODO add exception wrong type and non-existing variables
public abstract class Condition implements Expression {

	public enum PredicateType {
		FALSE, TRUE, ISEQUAL, ISNOTEQUAL, ISGTR, ISGEQ, ISLEQ, ISLES, AND, OR, NOT, VAR
	}

	public static final BooleanValue TRUE = new BooleanValue(true);
	public static final BooleanValue FALSE = new BooleanValue(false);

	private PredicateType type;

	public boolean isSatisfiedBy(Store store) throws AttributeTypeException, AttributeNotInStoreException{
		BooleanValue value = valueIn(store);
		if(value.equals(TRUE)){
			return true;
		} else{
			return false;
		}
	}

	public Condition(PredicateType type) {
		this.type = type;
	}

	public PredicateType getType() {
		return this.type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof Condition) {
			return this.type == ((Condition) obj).type;
		}
		return false;
	}

	public abstract BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException;
	public abstract Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException;

	public static Parser typeParser(TypingContext context){
		return Condition.parser(context);
	}

	public static org.petitparser.parser.Parser parser(TypingContext context) {
		org.petitparser.parser.Parser arithmeticExpression = ArithmeticExpression.typeParser(context);
		org.petitparser.parser.Parser channelExpression = ChannelExpression.typeParser(context);
		org.petitparser.parser.Parser stringExpression = StringExpression.typeParser(context);

		SettableParser parser = SettableParser.undefined();
		SettableParser basic = SettableParser.undefined();

		SettableParser expression = SettableParser.undefined();
		expression.set((arithmeticExpression).or(channelExpression).or(stringExpression));

		org.petitparser.parser.Parser and = And.parser(basic);
		org.petitparser.parser.Parser or = Or.parser(basic);
		org.petitparser.parser.Parser not = Not.parser(basic);

		org.petitparser.parser.Parser isEqualTo = IsEqualTo.parser(expression);
		org.petitparser.parser.Parser isNotEqualTo = IsNotEqualTo.parser(expression);

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

		org.petitparser.parser.Parser value = BooleanValue.parser();
		org.petitparser.parser.Parser variable = BooleanVariable.parser(context);

		parser.set(and
				.or(or)
				.or(basic));

		basic.set(value
				.or(variable)
				.or(not)
				.or(comparators)
				.or(CharacterParser.of('(').trim().seq(parser).seq(CharacterParser.of(')'))
						.map((List<Object> values) -> values.get(1)))
				);

		return parser;
	}

	public abstract Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException;
}
