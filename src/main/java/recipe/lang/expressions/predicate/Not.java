package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Type;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class Not extends Condition {

	private Expression<Boolean> arg;

	public Not(Expression<Boolean> arg) {
		if ((arg == null)) {
			throw new NullPointerException();
		}
		this.arg = arg;
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
			Not p = (Not) obj;
			return arg.equals(p.arg);
		}
		return false;
	}

	@Override
	public String toString() {
		return "!(" + arg.toString() + ")";
	}

	public Expression getArgument() {
		return arg;
	}

	@Override
	public TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException {
		Expression argValue = arg.valueIn(store);

		if(argValue.equals(Condition.TRUE)){
			return Condition.FALSE;
		} else if(argValue.equals(Condition.FALSE)){
			return Condition.TRUE;
		} else{
			throw new AttributeTypeException();
		}
	}

	@Override
	public Expression<Boolean> close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException, TypeCreationException, MismatchingTypeException, RelabellingTypeException {
		Expression<Boolean> closure = arg.close(store, CV);
		if (closure.equals(Condition.FALSE)) {
			return Condition.TRUE;
		} else if(!closure.getClass().equals(TypedValue.class)){
			return new Not(closure);
		} else{
			return Condition.FALSE;
		}
	}

	@Override
	public Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException {
		return new Not(this.arg.relabel(relabelling));
	}
}
