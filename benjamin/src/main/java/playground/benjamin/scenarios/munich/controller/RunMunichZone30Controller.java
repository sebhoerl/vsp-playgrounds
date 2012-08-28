/* *********************************************************************** *
 * project: kai
 * GautengOwnController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.benjamin.scenarios.munich.controller;


import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.ModularPlanRouter;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PseudoTransitLegRouter;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;

/**
 * @author benjamin after nagel
 *
 */
public class RunMunichZone30Controller extends AbstractController {
	public static Logger logger = Logger.getLogger(RunMunichZone30Controller.class);

	private final Config config;
	private final Scenario scenario;	

	private EventsManager eventsManager;

	private Network network  ;
	private Network ptRoutingNetwork;
	private Population population  ;

	private CalcLegTimes legTimes;
	private TravelTimeCalculator travelTime;
	
	public RunMunichZone30Controller(Scenario sc) {
		this.scenario = sc;
		this.config = sc.getConfig();
	}

	public void run() {
		this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		checkConfigConsistencyAndWriteToLog(this.config, "Complete config dump after reading the config file:");
		this.setupOutputDirectory(config.controler().getOutputDirectory(), config.controler().getRunId(), true);
		this.network = this.scenario.getNetwork();
		this.population = this.scenario.getPopulation();
		this.eventsManager = EventsUtils.createEventsManager(config); 
		// add a couple of useful event handlers:
		this.eventsManager.addHandler(new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network));
		this.legTimes = new CalcLegTimes();
		this.eventsManager.addHandler(legTimes);
		this.travelTime = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		this.eventsManager.addHandler(travelTime);	
		super.run(config);
	}

	/**
	 * The order how the listeners are added may be important! As
	 * dependencies between different listeners exist or listeners may read
	 * and write to common variables, the order is important. 
	 * <br/> 
	 * The example given in the old Controler was: The
	 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
	 * turn is used by the PlansScoring-Listener. I would argue that such dependencies are not necessary with the
	 * code as designed her: One could first define the scoring function completely, and then add it where needed. kai, jun'12
	 * <br/>
	 * IMPORTANT: The execution order is reverse to the order the listeners
	 * are added to the list.
	 */
	@Override
	protected void loadCoreListeners() {

		final DumpDataAtEnd dumpDataAtEnd = new DumpDataAtEnd(scenario, controlerIO);
		this.addControlerListener(dumpDataAtEnd);
		
		final PlansScoring plansScoring = buildPlansScoring();
		this.addControlerListener(plansScoring);

		final StrategyManager strategyManager = buildStrategyManager() ;
		this.addCoreControlerListener(new PlansReplanning( strategyManager, this.population ));

		final PlansDumping plansDumping = new PlansDumping( this.scenario, this.config.controler().getFirstIteration(), 
				this.config.controler().getWritePlansInterval(), stopwatch, controlerIO );
		this.addCoreControlerListener(plansDumping);

		this.addCoreControlerListener(new LegTimesListener(legTimes, controlerIO));
		final EventsHandling eventsHandling = new EventsHandling((EventsManagerImpl) eventsManager,
				this.config.controler().getWriteEventsInterval(), this.config.controler().getEventsFileFormats(),
				controlerIO );
		this.addCoreControlerListener(eventsHandling); 
		// must be last being added (=first being executed)
	}
	private PlansScoring buildPlansScoring() {
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory( this.config.planCalcScore(), this.network );
		final PlansScoring plansScoring = new PlansScoring( this.scenario, this.eventsManager, scoringFunctionFactory );
		return plansScoring;
	}

	private StrategyManager buildStrategyManager() {
		StrategyManager strategyManager = new StrategyManager() ;
		{
			strategyManager.setPlanSelectorForRemoval( new WorstPlanForRemovalSelector() ) ;
		}
		{
			PlanStrategy strategy = new PlanStrategyImpl( new ExpBetaPlanChanger(this.config.planCalcScore().getBrainExpBeta()) ) ;
			strategyManager.addStrategy(strategy, 0.9) ;
		}
		{
			PlanStrategy strategy = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
			strategy.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {

				@Override
				public PlanAlgorithm getPlanAlgoInstance() {
					return createRoutingAlgorithm();
				}
				
			}) ;
			strategyManager.addStrategy(strategy, 0.1) ;
		}
		return strategyManager ;
	}


	private ModularPlanRouter createRoutingAlgorithm() {
		// factory to generate routes:
		final ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();


		// travel disutility (generalized cost)
		final TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutility(this.travelTime, this.config.planCalcScore());
		
		final FreespeedTravelTimeAndDisutility ptTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);

		// define the factory for the "computer science" router.  Needs to be a factory because it might be used multiple
		// times (e.g. for car router, pt router, ...)
		final LeastCostPathCalculatorFactory leastCostPathFactory = new DijkstraFactory();

		// plug it together
		final ModularPlanRouter plansCalcRoute = new ModularPlanRouter();
		
		Collection<String> networkModes = this.config.plansCalcRoute().getNetworkModes();
		for (String mode : networkModes) {
			plansCalcRoute.addLegHandler(mode, new NetworkLegRouter(this.network, leastCostPathFactory.createPathCalculator(this.network, travelDisutility, this.travelTime), routeFactory));
		}
		Map<String, Double> teleportedModeSpeeds = this.config.plansCalcRoute().getTeleportedModeSpeeds();
		for (Entry<String, Double> entry : teleportedModeSpeeds.entrySet()) {
			plansCalcRoute.addLegHandler(entry.getKey(), new TeleportationLegRouter(routeFactory, entry.getValue(), this.config.plansCalcRoute().getBeelineDistanceFactor()));
		}
		Map<String, Double> teleportedModeFreespeedFactors = this.config.plansCalcRoute().getTeleportedModeFreespeedFactors();
		for (Entry<String, Double> entry : teleportedModeFreespeedFactors.entrySet()) {
			plansCalcRoute.addLegHandler(entry.getKey(), new PseudoTransitLegRouter(this.ptRoutingNetwork, leastCostPathFactory.createPathCalculator(this.ptRoutingNetwork, ptTimeCostCalc, ptTimeCostCalc), entry.getValue(), this.config.plansCalcRoute().getBeelineDistanceFactor(), routeFactory));
		}
		
		// return it:
		return plansCalcRoute;
	}
	
	@Override
	protected void prepareForSim() {
		checkConfigConsistencyAndWriteToLog(this.config, "Config dump before doIterations:");
		ParallelPersonAlgorithmRunner.run(this.population, this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), RunMunichZone30Controller.this.scenario);
			}
		});
	}
	
	@Override
	protected void runMobSim(int iteration) {
		QSim qSim = new QSim( this.scenario, this.eventsManager ) ;
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, MatsimRandom.getRandom());
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
		if (config.controler().getWriteSnapshotsInterval() != 0 && iteration % config.controler().getWriteSnapshotsInterval() == 0) {
			// yyyy would be nice to have the following encapsulated in some way:
			// === begin ===
			SnapshotWriterManager manager = new SnapshotWriterManager(config);
			SnapshotWriterFactory snapshotWriterFactory = new OTFFileWriterFactory() ;
			String baseFileName = snapshotWriterFactory.getPreferredBaseFilename();
			String fileName = controlerIO.getIterationFilename(iteration, baseFileName);
			SnapshotWriter snapshotWriter = snapshotWriterFactory.createSnapshotWriter(fileName, this.scenario);
			manager.addSnapshotWriter(snapshotWriter);
			// === end ===
			qSim.addQueueSimulationListeners(manager);
		}
		qSim.run();
	}

	protected void setPtRoutingNetwork(Network ptRoutingNetwork) {
		this.ptRoutingNetwork = ptRoutingNetwork;
	}

}
