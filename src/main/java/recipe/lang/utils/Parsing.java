package recipe.lang.utils;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.lang.Config;
import recipe.lang.exception.TypeCreationException;
import recipe.lang.expressions.Expression;
import recipe.lang.definitions.GuardDefinition;
import recipe.lang.expressions.TypedVariable;
import recipe.lang.expressions.arithmetic.ArithmeticExpression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.types.*;
import recipe.lang.types.Enum;

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

    public static org.petitparser.parser.Parser expressionParser(TypingContext context) throws Exception {
        return Condition.parser(context)
                .or(ArithmeticExpression.parser(context))
                .or(context.variableParser())
                .or(Enum.generalValueParser());
    }

    public static org.petitparser.parser.Parser assignmentListParser(TypingContext variableContext,
                                                                     TypingContext expressionContext) throws Exception {
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
                .map((Object value) -> {
                    return recipe.lang.types.Integer.getType();
                }))
                .or(StringParser.ofIgnoringCase("real")
                        .map((Object value) -> {
                            return recipe.lang.types.Real.getType();
                        }))
                .or(CharacterParser.digit().plus().flatten()
                        .seq(CharacterParser.of('.').seq(CharacterParser.of('.').plus()).flatten())
                        .seq(CharacterParser.digit().plus().flatten())
                        .map((List<String> values) ->
                        {;
                            return new BoundedInteger(java.lang.Integer.parseInt(values.get(0)), java.lang.Integer.parseInt(values.get(2)));
                        }));
    }

    public static Parser booleanType(){
        return StringParser.ofIgnoringCase("boolean")
                .or(StringParser.ofIgnoringCase("bool"))
                .map((Object value) -> recipe.lang.types.Boolean.getType());
    }

    public static Parser enumType(){
        //Lazy parser is needed to wait for the channels to be set
        return new LazyParser<Object>((Object x) -> {
            List<String> labels = new ArrayList<>(recipe.lang.types.Enum.getEnumLabels());
            labels.remove(Config.channelWithoutBroadcastLabel);
            return disjunctiveStringParser(labels)
                    .map((String value) -> {
                        try {
                            return recipe.lang.types.Enum.getEnum(value);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
        }, null);
    }

    public static Parser processType(TypingContext messageContext,
                                     TypingContext localContext,
                                     TypingContext communicationContext){
        return StringParser.ofIgnoringCase("process")
                .map((Object value) ->
                        new recipe.lang.types.Process(messageContext, localContext, communicationContext));
    }

    public static Parser typedVariableList(){
        return typedVariableList(numberType().or(enumType()).or(booleanType()));
    }

    public static Parser typedVariableList(Parser typeParser){
        org.petitparser.parser.Parser stringParser = word().plus().seq(CharacterParser.word().not()).flatten().trim();

        org.petitparser.parser.Parser typedVariable = stringParser
                .seq(CharacterParser.of(':').trim())
                .seq(typeParser).trim()
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

    public static Parser typedAssignmentList(TypingContext channelValueContext) throws Exception {
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
                .seq(new LazyParser<List<recipe.lang.types.Enum>>((List<recipe.lang.types.Enum> enumList) -> {
                    return enumList.get(0).valueParser();
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
        org.petitparser.parser.Parser typedVariableAssignmentList = (typedVariableAssignment.plus().trim())
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

    public static Parser enumDefinitionParser(){
        org.petitparser.parser.Parser enumDefinitionParser =
                    StringParser.of("enum").trim()
                            .seq(CharacterParser.lowerCase().seq(CharacterParser.word().star()).flatten().trim())
                            .seq(CharacterParser.of('{').trim())
                            .seq(CharacterParser.word().plus().trim().flatten().separatedBy(CharacterParser.of(',').trim()))
                            .seq(CharacterParser.of('}').trim())
                            .map((List<Object> values) -> {
                                String enumName = ((String) values.get(1)).trim();
                                List<String> enumValues = new ArrayList<>();
                                for(Object v : (List<Object>) values.get(3)){
                                    if(v.getClass().equals(String.class)){
                                        enumValues.add(((String) v).trim());
                                    }
                                }
                                Enum enumm = null;
                                try {
                                    enumm = new Enum(enumName, enumValues);
                                } catch (TypeCreationException e) {
                                    e.printStackTrace();
                                }
                                return enumm;
                            });

        return enumDefinitionParser;
    }

    public static Parser guardDefinitionList(TypingContext typingContext) {
        try {
            if (!typingContext.get(Enum.getEnum(Config.channelLabel)).contains("channel"))
                typingContext.set("channel", Enum.getEnum(Config.channelLabel));
        } catch (Exception e){

        }
        org.petitparser.parser.Parser guardDefinitionParser = GuardDefinition.parser(typingContext);

        org.petitparser.parser.Parser guardDefinitionListParser = (guardDefinitionParser).plus()
                .map((List<Object> values) -> {
                    Map<String, Type> guardDefinitionContext = new HashMap<>();
                    for(Object v : values){
                        if(v.getClass().equals(GuardDefinition.class)) {
                            GuardDefinition vv = (GuardDefinition) v;
                            guardDefinitionContext.put(vv.getName(), vv.getType());
                            Guard.setDefinition(vv.getName(), vv);
                        }
                    }

                    return guardDefinitionContext;
                });

        return guardDefinitionListParser;
    }

    public static Parser channelValues() {
        Parser parser = ((CharacterParser.lowerCase().seq(word().star())).flatten().separatedBy(CharacterParser.of(',').trim())).seq(CharacterParser.whitespace())
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

    public static Parser labelledParser(String label, String separatedBy, Parser parser) {
        return (StringParser.of(label).trim()
                .map((Object v) -> {
                    return v;
                }))
                .seq(StringParser.of(separatedBy).trim().map((Object v) -> {
                    return v;
                }))
                .seq(parser.trim()
                        .map((Object v) -> {
                            return v;
                        }))
                .map((List<Object> values) -> {
                    return values.get(2);
                });
    }

    public static Parser labelledParser(String label, Parser parser){
        return labelledParser(label, ":", parser);
    }

    public static Parser conditionalFail(java.lang.Boolean yes){
        if(yes){
            return StringParser.of("").not();
        } else{
            return StringParser.of("");
        }
    }

    private static boolean isWhitespace(Object obj){
        if(obj.getClass().equals(Character.class)){
            return Character.isWhitespace((Character) obj);
        }

        return false;
    }

    public static Parser relabellingParser(TypingContext localContext, TypingContext communicationContext) throws Exception {
        return labelledParser("relabel", (communicationContext.variableParser().trim()
                .seq(StringParser.of("<-").trim())
                .seq(Parsing.expressionParser(localContext)).trim()).plus()
                ).trim().map((List<Object> values) -> {
                    values.removeIf(v -> isWhitespace(v));
                    Map<TypedVariable, Expression> relabellingMap = new HashMap<>();
                    for(Object relabelObj : values){
                        List relabel = (List) relabelObj;
                        relabellingMap.put((TypedVariable) relabel.get(0), (Expression) relabel.get(2));
                    }
                    return relabellingMap;
                });
    }

    public static Parser receiveGuardParser(TypingContext localContext) throws Exception {
        TypingContext receiveGuardContext = TypingContext.union(localContext, new TypingContext());

        //Broadcast channel cannot be mentioned in receive guard
        receiveGuardContext.set("channel", Enum.getEnum(Config.channelWithoutBroadcastLabel));

        return labelledParser("receive-guard", Condition.parser(receiveGuardContext))
                .map((Expression<recipe.lang.types.Boolean> cond) -> {
                    return cond;
                });
    }

    public static boolean compatible(Expression lhs, Expression rhs){
        //TODO is this a correct implementation?
        if(lhs.getType().getClass().equals(Enum.class) && rhs.getType().getClass().equals(Enum.class)){
            Enum lhsEnum = (Enum) lhs.getType();
            Enum rhsEnum = (Enum) rhs.getType();

            if(lhsEnum.name().equals(Config.channelLabel) && rhsEnum.name().equals(Config.channelWithoutBroadcastLabel)
                || rhsEnum.name().equals(Config.channelLabel) && lhsEnum.name().equals(Config.channelWithoutBroadcastLabel)){
                return true;
            }
        }
        return rhs.isValidAssignmentFor(new TypedVariable(lhs.getType(), "vv")) ||
                lhs.isValidAssignmentFor(new TypedVariable(rhs.getType(), "vv"));
    }
}