import React from 'react';
import { RepositoryService } from '../../services/RepositoryService';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Dropdown from 'react-bootstrap/Dropdown';
import Button from 'react-bootstrap/Button';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import BootstrapTable from 'react-bootstrap-table-next';
import filterFactory, { numberFilter } from 'react-bootstrap-table2-filter';
import { Codebase, Decomposition } from "../../type-declarations/types";

var HttpStatus = require('http-status-codes');

const filter = numberFilter({});
const sort = true;

const metricColumns = [
    {
        dataField: 'access',
        text: 'Access',
        sort,
        filter,
    }, 
    {
        dataField: 'write',
        text: 'Write',
        sort,
        filter,
    }, 
    {
        dataField: 'read',
        text: 'Read',
        sort,
        filter,
    }, 
    {
        dataField: 'sequence',
        text: 'Sequence',
        sort,
        filter,
    },
    {
        dataField: 'numberClusters',
        text: 'Number Clusters',
        sort,
        filter,
    }, 
    {
        dataField: 'maxClusterSize',
        text: 'Max Cluster Size',
        sort,
        filter,
    }, 
    {
        dataField: 'cohesion',
        text: 'Cohesion',
        sort,
        filter,
    }, 
    {
        dataField: 'coupling',
        text: 'Coupling',
        sort,
        filter,
    }, 
    {
        dataField: 'complexity',
        text: 'Complexity',
        sort,
        filter,
    }, 
    {
        dataField: 'performance',
        text: 'Performance',
        sort,
        filter,
    }, 
    {
        dataField: 'fmeasure',
        text: 'F-Score',
        sort,
        filter,
    }, 
    {
        dataField: 'accuracy',
        text: 'Accuracy',
        sort,
        filter,
    }, 
    {
        dataField: 'precision',
        text: 'Precision',
        sort,
        filter,
    }, 
    {
        dataField: 'recall',
        text: 'Recall',
        sort,
        filter,
    }, 
    {
        dataField: 'specificity',
        text: 'Specificity',
        sort,
        filter,
    },
    {
        dataField: 'mojoCommon',
        text: 'MoJo Common',
        sort,
        filter,
    },
    {
        dataField: 'mojoBiggest',
        text: 'MoJo Biggest',
        sort,
        filter,
    },
    {
        dataField: 'mojoNew',
        text: 'MoJo New',
        sort,
        filter,
    },
    {
        dataField: 'mojoSingletons',
        text: 'MoJo Singletons',
        sort,
        filter,
    }
];

export class Analyser extends React.Component<any, any> {
    constructor(props: any) {
        super(props);
        this.state = {
            codebases: [],
            codebase: {
                profiles: {}
            },
            selectedProfile: "",
            experts: [],
            expert: null,
            resultData: [],
            requestLimit: "0",
            importFile: null,
            amountOfTraces: "0",
            typeOfTraces: "ALL",
        };

        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleImportSubmit = this.handleImportSubmit.bind(this);
        this.handleRequestLimitChange = this.handleRequestLimitChange.bind(this);
        this.handleSelectImportFile = this.handleSelectImportFile.bind(this);
        this.handleChangeAmountOfTraces = this.handleChangeAmountOfTraces.bind(this);
        this.handleChangeTypeOfTraces = this.handleChangeTypeOfTraces.bind(this);
    }

    componentDidMount() {
        this.loadCodebases()
    }

    loadCodebases() {
        const service = new RepositoryService();
        service.getCodebases(
            [
                "name",
                "profiles",
            ]
        ).then(response => {
            this.setState({
                codebases: response.data
            });
        });
    }

    loadCodebaseDecompositions(codebaseName: string) {
        const service = new RepositoryService();
        
        service.getCodebaseDecompositions(
            codebaseName,
            [
                "name",
                "expert",
                "codebaseName",
                "clusters",
            ]
        ).then((response) => {
            if (response.data !== null) {
                this.setState({
                    experts: response.data.filter((decomposition: Decomposition) => decomposition.expert)
                });
            }
        });
    }

    setCodebase(codebase: Pick<Codebase, "name" | "profiles">) {
        this.setState({
            codebase: codebase,
        });
        
        this.loadCodebaseDecompositions(codebase.name!);
    }

    selectProfile(profile: string) {
        if (this.state.selectedProfile !== profile) {
            this.setState({
                selectedProfile: profile,
            });
        } else {
            this.setState({
                selectedProfile: "",
            });
        }
    }

