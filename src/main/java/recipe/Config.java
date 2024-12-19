package recipe;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import recipe.lang.System;
import recipe.lang.agents.Agent;
import recipe.lang.expressions.TypedValue;
import recipe.lang.types.Type;
import recipe.lang.types.UnionType;
import recipe.lang.utils.TypingContext;
import recipe.lang.utils.exceptions.MismatchingTypeException;

public class Config {
    public static final String locationLabel = "location";
    public static final String channelLabel = "channel";
    public static final String chanLabel = "chan";
    public static final String p2pLabel = "p2p";
    public static final String myselfKeyword = "myself";
    public static final String noAgentString = "no-agent";
    public static String rcheckPath = "rcheck/bin/cli.js";

    protected static TypedValue noAgent = null;

    public static void reset(){
        agentEnumTypeNames.clear();
    }

    public static String getRcheckPath() {
            String path = Config.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            try {
                String decodedPath = URLDecoder.decode(path, "UTF-8");
                String parent = Path.of(decodedPath).getParent().toString();
                Path file = Path.of(parent, "cli.js");
                if (Files.exists(file)) {
                    return file.toString();
                }
                else {
                    return rcheckPath;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }

    }

    private static final List<String> agentEnumTypeNames = new ArrayList<>();
    private static final Map<String, Agent> agentEnumTypeNamesToAgent = new HashMap<>();
    private static UnionType agentType = new UnionType();

    public static UnionType getAgentType() throws Exception {
        return agentType;
    }

    public static TypedValue getNoAgent() throws MismatchingTypeException {
        if (noAgent == null) {
            noAgent = new TypedValue(agentType, noAgentString);
        }
        return noAgent;
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

