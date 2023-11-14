import logo from './logo.svg';
import './App.css';
import { Container, Table, Dropdown, Spinner, FormControl, Row, Col, Tab, Tabs, Button, Form, InputGroup, ButtonGroup, ToggleButton, Badge, Navbar, OverlayTrigger, Tooltip, Modal, Nav } from 'react-bootstrap';
import AceEditor from "react-ace";
import React, { useEffect, useState, useRef } from 'react';
import axios from "axios";
import { useFetch } from "./hooks";
import Graph from "./Graph";

import Viz from "viz.js";
import * as All from "./mode-recipe";
import "ace-builds/src-noconflict/theme-tomorrow";
import ReactHtmlParser from 'react-html-parser';

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
  const [mcloading, setMCLoading] = useState(false);
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

  const [confirmBuild, setConfirmBuild] = useState(false);

  const radios = [
    { name: 'MC', value: '1' },
    { name: 'IC3', value: '2' },
    { name: 'BMC', value: '3' },
  ];

  const interpreterTable = useRef(null);

  function simulate() {
    if (built != true) {
      alert("Build model first.");
      return;
    }

    setSimLoading(true);

    const params = new URLSearchParams();
    params.append('reset', encodeURIComponent(!simstarted));
    params.append('constraint', encodeURIComponent(simcondition));

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
      params.append("trace", encodeURIComponent(e.target.result));
      axios.get(server + "/interpretLoadJSON", { params })
        .then((response) => { console.log(response) })
        .catch((error) => { console.log(error) });
    };
    reader.readAsText(file);
  }

  function loadTraceIntoInterpreter(output) {
    const params = new URLSearchParams();
    params.append("output", encodeURIComponent(output));
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
        setSimLoading(false);
      });
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
    if (built != true) buildModel();
    setInterpreterLoading(true);
  }

  useEffect(() => {
    if (built && interpreterloading) {
      const params = new URLSearchParams();
      params.append('reset', encodeURIComponent(!interpreterstarted));
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
    axios.get(server + "/modelCheckStop")
      .then((_) => { console.log("Stopped all MC tasks as requested by the user."); });
  }

  function modelCheck() {
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
          params.append('ic3', encodeURIComponent(true));
          if (bound > -1) {
            params.append('bound', encodeURIComponent(bound));
          }
        } else if (radioValue == 3) {
          params.append('bmc', encodeURIComponent(true));
          params.append('bound', encodeURIComponent(bound));
        }

        axios.get(server + "/modelCheck", { params })
          .then((response) => {
            console.log(response.data);
            if (response.data.error !== undefined) {
              alert(response.data.error);
            }
            else if (response.data !== undefined) {
              setMCResponse(response.data.results);
              response.data.results.map((x) => modelCheckSingle(x));
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
    axios.get(server + data.url).then((response) => {
      setMCResponse(oldmcresponse =>
        oldmcresponse.map((x, i) => {
          return (i == data.id) ? response.data : x;
        }));
    });
  }

  function visualise() {
    setVLoading(true);
    const params = new URLSearchParams();
    params.append('script', encodeURIComponent(code));

    axios.get(server + "/setSystem", { params })
      .then((response) => {
        axios.get(server + "/toDOTSVG")
          .then((response2) => {
            console.log(response2.data);
            setDot(response2.data.map(x => {
              var svg = new DOMParser().parseFromString(x.svg, "image/svg+xml").getElementsByTagNameNS("http://www.w3.org/2000/svg", "svg").item(0);
              console.log("svg: " + svg);
              return svg;
            }));
            setVLoading(false);
          })
          .catch((err) => {
            alert(err.message);
            setVLoading(false);
          })
      })
      .catch((err) => {
        alert(err.message);
        setVLoading(false);
      });
  }

  const handleClose = () => setConfirmBuild(false);
  function confirmBuildModel() {
    if (mcresponse.length > 0) {
      setConfirmBuild(true);
    } else {
      buildModel();
    }
  }

  function buildModel(obeyMCpane = false) {
    setBLoading(true);
    setConfirmBuild(false);
    setInterpreterBadge(false);
    setMCResponse([]);
    setBound(-1);
    resetInterpreter();
    const params = new URLSearchParams();
    params.append('script', encodeURIComponent(code));
    console.log(server);

    var askSymbolic = obeyMCpane ? radioValue != 1 : symbolicBuild;


    axios.get(server + "/setSystem", { params })
      .then((response) => {
        console.log("system set");
        params.append('symbolic', askSymbolic);
        axios.get(server + "/buildModel", { params })
          .then((response2) => {
            if (response2.data.hasOwnProperty("error")) {
              alert(response2.data.error);
            } else {
              setBuilt(true);
              visualise();
              alert("Model built successfully");
            }
            setBLoading(false);
          })
          .catch((err) => {
            console.log(err.message);
            setBLoading(false);
          })
      })
      .catch((err) => {
        console.log(err.message);
        setBLoading(false);
      });
  }

  function xmlToJSX(input) {
    return input.replace("xmlns:xlink", "xmlnsXlink")
      .replace("xlink:title", "xlinkTitle")
      .replace("font-size", "fontSize")
      .replace("font-family", "fontFamily")
      .replace("text-anchor", "textAnchor")
      .replace("viewbox", "viewBox");
  }

  return (
    <div className="App">
      <Modal show={confirmBuild} onHide={handleClose}>
        <Modal.Header closeButton>
          <Modal.Title>Warning</Modal.Title>
        </Modal.Header>
        <Modal.Body>This will reset the Model Checking results. Do you want to proceed?</Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleClose}>
            No
          </Button>
          <Button variant="primary" onClick={buildModel}>
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
              <Tab eventKey="ed" title="Editor" style={TabStyle}>
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
                  <Button variant="primary" size="lg" onClick={confirmBuildModel} disabled={bloading}>
                    Build model
                    {bloading && spinner}
                  </Button>
                </InputGroup>
              </Tab>
              <Tab eventKey="mc" title="Model Checker" style={TabStyle}>
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
                        <Button variant="primary" size="lg" disabled={mcloading} onClick={modelCheck}>
                          Start {mcloading && spinner}
                        </Button>
                        <Button variant="danger" size="lg" onClick={modelCheckStop}>
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
                            <Button onClick={() => loadTraceIntoInterpreter(mcresponse[i].output)}>
                              Load into interpreter
                            </Button>
                            <Dropdown.Toggle split id="dropdown-split-basic" />
                            <Dropdown.Menu>
                              <Dropdown.Item onClick={() => loadTraceIntoInterpreter(mcresponse[i].output)}>Load into interpreter</Dropdown.Item>
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
                            <Button variant="primary" size="lg" disabled={simloading} onClick={simulate}>
                              {!simstarted && <span>Start</span>}
                              {simstarted && <span>Next</span>}
                              { simloading && spinner}
                            </Button>
                            <Button variant="secondary" size="lg" disabled={resetsimloading} onClick={resetSimulate}>
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
              <Tab eventKey="interpreter" title={
                <React.Fragment>
                  Interpreter{' '}
                  {interpreterbadge && <Badge pill bg="primary" show="false">!</Badge>}
                </React.Fragment>
              } style={TabStyle}>
                <Container fluid>
                  <Row>
                    <Col xs={12}>
                      <InputGroup className="mb-3">
                        <Button variant="primary" size="lg" disabled={interpreterbacking || interpreterloading || interpreterresponse.length == 0} onClick={backtrackInterpreter}>
                          Back
                          {interpreterbacking && spinner}
                        </Button>
                        <Button variant="primary" size="lg"
                          disabled={interpreterbacking || interpreterloading || (interpreterstarted && interpretertransitions.length == 0)}
                          onClick={interpret}>
                          {!interpreterstarted && <span>Start</span>}
                          {interpreterstarted && <span>Next</span>}
                          {interpreterloading && spinner}
                        </Button>
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
                        <Button variant="secondary" size="lg" disabled={resetinterpreterloading} onClick={resetInterpreter}>
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

