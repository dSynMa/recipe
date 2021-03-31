package recipe.lang.process;

import org.petitparser.parser.Parser;
import org.petitparser.parser.combinators.SettableParser;
import org.petitparser.parser.primitive.CharacterParser;
import recipe.lang.agents.State;
import recipe.lang.agents.Transition;
import recipe.lang.utils.TypingContext;

import java.util.List;
import java.util.Set;

public abstract class Process {

    protected static int stateSeed = 0;
    public abstract Set<Transition> asTransitionSystem(State start, State end);

    public static Parser parser(TypingContext messageContext,
                                TypingContext localContext,
                                TypingContext communicationContext,
                                TypingContext channelContext){
        SettableParser parser = SettableParser.undefined();
        SettableParser basic = SettableParser.undefined();
        Parser choice = Choice.parser(basic);
        Parser sequence = Sequence.parser(basic);
        Parser receiveProcess = ReceiveProcess.parser(messageContext, localContext, channelContext);
        Parser sendProcess = SendProcess.parser(messageContext, localContext, communicationContext, channelContext);

        parser.set(choice
                    .or(sequence)
                    .or(basic));

        basic.set(receiveProcess
                .or(sendProcess)
                .or(CharacterParser.of('(').trim()
                    .seq(parser)
                    .seq(CharacterParser.of(')'))
                    .map((List<Object> values) -> values.get(1))));

        return parser;
    }
}
