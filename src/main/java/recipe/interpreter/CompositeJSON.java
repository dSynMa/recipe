package recipe.interpreter;

import java.util.HashSet;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CompositeJSON extends JSONObject {
    private JSONObject base;
    private JSONObject diff;
    private Set<String> keys;

    @Override
    public JSONObject put(String key, Object value) throws JSONException {
        throw new JSONException("CompositeJSON must be immutable");
    }

    public JSONObject getDiff() { return diff; }

    public CompositeJSON(JSONObject oldJ, JSONObject newJ) {
        base = oldJ;
        diff = newJ;
        keys = this.keySet();
    }

    @Override
    public boolean has(String key) {
        return this.keys.contains(key);
    }

    @Override
    public boolean isEmpty() {
        return this.keys.isEmpty();
    }

    @Override
    public JSONArray names() {
        if (this.isEmpty()) {
            return null;
        }
        return new JSONArray(this.keys);
    }

    @Override
    public Object opt(String key) {
        Object oldObject = base.opt(key);
        Object newObject = diff.opt(key);
        if (newObject == null) return oldObject;
        else if (oldObject instanceof JSONObject && newObject instanceof JSONObject)
            return new CompositeJSON((JSONObject) oldObject, (JSONObject) newObject);
        else return newObject;
    }

    @Override
    public int length() {
        return this.keySet().size();
    }

    @Override
    public Set<String> keySet() {
        if (this.keys == null) {
            keys = new HashSet<>(diff.keySet());
            keys.addAll(base.keySet());
        }
        return this.keys;
    }

    @Override
    protected Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> result = new HashSet<>();
        for (String key : this.keySet()) {
            result.add(new SimpleImmutableEntry<String, Object>(key, this.opt(key)));
        }
        return result;
    }
}