package recipe.lang.agents.behaviour;

import org.petitparser.context.Result;
import org.petitparser.parser.Parser;

public abstract class RecipeParser {
    Parser parser;

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    public Parser getParser(){
        return parser;
    }

    public boolean parse(String s){
        Parser start = parser.end();
        Result r = start.parse(s);
        if(r.isFailure()) {
            System.out.println(r.getMessage());
            System.out.println("At position: " + r.getPosition());
        }

        return r.isSuccess();
    }

}