    setExpert(expert: any) {
        this.setState({
            expert: expert
        });
    }

    handleSubmit(event: any) {
        event.preventDefault()

        this.setState({
            isUploaded: "Uploading..."
        });

        const service = new RepositoryService();
        service.analyser(
            this.state.codebase.name,
            this.state.expert,
            this.state.selectedProfile,
            Number(this.state.requestLimit),
            Number(this.state.amountOfTraces),
            this.state.typeOfTraces,
        )
        .then(response => {
            if (response.status === HttpStatus.OK) {
                this.setState({
                    isUploaded: "Upload completed successfully."
                });
            } else {
                this.setState({
                    isUploaded: "Upload failed."
                });
            }
        })
        .catch(error => {
            this.setState({
                isUploaded: "Upload failed."
            });
        });
    }

    handleRequestLimitChange(event: any) {
        this.setState({
            requestLimit: event.target.value
        });
    }

    handleImportSubmit(event: any) {
        event.preventDefault();
        this.setState({
            resultData: Object.values(JSON.parse(this.state.importFile))
        });
    }

    handleSelectImportFile(event: any) {
        let that = this;
        let reader = new FileReader();
        reader.onload = function(e: any) {
            that.setState({
                importFile: e.target.result
            });
        };
        
        if (event.target.files.length > 0)
            reader.readAsText(event.target.files[0]);
    }

    handleChangeAmountOfTraces(event: any) {
        this.setState({
           amountOfTraces: event.target.value
        });
    }

    handleChangeTypeOfTraces(event: any) {
        this.setState({
            typeOfTraces: event.target.value
        });
    }

    renderBreadCrumbs = () => {
        return (
            <Breadcrumb>
                <Breadcrumb.Item href="/">
                    Home
                </Breadcrumb.Item>
                <Breadcrumb.Item active>
                    Analyser
                </Breadcrumb.Item>
            </Breadcrumb>
        );
    }

    render() {

        const {
            resultData,
            codebase,
            codebases,
            expert,
            selectedProfile,
            requestLimit,
            experts,
            isUploaded,
            importFile,
            amountOfTraces,
            typeOfTraces,
        } = this.state;

        const metricRows = resultData.map((data: any, index: number) => {
            return {
                id: index,
                access: data.accessWeight,
                write: data.writeWeight,
                read: data.readWeight,
                sequence: data.sequenceWeight,
                numberClusters: data.numberClusters,
                maxClusterSize: data.maxClusterSize,
                cohesion: data.cohesion,
                coupling: data.coupling,
                complexity: data.complexity,
                performance: data.performance,
                fmeasure: data.fmeasure,
                accuracy: data.accuracy,
                precision: data.precision,
                recall: data.recall,
                specificity: data.specificity,
                mojoCommon: data.mojoCommon,
                mojoBiggest: data.mojoBiggest,
                mojoNew: data.mojoNew,
                mojoSingletons: data.mojoSingletons
            } 
        });

        return (
            <>
                {this.renderBreadCrumbs()}
                <h4 style={{color: "#666666"}}>
                    Analyser
                </h4>

                <Form onSubmit={this.handleSubmit}>
                    <Form.Group as={Row} controlId="codebase">
                        <Form.Label column sm={3}>
                            Codebase
                        </Form.Label>
                        <Col sm={3}>
                            <DropdownButton title={codebase?.name || "Select Codebase"}>
                                {
                                    codebases.map((codebase: any) => 
                                        <Dropdown.Item 
                                            key={codebase.name}
                                            onClick={() => this.setCodebase(codebase)}
                                        >
                                            {codebase.name}
                                        </Dropdown.Item>
                                    )
                                }
                            </DropdownButton>
                        </Col>
                    </Form.Group>

                    <Form.Group as={Row} controlId="selectControllerProfiles">
                        <Form.Label column sm={3}>
                            Select Controller Profiles
                        </Form.Label>
                        <Col sm={3}>
                            <DropdownButton title={'Controller Profiles'}>
                                {Object.keys(codebase.profiles).map((profile: any) =>
                                    <Dropdown.Item
                                        key={profile}
                                        onSelect={() => this.selectProfile(profile)}
                                        active={selectedProfile === profile}
                                    >
                                        {profile}
                                    </Dropdown.Item>
                                )}
                            </DropdownButton>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} controlId="amountOfTraces">
                        <Form.Label column sm={3}>
                            Amount of Traces per Controller
                        </Form.Label>
                        <Col sm={3}>
                            <FormControl 
                                type="number"
                                value={amountOfTraces}
                                onChange={this.handleChangeAmountOfTraces}
                            />
                            <Form.Text className="text-muted">
                                If no number is inserted, 0 is assumed to be the default value meaning the maximum number of traces
                            </Form.Text>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} className="align-items-center">
                        <Form.Label as="legend" column sm={3}>
                            Type of traces
                        </Form.Label>
                        <Col sm={3} style={{ paddingLeft: 0 }}>
                            <Col sm="auto">
                                <Form.Check
                                    defaultChecked
                                    id="allTraces"
                                    value="ALL"
                                    type="radio"
                                    label="All"
                                    name="typeOfTraces"
                                    onClick={this.handleChangeTypeOfTraces}
                                />
                            </Col>
                            <Col sm="auto">
                                <Form.Check
                                    type="radio"
                                    id="longest"
                                    value="LONGEST"
                                    label="Longest"
                                    name="typeOfTraces"
                                    onClick={this.handleChangeTypeOfTraces}
                                />
                            </Col>
                            <Col sm="auto">
                                <Form.Check
                                    id="withMoreDifferentTraces"
                                    value="WITH_MORE_DIFFERENT_ACCESSES"
                                    type="radio"
                                    label="With more different accesses"
                                    name="typeOfTraces"
                                    onClick={this.handleChangeTypeOfTraces}
                                />

