/*
 * 2016 Royal Institute of Technology (KTH)
 *
 * LSelector is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.news.sim;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import se.kth.news.sim.compatibility.SimNodeIdExtractor;
import se.kth.news.sim.util.RandomDistinctDistribution;
import se.kth.news.stats.StatComp;
import se.kth.news.system.HostMngrComp;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation;
import se.sics.kompics.simulator.adaptor.Operation1;
import se.sics.kompics.simulator.adaptor.distributions.Distribution;
import se.sics.kompics.simulator.adaptor.distributions.IntegerUniformDistribution;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.KillNodeEvent;
import se.sics.kompics.simulator.events.system.SetupEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;
import se.sics.kompics.simulator.network.identifier.IdentifierExtractor;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapServerComp;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.overlays.id.OverlayIdRegistry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ScenarioGen {

    static Operation<SetupEvent> systemSetupOp = new Operation<SetupEvent>() {
        @Override
        public SetupEvent generate() {
            return new SetupEvent() {
                @Override
                public void setupSystemContext() {
                    OverlayIdRegistry.registerPrefix("newsApp", ScenarioSetup.overlayOwner);
                }

                @Override
                public IdentifierExtractor getIdentifierExtractor() {
                    return new SimNodeIdExtractor();
                }
            };
        }
    };

    static Operation<StartNodeEvent> startBootstrapServerOp = new Operation<StartNodeEvent>() {

        @Override
        public StartNodeEvent generate() {
            return new StartNodeEvent() {
                KAddress selfAdr;

                {
                    selfAdr = ScenarioSetup.bootstrapServer;
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return BootstrapServerComp.class;
                }

                @Override
                public BootstrapServerComp.Init getComponentInit() {
                    return new BootstrapServerComp.Init(selfAdr);
                }
            };
        }
    };

    static Operation<StartNodeEvent> startStatCompOp = new Operation<StartNodeEvent>() {

        @Override
        public StartNodeEvent generate() {
            return new StartNodeEvent() {
                KAddress selfAdr;

                {
                    selfAdr = ScenarioSetup.statServer;
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return StatComp.class;
                }

                @Override
                public StatComp.Init getComponentInit() {
                    return new StatComp.Init(selfAdr);
                }
            };
        }
    };

    static Operation1<StartNodeEvent, Integer> startNodeOp = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer nodeId) {
            return new StartNodeEvent() {
                KAddress selfAdr;


                {
                    selfAdr = ScenarioSetup.getNodeAdr(nodeId);
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return HostMngrComp.class;
                }

                @Override
                public HostMngrComp.Init getComponentInit() {
                    return new HostMngrComp.Init(selfAdr, ScenarioSetup.bootstrapServer, ScenarioSetup.statServer, ScenarioSetup.newsOverlayId);
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    Map<String, Object> nodeConfig = new HashMap<>();
                    nodeConfig.put("system.id", nodeId);
                    nodeConfig.put("system.seed", ScenarioSetup.getNodeSeed(nodeId));
                    nodeConfig.put("system.port", ScenarioSetup.appPort);
                    return nodeConfig;
                }
            };
        }
    };

    static Operation1 killNodeOp = new Operation1<KillNodeEvent, Integer>(){

        public KillNodeEvent generate(final Integer nodeId) {
            return new KillNodeEvent() {

                {
                    nodeAdr = ScenarioSetup.getNodeAdr(nodeId);
                    System.out.println("Greatest logging killed node with id "+nodeId);
                }

                private KAddress nodeAdr;

                @Override
                public Address getNodeAddress() {
                    return nodeAdr;
                }

                @Override
                public String toString() {
                    return "Killed node <"+nodeAdr.toString()+">";
                }
            };
        }
    };

    public static SimulationScenario simpleBoot() {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess systemSetup = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, systemSetupOp);
                    }
                };
                StochasticProcess startBootstrapServer = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startBootstrapServerOp);
                    }
                };
                StochasticProcess startStatServer = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startStatCompOp);
                    }
                };
                StochasticProcess startPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(uniform(10, 20));
                        raise(200, startNodeOp, new BasicIntSequentialDistribution(2));
                    }
                };
                StochasticProcess killNode = new StochasticProcess() {
                    {
                        eventInterArrivalTime(uniform(10, 20));
                        //raise(3, killNodeOp, new BasicIntSequentialDistribution(2));
                        raise(20, killNodeOp, new RandomDistinctDistribution(2, 202));
                    }
                };
                StochasticProcess startFewPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(uniform(10, 20));
                        raise(5, startNodeOp, new BasicIntSequentialDistribution(400));
                    }
                };

                systemSetup.start();
                startBootstrapServer.startAfterTerminationOf(1000, systemSetup);
                startStatServer.startAfterTerminationOf(1000, startBootstrapServer);
                startPeers.startAfterTerminationOf(1000, startStatServer);
                killNode.startAfterStartOf(50000, startBootstrapServer);
                startFewPeers.startAfterTerminationOf(1000, killNode);
                terminateAfterTerminationOf(1000*1000, startPeers);
            }
        };

        return scen;
    }
}
