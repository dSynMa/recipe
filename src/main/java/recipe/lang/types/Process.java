package recipe.lang.types;

import org.petitparser.parser.Parser;
import recipe.lang.utils.exceptions.InfiniteValueTypeException;
import recipe.lang.utils.exceptions.MismatchingTypeException;
import recipe.lang.utils.TypingContext;

import java.util.Set;

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
    public Set getAllValues() throws InfiniteValueTypeException {
        throw new InfiniteValueTypeException("Process does not have a finite set of values.");
    }
    @Override
    public Parser valueParser() throws Exception {
        return recipe.lang.process.Process.parser(messageContext, localContext, communicationContext);
    }
}
