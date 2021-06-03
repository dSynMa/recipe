package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Type;
import recipe.lang.utils.Parsing;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class IsEqualTo<T extends Type> extends Condition {

	private Expression<T> lhs;
	private Expression<T> rhs;

	public IsEqualTo(Expression<T> lhs, Expression<T> rhs) throws MismatchingTypeException {
		if(Parsing.compatible(lhs, rhs)){
			this.lhs = lhs;
			this.rhs = rhs;
		} else{
			throw new MismatchingTypeException(lhs.toString() + " not comparable to " + rhs.toString());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof IsEqualTo) {
			IsEqualTo hv = (IsEqualTo) obj;
			return lhs.equals(hv.lhs) && rhs.equals(hv.rhs);
		}
		return false;
	}

	@Override
	public String toString() {
		return lhs + "=" + rhs;
	}

	@Override
	public TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException {
		Expression lhsValue = lhs.valueIn(store);
		Expression rhsValue = rhs.valueIn(store);

		if(lhsValue.equals(rhsValue)){
			return Condition.TRUE;
		} else{
			return Condition.FALSE;
		}
	}

	@Override
	public Expression<Boolean> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
		Expression lhsObject = lhs.close(store, CV);
		Expression rhsObject = rhs.close(store, CV);
		if (lhsObject.equals(rhsObject)) {
			return Condition.TRUE;
		} else if(!lhsObject.getClass().equals(TypedValue.class) &&
				!rhsObject.getClass().equals(TypedValue.class)){
			return new IsEqualTo(lhsObject, rhsObject);
		} else{
			return Condition.FALSE;
		}
	}

	public static org.petitparser.parser.Parser parser(Parser expression) {
		org.petitparser.parser.Parser parser =
				(expression.map((Object v) -> {
					return v;
				}))
						.seq(StringParser.of("=").seq(StringParser.of("=").optional()).trim())
						.seq(expression.map((Object v) -> {
							return v;
						}))
						.map((List<Object> values) -> {
							try {
								return new IsEqualTo((Expression) values.get(0), (Expression) values.get(2));
							} catch (MismatchingTypeException e) {
								e.printStackTrace();
							}

							return null;
						});

		return parser;
	}

	@Override
	public Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
		return new IsEqualTo(this.lhs.relabel(relabelling), this.rhs.relabel(relabelling));
	}
}

