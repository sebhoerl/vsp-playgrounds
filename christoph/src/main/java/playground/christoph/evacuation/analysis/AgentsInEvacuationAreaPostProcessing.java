/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInEvacuationAreaPostProcessing.java
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

package playground.christoph.evacuation.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEventImpl;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEventImpl;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.controler.PrepareEvacuationScenario;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PanicModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PickupModel;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Class to produce AgentInEvacuationArea data using output data from a 
 * previous simulation run.
 * Expect three input arguments:
 * <ul>
 * 	<li>config file</li>
 * 	<li>evacuation config file</li>
 * 	<li>events file</li>
 * </ul>
 * 
 * Note: due to changes in PersonDriverAgentImpl this code does not run at the moment!
 * 
 * @author cdobler
 */
public class AgentsInEvacuationAreaPostProcessing {

	public static void main(String[] args) throws IOException {
		if (args.length == 3) {
			new AgentsInEvacuationAreaPostProcessing(args[0], args[1], args[2]);
		}
	}
	
	public AgentsInEvacuationAreaPostProcessing(String configFile, String evacuationConfigFile, String eventsFile) throws IOException {
		
		Config config = ConfigUtils.loadConfig(configFile);	
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager(); 
		
		new EvacuationConfigReader().readFile(evacuationConfigFile);
		EvacuationConfig.printConfig();
		
		// load household object attributes
		ObjectAttributes householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdObjectAttributes).parse(EvacuationConfig.householdObjectAttributesFile);
		
		/*
		 * Prepare the scenario:
		 * 	- connect facilities to network
		 * 	- add exit links to network
		 * 	- add pickup facilities
		 *  - add z Coordinates to network
		 */
		new PrepareEvacuationScenario().prepareScenario(scenario);
		
		DummyController dummyController = new DummyController(scenario);
		
		List<ControlerListener> controlerListeners = new ArrayList<ControlerListener>();
		List<MobsimListener> mobsimListeners = new ArrayList<MobsimListener>();
		
		Set<Feature> features = new HashSet<Feature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(util.readFile(file));		
		}
		Geometry affectedArea = util.mergeGeomgetries(features);
		
		CoordAnalyzer coordAnalyzer = new CoordAnalyzer(affectedArea);
		
