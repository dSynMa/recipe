package recipe.analysis;

import recipe.lang.agents.*;
import recipe.lang.expressions.Expression;
import recipe.lang.expressions.predicate.Condition;
import recipe.lang.store.Store;
import recipe.lang.utils.Pair;

import java.util.*;
import java.util.function.Function;

public class Composition {
    Store store;
    Set<Transition<Pair<Condition, Map<String, Expression>>>> transitions;
}
