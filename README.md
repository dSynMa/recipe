# ReCiPe

This is a repository for tooling to support the [ReCiPe formalism](https://arxiv.org/abs/2104.10998) for modelling and verification of reconfigurable multi-agent systems.

Currently it contains the [R-CHECK tool](https://arxiv.org/abs/2201.06312) that includes a language to specify reconfigurable multi-agent systems, and support for the model checking of these through the state-of-the-art model checker nuXmv, described better below.

## R-CHECK

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
1. Java 18
2. [nuXmv 2.0.0](https://nuxmv.fbk.eu/)
3. Maven 4

We suggest using the same or similar versions for compilation and running. Check `pom.xml` for the right versions.

For compilation to a single jar file simple run `mvn clean package`, and the jar file `target/recipe-0.1.jar` will be produced.


### Execution

We assume that the following command is available from the environment PATH variable:
1. `nuxmv` (for analysis and simulation)

Or for the case of nuxmv:

The folder you downloaded from nuXmv developer site should be renamed `nuxmv` be placed in `./nuxmv/bin/`

Note also that the executable `/bin/nuXmv` should be renamed in small letters `/bin/nuxmv` to avoid any interruption due to case sensitivity.

For windows users: make sure that you can access the localhost and that your firewall does not block that, in case the API server or nuXmv need to be reached over the network. 

After compilation, run the jar, e.g.`java -jar ./target/rcheck-0.1.jar` with the below arguments for the CLI functionality:

```
 -bmc,--bmc <arg>     info: bounded model checks input script file
                      args: bound (by default 10)
 -d,--dot             info: output agents DOT files
 -i,--input <arg>     info: input recipe script file
                      args: <recipe script>
 -mc,--mc             info: model checks input script file
 -n,--smv             info: output to smv file
 -p,--port <arg>      info: port the server will listen to (default 54044)
 -sim,--simulate      info: opens file in simulation mode
 -t,--threads <arg>   info: how many threads to use in model-checking
                      (default=num of hw threads) ```

To run the API server (used by the R-CHECK tooling, without a GUI) use:
```
java -jar ./target/rcheck-0.1.jar -a
```

 If you want to work directly with the command line tool, you have to supply the file input file with `-i' and specify what you want to do with it.
 
For the simulation from the command line, you can specify a constraint or you can just type "TRUE" to see a possible execution. 

Example: 

```
java -jar ./target/rcheck-0.1.jar -i example.rcp -sim
``` 
