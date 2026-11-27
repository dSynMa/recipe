# ReCiPe

This is a repository for tooling to support the [ReCiPe formalism](https://arxiv.org/abs/2104.10998) for modelling and verification of reconfigurable multi-agent systems.

Currently it contains the [R-CHECK tool](https://arxiv.org/abs/2201.06312) that includes a language to specify reconfigurable multi-agent systems, and support for the model checking of these through the state-of-the-art model checker nuXmv, described better below.

## R-CHECK

### Language

The language allows the definition of a set of agents that interact synchronously. See `./example.rcp' for an example.

### LTL Model Checking

For model checking add specifications at the end of the script in the form of `LTLSPEC <spec>;`, where `<spec>` respects nuXmv syntax for LTL predicates. When making reference to the variables of an agent instance prefix them by the name of the agent in the following form `id-var`, similarly for their state: `id-state`, and any transition label.

A deadlock has `progress = FALSE` in the generated model. Use `SPEC F !PROGRESS;` to check that every execution eventually reaches a state with no enabled interaction. `keep-all` describes an unchanged transition and can also hold during an enabled self-loop.

When the agents use infinite-state variables the tool will use symbolic bounded model checking (using `msat`).

### Simulation

Simulation is performed using nuXmv, with a symbolic model.

Send labels (`instance-label`) denote whether that send is currently enabled, including the required receiver participation. They do not indicate execution. Receive labels are Boolean event flags: initially false, true after that receive, and cleared by the next step unless the same label occurs again. Supply/get labels are supported as an extension with the same event-flag convention. Repeated send labels combine their alternatives; a label cannot name both a send and an event action. This follows the send/receive distinction in [Model Checking Reconfigurable Interacting Systems](https://doi.org/10.1007/978-3-031-19759-8_23) and the send-enabledness explanation in [Language Support for Verifying Reconfigurable Interacting Systems, §6.1](https://www.lucadistefano.eu/papers/sttt2023rcheck.pdf).

### Compilation

This project was compiled and tested successfully with:
1. Java 15
2. [nuXmv 2.0.0](https://nuxmv.fbk.eu/)
3. Maven 4
4. NPM (to use frontend)

We suggest using the same or similar versions for compilation and running. Check pom.xml for the right versions.

For compilation to a single jar file simple run `mvn clean package`, and the jar file `target/recipe-0.1.jar` will be produced.


### Execution

We assume that the following commands are available from the environment PATH variable:
1. `nuxmv` (for analysis and simulation)
2. `npm' (for frontend)

nuXmv is resolved in this order:

1. the `RCHECK_NUXMV` environment variable, which may name a command or an absolute path;
2. `./nuxmv/bin/nuxmv` or `./bin/nuxmv` in the working directory, if present;
3. otherwise the bare command `nuxmv`, which the operating system resolves through `PATH`.

Alternatively, the folder downloaded from the nuXmv developer site can be renamed `nuxmv` and placed in `./nuxmv/bin/`.

Note also that the executable `/bin/nuXmv` should be renamed in small letters `/bin/nuxmv` to avoid any interruption due to case sensitivity.

For windows users: make sure that you can access the localhost and that your firewall does not block that. This is important if you want to use the GUI app, but in any case, the command line should work normally. 

After compilation, run the jar, e.g.`java -jar ./target/rcheck-0.1.jar` with the below arguments for the CLI functionality:

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
 -g,--gui           info: opens gui, (make sure 'npm install' is called in ./frontend-react before, and that ports 54044 and 3000 are free)
 ```
 
 For example, when you are in the main folder "rcheck" of the artefact submission, you can do the following:
 
 Go to https://nuxmv.fbk.eu and download nuXmv for command line of your operating system.
 You will get a folder and you should name it nuXmv and place it in the main folder ./rcheck
 
 Now you can execute the following commands to work with the GUI web interface for the first time, which will be served on http://localhost:3000:

```
 cd ./frontend-react
 npm install
 cd ..
```

Only execute the above commands once to build the web interface, and then you execute the following command to build the R-CHECK excutable:
 ```
 mvn clean
 mvn install
  ```

Then you can run the following to access R-CHECK's functionality through the web interface:
```
java -jar ./target/rcheck-0.1.jar -g
```

 If you want to work directly with the command line tool, you have to supply the file input file with `-i' and specify what you want to do with it.
 
For the simulation from the command line, you can specify a constraint or you can just type "TRUE" to see a possible execution. 

Example: 

```
java -jar ./target/rcheck-0.1.jar -i example.rcp -sim
``` 
