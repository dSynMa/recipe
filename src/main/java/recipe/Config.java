package recipe;

import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.types.Type;
import recipe.lang.types.UnionType;
import recipe.lang.utils.TypingContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public static final String locationLabel = "location";
    public static final String channelLabel = "channel";
    public static final String p2pLabel = "p2p";

    public static void reset(){
        agentEnumTypeNames.clear();
    }

    private static final List<String> agentEnumTypeNames = new ArrayList<>();
    private static final Map<String, Agent> agentEnumTypeNamesToAgent = new HashMap<>();
    private static UnionType agentType = new UnionType();

    public static UnionType getAgentType() throws Exception {
        return agentType;
    }

    public static List<String> getAgentTypeNames(){
        return agentEnumTypeNames;
    }

    public static Agent getAgent(String name){
        return agentEnumTypeNamesToAgent.get(name);
    }

    public static Agent getAgent(Type type){
        return agentEnumTypeNamesToAgent.get(type.toString());
    }

    public static void addAgentTypeName(String name, Agent agentTemplate) throws Exception {
        agentEnumTypeNames.add(name);
        agentEnumTypeNamesToAgent.put(name, agentTemplate);
        agentType.addType(recipe.lang.types.Enum.getEnum(name));
    }

    public static final String broadcast = "*";

    public static String getNuxmvPath() {
        String nuxmvPath = "";
        if(Files.exists(Path.of("./nuxmv/bin/nuxmv"))||Files.exists(Path.of("./nuxmv/bin/nuxmv.exe"))) {
            nuxmvPath = "./nuxmv/bin/nuxmv";
        } else if(Files.exists(Path.of("./bin/nuxmv"))||Files.exists(Path.of("./bin/nuxmv.exe"))) {
            nuxmvPath = "./bin/nuxmv";
        } else {
            nuxmvPath = "nuxmv";
        }

        return nuxmvPath;
    }

    public static TypingContext commVariableReferences(TypingContext commVariables){
        TypingContext commVariablesRefs = new TypingContext();
        commVariables.getVarType().entrySet().forEach(x -> commVariablesRefs.set("@" + x.getKey(), x.getValue()));
        return commVariablesRefs;
    }

    public static Map<String, Type> commVariableReferences(Map<String, Type> commVariables){
        Map<String, Type> commVariablesRefs = new HashMap<>();
        commVariables.entrySet().forEach(x -> commVariablesRefs.put("@" + x.getKey(), x.getValue()));
        return commVariablesRefs;
    }

    public static Boolean isCvRef(System system, String name){
        String nameWithOutRef = name.replaceAll("^@", "");
        return system.getCommunicationVariables().containsKey(nameWithOutRef);
    }
}

