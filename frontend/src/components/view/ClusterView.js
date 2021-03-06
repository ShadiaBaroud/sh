import React from 'react';
import { RepositoryService } from '../../services/RepositoryService';
import { ClusterOperationsMenu, operations } from './ClusterOperationsMenu';
import { VisNetwork } from '../util/VisNetwork';
import { ModalMessage } from '../util/ModalMessage';
import { DataSet } from "vis";
import { views, types } from './Views';
import BootstrapTable from 'react-bootstrap-table-next';
import Button from 'react-bootstrap/Button';
import ButtonGroup from 'react-bootstrap/ButtonGroup';
import AppContext from "./../AppContext";

export const clusterViewHelp = (<div>
    Hover or double click cluster to see entities inside.<br />
    Hover or double click edge to see controllers in common.<br />
    Select cluster or edge for highlight and to open operation menu.
</div>);

const options = {
    height: "700",
    layout: {
        hierarchical: false
    },
    edges: {
        smooth: false,
        width: 0.5,
        arrows: {
            from: {
                enabled: false,
                scaleFactor: 0.5
            }
        },
        color: {
            color: "#2B7CE9",
            hover: "#2B7CE9",
            highlight: "#FFA500"
        }
    },
    nodes: {
        shape: 'ellipse',
        color: {
            border: "#2B7CE9",
            background: "#D2E5FF",
            highlight: {
                background: "#FFA500",
                border: "#FFA500"
            }
        }
    },
    interaction: {
        hover: true
    },
    physics: {
        enabled: true,
        hierarchicalRepulsion: {
            centralGravity: 0.0,
            springLength: 500,
            springConstant: 0.01,
            nodeDistance: 100,
            damping: 0.09
        },
        solver: 'hierarchicalRepulsion'
    },
};

export class ClusterView extends React.Component {
    static contextType = AppContext;
    
    constructor(props) {
        super(props);

        this.state = {
            visGraph: {},
            clusters: [],
            clustersControllers: {},
            showMenu: false,
            selectedCluster: {},
            mergeWithCluster: {},
            transferToCluster: {},
            clusterEntities: [],
            error: false,
            errorMessage: '',
            operation: operations.NONE,
            currentSubView: 'Graph'
        };

        this.setClusterEntities = this.setClusterEntities.bind(this);
        this.handleSelectOperation = this.handleSelectOperation.bind(this);
        this.handleSelectCluster = this.handleSelectCluster.bind(this);
        this.handleSelectEntities = this.handleSelectEntities.bind(this);
        this.handleOperationSubmit = this.handleOperationSubmit.bind(this);
        this.handleOperationCancel = this.handleOperationCancel.bind(this);
        this.closeErrorMessageModal = this.closeErrorMessageModal.bind(this);
        this.loadClusterGraph = this.loadClusterGraph.bind(this);
    }

    componentDidMount() {
        this.loadDecomposition();
    }

    loadDecomposition() {
        const {
            codebaseName,
            dendrogramName,
            decompositionName,
        } = this.props;

        const service = new RepositoryService();

        const firstRequest = service.getClustersControllers(
            codebaseName,
            dendrogramName,
            decompositionName
        ).then(response => {
            this.setState({
                clustersControllers: response.data
            });
        });

        const secondRequest = service.getDecomposition(
            codebaseName,
            dendrogramName,
            decompositionName,
            ["clusters"]
        ).then(response => {
            this.setState({
                clusters: Object.values(response.data.clusters),
                showMenu: false,
                selectedCluster: {},
                mergeWithCluster: {},
                transferToCluster: {},
                clusterEntities: [],
                operation: operations.NONE
            });
        });

        Promise.all([firstRequest, secondRequest]).then(() => {
            this.loadClusterGraph();
        });
    }

    loadClusterGraph() {
        const visGraph = {
            nodes: new DataSet(this.state.clusters.map(cluster => this.convertClusterToNode(cluster))),
            edges: new DataSet(this.createEdges())
        };

        this.setState({
            visGraph: visGraph
        });
    }

    convertClusterToNode(cluster) {
        const { translateEntity } = this.context;

        return {
            id: cluster.name,
            title: cluster.entities.sort((a, b) => a - b).map(entityID => translateEntity(entityID)).join('<br>') + "<br>Total: " + cluster.entities.length,
            label: cluster.name,
            value: cluster.entities.length,
            type: types.CLUSTER,
        };
    };