//		PostProcessingHouseholdsTracker householdsTracker = new PostProcessingHouseholdsTracker(householdObjectAttributes);
//		householdsTracker.createAgents(scenario, eventsManager);
//		eventsManager.addHandler(householdsTracker);
//		mobsimListeners.add(householdsTracker);

		HouseholdsTracker householdsTracker = new HouseholdsTracker();
		DecisionDataProvider decisionDataProvider = new DecisionDataProvider();
		
		/*
		 * Create a DecisionDataGrabber and run notifyMobsimInitialized(...)
		 * which inserts decision data into the DecisionDataProvider.
		 */
		DecisionDataGrabber decisionDataGrabber = new DecisionDataGrabber(scenario, decisionDataProvider, coordAnalyzer, 
				householdsTracker, householdObjectAttributes);	
		
		// create mobsim and run it for 1 second - doing so is required to create the mobsim agents
		config.getQSimConfigGroup().setEndTime(1.0);
		Mobsim mobsim = new QSimFactory().createMobsim(scenario, EventsUtils.createEventsManager());
		// SimulationListeners are fired in reverse order! Therefore add householdsTracker after decisionDataGrabber.
		((QSim) mobsim).addQueueSimulationListeners(decisionDataGrabber);
		((QSim) mobsim).addQueueSimulationListeners(householdsTracker);		
		mobsim.run();
		
		// read people in panic from file
		String panicFile = dummyController.getControlerIO().getIterationFilename(0, PanicModel.panicModelFile);
		PanicModel panicModel = new PanicModel(decisionDataProvider, EvacuationConfig.panicShare);
		panicModel.readDecisionsFromFile(panicFile);
		panicModel.printStatistics();
		
		// read pickup behavior from file
		String pickupFile = dummyController.getControlerIO().getIterationFilename(0, PickupModel.pickupModelFile);
		PickupModel pickupModel = new PickupModel(decisionDataProvider);
		pickupModel.readDecisionsFromFile(pickupFile);
		pickupModel.printStatistics();
		
		// read evacuation decisions from file
		String evacuationDecisionFile = dummyController.getControlerIO().getIterationFilename(0, EvacuationDecisionModel.evacuationDecisionModelFile);
		EvacuationDecisionModel evacuationDecisionModel = new EvacuationDecisionModel(scenario, MatsimRandom.getLocalInstance(), decisionDataProvider);
		evacuationDecisionModel.readDecisionsFromFile(evacuationDecisionFile);
		evacuationDecisionModel.printStatistics();
				
		// Create the set of analyzed modes.
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(TransportMode.bike);
		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.ride);
		transportModes.add(TransportMode.walk);
		transportModes.add(PassengerDepartureHandler.passengerTransportMode);
		
		/*
		 * Class to create dummy MobsimAfterSimStepEvents.
		 * Has to be added as first Listener to the EventsManager!
		 */
		AfterSimStepEventsCreator afterSimStepEventsCreator = new AfterSimStepEventsCreator(mobsimListeners);
		eventsManager.addHandler(afterSimStepEventsCreator);
		
		// Initialize AgentsInEvacuationAreaCounter
		double scaleFactor = 1 / config.getQSimConfigGroup().getFlowCapFactor();
		AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(scenario, transportModes, 
				coordAnalyzer.createInstance(), decisionDataProvider, scaleFactor);
		controlerListeners.add(agentsInEvacuationAreaCounter);
		mobsimListeners.add(agentsInEvacuationAreaCounter);
		eventsManager.addHandler(agentsInEvacuationAreaCounter);
		
		// Initialize AgentsInEvacuationAreaActivityCounter
		AgentsInEvacuationAreaActivityCounter agentsInEvacuationAreaActivityCounter = new AgentsInEvacuationAreaActivityCounter(scenario, 
				coordAnalyzer.createInstance(), decisionDataProvider, scaleFactor);
		controlerListeners.add(agentsInEvacuationAreaActivityCounter);
		mobsimListeners.add(agentsInEvacuationAreaActivityCounter);
		eventsManager.addHandler(agentsInEvacuationAreaActivityCounter);
		
		// MobsimInitializedListener
		MobsimInitializedEvent<Mobsim> mobsimInitializedEvent = new MobsimInitializedEventImpl<Mobsim>(null);
		for(MobsimListener mobsimListener : mobsimListeners) {
			if (mobsimListener instanceof MobsimInitializedListener) {
				((MobsimInitializedListener) mobsimListener).notifyMobsimInitialized(mobsimInitializedEvent);
			}
		}
		
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);
		
		//IterationEndsListener
		IterationEndsEvent iterationEndsEvent = new IterationEndsEvent(dummyController, 0);
		for(ControlerListener controlerListener : controlerListeners) {
			if (controlerListener instanceof IterationEndsListener) {
				((IterationEndsListener) controlerListener).notifyIterationEnds(iterationEndsEvent);
			}
		}
	}
	
	private static class AfterSimStepEventsCreator implements BasicEventHandler {
	
		private final List<MobsimListener> mobsimListeners;
		private double lastSimStep = 0.0; 
		
		public AfterSimStepEventsCreator(List<MobsimListener> mobsimListeners) {
			this.mobsimListeners = mobsimListeners;
		}
		
		@Override
		public void handleEvent(Event event) {
			double time = event.getTime();
			while (time > lastSimStep) {
				MobsimAfterSimStepEvent<Mobsim> e = new MobsimAfterSimStepEventImpl<Mobsim>(null, lastSimStep);
				
				for(MobsimListener mobsimListener : mobsimListeners) {
					if (mobsimListener instanceof MobsimAfterSimStepListener) {
						((MobsimAfterSimStepListener) mobsimListener).notifyMobsimAfterSimStep(e);
					}
				}
				
				lastSimStep++;
			}
		}
		
		@Override
		public void reset(int iteration) {
			// nothing to do here
		}
	}
	
	/**
	 * Create a dummy controler that provides a ControlerIO object but that
	 * does not run.
	 * 
	 * @author cdobler
	 */
	private static class DummyController extends Controler {

		private final OutputDirectoryHierarchy controlerIO;
		
		public DummyController(Scenario scenario) {
			super(scenario);
			
			String outputPath = this.scenarioData.getConfig().controler().getOutputDirectory();
			if (outputPath.endsWith("/")) {
				outputPath = outputPath.substring(0, outputPath.length() - 1);
			}
			if (this.scenarioData.getConfig().controler().getRunId() != null) {
				this.controlerIO = new OutputDirectoryHierarchy(outputPath, this.scenarioData.getConfig().controler().getRunId(), true);
			} else {
				this.controlerIO = new OutputDirectoryHierarchy(outputPath, true);
			}
		}
		
		@Override
		public OutputDirectoryHierarchy getControlerIO() {
			return this.controlerIO;
		}
		
		@Override
		public void run() {
			// don't do anything here
		}
	}
}
