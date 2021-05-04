package recipe.lang.utils;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.Config;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.BoundedInteger;
import recipe.lang.types.Enum;
import recipe.lang.types.Integer;
import recipe.lang.types.Type;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.petitparser.parser.primitive.CharacterParser.*;

public class Parsing {

    public static org.petitparser.parser.Parser disjunctiveStringParser(List<String> allowed) {
        if(allowed == null || allowed.size() == 0){
            return FailureParser.withMessage("No variables of expected type.");
        }

        org.petitparser.parser.Parser parser = StringParser.of(allowed.get(0));
        for (int i = 1; i < allowed.size(); i++) {
            //DOC: .seq(CharacterParser.word().not()) is added with each parser to allow for parsing when
            // one element in allowed is a prefix of another.
            parser = parser.or(StringParser.of(allowed.get(i)).seq(CharacterParser.word().not()).flatten());
        }

        return parser;
    }

    public static org.petitparser.parser.Parser disjunctiveWordParser(Set<String> allowed, Function<String, Expression> transformation) {
        return disjunctiveWordParser(new ArrayList<>(allowed), transformation);
    }

    public static org.petitparser.parser.Parser disjunctiveWordParser(List<String> allowed, Function<String, Expression> transformation) {
        org.petitparser.parser.Parser parser = disjunctiveStringParser(allowed);

        parser = (parser).map((String value) -> {
            return transformation.apply(value);
        });

        return parser;
    }

    public static Parser eof(){
        return any().not();
    }

    public static org.petitparser.parser.Parser expressionParser(TypingContext context) {
        return Condition.parser(context)
                .or(ArithmeticExpression.parser(context))
                .or(context.variableParser())
                .or(context.valueParser());
    }

    public static org.petitparser.parser.Parser assignmentListParser(TypingContext variableContext,
                                                                     TypingContext expressionContext) {
        Parser assignment =
                variableContext.variableParser()
                        .seq(StringParser.of(":=").trim())
                        .seq(expressionParser(expressionContext))
                        .map((List<Object> values) -> {
                            return new Pair(values.get(0).toString(), values.get(2));
                        });

        Parser assignmentList =
                assignment
                        .delimitedBy(CharacterParser.of(',').or(CharacterParser.of(';')).trim())
                        .map((List<Object> values) -> {
                            HashMap<String, Expression> map = new HashMap();
                            for(Object v : values){
                                if(v.getClass().equals(Character.class)) continue;
                                Pair<String, Expression> pair = (Pair<String, Expression>) v;
                                map.put(pair.getLeft(), pair.getRight());
                            }

                            return map;
                        });

        return assignmentList;
    }

    public static Parser numberType(){
        return (StringParser.ofIgnoringCase("integer")
                .or(StringParser.ofIgnoringCase("int"))
                .map((Object value) -> recipe.lang.types.Integer.getType()))
                .or(StringParser.ofIgnoringCase("real")
                        .map((Object value) -> recipe.lang.types.Real.getType()))
                .or(CharacterParser.digit().plus().flatten()
                        .seq(CharacterParser.of('.').seq(CharacterParser.of('.').plus()).flatten())
                        .seq(CharacterParser.digit().plus().flatten())
                        .map((List<String> values) ->
                                new BoundedInteger(java.lang.Integer.parseInt(values.get(0)), java.lang.Integer.parseInt(values.get(2)))));
    }


    public static Parser booleanType(){
        return StringParser.ofIgnoringCase("boolean")
                .or(StringParser.ofIgnoringCase("bool"))
                .map((Object value) -> recipe.lang.types.Boolean.getType());
    }

