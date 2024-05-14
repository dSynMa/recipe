package recipe.interpreter;

import java.util.Set;

import org.json.JSONObject;

import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.process.BasicProcessWithMessage;

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

    public Boolean satisfies(JSONObject constraint);
}