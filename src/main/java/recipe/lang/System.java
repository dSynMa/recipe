package recipe.lang;

import recipe.lang.agents.Agenttt;

import java.util.Set;

public class System<Label, TypedLabel, LocalState> {
    Set<Label> channels;
    Set<TypedLabel> communicationVariables;
    Set<TypedLabel> messageAttributes;
    Set<Agenttt<TypedLabel, LocalState>> agents;
}
