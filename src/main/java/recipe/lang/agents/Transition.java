package recipe.lang.agents;

import recipe.lang.process.Process;

public class Transition {
    // three attributes of transition: source,action,destination
    private String agent;
    private String source;
    private String destination;
    private Process action;

    public Transition() {
      
    }

    public Transition(String agent, String source, String destination, Process action) {
        this.agent=agent;
        this.source = source;
        this.destination = destination;
        this.action = action;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Process getAction() {
        return action;
    }

    public void setAction(Process action) {
        this.action = action;
    }

    // rewrite toString function
    public String toString() {
        return this.getSource() + "        -" + this.getAction() + "->       " + this.getDestination();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((destination == null) ? 0 : destination.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Transition other = (Transition) obj;
        if (action == null) {
            if (other.action != null)
                return false;
        } else if (!action.equals(other.action))
            return false;
        if (destination == null) {
            if (other.destination != null)
                return false;
        } else if (!destination.equals(other.destination))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        return true;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

}