                            </Col>
                            <Col sm="auto">
                                <Form.Check
                                    id="representativeSetOfAccesses"
                                    value="REPRESENTATIVE"
                                    type="radio"
                                    label="Representative (set of accesses)"
                                    name="typeOfTraces"
                                    onClick={this.handleChangeTypeOfTraces}
                                />
                            </Col>
                            {/* WIP */}
                            <Col sm="auto">
                                <Form.Check
                                    disabled
                                    id="complete"
                                    value="?"
                                    type="radio"
                                    label="Representative (subsequence of accesses)"
                                    name="typeOfTraces"
                                    onClick={undefined}
                                />
                            </Col>
                        </Col>
                    </Form.Group>
                    <Form.Group as={Row} controlId="expert">
                        <Form.Label column sm={3}>
                            Expert
                        </Form.Label>
                        <Col sm={3}>
                            <DropdownButton title={expert?.name || "Select Expert Cut"}>
                                {experts.map((expert: any) => 
                                    <Dropdown.Item 
                                        key={expert.name}
                                        onClick={() => this.setExpert(expert)}
                                    >
                                        {expert.name}
                                    </Dropdown.Item>
                                )}
                            </DropdownButton>
                        </Col>
                    </Form.Group>

                    <Form.Group as={Row} controlId="requestLimit">
                        <Form.Label column sm={3}>
                            Request limit
                        </Form.Label>
                        <Col sm={3}>
                            <FormControl 
                                type="number"
                                placeholder="Request Limit"
                                value={requestLimit}
                                onChange={this.handleRequestLimitChange}
                            />
                        </Col>
                    </Form.Group>

                    <Form.Group as={Row}>
                        <Col sm={{ span: 5, offset: 3 }}>
                            <Button 
                                type="submit"
                                disabled={
                                    isUploaded === "Uploading..." ||
                                    selectedProfile.length === "" ||
                                    requestLimit === "" ||
                                    (typeOfTraces === "" || amountOfTraces === "")
                                }
                            >
                                Submit
                            </Button>
                            <Form.Text>
                                {isUploaded}
                            </Form.Text>
                        </Col>
                    </Form.Group>
                </Form>
                <br />
                <Form onSubmit={this.handleImportSubmit}>
                    <Form.Group as={Row} controlId="importFile">
                        <Form.Label column sm={3}>
                            Import Analyser Results from File
                        </Form.Label>
                        <Col sm={3}>
                            <FormControl 
                                type="file"
                                onChange={this.handleSelectImportFile}
                            />
                        </Col>
                    </Form.Group>

                    <Form.Group as={Row}>
                        <Col sm={{ span: 5, offset: 3 }}>
                            <Button 
                                type="submit"
                                disabled={ importFile === null }
                            >
                                Import Analyser Results
                            </Button>
                        </Col>
                    </Form.Group>
                </Form>
                <BootstrapTable
                    keyField='id'
                    data={ metricRows }
                    columns={ metricColumns }
                    filter={ filterFactory() }
                />
            </>
        )
    }
}