    createEdges() {
        let edges = [];
        let edgeLengthFactor = 1000;

        const {
            clusters,
            clustersControllers,
        } = this.state;

        for (var i = 0; i < clusters.length; i++) {
            let cluster1 = clusters[i];
            let cluster1Controllers = clustersControllers[cluster1.name].map(c => c.name);

            for (var j = i + 1; j < clusters.length; j++) {
                let cluster2 = clusters[j];
                let cluster2Controllers = clustersControllers[cluster2.name].map(c => c.name);

                let controllersInCommon = cluster1Controllers.filter(controllerName => cluster2Controllers.includes(controllerName))

                let couplingC1C2 = cluster1.couplingDependencies[cluster2.name] === undefined ? 0 : cluster1.couplingDependencies[cluster2.name].length;
                let couplingC2C1 = cluster2.couplingDependencies[cluster1.name] === undefined ? 0 : cluster2.couplingDependencies[cluster1.name].length;

                let edgeTitle = cluster1.name + " -> " + cluster2.name + " , Coupling: " + couplingC1C2 + "<br>";
                edgeTitle += cluster2.name + " -> " + cluster1.name + " , Coupling: " + couplingC2C1 + "<br>";
                edgeTitle += "Controllers in common:<br>"

                let edgeLength = (1 / controllersInCommon.length) * edgeLengthFactor;
                if (edgeLength < 100) edgeLength = 300;
                else if (edgeLength > 500) edgeLength = 500;

                controllersInCommon.sort()
                if (controllersInCommon.length > 0)
                    edges.push({
                        from: cluster1.name,
                        to: cluster2.name,
                        length: edgeLength,
                        value: controllersInCommon.length,
                        label: controllersInCommon.length.toString(),
                        title: edgeTitle + controllersInCommon.join('<br>')
                    });
            }
        }
        return edges;
    }

    setClusterEntities(selectedCluster) {
        console.log(selectedCluster);
        
        this.setState({
            selectedCluster: selectedCluster,
            mergeWithCluster: {},
            clusterEntities: selectedCluster.entities.sort((a, b) => (a > b) ? 1 : -1)
                .map(e => ({
                    name: e,
                    value: e,
                    label: e,
                    active: false
                })),
        });
    }

    handleSelectOperation(operation) {
        if (operation === operations.SPLIT || operation === operations.TRANSFER) {
            this.setClusterEntities(this.state.selectedCluster);
            this.setState({
                operation: operation
            });
        } else {
            this.setState({
                mergeWithCluster: {},
                transferToCluster: {},
                clusterEntities: [],
                operation: operation
            });
        }
    }

    handleSelectCluster(nodeId) {

        const {
            operation,
            clusters,
            selectedCluster,
        } = this.state;

        if (operation === operations.NONE ||
            operation === operations.RENAME) {
            this.setState({
                showMenu: true,
                selectedCluster: clusters.find(c => c.name === nodeId)
            });
        }

        if (operation === operations.MERGE) {
            const mergeWithCluster = clusters.find(c => c.name === nodeId);
            if (selectedCluster === mergeWithCluster) {
                this.setState({
                    error: true,
                    errorMessage: 'Cannot merge a cluster with itself'
                });
            } else {
                this.setState({
                    mergeWithCluster: mergeWithCluster
                });
            }
        }

        if (operation === operations.TRANSFER) {
            const transferToCluster = clusters.find(c => c.name === nodeId);
            if (selectedCluster === transferToCluster) {
                this.setState({
                    error: true,
                    errorMessage: 'Cannot transfer entities to the same cluster'
                });
            } else {
                this.setState({
                    transferToCluster: transferToCluster
                });
            }
        }

        if (operation === operations.SPLIT) {
            this.setClusterEntities(clusters.find(c => c.name === nodeId));
        }
    }

    handleSelectEntities(entities) {
        if (entities === null) {
            const clusterEntities = this.state.clusterEntities.map(e => {
                return { ...e, active: false };
            });
            this.setState({
                clusterEntities: clusterEntities
            });
        } else {
            const clusterEntities = this.state.clusterEntities.map(e => {
                if (entities.map(e => e.name).includes(e.name)) {
                    return { ...e, active: true };
                } else {
                    return { ...e, active: false };
                }
            });
            this.setState({
                clusterEntities: clusterEntities
            });
        }
    }

    handleOperationSubmit(operation, inputValue) {
        const service = new RepositoryService();

        const {
            selectedCluster,
            clusterEntities,
            mergeWithCluster,
            transferToCluster,
        } = this.state;

        const {
            codebaseName,
            dendrogramName,
            decompositionName,
        } = this.props;

        switch (operation) {
            case operations.RENAME:
                service.renameCluster(
                    codebaseName,
                    dendrogramName,
                    decompositionName,
                    selectedCluster.name,
                    inputValue
                )
                    .then(() => {
                        this.loadDecomposition();

                    }).catch((err) => {
                        this.setState({
                            error: true,
                            errorMessage: 'ERROR: rename cluster failed.'
                        });
                    });

                break;

            case operations.MERGE:
                service.mergeClusters(
                    codebaseName,
                    dendrogramName,
                    decompositionName,
                    selectedCluster.name,
                    mergeWithCluster.name,
                    inputValue
                )
                    .then(() => {
                        this.loadDecomposition();
                    }).catch((err) => {

                        this.setState({
                            error: true,
                            errorMessage: 'ERROR: merge clusters failed.'
                        });
                    });

                break;

            case operations.SPLIT:
                let activeClusterEntitiesSplit = clusterEntities.filter(e => e.active).map(e => e.name).toString();
                
                service.splitCluster(
                    codebaseName,
                    dendrogramName,
                    decompositionName,
                    selectedCluster.name,
                    inputValue,
                    activeClusterEntitiesSplit
                )
                    .then(() => {
                        this.loadDecomposition();

                    }).catch((err) => {
                        this.setState({
                            error: true,
                            errorMessage: 'ERROR: split cluster failed.'
                        });
                    });

                break;

            case operations.TRANSFER:
                let activeClusterEntitiesTransfer = clusterEntities.filter(e => e.active).map(e => e.name).toString();

                service.transferEntities(
                    codebaseName,
                    dendrogramName,
                    decompositionName,
                    selectedCluster.name,
                    transferToCluster.name,
                    activeClusterEntitiesTransfer
                )
                    .then(() => {
                        this.loadDecomposition();
                    }).catch((err) => {
                        this.setState({
                            error: true,
                            errorMessage: 'ERROR: transfer entities failed.'
                        });
                    });
                break;

            default:
        }
    }

