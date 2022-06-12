package recipe.lang.types;

import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.expressions.TypedValue;
import recipe.lang.utils.Parsing;

import java.util.*;

public class BoundedInteger extends Integer {
    private int min;
    private int max;

    public BoundedInteger(int min, int max){
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundedInteger that = (BoundedInteger) o;
        return min == that.min && max == that.max;
    }

    public org.petitparser.parser.Parser valueParser(){
        List<String> nums = new ArrayList<>();
        int i = min;
        while(i <= max){
            nums.add(i + "");
            i ++;
        }

        return Parsing.disjunctiveWordParser(nums, (String name) -> {
            try {
                return new TypedValue(this, name);
            } catch (MismatchingTypeException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public java.lang.Number interpret(String value) throws MismatchingTypeException {
        try{
            int val = java.lang.Integer.parseInt(value);
            if(val >= min && val <= max){
                return val;
            } else{
                throw new MismatchingTypeException(value + " is not of type " + name());
            }

        } catch (Exception e) {
            throw new MismatchingTypeException(value + " is not of type " + name());
        }
    }
    public Set<TypedValue> getAllValues() throws MismatchingTypeException {
        Set<TypedValue> values = new HashSet<>();
        for(int i = min; i < max; i++){
            values.add(new TypedValue<BoundedInteger>(this, "" + i));
        }
        return values;
    }
    @Override
    public String name() {
        return min + ".." + max;
    }
}
