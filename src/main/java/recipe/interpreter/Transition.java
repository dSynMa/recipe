package recipe.interpreter;

import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.expressions.Expression;
import recipe.lang.process.BasicProcessWithMessage;
import recipe.lang.types.Type;

interface Transition {
    // Producer = either sender or supplier (the one that provides data)
    public AgentInstance getProducer();
    // Consumers = either receiver or getter (the one(s) that use data)
    public Set<AgentInstance> getConsumers();
    // Initiator = either sender or getter
    public AgentInstance getInitiator();
    public ProcessTransition getProducerTransition();
    public BasicProcessWithMessage getProducerProcess();
    public ProcessTransition findTransitionForAgent(AgentInstance instance);
    public void setProducer(AgentInstance instance, ProcessTransition transition) throws Exception;
    public void pushConsumer(AgentInstance instance, ProcessTransition transition) throws Exception;
    public JSONObject toJSON();

    // Returns all agents requiring data not provided by the producer
    public Set<AgentInstance> getUnhappyConsumers(Interpreter interpreter);

    public Boolean satisfies(JSONObject constraint);
    public Step next(Interpreter interpreter);

    public Expression<recipe.lang.types.Boolean> getSpecializedObservation(Map<String, Type> cvs, Expression<recipe.lang.types.Boolean> obs) throws Exception;
}