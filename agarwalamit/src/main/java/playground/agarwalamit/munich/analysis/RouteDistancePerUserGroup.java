/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.legModeHandler.LegModeRouteDistanceDistributionHandler;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class RouteDistancePerUserGroup {

	public RouteDistancePerUserGroup() {
		super();
		this.sc = LoadMyScenarios.loadScenarioFromNetworkPlansAndConfig(plansFile, networkFile, configFile);
		this.usrGrpExtended = new UserGroupUtilsExtended();
		userGrpToBoxPlotData = new TreeMap<UserGroup, List<Double>>();
	}

	private Logger logger = Logger.getLogger(RouteDistancePerUserGroup.class);
	private  String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/ci/";/*"./output/run2/";*/
	private  String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private  String plansFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private String configFile = outputDir+"/output_config.xml";
	private int lastIteration = LoadMyScenarios.getLastIteration(configFile);
	private String eventsFile = outputDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
	private Scenario sc;
	private UserGroupUtilsExtended usrGrpExtended;
	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MeanDistance = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MedianDistance = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private SortedMap<String, Map<Id<Person>, List<Double>>> mode2PersonId2RouteDist;
	private SortedMap<UserGroup, List<Double>> userGrpToBoxPlotData;
	private final String mainMode = "car";
	
	public static void main(String[] args) {
		RouteDistancePerUserGroup routeDistUG = new RouteDistancePerUserGroup();
		routeDistUG.run();
	}

	private void run(){
		LegModeRouteDistanceDistributionHandler	lmdfed = new LegModeRouteDistanceDistributionHandler(sc);
		
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(lmdfed);
		reader.readFile(eventsFile);
		
		this.mode2PersonId2RouteDist = lmdfed.getMode2PersonId2TravelDistances();
		getUserGroupDistanceMeanAndMeadian();
		createBoxPlotData(lmdfed.getLegMode2PersonId2TotalTravelDistance().get(mainMode));
		writeResults(this.outputDir+"/analysis/");
	}

	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/usrGrp2TravelMode2MeanAndMedianRouteDistance.txt");
		try {
			writer.write("UserGroup \t travelMode \t MeanRouteDistance \t MedianRouteDistance \n");
			for(UserGroup ug:this.usrGrp2Mode2MeanDistance.keySet()){
				for(String travelMode:this.usrGrp2Mode2MeanDistance.get(ug).keySet()){
					writer.write(ug+"\t"+travelMode+"\t"+this.usrGrp2Mode2MeanDistance.get(ug).get(travelMode)+"\t"+this.usrGrp2Mode2MedianDistance.get(ug).get(travelMode)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}
		this.logger.info("Data writing is finished.");
		
		
		try {
			for(UserGroup ug :this.userGrpToBoxPlotData.keySet()){
				writer = IOUtils.getBufferedWriter(outputFolder+"/usrGrp2TravelDistanceBoxPlotData_"+ug+".txt");
				writer.write(ug+"\n");
				for(double d :this.userGrpToBoxPlotData.get(ug)){
					writer.write(d+"\n");
				}
				writer.close();
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}
	}
	
	private void createBoxPlotData (Map<Id<Person>, Double> map){
		PersonFilter pf = new PersonFilter();
		
		for(UserGroup ug:UserGroup.values()){
			Population relevantPop = pf.getPopulation(sc.getPopulation(), ug);
			userGrpToBoxPlotData.put(ug, this.usrGrpExtended.getTotalStatListForBoxPlot(map, relevantPop));
		}
	}
	private void getUserGroupDistanceMeanAndMeadian(){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug:UserGroup.values()){
			Population pop = pf.getPopulation(sc.getPopulation(), ug);
			this.usrGrp2Mode2MeanDistance.put(ug, this.usrGrpExtended.calculateTravelMode2MeanFromLists(mode2PersonId2RouteDist, pop));
			this.usrGrp2Mode2MedianDistance.put(ug, this.usrGrpExtended.calculateTravelMode2MedianFromLists(mode2PersonId2RouteDist, pop));
		}
	}
}
