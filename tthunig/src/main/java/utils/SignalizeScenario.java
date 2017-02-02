/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesFactory;
import org.matsim.lanes.data.LanesToLinkAssignment;

import com.amazonaws.services.kms.model.UnsupportedOperationException;

/**
 * This class converts a scenario without signals into a scenario with signals at all intersections.
 * The user may choose between (1) a simpler approach without lanes, where only one signal is created for every link
 * and (2) a more detailed approach with lanes, where a signal is created for every possible turning relation.
 * 
 * @author tthunig
 */
public class SignalizeScenario {

	/**
	 * call method with same name with signal control identifier DefaultPlanbasedSignalSystemController
	 */
	public static void createSignalsForAllLinks(Scenario scenario){
		createSignalsForAllLinks(scenario, DefaultPlanbasedSignalSystemController.IDENTIFIER);
	}
	
	/**
	 * Creates a signal system at every intersection which contains a signal on every link. 
	 * No lanes are used, i.e. from every signal one can reach all outgoing links.
	 * The signal information is saved in the given Scenario.
	 * 
	 * @param scenario a scenario which contains a network, but no signal or lane information
	 */
	public static void createSignalsForAllLinks(Scenario scenario, String signalControlIdentifier){
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setUseSignalSystems(true);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
	
		createSystemAtEveryNode(scenario, false);
		createGroupForEverySignal(scenario);
		createControlForEveryGroup(scenario, signalControlIdentifier);
	}
	
	/**
	 * call method with same name with signal control identifier DefaultPlanbasedSignalSystemController
	 */
	public static void createSignalsAndLanesForAllTurnings(Scenario scenario){
		createSignalsAndLanesForAllTurnings(scenario, DefaultPlanbasedSignalSystemController.IDENTIFIER);
	}

	/**
	 * On every link a lane is created for each outgoing link.
	 * Afterwards a signal system is created at every intersection which contains 
	 * a signal on every lane, i.e. for every turning relation.
	 * The lane and signal information is saved in the given Scenario.
	 * 
	 * @param scenario a scenario which contains a network, but no signal oder lane information
	 * @param signalControlIdentifier 
	 */
	public static void createSignalsAndLanesForAllTurnings(Scenario scenario, String signalControlIdentifier){
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), 
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setUseSignalSystems(true);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		scenario.getConfig().qsim().setUseLanes(true);

