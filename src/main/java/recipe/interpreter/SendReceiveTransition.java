package recipe.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.process.BasicProcessWithMessage;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;

class SendReceiveTransition implements Transition {
    private AgentInstance sender;
    private ProcessTransition send;
    private Map<AgentInstance, ProcessTransition> receivers;

    public SendReceiveTransition() {
        receivers = new HashMap<AgentInstance, ProcessTransition>();
    }

    @Override
    public ProcessTransition findTransitionForAgent(AgentInstance instance) {
        if (instance.getLabel() == sender.getLabel()) {
            return send;
        }
        for (AgentInstance receiver : receivers.keySet()) {
            if (receiver.getLabel() == instance.getLabel()) {
                return receivers.get(receiver);
            }
        }
        // The agent did not take part in the transition
        return null;
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        List<String> receiverNames = new ArrayList<>(receivers.size());
        for (AgentInstance receiver : receivers.keySet()) {
            receiverNames.add(receiver.getLabel());
        }

        result.put("sender", sender.getLabel());
        result.put("send", send.getLabel().toString());
        result.put("receivers", receiverNames);
        return result;
    }

    @Override
    public void setProducer(AgentInstance instance, ProcessTransition transition) throws Exception {
        sender = instance;
        send = transition;
        if (!transition.getLabel().getClass().equals(SendProcess.class)) {
            throw new Exception("sender's transition must contain a SendProcess");
        }
    }

    public void pushConsumer(AgentInstance instance, ProcessTransition receive) throws Exception {
        if (!receive.getLabel().getClass().equals(ReceiveProcess.class)) {
            throw new Exception("receiver's transition must contain a ReceiveProcess");
        }
        receivers.put(instance, receive);
    }

    @Override
    public ProcessTransition getProducerTransition() {
        return send;
    }

    @Override
    public BasicProcessWithMessage getProducerProcess() {
        return (BasicProcessWithMessage) send.getLabel();
    }

    @Override
    public AgentInstance getProducer() {
        return sender;
    }

    @Override
    public Set<AgentInstance> getConsumers() {
        return receivers.keySet();
    }

    @Override
    public Boolean satisfies(JSONObject constraint) {
        return true;
    }

    @Override
    public AgentInstance getInitiator() {
        return sender;
    }
}