    handleOperationCancel() {
        this.setState({
            showMenu: false,
            selectedCluster: {},
            mergeWithCluster: {},
            transferToCluster: {},
            clusterEntities: [],
            operation: operations.NONE
        });
    }

    closeErrorMessageModal() {
        this.setState({
            error: false,
            errorMessage: ''
        });
    }

    handleDeselectNode(nodeId) { }

    changeSubView(value) {
        this.setState({
            currentSubView: value
        });
    }

    render() {
        const {
            clusters,
            clustersControllers,
            currentSubView,
            error,
            errorMessage,
            showMenu,
            selectedCluster,
            mergeWithCluster,
            transferToCluster,
            clusterEntities,
            visGraph,
        } = this.state;

        const metricsRows = clusters.map(({
            name,
            entities,
            cohesion,
            coupling,
            complexity,
        }) => {
            return {
                cluster: name,
                entities: entities.length,
                controllers: clustersControllers[name] === undefined ? 0 : clustersControllers[name].length,
                cohesion: cohesion,
                coupling: coupling,
                complexity: complexity
            }
        });

        const metricsColumns = [{
            dataField: 'cluster',
            text: 'Cluster'
        }, {
            dataField: 'entities',
            text: 'Entities',
            sort: true
        }, {
            dataField: 'controllers',
            text: 'Controllers',
            sort: true
        }, {
            dataField: 'cohesion',
            text: 'Cohesion',
            sort: true
        }, {
            dataField: 'coupling',
            text: 'Coupling',
            sort: true
        }, {
            dataField: 'complexity',
            text: 'Complexity',
            sort: true
        }];

        const couplingRows = clusters.map(c1 => {
            return Object.assign({ id: c1.name }, ...clusters.map(c2 => {
                return {
                    [c2.name]: c1.name === c2.name ? "---" :
                        c1.couplingDependencies[c2.name] === undefined ? 0 :
                            parseFloat(c1.couplingDependencies[c2.name].length / Object.keys(c2.entities).length).toFixed(2)
                }
            }))
        });

        const couplingColumns = [{ dataField: 'id', text: '', style: { fontWeight: 'bold' } }]
            .concat(clusters.map(c => {
                return {
                    dataField: c.name,
                    text: c.name
                }
            }));

        return (
            <>
                {
                    error &&
                    <ModalMessage
                        title='Error Message'
                        message={errorMessage}
                        onClose={this.closeErrorMessageModal}
                    />
                }
                <ButtonGroup className="mb-2">
                    <Button
                        disabled={currentSubView === "Graph"}
                        onClick={() => this.changeSubView("Graph")}
                    >
                        Graph
                    </Button>
                    <Button
                        disabled={currentSubView === "Metrics"}
                        onClick={() => this.changeSubView("Metrics")}
                    >
                        Metrics
                    </Button>
                    <Button
                        disabled={currentSubView === "Coupling Matrix"}
                        onClick={() => this.changeSubView("Coupling Matrix")}
                    >
                        Coupling Matrix
                    </Button>
                </ButtonGroup>

                {currentSubView === "Graph" &&
                    <span>
                        {showMenu &&
                        <ClusterOperationsMenu
                            selectedCluster={selectedCluster}
                            mergeWithCluster={mergeWithCluster}
                            transferToCluster={transferToCluster}
                            clusterEntities={clusterEntities}
                            handleSelectOperation={this.handleSelectOperation}
                            handleSelectEntities={this.handleSelectEntities}
                            handleSubmit={this.handleOperationSubmit}
                            handleCancel={this.handleOperationCancel}
                        />}

                        <div style={{height: '700px'}}>
                            <VisNetwork
                                visGraph={visGraph}
                                options={options}
                                onSelection={this.handleSelectCluster}
                                onDeselection={this.handleDeselectNode}
                                view={views.CLUSTERS}/>
                        </div>
                    </span>
                }


                {
                    currentSubView === "Metrics" &&
                    <BootstrapTable
                        bootstrap4
                        keyField='cluster'
                        data={metricsRows}
                        columns={metricsColumns}
                    />
                }

                {
                    currentSubView === "Coupling Matrix" &&
                    <BootstrapTable
                        bootstrap4
                        keyField='id'
                        data={couplingRows}
                        columns={couplingColumns}
                    />
                }
            </>
        );
    }
}