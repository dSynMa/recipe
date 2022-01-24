package recipe.lang.types;

import org.petitparser.parser.Parser;
import recipe.lang.exception.MismatchingTypeException;
import recipe.lang.utils.TypingContext;

public class Process extends Type {
    TypingContext messageContext;
    TypingContext localContext;
    TypingContext communicationContext;

    public Process(TypingContext messageContext,
                   TypingContext localContext,
                   TypingContext communicationContext){
        this.messageContext = messageContext;
        this.localContext = localContext;
        this.communicationContext = communicationContext;
    }

    @Override
    public Object interpret(String value) throws MismatchingTypeException {
        try {
            return this.valueParser().parse(value);
        } catch (Exception e) {
            throw new MismatchingTypeException(e.getMessage());
        }
    }

    @Override
    public String name() {
        return "process";
    }

    @Override
    public Parser valueParser() throws Exception {
        return recipe.lang.process.Process.parser(messageContext, localContext, communicationContext);
    }
}
