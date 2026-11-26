import './App.css';
import { Container, Table, Dropdown, Spinner, FormControl, Row, Col, Tab, Tabs, Button, Form, InputGroup, ButtonGroup, ToggleButton, Badge, Navbar, OverlayTrigger, Tooltip, Modal, Nav } from 'react-bootstrap';
import AceEditor from "react-ace";
import React, { useEffect, useState, useRef } from 'react';
import axios from "axios";
import { useFetch } from "./hooks";
import Graph from "./Graph";

import "./mode-recipe";
import "ace-builds/src-noconflict/theme-tomorrow";

const server = 'http://localhost:54044';

const spinner = <span>&nbsp;<Spinner
  as="span"
  animation="border"
  size="sm"
  role="status"
  aria-hidden="true"
  style={{}}
/></span>;

const Bg = {
  background: "whitesmoke",
  minHeight: "400px",
  width: "100%",
  overflow: "auto",
  paddingTop: "10px"
}

const TabStyle = {
  minHeight: "450px",
  width: "100%",
  overflow: "auto",
}

const SVGBg = {
  float: "left",
  display: "contents",
  overflow: "auto"
}


function App() {
  const [simcondition, setSimCondition] = useState("TRUE");
  const [simstarted, setSimStarted] = useState(false);
  const [simresponse, setSimResponse] = useState([]);
  const [mcresponse, setMCResponse] = useState([]);
  const [bound, setBound] = useState(-1);
  const [vloading, setVLoading] = useState(false);
  const [bloading, setBLoading] = useState(false);
  const [cancellingBuild, setCancellingBuild] = useState(false);
  const activeBuild = useRef(null);
  const [mcloading, setMCLoading] = useState(false);
  const [mcstopping, setMCStopping] = useState(false);
  const [simloading, setSimLoading] = useState(false);
  const [resetsimloading, setResetSimLoading] = useState(false);
  const [symbolicBuild, setSymbolicBuild] = useState(true);
  const [built, setBuilt] = useState(false);
  const [code, setCode] = useFetch("/example.rcp");
  const [radioValue, setRadioValue] = useState('1');
  const [dot, setDot] = useState([]);


  const [interpreterstarted, setInterpreterStarted] = useState(false);
  const [interpreterresponse, setInterpreterResponse] = useState([]);
  const [interpretertransitions, setInterpreterTransitions] = useState([]);
  const [interpreterloading, setInterpreterLoading] = useState(false);
  const [interpreterbacking, setInterpreterBacking] = useState(false);
  const [resetinterpreterloading, setResetInterpreterLoading] = useState(false);
  const [interpreternextindex, setInterpreterNextIndex] = useState(0);
  const [interpreterbadge, setInterpreterBadge] = useState(false);

  const [traceLoading, setTraceLoading] = useState(false);
  const busy = bloading || mcloading || mcstopping || simloading || interpreterloading
    || interpreterbacking || traceLoading || vloading;

  const [confirmBuild, setConfirmBuild] = useState(false);

  const radios = [
    { name: 'MC', value: '1' },
    { name: 'IC3', value: '2' },
    { name: 'BMC', value: '3' },
  ];

  const interpreterTable = useRef(null);

  function simulate() {
    if (busy) return;
    if (built != true) {
      alert("Build model first.");
      return;
    }

    setSimLoading(true);

    const params = new URLSearchParams();
    params.append('reset', !simstarted);
    params.append('constraint', simcondition);

    axios.get(server + "/simulateNext", { params })
      .then((response) => {
        console.log(JSON.stringify(response.data.svgs));
        var res = response.data;
        console.log("svgs");
        console.log((res.svgs));
        setDot([]);
        setDot(res.svgs.map(x => {
          var svg = new DOMParser().parseFromString(x.svg, "image/svg+xml").getElementsByTagNameNS("http://www.w3.org/2000/svg", "svg").item(0);
          console.log("svg: " + svg);
          return svg;
        }));
        delete res.svgs;
        setSimResponse(simresponse.concat([].concat(res)));
        console.log(simresponse);
        setSimLoading(false);
        setSimStarted(true);
      })
      .catch((err) => {
        alert(err.message);
        setSimLoading(false);
      });
  }

  function resetSimulate() {
    setResetSimLoading(true);

    setSimStarted(false);
    setSimResponse([]);

    setResetSimLoading(false);
  }

  function renderStep(x) {
    if (x.depth === 0) { return x.state; }
    var last = interpreterresponse[x.depth - 1];

    var render = {};
    Object.keys(x.state).forEach(agent => {

      render[agent] = {};
      Object.keys(x.state[agent]).forEach(k => {
        if (
          // Ignore all metadata except **state**
          (k === "**state**" || !k.startsWith("**")) &&
          // Only add variables that have changed
          x.state[agent][k] != last.state[agent][k]
        ) {
          render[agent][k] = x.state[agent][k];
        }
      });
      console.log(agent, render[agent]);
      if (Object.keys(render[agent]).length === 0) { delete render[agent]; }
    });
    return render;
  }

  function formatStep(render) {
    return (
      <table>
        {
          Object.keys(render).sort().map(agent => {
            return (<tr key={agent}>
              <td>{agent}:</td>
              <td> {
                Object.keys(render[agent]).sort().map((k, index) => {
                  return <span>
                    {/* {index > 0 && ', '} */}
                    {(k === "**state**") ? <em>state</em> : !k.startsWith("**") && k}
                    {(k === "**state**" || !k.startsWith("**")) && ": "}
                    {(k === "**state**" || !k.startsWith("**")) && render[agent][k]}
                    {(k === "**state**" || !k.startsWith("**")) && index < Object.keys(render[agent]).length - 1 && ', '}
                  </span>
                })}</td>
            </tr>)
          })
        }
      </table>
    )
  }

  // Needed by the Import button
  // const importFile = useRef(null);
  function loadJSONIntoInterpreter(file) {
    // var formData = new FormData();
    // formData.append("trace", file);
    const reader = new FileReader();
    reader.onload = async (e) => {
      const params = new URLSearchParams();
      params.append("trace", e.target.result);
      axios.get(server + "/interpretLoadJSON", { params })
        .then((response) => { console.log(response) })
        .catch((error) => { console.log(error) });
    };
    reader.readAsText(file);
  }

  function loadTraceIntoInterpreter(output) {
    if (busy) return;
    setTraceLoading(true);
    const params = new URLSearchParams();
    params.append("output", output);
    axios
      .get(server + "/interpretLoad", { params })
      .then((response) => {
        var trace = response.data.trace;
        if (response.data.error != undefined) {
          alert(response.data.error);
        }
        if (trace != undefined) {
          setDot([]);
          setDot(response.data.svgs.map(x => {
            var svg = new DOMParser().parseFromString(x.svg, "image/svg+xml").getElementsByTagNameNS("http://www.w3.org/2000/svg", "svg").item(0);
            return svg;
          }));
          setInterpreterTransitions(trace[trace.length - 1].transitions);
          setInterpreterResponse(trace);
          alert("Counterexample has been loaded in the Interpreter tab.");
          setInterpreterBadge(true);
          setInterpreterStarted(true);
        }
      })
      .catch((err) => {
        alert(err.message);
      })
      .finally(() => setTraceLoading(false));
  }


  function exportRaw(output) {
    const exportTxt = `data:text/plain;chatset=utf-8,${encodeURIComponent(
      output
    )}`;
    const link = document.createElement("a");
    link.href = exportTxt;
    link.download = "output.txt";
    link.click();
  }

  const exportData = () => {
    const jsonString = `data:text/json;chatset=utf-8,${encodeURIComponent(
      JSON.stringify(interpreterresponse)
    )}`;
    const link = document.createElement("a");
    link.href = jsonString;
    link.download = "trace.json";
    link.click();
  };


  function formatTransition(t) {
    const isSupplyGet = t.hasOwnProperty("___get-supply___");
    return t === undefined ? "" : (<table>
      <tr><td><em>{isSupplyGet ? "Supplier" : "Sender"}: </em></td><td>{t.sender}</td></tr>
      <tr><td><em>Command: </em></td><td>{t.send}</td></tr>
      <tr><td><em>{isSupplyGet ? "Getter" : "Receivers"}: </em></td><td>{t.receivers.join(", ")}</td></tr>
    </table>)
  }

  function interpret() {
    if (busy) return;
    if (built != true) buildModel();
    setInterpreterLoading(true);
  }

  useEffect(() => {
    if (built && interpreterloading) {
      const params = new URLSearchParams();
      params.append('reset', !interpreterstarted);
      params.append('index', interpreternextindex || 0);

      // var url;
      axios.get(server + "/interpretNext", { params })
        .then((response) => {
          if (response.data.hasOwnProperty("error")) {
            alert(response.data.error);
            setInterpreterLoading(false);
            setInterpreterStarted(false);
            setInterpreterTransitions([]);
          } else {
            var res = response.data;
            setInterpreterTransitions(response.data.transitions);
            setInterpreterNextIndex(0);
            // Handle SVGs
            setDot([]);
            setDot(res.svgs.map(x => {
              var svg = new DOMParser().parseFromString(x.svg, "image/svg+xml").getElementsByTagNameNS("http://www.w3.org/2000/svg", "svg").item(0);
              return svg;
            }));
            delete res.svgs;
            setInterpreterResponse(interpreterresponse.concat([res]));
            setInterpreterLoading(false);
            setInterpreterStarted(true);
            interpreterTable.current.scrollIntoView({ behavior: "smooth", block: "end" });
          }
        })
        .catch((err) => {
          alert(err.message);
          setInterpreterLoading(false);
        });
    }
  }, [built, interpreterloading]);

  function backtrackInterpreter() {
    if (interpreterresponse.length <= 1) {
      resetInterpreter();
    }
    else {
      setInterpreterBacking(true);
      axios.get(server + "/interpretBack", {})
        .then((response) => {
          var res = response.data.state;
          setInterpreterTransitions(response.data.transitions);
          setInterpreterNextIndex(0);
          setInterpreterResponse(interpreterresponse.slice(0, -1));
          setInterpreterBacking(false);
          setDot([]);
          setDot(response.data.svgs.map(x => {
            var svg = new DOMParser().parseFromString(x.svg, "image/svg+xml").getElementsByTagNameNS("http://www.w3.org/2000/svg", "svg").item(0);
            return svg;
          }));
        })
        .catch((err) => {
          alert(err.message);
          setInterpreterBacking(false);
        });
    }
  }

  function resetInterpreter() {
    setResetInterpreterLoading(true);
    setInterpreterStarted(false);
    setInterpreterResponse([]);
    setInterpreterTransitions([]);
    setInterpreterNextIndex("");
    setDot([]);

    setResetInterpreterLoading(false);
  }

  function modelCheckStop() {
    setMCStopping(true);
    axios.get(server + "/modelCheckStop")
      .catch((err) => alert(err.message))
      .finally(() => setMCStopping(false));
    // Keep the interface busy until all outstanding check requests have settled.
  }

  function modelCheck() {
    if (busy) return;
    // The "true" parameter forces to build a BDD model if "MC" is selected
    if (built != true) buildModel(true);

    setMCResponse([]);
    //This will trigger the effect right after this function
    setMCLoading(true);
  }

  useEffect(() => {
    if (built && mcloading) {
      if (symbolicBuild && radioValue == 1) {
        alert("Cannot explicitly model check with abstract model. Build explicit model.");
        setMCLoading(false);
      } else if (!symbolicBuild && radioValue > 1) {
        alert("Cannot use ic3 or bmc model checking with explicit model. Build abstract model.");
        setMCLoading(false);
      } else {
        const params = new URLSearchParams();
        if (radioValue == 2) {
          params.append('ic3', true);
          if (bound > -1) {
            params.append('bound', bound);
          }
        } else if (radioValue == 3) {
          params.append('bmc', true);
          params.append('bound', bound);
        }

        axios.get(server + "/modelCheck", { params })
          .then(async (response) => {
            console.log(response.data);
            if (response.data.error !== undefined) {
              alert(response.data.error);
            }
            else if (response.data !== undefined) {
              setMCResponse(response.data.results);
              await Promise.allSettled(response.data.results.map((x) => modelCheckSingle(x)));
            }
            setMCLoading(false);
          })
          .catch((err) => {
            alert(err.message);
            setMCLoading(false);
          });
      }
    }
  }, [built, mcloading]);

  function modelCheckSingle(data) {
    return axios.get(server + data.url).then((response) => {
      setMCResponse(oldmcresponse =>
        oldmcresponse.map((x, i) => {
          return (i == data.id) ? response.data : x;
        }));
    }).catch((err) => {
      setMCResponse(old => old.map((x, i) => i === data.id ? { error: err.message } : x));
    });
  }

  function visualise() {
    setVLoading(true);
    // The build already selected the model. Selecting it again would discard
    // any interpreter or model-check session started by the built effect.
    axios.get(server + "/toDOTSVG")
      .then((response) => {
        setDot(response.data.map(x => {
          return new DOMParser().parseFromString(x.svg, "image/svg+xml")
            .getElementsByTagNameNS("http://www.w3.org/2000/svg", "svg").item(0);
        }));
        setVLoading(false);
      })
      .catch((err) => {
        alert(err.message);
        setVLoading(false);
      });
  }

  const handleClose = () => setConfirmBuild(false);
  function confirmBuildModel() {
    if (busy) return;
    if (built) {
      setConfirmBuild(true);
    } else {
      buildModel();
    }
  }

  function stopBuild(job) {
    return axios.get(server + "/cancelBuild", { params: { buildId: job.id } })
      .then(({ data }) => { if (data.error) throw new Error(data.error); })
      .catch(error => {
        alert("Could not cancel build: " + error.message);
        setCancellingBuild(false);
      });
  }

  function cancelBuild() {
    const job = activeBuild.current;
    if (!job || cancellingBuild) return;
    job.cancelled = true;
    setCancellingBuild(true);
    if (job.id) job.stopRequest = stopBuild(job);
  }

  async function buildModel(obeyMCpane = false) {
    if (busy || activeBuild.current) return;
    const job = { cancelled: false };
    activeBuild.current = job;
    setBLoading(true);
    setBuilt(false);
    setConfirmBuild(false);
    setInterpreterBadge(false);
    setMCResponse([]);
    setBound(-1);
    resetInterpreter();
    resetSimulate();
    try {
      const prepared = await axios.get(server + "/prepareBuild");
      if (!prepared.data.buildId) throw new Error(prepared.data.error || "Could not start build");
      job.id = prepared.data.buildId;
      if (job.cancelled) {
        job.stopRequest = stopBuild(job);
        return;
      }
      const params = new URLSearchParams({
        script: code, symbolic: obeyMCpane ? radioValue != 1 : symbolicBuild, buildId: job.id,
      });
      const { data } = await axios.get(server + "/buildModel", { params });
      if (job.cancelled) return;
      if (!data.success) throw new Error(data.error || "Could not build model");
      setBuilt(true);
      visualise();
      alert("Model built successfully");
    } catch (error) {
      if (!job.cancelled) alert(error.message);
      setMCLoading(false);
      setInterpreterLoading(false);
    } finally {
      if (job.cancelled) {
        await job.stopRequest;
        setMCLoading(false);
        setInterpreterLoading(false);
      }
      activeBuild.current = null;
      setCancellingBuild(false);
      setBLoading(false);
    }
  }

  const cancelBuildButton = (
    <Button variant="outline-danger" size="lg" onClick={cancelBuild} disabled={cancellingBuild}>
      {cancellingBuild ? "Cancelling build…" : "Cancel build"} {spinner}
    </Button>
  );

  return (
    <div className="App">
      <Modal show={confirmBuild} onHide={handleClose}>
        <Modal.Header closeButton>
          <Modal.Title>Warning</Modal.Title>
        </Modal.Header>
        <Modal.Body>Building replaces the active model and clears its checking results, simulation, and interpreter trace. Do you want to proceed?</Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleClose}>
            No
          </Button>
          <Button variant="primary" onClick={() => buildModel()} disabled={busy}>
            Yes
          </Button>
        </Modal.Footer>
      </Modal>
      <Container fluid className='text-center' id="TabContainer">
        <Row className="justify-content-md-center">
          <Col xs={12} xl={10} xxl={8}>
            <Navbar expand="xs">
              <Navbar.Brand>R-CHECK</Navbar.Brand>
              <Nav className="justify-content-end">
                <Nav.Link href="https://github.com/dsynMa/recipe">GitHub</Nav.Link>
              </Nav>
            </Navbar>
          </Col>
        </Row>
        <Row className="justify-content-md-center">
          <Col xs={12} xl={10} xxl={8}>
            <Tabs defaultActiveKey="ed" id="uncontrolled-tab-example" className="mb-3"
              onSelect={(e) => { if (e === "interpreter") setInterpreterBadge(false); }}>
              <Tab role="tabpanel" eventKey="ed" title="Editor" style={TabStyle}>
                <AceEditor style={Bg}
                  mode="recipe"
                  theme="tomorrow"
                  fontSize="15px"
                  wrapEnabled
                  highlightActiveLine
                  focus
                  height="400px"
                  name="UNIQUE_ID_OF_DIV"
                  editorProps={{ $blockScrolling: true }}
                  value={code}
                  onChange={(v) => setCode(v)}
                />
                <InputGroup>
                  <Form.Select aria-label="1"
                    onChange={(e) => { setSymbolicBuild(e.target.value === "smt") }}>
                    <option value="smt">SMT model (allows for infinite-state verification)</option>
                    <option value="bdd">BDD model (only for finite-state verification)</option>
                  </Form.Select>
                  {bloading ? cancelBuildButton : <Button variant="primary" size="lg" onClick={confirmBuildModel} disabled={busy}>
                    Build model
                  </Button>}
                </InputGroup>
              </Tab>
              <Tab role="tabpanel" eventKey="mc" title="Model Checker" style={TabStyle}>
                <Container fluid>
                  <Row>
                    <Col xs={12}>
                      <InputGroup className="mb-3">
                        <InputGroup.Text id="basic-addon1">Type</InputGroup.Text>
                        <ButtonGroup>
                          {radios.map((radio, idx) => (
                            <ToggleButton
                              key={idx}
                              size="lg"
                              id={`radio-${idx}`}
                              type="radio"
                              variant={idx % 2 ? 'outline-success' : 'outline-danger'}
                              name="radio"
                              value={radio.value}
                              checked={radioValue === radio.value}
                              onChange={(e) => setRadioValue(e.currentTarget.value)}
                            >
                              {radio.name}
                            </ToggleButton>
                          ))}
                        </ButtonGroup>
                        {(radioValue != 1) &&
                          <FormControl
                            size="lg"
                            placeholder="Enter bound (optional for IC3)."
                            aria-label="bound"
                            aria-describedby="basic-addon1"
                            onEmptied={() => setBound(-1)}
                            onChange={(e) => { setBound(e.currentTarget.value.trim()) }}
                          />
                        }
                        {bloading ? cancelBuildButton : <Button variant="primary" size="lg" disabled={busy} onClick={modelCheck}>
                          Start {mcloading && spinner}
                        </Button>}
                        <Button variant="danger" size="lg" disabled={bloading || !mcloading || mcstopping} onClick={modelCheckStop}>
                          Stop
                        </Button>
                      </InputGroup>

                    </Col>
                  </Row>
                  {mcresponse && mcresponse.map((x, i) => {
                    return <Row className={i % 2 ? "border py-2" : "bg-light border py-2"}>
                      <Col style={{ textAlign: "start" }} xs={x.result == "false" ? 9 : 12}>
                        <h5 className='my-auto'>{x.spec}{' '}
                          {x.result != "error" && x.result != "unknown" &&
                            <Badge bg={x.result == "true" ? "success" : x.result == "false" ? "danger" : "secondary"}>
                              {x.result == "true" ? "pass" : x.result == "false" ? "fail" : x.result}
                            </Badge>
                          }
                          {(x.result == "error" || x.result == "unknown") &&
                            <OverlayTrigger placement='bottom' overlay={<Tooltip>{x.output}</Tooltip>}>
                              <Badge bg={"secondary"}>{x.result}</Badge>
                            </OverlayTrigger>
                          }
                          {(x.result === undefined) && spinner}
                        </h5>
                      </Col>
                      {x.result == "false" &&
                        <Col xs={3} style={{ textAlign: "end" }} className="align-self-center">
                          {/* <div className="d-grid"> */}
                          <Dropdown as={ButtonGroup}>
                            <Button disabled={busy} onClick={() => loadTraceIntoInterpreter(mcresponse[i].output)}>
                              Load into interpreter
                            </Button>
                            <Dropdown.Toggle split id="dropdown-split-basic" />
                            <Dropdown.Menu>
                              <Dropdown.Item disabled={busy} onClick={() => loadTraceIntoInterpreter(mcresponse[i].output)}>Load into interpreter</Dropdown.Item>
                              <Dropdown.Item onClick={() => exportRaw(mcresponse[i].output)}>Download raw output</Dropdown.Item>
                            </Dropdown.Menu>
                          </Dropdown>
                        </Col>
                      }
                    </Row>
                  })}
                </Container>
              </Tab>
              {/* <Tab eventKey="sim" title="Simulation">
              <Container fluid>
                  <Row>
                    <Col xs={12}>
                      <InputGroup className="mb-3">
                            {(simstarted || simcondition != "TRUE") &&
                                <FormControl
                                  size="xs"
                                  placeholder="Enter condition on next state (no LTL operators)."
                                  aria-label="sim-condition"
                                  aria-describedby="basic-addon2"
                                  onChange={(e) => setSimCondition(e.currentTarget.value)}
                                />
                            }
                            <Button variant="primary" size="lg" disabled={busy} onClick={simulate}>
                              {!simstarted && <span>Start</span>}
                              {simstarted && <span>Next</span>}
                              { simloading && spinner}
                            </Button>
                            <Button variant="secondary" size="lg" disabled={busy || resetsimloading} onClick={resetSimulate}>
                              Reset
                              { resetsimloading && spinner}
                            </Button>
                      </InputGroup>
                    </Col>
                  </Row>
                  <Row>
                    <Col style={{height: "345px", overflowY: "auto", overflowX: "auto"}}>
                      <Table striped bordered hover>
                        <thead>
                          <tr>
                            <th>#Step</th>
                            <th>Changed Variables</th>
                          </tr>
                        </thead>
                        <tbody>
                        {simresponse.map((x, i) => {
                          return <tr key={i}>
                          <td>{i}</td>
                          <td>{JSON.stringify(x)}</td>
                        </tr>;
                        })}
                        </tbody>
                      </Table>
                    </Col>
                  </Row>
                </Container>
              </Tab> */}
              <Tab role="tabpanel" eventKey="interpreter" title={
                <React.Fragment>
                  Interpreter{' '}
                  {interpreterbadge && <Badge pill bg="primary" show="false">!</Badge>}
                </React.Fragment>
              } style={TabStyle}>
                <Container fluid>
                  <Row>
                    <Col xs={12}>
                      <InputGroup className="mb-3">
                        <Button variant="primary" size="lg" disabled={busy || interpreterresponse.length == 0} onClick={backtrackInterpreter}>
                          Back
                          {interpreterbacking && spinner}
                        </Button>
                        {bloading ? cancelBuildButton : <Button variant="primary" size="lg"
                          disabled={busy || (interpreterstarted && interpretertransitions.length == 0)}
                          onClick={interpret}>
                          {!interpreterstarted && <span>Start</span>}
                          {interpreterstarted && <span>Next</span>}
                          {interpreterloading && spinner}
                        </Button>}
                        {
                          <Form.Select
                            aria-label="interpreter-next"
                            value={interpreternextindex}
                            onChange={(e) => { setInterpreterNextIndex(e.target.value) }}>
                            {(!interpreterstarted) && <option value="" disabled selected>Choose a transition here</option>}
                            {interpretertransitions.map((x, i) => {
                              return <option key="{i}-option" value={i}>({i}): {x.send} from {x.sender} to [{x.receivers.join(", ")}]</option>
                            })}
                          </Form.Select>
                        }
                        <Button variant="secondary" size="lg" disabled={busy || resetinterpreterloading} onClick={resetInterpreter}>
                          Reset
                          {resetinterpreterloading && spinner}
                        </Button>
                        {/* <Button variant="secondary" size="lg" disabled={interpreterloading || interpretertransitions.length == 0} onClick={exportData}>
                              Export
                              { interpreterloading && spinner }
                            </Button>
                            <Button variant="secondary" size="lg" disabled={interpreterloading} onClick={() => { importFile.current.click(); }}>
                              Import
                              { interpreterloading && spinner }
                            </Button> */}
                        {/* Dummy/invisible field for Import */}
                        {/* <input type='file' id='file' ref={importFile} style={{display: 'none'}} onChange={(e) => loadJSONIntoInterpreter(e.target.files[0])}/>  */}
                      </InputGroup>
                    </Col>
                  </Row>
                  <Row>
                    <Col style={{ height: "345px", overflowY: "auto", overflowX: "auto", textAlign: "start" }}>
                      <Table ref={interpreterTable} striped bordered hover>
                        <thead>
                          <tr>
                            <th>#Step</th>
                            <th>Changed Variables</th>
                          </tr>
                        </thead>
                        <tbody>
                          {interpreterresponse.map((x, i) => {

                            return (
                              <React.Fragment>{
                                x.inboundTransition !== undefined ?
                                  // Transition
                                  <tr key="{i}-transition">
                                    <td></td>
                                    <td>{formatTransition(x.inboundTransition)}</td>
                                  </tr>
                                  :
                                  ""
                              }
                                {/* 
                            // State
                            // TODO store the renders somewhere, instead of
                            // recomputing them all the time */}
                                <tr key="{i}-state">
                                  <td>{x.depth}
                                    {x.___LOOP___ && !x.___DEADLOCK___ && <React.Fragment><br /><em>Loop starts here</em></React.Fragment>}
                                    {x.___DEADLOCK___ && <React.Fragment><br /><em>Deadlock state</em></React.Fragment>}
                                  </td>
                                  <td>{formatStep(renderStep(x))}</td>
                                </tr>
                              </React.Fragment>)
                          })}
                        </tbody>
                      </Table>
                    </Col>
                  </Row>
                </Container>
              </Tab>
            </Tabs>
          </Col>
        </Row>
        <Row className="justify-content-md-center">
          <Col xs={12} xl={10} xxl={8}>
            <hr />
            {/* <Button variant="primary" 
                        size="lg" 
                        onClick={() => visualise()}
                        disabled={vloading}>
                Visualise
                        { vloading && spinner}
                </Button> */}
            <Container fluid style={Bg}>
              <Row>
                {dot.map((x, i) => {
                  return <Col style={SVGBg} key={i}><Graph svg={x} /></Col>;
                })}
              </Row>
            </Container>
          </Col>
        </Row>
      </Container>
    </div>
  );
}

export default App;

