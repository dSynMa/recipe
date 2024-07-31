package recipe.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import recipe.Config;
import recipe.analysis.ToNuXmv;
import recipe.lang.agents.AgentInstance;
import recipe.lang.agents.ProcessTransition;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.TypedValue;
import recipe.lang.process.BasicProcessWithMessage;
import recipe.lang.process.ReceiveProcess;
import recipe.lang.process.SendProcess;
import recipe.lang.types.Type;


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

    @Override
    public Expression<recipe.lang.types.Boolean> getSpecializedObservation(Map<String, Type> cvs, Expression<recipe.lang.types.Boolean> obs) throws Exception {
        Type agentType = Config.getAgentType();
        TypedValue<Type> producerTV = new TypedValue<Type>(agentType, getProducer().getLabel());
        SendProcess sendProcess = (SendProcess) getProducerProcess();
        
        Expression<recipe.lang.types.Boolean> msgGuard = sendProcess.getMessageGuard().relabel(v -> {
                return cvs.containsKey("@" + v.getName())
                    ? v
                    : v.sameTypeWithName(getProducer().getLabel() + "-" + v);
        }).simplify();

        return ToNuXmv.specialiseObservationToSendTransition(
            cvs, obs, msgGuard, sendProcess.getMessage(), producerTV, Config.getNoAgent(),  sendProcess.getChannel());
    }
}