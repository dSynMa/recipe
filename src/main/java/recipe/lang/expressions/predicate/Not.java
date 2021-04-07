package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.exception.AttributeNotInStoreException;
import recipe.lang.exception.AttributeTypeException;
import recipe.lang.exception.RelabellingTypeException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.store.Store;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class Not extends Condition {

	private Condition arg;

	public Not(Condition arg) {
		super(Condition.PredicateType.NOT);
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
	public BooleanValue valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException {
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
	public Condition close(Store store, Set<String> CV) throws AttributeNotInStoreException, AttributeTypeException {
		Condition closure = arg.close(store, CV);
		if (closure.equals(Condition.FALSE)) {
			return Condition.TRUE;
		} else if(!closure.getClass().equals(BooleanValue.class)){
			return new Not(closure);
		} else{
			return Condition.FALSE;
		}
	}

	public static org.petitparser.parser.Parser parser(Parser basicCondition) {
		org.petitparser.parser.Parser parser =
				CharacterParser.of('!').trim()
						.seq(basicCondition)
						.map((List<Object> values) -> {
							return new Not((Condition) values.get(1));
						});

		return parser;
	}

	@Override
	public Condition relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException {
		return new Not(this.arg.relabel(relabelling));
	}
}
