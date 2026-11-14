package recipe.lang.utils;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.CharacterParser;
import org.petitparser.parser.primitive.FailureParser;
import org.petitparser.parser.primitive.StringParser;
import recipe.Config;
import recipe.lang.utils.exceptions.TypeCreationException;
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

    // Identifier keywords from the nuXmv 2.2 manual, including timed-system keywords.
    // Retain process for compatibility with the bundled nuXmv 2.0 parser.
    // https://nuxmv.fbk.eu/downloads/nuxmv-user-manual.pdf (Identifiers)
    private static final java.util.Set<String> RESERVED_CONSTANTS = java.util.Set.of(
        "A", "ABF", "ABG", "abs", "acos", "AF", "AG", "array", "asin", "atan", "at_next", "at_last", "ASSIGN", "AX", "bool",
        "boolean", "BU", "case", "Clock", "clock", "COMPASSION", "COMPID", "COMPUTE", "COMPWFF", "CONSTANTS", "CONSTARRAY", "CONSTRAINT",
        "cos", "count", "CTLSPEC", "CTLWFF", "DEFINE", "E", "EBF", "EBG", "EF", "EG",
        "esac", "EX", "exp", "extend", "F", "FAIRNESS", "FALSE", "floor", "FROZENVAR", "FUN",
        "G", "H", "IN", "in", "INIT", "init", "Integer", "integer", "INVAR", "INVARSPEC",
        "ISA", "ITYPE", "IVAR", "JUSTICE", "ln", "LTLSPEC", "LTLWFF", "MAX", "max", "MDEFINE",
        "MIN", "min", "MIRROR", "mod", "MODULE", "NAME", "next", "NEXTWFF", "noncontinuous", "O",
        "of", "PRED", "PREDICATES", "pi", "pow", "PSLSPEC", "PARSYNTH", "READ", "Real", "real",
        "resize", "S", "SAT", "self", "signed", "SIMPWFF", "sin", "sizeof", "SPEC", "sqrt", "swconst",
        "T", "tan", "time", "time_since", "time_until", "toint", "TRANS", "TRUE", "typeof", "U", "union",
        "unsigned", "URGENT", "uwconst", "V", "VALID", "VAR", "Word", "word", "word1", "WRITE",
        "X", "xnor", "xor", "Y", "Z", "process");

    /** Check names emitted as unqualified nuXmv constants, not prefixed local variables. */
    public static String constantName(String name) {
        if (name.contains("-")) {
            throw new IllegalArgumentException("Invalid constant '" + name
                + "': hyphens are reserved for qualified references.");
        }
        if (Config.nuxmvBroadcast.equals(name)) {
            throw new IllegalArgumentException("'broadcast' is reserved for the broadcast channel; use '*' to broadcast.");
        }
        if (RESERVED_CONSTANTS.contains(name) || name.equals("progress") || name.equals("transition")
                || name.matches("obs[0-9]+")) {
            throw new IllegalArgumentException("Invalid constant '" + name
                + "': this name is reserved by nuXmv or the translation.");
        }
        if (!name.matches("[A-Za-z_][A-Za-z0-9_$#-]*")) {
            throw new IllegalArgumentException("Invalid constant '" + name + "': expected a nuXmv symbolic identifier.");
        }
        return name;
    }


    public static org.petitparser.parser.Parser disjunctiveStringParser(List<String> allowed) {
        if(allowed == null || allowed.size() == 0){
            return FailureParser.withMessage("No variables of expected type.");
        }

        List<String> allowedSorted = new ArrayList<>(allowed);
        allowedSorted.sort(Comparator.comparingInt(String::length));
        Collections.reverse(allowedSorted);

        org.petitparser.parser.Parser parser = (StringParser.of(allowed.get(0)).seq(CharacterParser.word().not())).flatten();
        for (int i = 1; i < allowed.size(); i++) {
            //DOC: .seq(CharacterParser.word().not()) is added with each parser to allow for parsing when
            // one element in allowed is a prefix of another.
            parser = parser.or((StringParser.of(allowed.get(i)).seq(CharacterParser.word().not())).flatten());
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

    private static Parser assignmentValueParser(TypingContext expressionContext,
                                                AtomicReference<Type> assignmentType) throws Exception {
        Parser generalExpression = expressionParser(expressionContext);
        return new LazyParser<Type>(ignored -> {
            Type type = assignmentType.get();
            if (type instanceof Enum) {
                // Enum literals may also spell Boolean literals or values of another enum.
                // In-scope variables retain precedence over literal names.
                return expressionContext.variableParser().or(((Enum) type).valueParser())
                    .or(generalExpression).trim();
            }
            return generalExpression;
        }, null);
    }

    public static org.petitparser.parser.Parser assignmentListParser(TypingContext variableContext,
                                                                     TypingContext expressionContext) throws Exception {
        AtomicReference<Type> assignmentType = new AtomicReference<>();
        Parser assignmentValue = assignmentValueParser(expressionContext, assignmentType);
        Parser assignment =
                variableContext.variableParser().mapWithSideEffects((TypedVariable target) -> {
                            assignmentType.set(target.getType());
                            return target;
                        })
                        .seq(StringParser.of(":=").trim())
                        .seq(assignmentValue)
                        .map((List<Object> values) -> {
                            TypedVariable target = (TypedVariable) values.get(0);
                            Expression value = (Expression) values.get(2);
                            boolean numeric = target.getType() instanceof recipe.lang.types.Number
                                && value.getType() instanceof recipe.lang.types.Number;
                            // Numeric range/integrality is checked by process feasibility guards.
                            // Categorical mismatches cannot denote a well-typed assignment.
                            if (!numeric && !(target.getType().getClass().isAssignableFrom(value.getType().getClass())
                                    && value.isValidAssignmentFor(target))) {
                                throw new IllegalArgumentException("Invalid assignment to " + target.getName()
                                    + ": expected " + target.getType().name() + ", got " + value.getType().name());
                            }
                            try {
                                // Guard macros may contain free variables beyond their arguments.
                                value.relabel(reference -> {
                                    TypedVariable variable = (TypedVariable) reference;
                                    if (expressionContext.get(variable.getName()) == null) {
                                        throw new IllegalArgumentException("Variable " + variable.getName()
                                            + " is not available in assignment to " + target.getName());
                                    }
                                    return variable;
                                });
                            } catch (recipe.lang.utils.exceptions.RelabellingTypeException
                                    | recipe.lang.utils.exceptions.MismatchingTypeException e) {
                                throw new IllegalArgumentException("Cannot resolve assignment to " + target.getName(), e);
                            }
                            return new Pair(target.toString(), value);
                        });

        Parser assignmentList =
                assignment
                        .delimitedBy(CharacterParser.of(',').trim()).optional(new ArrayList<>())
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

    static org.petitparser.parser.Parser stringParser = word().plus().seq(CharacterParser.word().not()).flatten().trim();

    public static org.petitparser.parser.Parser variableNameParser(){
        return stringParser;
    }

    public static Parser typedVariableList(Parser typeParser){

        org.petitparser.parser.Parser typedVariable = stringParser
                .seq(CharacterParser.of(':').trim())
                .seq(typeParser).trim()
                .map((List<Object> values) -> {
                    if(List.class.isAssignableFrom(values.get(2).getClass())){
                        List<Type> types = new ArrayList<>();
                        for(String typeName : (List<String>) values.get(2)){
                            try {
                                types.add(Enum.getEnum(typeName));
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                        if(((List) values.get(2)).size() == 1){
                            return new TypedVariable((Type) types.get(0), (String) values.get(0));
                        } else{
                            UnionType type = new UnionType(types);
                            return new TypedVariable(type, (String) values.get(0));
                        }
                    }
                    else return new TypedVariable((Type) values.get(2), (String) values.get(0));
                });

        org.petitparser.parser.Parser typedVariableList = (typedVariable.separatedBy(CharacterParser.of(',').trim()))
                .map((List<Object> values) -> {
                    List<Object> delimitedTypedVariables = values;
                    List<TypedVariable> typedVariables = new ArrayList<>();
                    for (int i = 0; i < delimitedTypedVariables.size(); i += 2) {
                        TypedVariable typedVar = (TypedVariable) delimitedTypedVariables.get(i);
                        typedVariables.add(typedVar);
                    }
                    return typedVariables;
                });

        return typedVariableList;
    }

    public static Parser typedAssignmentList(TypingContext channelValueContext) throws Exception {
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
                                        enumValues.add(constantName(((String) v).trim()));
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

        org.petitparser.parser.Parser guardDefinitionListParser = (guardDefinitionParser).star()
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
                            vals.add(constantName((String) v));
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
        if(communicationContext.getVarType().size() > 0) {
            AtomicReference<Type> targetType = new AtomicReference<>();
            return labelledParser("relabel", (communicationContext.variableParser().trim()
                    .mapWithSideEffects((TypedVariable target) -> {
                        targetType.set(target.getType());
                        return target;
                    })
                    .seq(StringParser.of("<-").trim())
                    .seq(assignmentValueParser(localContext, targetType)).trim()).plus()
            ).trim().map((List<Object> values) -> {
                values.removeIf(v -> isWhitespace(v));
                Map<TypedVariable, Expression> relabellingMap = new HashMap<>();
                for (Object relabelObj : values) {
                    List relabel = (List) relabelObj;
                    TypedVariable target = (TypedVariable) relabel.get(0);
                    Expression value = (Expression) relabel.get(2);
                    boolean numeric = target.getType() instanceof recipe.lang.types.Number
                        && value.getType() instanceof recipe.lang.types.Number;
                    if (!numeric && !(target.getType().getClass().isAssignableFrom(value.getType().getClass())
                            && value.isValidAssignmentFor(target))) {
                        throw new IllegalArgumentException("Invalid relabelling for " + target.getName()
                            + ": expected " + target.getType().name() + ", got " + value.getType().name());
                    }
                    relabellingMap.put(target, value);
                }
                return relabellingMap;
            });
        } else{
            return labelledParser("relabel", StringParser.of("").map((Object values) -> {return new HashMap<>();})).optional(new HashMap<>());
        }
    }

    public static Parser receiveGuardParser(TypingContext localContext) throws Exception {
        TypingContext receiveGuardContext = TypingContext.union(localContext, new TypingContext());

        receiveGuardContext.set("channel", Enum.getEnum(Config.channelLabel));

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

            if(lhsEnum.name().equals(Config.channelLabel) && rhsEnum.name().equals(Config.channelLabel)){
                return true;
            }
        }
        return rhs.isValidAssignmentFor(new TypedVariable(lhs.getType(), "vv")) ||
                lhs.isValidAssignmentFor(new TypedVariable(rhs.getType(), "vv"));
    }
}