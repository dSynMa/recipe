package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Number;
import recipe.lang.utils.exceptions.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class IsGreaterOrEqualThan extends Condition {

	private Expression<Number> lhs;
	private Expression<Number> rhs;

	public IsGreaterOrEqualThan(Expression<Number> lhs, Expression<Number> rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (super.equals(obj)) {
			IsGreaterOrEqualThan p = (IsGreaterOrEqualThan) obj;
			if (!this.lhs.equals(p.lhs)) {
				return false;
			}
			return (this.rhs == p.rhs) || ((this.rhs != null) && (this.rhs.equals(p.rhs)));
		}
		return false;
	}

	@Override
	public String toString() {
		return lhs + " >= " + rhs.toString();
	}

	@Override
	public TypedValue<Boolean> valueIn(Store store) throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException, NotImplementedYetException {
		TypedValue<Number> lhsValue = lhs.valueIn(store);
		TypedValue<Number> rhsValue = rhs.valueIn(store);

		if(0 <= new BigDecimal(lhsValue.getValue().toString()).compareTo(new BigDecimal(rhsValue.getValue().toString()))) {
			return Condition.TRUE;
		} else {
			return Condition.FALSE;
		}
	}

	@Override
	public Expression<Boolean> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
		Expression<Number> lhsObject = lhs.simplify();
		Expression<Number> rhsObject = rhs.simplify();
		if (lhsObject.equals(rhsObject)) {
			return Condition.TRUE;
		} else if(!lhsObject.getClass().equals(TypedValue.class) ||
				!rhsObject.getClass().equals(TypedValue.class)) {
			return new IsGreaterOrEqualThan(lhsObject, rhsObject);
		} else{
			return Condition.FALSE;
		}
	}

	public static org.petitparser.parser.Parser parser(Parser arithmeticExpression) {
		org.petitparser.parser.Parser parser =
				(arithmeticExpression)
						.seq(StringParser.of(">=").trim())
						.seq(arithmeticExpression)
						.map((List<Object> values) -> {
							return new IsGreaterOrEqualThan((Expression<Number>) values.get(0), (Expression<Number>) values.get(2));
						});

		return parser;
	}

	@Override
	public Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
		return new IsGreaterOrEqualThan(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
	}

	public Set<Expression<Boolean>> subformulas(){
		Set<Expression<Boolean>> subformulas = new HashSet<>();
		subformulas.add(this);
		return subformulas;
	}

	public Expression<Boolean> replace(java.util.function.Predicate<Expression<Boolean>> cond,
									   Function<Expression<Boolean>, Expression<Boolean>> act) {
		if (cond.test(this)) {
			return act.apply(this);
		} else {
			return this;
		}
	}

	public Condition removePreds(){
		return this;
	}
}
