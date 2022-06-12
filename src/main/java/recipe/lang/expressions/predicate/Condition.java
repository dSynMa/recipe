package recipe.lang.expressions.predicate;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.StringParser;
import org.petitparser.tools.ExpressionBuilder;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.Predicate;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.*;
import recipe.lang.process.*;
import recipe.lang.process.Process;
import recipe.lang.store.Store;
import recipe.lang.types.Boolean;
import recipe.lang.types.Enum;
import recipe.lang.types.Type;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.of;

public abstract class Condition implements Expression<Boolean> {
	static TypedValue<Boolean> TRUE;
	static TypedValue<Boolean> FALSE;

	static {
		try {
			TRUE = new TypedValue<Boolean>(Boolean.getType(), "TRUE");
			FALSE = new TypedValue<Boolean>(Boolean.getType(), "FALSE");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static TypedValue<Boolean> getTrue(){
		return TRUE;
	}

	public static TypedValue<Boolean> getFalse(){
		return FALSE;
	}

	public boolean isSatisfiedBy(Store store) throws AttributeTypeException, AttributeNotInStoreException, MismatchingTypeException, NotImplementedYetException {
		TypedValue value = valueIn(store);
		if(value.getValue().equals(true)){
			return true;
		} else{
			return false;
		}
	}

	public abstract TypedValue<Boolean> valueIn(Store store) throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, NotImplementedYetException;
	public abstract Expression<Boolean> close() throws AttributeNotInStoreException, AttributeTypeException, MismatchingTypeException, TypeCreationException, RelabellingTypeException;

	public static Parser typeParser(TypingContext context) throws Exception {
		return Condition.parser(context);
	}

	public static org.petitparser.parser.Parser parser(TypingContext context) throws Exception {
		SettableParser condition = SettableParser.undefined();

		Parser primitiveBoolean =
				(context.getSubContext(Boolean.getType()).variableParser().map((TypedVariable<Boolean> value) -> {
					try {
						return new IsEqualTo<>(value, Condition.getTrue());
					} catch (MismatchingTypeException e) {
						e.printStackTrace();
						return null;
					}
				}))
						.or(Boolean.getType().valueParser().map((TypedValue<Boolean> value) -> {
							try {
								return new IsEqualTo<>(value, Condition.getTrue());
							} catch (MismatchingTypeException e) {
								e.printStackTrace();
								return null;
							}
						}))
						.or(GuardReference.parser(context, condition));

		Parser arithmeticOrEnums = ArithmeticExpression.parser(context);

		for(String label : Enum.getEnumLabels()){
			recipe.lang.types.Enum enumm = Enum.getEnum(label);
			TypingContext enumTypingContext = context.getSubContext(enumm);

			arithmeticOrEnums = arithmeticOrEnums.or(enumTypingContext.variableParser()).or(enumm.valueParser());
		}

		ExpressionBuilder builder = new ExpressionBuilder();
		builder.group()
				.primitive(IsEqualTo.parser(arithmeticOrEnums)
						.or(IsNotEqualTo.parser(arithmeticOrEnums))
						.or(IsGreaterOrEqualThan.parser(ArithmeticExpression.parser(context)))
						.or(IsGreaterThan.parser(ArithmeticExpression.parser(context)))
						.or(IsLessOrEqualThan.parser(ArithmeticExpression.parser(context)))
						.or(IsLessThan.parser(ArithmeticExpression.parser(context)))
						.or(primitiveBoolean).trim())
				.wrapper(of('(').trim(), of(')').trim(),
						(List<Expression<Boolean>> values) -> values.get(1));

		// negation is a prefix
		builder.group()
				.prefix(of('!').trim(), (List<Condition> values) -> new Not(values.get(1)));

		for(String pred : context.getPredicates()){
			// negation is a prefix
			builder.group()
					.prefix(StringParser.of(pred).trim(), (List<Condition> values) -> new Predicate(pred, values.get(1)));
		}

		// repetition is a prefix operator
		builder.group()
				.prefix(StringParser.of("rep").trim(), (List<Process> values) -> new Iterative(values.get(1)));

		// conjunction is right- and left-associative
		builder.group()
				.right(of('&').plus().trim(), (List<Condition> values) -> new And(values.get(0), values.get(2)))
				.left(of('&').plus().trim(), (List<Condition> values) -> new And(values.get(0), values.get(2)));

		// disjunction is right- and left-associative
		builder.group()
				.right(of('|').plus().trim(), (List<Condition> values) -> new Or(values.get(0), values.get(2)))
				.left(of('|').plus().trim(), (List<Condition> values) -> new Or(values.get(0), values.get(2)));

		// implication is right-associative
		builder.group()
				.right(StringParser.of("->").plus().trim(), (List<Condition> values) -> new Implies(values.get(0), values.get(2)));

		// iff is left and right-associative
		builder.group()
				.right(StringParser.of("<->").or(StringParser.of("=").plus()).trim(), (List<Condition> values) -> {
					try {
						return new IsEqualTo(values.get(0), values.get(2));
					} catch (Exception e) {
						return e;
					}
				})
				.left(StringParser.of("<->").or(StringParser.of("=").plus()).trim(), (List<Condition> values) -> {
					try {
						return new IsEqualTo(values.get(0), values.get(2));
					} catch (Exception e) {
						return e;
					}
				});

		// is not equal to is left and right-associative
		builder.group()
				.right(StringParser.of("!=").trim(), (List<Condition> values) -> {
					return new IsNotEqualTo(values.get(0), values.get(2));
				})
				.left(StringParser.of("!=").trim(), (List<Condition> values) -> {
					return new IsNotEqualTo(values.get(0), values.get(2));
				});

		condition.set(builder.build());

		return condition;
	}

	public abstract Expression<Boolean> relabel(Function<TypedVariable, Expression> relabelling) throws RelabellingTypeException, MismatchingTypeException;

	@Override
	public int hashCode(){
		return Objects.hash(this.toString());
	}

	@Override
	public java.lang.Boolean isValidAssignmentFor(TypedVariable var){
		return var.getType().equals(Boolean.getType());
	}

	@Override
	public Type getType(){
		return Boolean.getType();
	}
}
