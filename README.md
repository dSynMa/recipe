# R-CHECK

### Language

The language allows the definition of a set of agents that interact synchronously. See `./example.rcp' for an example.

### LTL Model Checking

For model checking add specifications at the end of the script in the form of `LTLSPEC <spec>;`, where `<spec>` respects nuXmv syntax for LTL predicates. When making reference to the variables of an agent instance prefix them by the name of the agent in the following form `id-var`, similarly for their state: `id-state`, and any transition label.

To check whether the system is stuck, the predicate `keep-all` can be queried, e.g. `F keep-all` will be true if the system eventually settles in a state (where from then on no variable values may change).

When the agents use infinite-state variables the tool will use symbolic bounded model checking (using `msat`).

### Simulation

Simulation is performed using nuXmv, with a symbolic model.

Simulation will show changes in variables in the next state only, with the exception of receive label variables which are only shown when they are true. Send labels do not appear in the simulation output since they are defined as nuXmv predicate definitions rather than as variables.

### Compilation

This project was compiled and tested successfully with:
1. Java 15
2. [nuXmv 2.0.0](https://nuxmv.fbk.eu/)
3. python 3.9.7 (for the frontend)
4. Maven 4

We suggest using the same or similar versions for compilation and running.

For compilation to a single jar file simple run `mvn clean package`, and the jar file `target/recipe-0.1.jar` will be produced.


### Execution

We assume that the following commands are available from the environment PATH variable:
1. `nuxmv` (for analysis and simulation)
2. `python3` (for the GUI).

`nuxmv` can also be placed in `./nuxmv/bin/` or in `./bin/`.

After compilation, run the jar, e.g.`java -jar ./target/recipe-0.1.jar` with the below arguments for the CLI functionality:

```
 -i,--input <arg>   info: input recipe script file
 -d,--dot           info: output agents DOT files
 -n,--smv           info: output to smv file
                    args: <recipe script>
 -mc,--mc           info: model checks input script file
 -bmc,--bmc <arg>   info: bounded model checks input script file
                    args: bound (by default 10)
 -sim,--simulate    info: opens file in simulation mode
 ```

and with the following to run the GUI app:
```
 -g,--gui           info: opens gui
 ```