		createLanesForEveryInLinkOutLinkPair(scenario);
		createSystemAtEveryNode(scenario, true);
		createGroupForEverySignal(scenario);
		createControlForEveryGroup(scenario, signalControlIdentifier);
	}

	private static void createLanesForEveryInLinkOutLinkPair(Scenario scenario) {
		Lanes lanes = scenario.getLanes();
		LanesFactory fac = lanes.getFactory();

		if (!lanes.getLanesToLinkAssignments().isEmpty()){
			throw new UnsupportedOperationException("Creating lanes and signals at all intersections is only supported for scenarios that do not contain lanes yet.");
		}
		
		for (Link inLink : scenario.getNetwork().getLinks().values()){
			// Create lanes for every link with outgoing links
			if ((inLink.getToNode().getOutLinks() != null) && (!inLink.getToNode().getOutLinks().isEmpty())){
				LanesToLinkAssignment linkLanes = fac.createLanesToLinkAssignment(inLink.getId());
				lanes.addLanesToLinkAssignment(linkLanes);
				List<Id<Lane>> linkLaneIds = new ArrayList<>();
				for (Link outLink : inLink.getToNode().getOutLinks().values()) {
					// TODO decide for suitable capacity, lane length and alignment. use dg utils or lane v1 to v2 converter logik?
					double laneCap = inLink.getCapacity();
					double laneStart_mFromLinkEnd = inLink.getLength()/2;
					int alignment = 0;
					Id<Lane> laneId = Id.create(inLink.getId() + "." + outLink.getId(), Lane.class);
					LanesUtils.createAndAddLane(linkLanes, fac, laneId, laneCap, laneStart_mFromLinkEnd, alignment, 1, Collections.singletonList(outLink.getId()), null);
					linkLaneIds.add(laneId);
				}
				// create original lane
				LanesUtils.createAndAddLane(linkLanes, fac, Id.create(inLink.getId() + ".ol", Lane.class), inLink.getCapacity(), inLink.getLength(), 0, 1, null, linkLaneIds);
			}
		}
	}

	/**
	 * create a signal system at every node with signals at all links or lanes (if useLanes is true)
	 * 
	 * @param scenario
	 * @param useLanes
	 */
	private static void createSystemAtEveryNode(Scenario scenario, boolean useLanes) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsDataFactory fac = new SignalSystemsDataFactoryImpl();
	
		if (!signalsData.getSignalSystemsData().getSignalSystemData().isEmpty()){
			throw new UnsupportedOperationException("Creating signals at all intersections is only supported for scenarios that do not contain signals yet.");
		}
		
		for (Node node : scenario.getNetwork().getNodes().values()){
			// Create system for every node with outgoing and ingoing links
			if ((node.getOutLinks() != null) && (!node.getOutLinks().isEmpty())
					&& (node.getInLinks() != null) && (!node.getInLinks().isEmpty())) {
				// create signal system
				SignalSystemData signalSystem = fac.createSignalSystemData(Id.create("signalSystem" + node.getId(), SignalSystem.class));
				signalsData.getSignalSystemsData().addSignalSystemData(signalSystem);

				// go through all ingoing links
				for (Id<Link> linkId : node.getInLinks().keySet()) {
					if (useLanes) {
						// go through all ingoing lanes
						for (Lane lane : scenario.getLanes().getLanesToLinkAssignments().get(linkId).getLanes().values()) {
							// do not create signals for original lanes
							if (!lane.getId().toString().endsWith(".ol")) {
								SignalData signal = fac.createSignalData(Id.create("signal" + linkId + "." + lane.getId(), Signal.class));
								signalSystem.addSignalData(signal);
								signal.setLinkId(linkId);
								signal.addLaneId(lane.getId());
								for (Id<Link> toLinkId : lane.getToLinkIds()) {
									signal.addTurningMoveRestriction(toLinkId);
								}
							}
						}
					} else { // i.e. no lanes
						SignalData signal = fac.createSignalData(Id.create("signal" + linkId, Signal.class));
						signalSystem.addSignalData(signal);
						signal.setLinkId(linkId);
					}
				}
			}
		}
	}

	private static void createGroupForEverySignal(Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		for (SignalSystemData system : signalsData.getSignalSystemsData().getSignalSystemData().values()) {
			SignalUtils.createAndAddSignalGroups4Signals(signalsData.getSignalGroupsData(), system);
		}
	}

	/**
	 * Create an all day green signal control for every signal group.
	 * 
	 * @param scenario
	 * @param signalControlIdentifier 
	 */
	private static void createControlForEveryGroup(Scenario scenario, String signalControlIdentifier) {
		int CYCLE_TIME = 90;
		
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory fac = signalControl.getFactory();
		
		// create a signal control for all signal systems
		for (SignalSystemData signalSystem : signalSystems.getSignalSystemData().values()) {

			SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystem.getId());
			// create a default plan for the signal system (with defined cycle time and offset 0)
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
			signalSystemControl.addSignalPlanData(signalPlan);
			signalSystemControl.setControllerIdentifier(signalControlIdentifier);
			// add the signalSystemControl to the signalControl
			signalControl.addSignalSystemControllerData(signalSystemControl);

			// specify signal group settings for all signal groups of this signal system
			for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
				/* TODO decide which kind of signal control to create
				 * devide 90 sec into equal time slots for all directions?! 
				 * problem: corresponding directions cannot be found easily
				 * currently used: all day green: */
				int onset = 0;
				int dropping = CYCLE_TIME-1;
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroup.getId(), onset, dropping));
			}
		}
	}
	
}
