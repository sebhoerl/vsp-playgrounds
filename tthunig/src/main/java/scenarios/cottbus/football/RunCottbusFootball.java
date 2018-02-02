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
package scenarios.cottbus.football;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.base.config.ModifiableConfiguration;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.signalsystems.cottbus.CottbusFansControlerListener;
import playground.dgrether.signalsystems.cottbus.CottbusFootballAnalysisControllerListener;
import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFanCreator;
import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFootballStrings;
import playground.dgrether.signalsystems.cottbus.footballdemand.SimpleCottbusFanCreator;
import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerConfig;
import signals.sylvia.controler.DgSylviaConfig;
import utils.ModifyNetwork;

/**
 * @author tthunig, based on dgrether CottbusFootballBatch
 *
 */
public class RunCottbusFootball {
	private static final Logger LOG = Logger.getLogger(RunCottbusFootball.class);
	
	private enum SignalControl {FIXED, FIXED_IDEAL, SYLVIA, SYLVIA_IDEAL, LAEMMER, NONE};
	private static final SignalControl CONTROL_TYPE = SignalControl.LAEMMER;
	private static final boolean CHECK_DOWNSTREAM = false;
	
	private static final boolean LONG_LANES = true;
	private static final double FLOW_CAP = .7;
	private static final int STUCK_TIME = 600;
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config baseConfig;
		if (args != null && args.length != 0){
			baseConfig = ConfigUtils.loadConfig(args[0]);
		} else {
			String configFileName = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/config.xml";
			baseConfig = ConfigUtils.loadConfig(configFileName);
			String scenarioDescription = "flowCap" + FLOW_CAP + "_longLanes" + LONG_LANES + "_stuckTime" + STUCK_TIME;
			baseConfig.controler().setOutputDirectory("../../runs-svn/cottbus/football/" + scenarioDescription + "/run1200/");
			baseConfig.controler().setRunId("1200");
		}
		baseConfig.controler().setLastIteration(0);
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(baseConfig, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		switch (CONTROL_TYPE){
		case FIXED_IDEAL:
			signalsConfigGroup.setSignalControlFile("signal_control_no_13_laemmersFixedGroups.xml");
			break;
		case SYLVIA:
			signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13.xml");
			break;
		case SYLVIA_IDEAL:
			signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13_idealized.xml");
		case LAEMMER:
			signalsConfigGroup.setSignalControlFile("signal_control_laemmer.xml");
			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmer.xml");
//			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmerLinkBased.xml");
//			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmer2phases.xml");
//			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmer_doublePhases.xml");
			break;
		case NONE:
			signalsConfigGroup.setUseSignalSystems(false);
			signalsConfigGroup.setSignalControlFile(null);
			signalsConfigGroup.setSignalGroupsFile(null);
			signalsConfigGroup.setSignalSystemFile(null);
			break;
		default:
			break;
		}
		
		baseConfig.plans().setInputFile("cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only_100it_MS_cap"+FLOW_CAP+".xml.gz");
		
		baseConfig.qsim().setFlowCapFactor(FLOW_CAP);
		baseConfig.qsim().setStorageCapFactor( FLOW_CAP / Math.pow(FLOW_CAP,1/4.) );
		
		baseConfig.qsim().setStuckTime(STUCK_TIME);
		baseConfig.qsim().setEndTime(36*3600);
		
		Scenario baseScenario = ScenarioUtils.loadScenario(baseConfig);
		if (LONG_LANES){
			// extend short lanes (needed for laemmer)
			ModifyNetwork.lengthenAllLanes(baseScenario);
		}
		
		// add missing scenario elements
		if (!CONTROL_TYPE.equals(SignalControl.NONE))
			baseScenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(baseConfig).loadSignalsData());
		//create the output directoy
		String baseOutputDirectory = baseConfig.controler().getOutputDirectory();
		if (! baseOutputDirectory.endsWith("/")){
			baseOutputDirectory = baseOutputDirectory.concat("/");
		}
		switch (CONTROL_TYPE){
		case FIXED_IDEAL:
			baseOutputDirectory+= "fixedTimeIdeal";
			break;
		case FIXED:
			baseOutputDirectory+= "fixedTime"; 
			break;
		case SYLVIA:
			baseOutputDirectory+= "sylvia_maxExt1.5_fixedCycle";
			break;
		case SYLVIA_IDEAL:
			baseOutputDirectory+= "sylviaIdeal_maxExt1.5_fixedCycle";
		case LAEMMER:
//			baseOutputDirectory+= "laemmer_doubleGroups";
			baseOutputDirectory+= "laemmer_nicoGroups";
			break;
		case NONE:
			baseOutputDirectory+= "noSignals";
			break;
		}
		baseOutputDirectory+= "/";
		LOG.info("using base output directory: " + baseOutputDirectory);
		Population fanPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		//initialize variables needed in the loop
		String runId = baseConfig.controler().getRunId();
		Map<Integer, Double> percentageOfFans2AverageTTMap = new HashMap<>();
		Map<Integer, Double> percentageOfFans2TotalTTMap = new HashMap<>();
		Map<Integer, Double> percentageOfFans2noStuckedAgents = new HashMap<>();
		//fan creator
		String kreisShapeFile = "../../shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		CottbusFanCreator fanCreator = new SimpleCottbusFanCreator(kreisShapeFile);
		//start the runs
		int increment = 5;
		for (int numberOfFootballFans = 0; numberOfFootballFans <= 100; numberOfFootballFans = numberOfFootballFans + increment){
			if (numberOfFootballFans != 0) {
				// create additional football fans (from 0 to 2000)
				Population p = fanCreator.createAndAddFans(baseScenario, 20 * increment);
				for (Person pers : p.getPersons().values()){
					fanPop.addPerson(pers);
				}
			}
		
			baseConfig.controler().setOutputDirectory(baseOutputDirectory + numberOfFootballFans + "_football_fans/");
			baseConfig.controler().setRunId(runId + "_" + numberOfFootballFans + "_football_fans");
			Controler controler = new Controler(baseScenario);
			controler.addControlerListener(new CottbusFansControlerListener(fanPop));
			//add average tt handler for football fans
			CottbusFootballAnalysisControllerListener cbfbControllerListener = new CottbusFootballAnalysisControllerListener();
			controler.addControlerListener(cbfbControllerListener);
			//add the signals module
			if (!CONTROL_TYPE.equals(SignalControl.NONE)) {
				CombinedSignalsModule signalsModule = new CombinedSignalsModule();
				DgSylviaConfig sylviaConfig = new DgSylviaConfig();
				sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(false);
				sylviaConfig.setSignalGroupMaxGreenScale(1.5);
				sylviaConfig.setCheckDownstream(CHECK_DOWNSTREAM);
				signalsModule.setSylviaConfig(sylviaConfig);
				LaemmerConfig laemmerConfig = new LaemmerConfig();
				laemmerConfig.setDesiredCycleTime(90);
		        laemmerConfig.setMaxCycleTime(135);
//				laemmerConfig.setMinGreenTime(5);
//				laemmerConfig.setAnalysisEnabled(true);
				laemmerConfig.setCheckDownstream(CHECK_DOWNSTREAM);
				signalsModule.setLaemmerConfig(laemmerConfig);
				controler.addOverridingModule(signalsModule);
			}
			
			controler.run();
			if (cbfbControllerListener.getAverageTraveltime() != null){
				percentageOfFans2AverageTTMap.put(numberOfFootballFans, cbfbControllerListener.getAverageTraveltime());
				percentageOfFans2TotalTTMap.put(numberOfFootballFans, cbfbControllerListener.getTotalTraveltime());
				percentageOfFans2noStuckedAgents.put(numberOfFootballFans, cbfbControllerListener.getNumberOfStuckedPersons() + 0.);
			}
		}
		
//		try {
//			new SelectedPlans2ESRIShape(baseScenario.getPopulation(), baseScenario.getNetwork(), MGC.getCRS(TransformationFactory.WGS84_UTM33N), "/media/data/work/matsimOutput/run1219/" ).write();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		writeAnalysis(percentageOfFans2AverageTTMap, baseOutputDirectory + "average_traveltimes_last_iteration.csv", "Average travel time");
		writeAnalysis(percentageOfFans2TotalTTMap, baseOutputDirectory + "total_traveltimes_last_iteration.csv", "Total travel time");
		writeAnalysis(percentageOfFans2noStuckedAgents, baseOutputDirectory + "numberOfStuckedAgents_last_iteration.csv", "Number of stucked agents");
	}
		
	private static void writeAnalysis(Map<Integer, Double> map, String filename, String headerColumn2) throws FileNotFoundException, IOException{
		SortedMap<Integer, Double> sorted = new TreeMap<>();
		sorted.putAll(map);
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("Football fans %\t" + headerColumn2);
		writer.newLine();
		for (Entry<Integer, Double> e : sorted.entrySet()){
			writer.write(e.getKey().toString() + "\t" + e.getValue().toString());
			writer.newLine();
		}
		writer.close();
	}

	
	private static void createOutputDirectory(String outputDirectory){
		File outdir = new File(outputDirectory);
		if (outdir.exists()){
			throw new IllegalArgumentException("Output directory " + outputDirectory + " already exists!");
		}
		else {
			outdir.mkdir();
		}
	}

	
}
