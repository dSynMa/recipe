# recipe

### Language

The language allows the definition of a set of agents that interact synchronously. Find an example of the language in ./example-current-syntax.txt.

### LTL Model Checking

For model checking add specifications at the end of the script in the form of `LTLSPEC <spec>;`. When making reference to the variables of an agent instance prefix them by the name of the agent in the following form `id-var`, similarly for their state: `id-state`, and any transition label.

To check whether the system is stuck, the predicate `keep-all` can be queried, e.g. `F keep-all` will be true if the system eventually settles in a state (where from then on no variable values may change).

When the agents use infinite-state variables the tool will use symbolic bounded model checking (using `msat`).

### Compilation

This project was compiled and tested successfully with:
1. Java 15
2. nuXmv 2.0.0
3. python 3.9.7 (for the frontend)
4. Maven 4

We suggest using the same or similar versions for compilation and running.

The project is implemented in Java, and can be compiled using Maven (e.g., from the command line run `mvn clean install` in the root directory). 

### Execution

We assume that the following commands are available from the environment PATH variable:
1. `nuxmv` (for analysis and simulation)
2. `python3` (for the frontend).

After compilation, run the jar, e.g.`java -jar recipe.jar` with the below options:

```
-d,--dot              info: output agents DOT files

-f,--frontend <arg>   info: opens front end and server on given ports
                      args: <server-port>,<frontend-port>

-i,--input <arg>      info: input recipe script file
                      args: <recipe script>

-mc,--mc              info: model checks input script file

-n,--smv              info: output to smv file

-s,--server <arg>     info: open server on given port
                      args: <port>

-sim,--simulate       info: opens file in simulation mode
```