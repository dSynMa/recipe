package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.arithmetic.*;
import recipe.lang.expressions.strings.StringExpression;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;

//TODO add exception wrong type and non-existing variables
public abstract class Condition implements Expression {

	public enum PredicateType {
		FALSE, TRUE, ISEQUAL, ISGTR, ISGEQ, ISLEQ, ISLES, AND, OR, NOT, VAR
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

	public static Parser typeParser(){
		return Condition.parser(ArithmeticExpression.typeParser());
	}

	public static org.petitparser.parser.Parser parser(org.petitparser.parser.Parser arithmeticExpression) {
		SettableParser parser = SettableParser.undefined();
		SettableParser bracketed = SettableParser.undefined();
		org.petitparser.parser.Parser and = And.parser(bracketed);
		org.petitparser.parser.Parser or = Or.parser(bracketed);
		org.petitparser.parser.Parser not = Not.parser(bracketed);
		org.petitparser.parser.Parser isEqualTo = IsEqualTo.parser(arithmeticExpression);
		org.petitparser.parser.Parser isLessThan = IsLessThan.parser(arithmeticExpression);
		org.petitparser.parser.Parser isLessOrEqualThan = IsLessOrEqualThan.parser(arithmeticExpression);
		org.petitparser.parser.Parser isGreaterOrEqualThan = IsGreaterOrEqualThan.parser(arithmeticExpression);
		org.petitparser.parser.Parser isGreaterThan = IsGreaterThan.parser(arithmeticExpression);
		org.petitparser.parser.Parser value = BooleanValue.parser();
		org.petitparser.parser.Parser variable = BooleanVariable.parser();
		org.petitparser.parser.Parser myVariable = MyBooleanVariable.parser();

		parser.set(and
				.or(or)
				.or(isEqualTo)
				.or(isLessThan)
				.or(isLessOrEqualThan)
				.or(isGreaterOrEqualThan)
				.or(isGreaterThan)
				.or(bracketed));

		bracketed.set((CharacterParser.of('(').trim().seq(parser).seq(CharacterParser.of(')')).map((List<Object> values) -> values.get(1)))
				.or(not)
				.or(value)
				.or(variable)
				.or(myVariable));


		return parser;
	}
}
