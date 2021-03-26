package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.arithmetic.*;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;

public class And extends Condition {

	private Condition lhs;
	private Condition rhs;

	public And(Condition lhs, Condition rhs) {
		super(Condition.PredicateType.AND);
		if ((lhs == null) || (rhs == null)) {
			throw new NullPointerException();
		}
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
			And p = (And) obj;
			return lhs.equals(p.lhs) && rhs.equals(p.rhs);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.lhs.hashCode() ^ this.rhs.hashCode();
	}

	@Override
	public String toString() {
		return "(" + lhs.toString() + ") & (" + rhs.toString() + ")";
	}

	public Expression getLhs() {
		return lhs;
	}

	public Expression getRhs() {
		return rhs;
	}

	@Override
	public BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
		Expression lhsObject = lhs.valueIn(store);
		Expression rhsObject = rhs.valueIn(store);
		if (lhsObject.equals(Condition.TRUE) && rhsObject.equals(Condition.TRUE)) {
			return Condition.TRUE;
		} else if(lhsObject.equals(Condition.FALSE) || rhsObject.equals(Condition.FALSE)){
			return Condition.FALSE;
		} else{
			throw new AttributeTypeException();
		}
	}

	@Override
	public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
		Condition lhsObject = lhs.close(store, CV);
		Condition rhsObject = rhs.close(store, CV);
		if (lhsObject.equals(Condition.TRUE) && rhsObject.equals(Condition.TRUE)) {
			return Condition.TRUE;
		} else if(!lhsObject.equals(Condition.FALSE) || !rhsObject.equals(Condition.FALSE)){
			return new Or(lhsObject, rhsObject);
		} else{
			return Condition.FALSE;
		}
	}

	public static org.petitparser.parser.Parser parser(Parser basicCondition) {
		org.petitparser.parser.Parser parser =
				(basicCondition)
						.seq(CharacterParser.of('&').trim())
						.seq(basicCondition)
						.map((List<Object> values) -> {
							return new And((Condition) values.get(0), (Condition) values.get(2));
						});

		return parser;
	}
}
