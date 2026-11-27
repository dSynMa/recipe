package recipe.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Small output syntax tree. Source-language typing and simplification stay in the source AST. */
final class Smv {
    private Smv() { }

    interface Expr {
        void appendTo(StringBuilder output);
    }

    private record Text(String text) implements Expr {
        private Text { Objects.requireNonNull(text); }
        public void appendTo(StringBuilder output) { output.append(text); }
    }

    private record Unary(String operator, Expr operand) implements Expr {
        public void appendTo(StringBuilder output) {
            output.append(operator).append('(');
            operand.appendTo(output);
            output.append(')');
        }
    }

    /** Every operand is grouped, including opaque expressions from the source AST. */
    private record Joined(String operator, List<Expr> operands) implements Expr {
        private Joined { operands = List.copyOf(operands); }
        public void appendTo(StringBuilder output) {
            for (int i = 0; i < operands.size(); i++) {
                if (i > 0) output.append(' ').append(operator).append(' ');
                output.append('(');
                operands.get(i).appendTo(output);
                output.append(')');
            }
        }
    }

    private record Conditional(Expr condition, Expr whenTrue, Expr whenFalse) implements Expr {
        public void appendTo(StringBuilder output) {
            output.append("case (");
            condition.appendTo(output);
            output.append(") : (");
            whenTrue.appendTo(output);
            output.append("); TRUE : (");
            whenFalse.appendTo(output);
            output.append("); esac");
        }
    }

    static Expr conditional(Expr condition, Expr whenTrue, Expr whenFalse) {
        return new Conditional(condition, whenTrue, whenFalse);
    }

    static final Expr TRUE = atom("TRUE");
    static final Expr FALSE = atom("FALSE");

    static Expr atom(String text) { return new Text(text); }

    static Expr local(String agent, String variable) { return atom(agent + "-" + variable); }
    static Expr state(String agent) { return local(agent, "automaton-state"); }
    static Expr next(Expr expression) { return new Unary("next", expression); }
    static Expr not(Expr expression) { return new Unary("!", expression); }
    static Expr eq(Expr left, Expr right) { return new Joined("=", List.of(left, right)); }

    static Expr and(Expr... expressions) { return and(Arrays.asList(expressions)); }
    static Expr and(List<Expr> expressions) { return join("&", expressions, TRUE); }
    static Expr or(Expr... expressions) { return or(Arrays.asList(expressions)); }
    static Expr or(List<Expr> expressions) { return join("|", expressions, FALSE); }

    private static Expr join(String operator, List<Expr> expressions, Expr empty) {
        if (expressions.isEmpty()) return empty;
        if (expressions.size() == 1) return expressions.get(0);
        return new Joined(operator, expressions);
    }

    static String render(Expr expression) {
        StringBuilder output = new StringBuilder();
        expression.appendTo(output);
        return output.toString();
    }

    /** Ordered module sections; expressions are rendered only at the output boundary. */
    static final class Module {
        private record Variable(String name, String type) { }
        private record Definition(String name, Expr expression) { }

        private final List<Variable> variables = new ArrayList<>();
        private final List<Definition> definitions = new ArrayList<>();
        private final List<Expr> initial = new ArrayList<>();
        private final Set<String> constants = new LinkedHashSet<>();
        private final List<String> specifications = new ArrayList<>();
        private Expr transition;

        void variable(String name, String type) { variables.add(new Variable(name, type)); }
        void define(String name, Expr expression) { definitions.add(new Definition(name, expression)); }
        void initially(Expr expression) { initial.add(expression); }
        void constants(List<String> values) { constants.addAll(values); }
        void comment(String text) { specifications.add("--" + text); }
        void specification(String expression) { specifications.add("LTLSPEC " + expression); }
        void transition(Expr expression) { transition = expression; }

        String render() {
            StringBuilder output = new StringBuilder("MODULE main\n");
            if (!variables.isEmpty()) output.append("VAR\n");
            for (Variable variable : variables)
                output.append('\t').append(variable.name()).append(" : ").append(variable.type()).append(";\n");
            if (!definitions.isEmpty()) output.append("DEFINE\n");
            for (Definition definition : definitions) {
                output.append('\t').append(definition.name()).append(" := ");
                definition.expression().appendTo(output);
                output.append(";\n");
            }
            if (!constants.isEmpty())
                output.append("CONSTANTS\n\t").append(String.join(", ", constants)).append(";\n");
            if (!initial.isEmpty()) {
                output.append("INIT\n");
                and(initial).appendTo(output);
                output.append('\n');
            }
            if (transition != null) {
                output.append("TRANS\n\t");
                transition.appendTo(output);
                output.append('\n');
            }
            output.append(String.join("\n", specifications));
            return output.toString();
        }
    }

}
