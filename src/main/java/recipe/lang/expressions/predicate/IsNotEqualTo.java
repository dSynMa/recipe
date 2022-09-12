package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.utils.exceptions.*;

import java.util.List;
import java.util.function.Function;

public class IsNotEqualTo extends Condition {
	private Expression lhs;
	private Expression rhs;

	public IsNotEqualTo(Expression lhs, Expression rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof IsNotEqualTo) {
			IsNotEqualTo hv = (IsNotEqualTo) obj;
			return lhs.equals(hv.lhs) && rhs.equals(hv.rhs);
		}
		return false;
	}

	@Override
	public String toString() {
		return lhs + " != " + rhs;
	}

	@Override
	public TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException {
		Expression lhsValue = lhs.valueIn(store);
		Expression rhsValue = rhs.valueIn(store);

		if(!lhsValue.equals(rhsValue)){
			return Condition.TRUE;
		} else{
			return Condition.FALSE;
		}
	}

	@Override
	public Expression<Boolean> simplify() throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
		Expression lhsObject = lhs.simplify();
		Expression rhsObject = rhs.simplify();
		if (lhsObject.equals(rhsObject)) {
			return Condition.FALSE;
		} else if(!lhsObject.getClass().equals(TypedValue.class) ||
				!rhsObject.getClass().equals(TypedValue.class)){
			return new IsNotEqualTo(lhsObject, rhsObject);
		} else{
			return Condition.TRUE;
		}
	}

	public static Parser parser(Parser expression) {
		Parser parser =
				(expression)
						.seq(StringParser.of("!=").trim())
						.seq(expression)
						.map((List<Object> values) -> {
							return new IsNotEqualTo((Expression) values.get(0), (Expression) values.get(2));
						});

		return parser;
	}

	@Override
	public Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
		return new IsNotEqualTo(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
	}
}

