package recipe.lang.types;

import org.petitparser.parser.Parser;
import org.petitparser.parser.primitive.FailureParser;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.petitparser.parser.combinators.ChoiceParser;

public class UnionType extends Type {
    public List<Type> getTypes() {
        return types;
    }

    List<Type> types;

    public UnionType() {
        this.types = new ArrayList<>();
    }

    public UnionType(List<Type> types) {
        this.types = types;
    }

    public void addType(Type type){
        types.add(type);
    }

    @Override
    public Object interpret(String value) throws MismatchingTypeException {
        String exceptionMessage = "";
        for(Type type : types) {
            try {
                return type.valueParser().parse(value);
            } catch (Exception e) {
                exceptionMessage += e.getMessage() + "\n\n";
            }
        }
        throw new MismatchingTypeException(exceptionMessage);
    }

    @Override
    public String name() {
        List<String> names = new ArrayList<>();
        for(Type type : types) {
            names.add(type.name());
        }
        return String.join(" | ", names);
    }
    public Set getAllValues() throws InfiniteValueTypeException, MismatchingTypeException {
        Set<Object> values = new HashSet<>();
        for(Type type : types) {
            values.addAll(type.getAllValues());
        }
        return values;
    }
    @Override
    public Parser valueParser() throws Exception {
        if(types.size() == 0) {
            return FailureParser.withMessage("There are no agents declared.");
        }

        Parser[] parsers = new Parser[types.size()];

        for(int i = 0; i < types.size(); i++) {
            parsers[i] = types.get(i).valueParser();
        }
        Parser parser = new ChoiceParser(parsers);

        parser = parser.map((List<Type> values) -> {
            if(values.size() != 1) return null;
            else{
                return values.get(1);
            }
        });

        return parser;
    }
}