    public static Parser enumType(){
        List<String> labels = new ArrayList<>(recipe.lang.types.Enum.getEnumLabels());
        return disjunctiveStringParser(labels)
                .map((String value) -> {
                    try {
                        return recipe.lang.types.Enum.getEnum(value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });
    }

    public static Parser typedVariableList(){
        org.petitparser.parser.Parser stringParser = word().plus().seq(CharacterParser.word().not()).flatten().trim();

        org.petitparser.parser.Parser typedVariable = stringParser
                .seq(CharacterParser.of(':').trim())
                .seq(numberType().or(enumType()).or(booleanType())).trim()
                .map((List<Object> values) -> {
                    return new TypedVariable((Type) values.get(2), (String) values.get(0));
                });

        org.petitparser.parser.Parser typedVariableList = (typedVariable.separatedBy(CharacterParser.of(',').trim()))
                .map((List<Object> values) -> {
                    List<Object> delimitedTypedVariables = values;
                    Map<String, Type> typedVariables = new HashMap<>();
                    for (int i = 0; i < delimitedTypedVariables.size(); i += 2) {
                        TypedVariable typedVar = (TypedVariable) delimitedTypedVariables.get(i);
                        typedVariables.put(typedVar.getName(), typedVar.getType());
                    }
                    return typedVariables;
                });

        return typedVariableList;
    }

    public static Parser typedAssignmentList(TypingContext channelValueContext){
        org.petitparser.parser.Parser stringParser = (word().plus().seq(CharacterParser.word().not())).flatten().trim();

        org.petitparser.parser.Parser numberVarParser = stringParser
                .seq(CharacterParser.of(':').trim())
                .seq(numberType().trim())
                .seq(StringParser.of(":=").trim())
                .seq(ArithmeticExpression.typeParser(new TypingContext()))
                .map((List<Object> values) -> {
                    return new Pair(new TypedVariable((Type) values.get(2), (String) values.get(0)), values.get(4));
                });

        AtomicReference<List<recipe.lang.types.Enum>> enumType = new AtomicReference<>(new ArrayList<>());

        org.petitparser.parser.Parser enumVarParser = stringParser
                .seq(CharacterParser.of(':').trim())
                .seq(enumType().trim().mapWithSideEffects((recipe.lang.types.Enum value) -> {
                    enumType.get().add(value);
                    return value;
                }))
                .seq(StringParser.of(":=").trim())
                .seq(new LazyParser<>((List<recipe.lang.types.Enum> enumList) -> {
                    return enumList.get(0).parser();
                },
                        enumType.get()))
                .map((List<Object> values) -> {
                    return new Pair(new TypedVariable((Type) values.get(2), (String) values.get(0)), values.get(4));
                });

        org.petitparser.parser.Parser boolVarParser = stringParser
                .seq(CharacterParser.of(':').trim())
                .seq(booleanType().trim())
                .seq(StringParser.of(":=").trim())
                .seq(Condition.typeParser(channelValueContext))
                .map((List<Object> values) -> {
                    return new Pair(new TypedVariable((Type) values.get(2), (String) values.get(0)), values.get(4));
                });

        org.petitparser.parser.Parser typedVariableAssignment = numberVarParser.or(boolVarParser).or(enumVarParser);//.or(channelVarParser);
        org.petitparser.parser.Parser typedVariableAssignmentList = (typedVariableAssignment.delimitedBy(CharacterParser.of('\n')))
                .map((List<Object> values) -> {
                    List<Object> delimitedTypedVariablesAssignment = values;
                    Map<String, TypedVariable> typedVariables = new HashMap<>();
                    Map<String, Expression> typedVariableValues = new HashMap<>();
                    for (int i = 0; i < delimitedTypedVariablesAssignment.size(); i += 2) {
                        Pair<TypedVariable, Expression> varVal = ((Pair<TypedVariable, Expression>) delimitedTypedVariablesAssignment.get(i));
                        typedVariables.put(varVal.getLeft().getName(), varVal.getLeft());
                        typedVariableValues.put(varVal.getLeft().getName(), varVal.getRight());
                    }
                    return new Pair(typedVariables, typedVariableValues);
                });

        return typedVariableAssignmentList;
    }

    public static Parser guardDefinitionList(){
        AtomicReference<TypingContext> typedVariableList = new AtomicReference<>(new TypingContext());
        org.petitparser.parser.Parser guardDefinitionParser = StringParser.of("guard").trim()
                .seq(word().plus().trim().flatten())
                .seq(CharacterParser.of('(').trim())
                .seq(typedVariableList()
                        .mapWithSideEffects((Map<String, Type> value) -> {
                            typedVariableList.get().setAll(new TypingContext(value));
                            return value;
                        }))
                .seq(CharacterParser.of(')').trim())
                .seq(StringParser.of(":=").trim())
                .seq(new LazyParser<>((TypingContext context) -> Condition.parser(context), typedVariableList.get()))
                .map((List<Object> values) -> {
                    return new HashMap.SimpleEntry<>(values.get(1), new Pair(values.get(3), values.get(6)));
                });

        org.petitparser.parser.Parser guardDefinitionListParser = guardDefinitionParser.separatedBy(CharacterParser.of('\n').star())
                .map((List<Object> values) -> {
                    Map<String, Map<String, Expression>> guardsParams = new HashMap();
                    Map<String, Condition> guards = new HashMap();
                    for(Object v : values){
                        HashMap.SimpleEntry entry = (HashMap.SimpleEntry) v;
                        guardsParams.put((String) entry.getKey(), (Map<String, Expression>) ((Pair) entry.getValue()).getLeft());
                        guards.put((String) entry.getKey(), (Condition) ((Pair) entry.getValue()).getRight());
                    }
                    return new Pair(guardsParams, guards);
                });

        return guardDefinitionListParser;
    }

    public static Parser channelValues() {
        Parser parser = (word().plus().flatten().separatedBy(CharacterParser.of(',').trim())).seq(CharacterParser.of('\n'))
                .map((List<Object> values) -> {
                    List<String> vals = new ArrayList<>();

                    for(Object v : (List) values.get(0)){
                        if(!v.getClass().equals(Character.class)){
                            vals.add((String) v);
                        }
                    }
                    return vals;
                });

        return parser;
    }

    public static Parser labelledParser(String label, Parser parser){
        return (StringParser.of(label).trim()
                .map((Object v) -> {
                    return v;
                }))
                .seq(CharacterParser.of(':').trim())
                .seq(parser.trim()
                        .map((Object v) -> {
                            return v;
                        }))
                .map((List<Object> values) -> {
                    return values.get(2);
                });
    }

    public static Parser conditionalFail(Boolean yes){
        if(yes){
            return StringParser.of("").not();
        } else{
            return StringParser.of("");
        }
    }

    public static Parser relabellingParser(TypingContext localContext, TypingContext communicationContext){
        return labelledParser("relabel", (Parsing.expressionParser(communicationContext).trim()
                .seq(StringParser.of("<-").trim())
                .seq(Parsing.expressionParser(localContext)).delimitedBy(CharacterParser.of('\n'))
                )).map((List<Object> values) -> {
                    values.removeIf(v -> v.equals('\n'));
                    Map<TypedVariable, Expression> relabellingMap = new HashMap<>();
                    for(Object relabelObj : values){
                        List relabel = (List) relabelObj;
                        relabellingMap.put((TypedVariable) relabel.get(0), (Expression) relabel.get(2));
                    }
                    return relabellingMap;
                });
    }

    public static Parser receiveGuardParser(TypingContext localContext, TypingContext channelContext) throws Exception {
        TypingContext receiveGuardContext = TypingContext.union(channelContext, localContext);
        receiveGuardContext.set("channel", Enum.getEnum(Config.channelLabel));

        return labelledParser("receive-guard", Condition.parser(receiveGuardContext))
                .map((Expression<recipe.lang.types.Boolean> cond) -> {
                    return cond;
                });
    }
}