/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.acmarmol.microcensus2010;

import java.util.ArrayList;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.acmarmol.utils.CoordConverter;
import playground.acmarmol.utils.EtappeConverter;

/**
* 
* Creates MATSim-DB xml files from Microcensus2010 database.
* 
*
* @author acmarmol
* 
*/

public class MZ2010ToXmlFiles {

	private final static Logger log = Logger.getLogger(MZ2010ToXmlFiles.class);
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {
		
		System.out.println("MATSim-DB: creating xml files from MicroCensus 2010 database \n");
		
		// from local directory
		//	String inputBase = "D:/balmermi/documents/data/mz/2010/3_DB_SPSS/dat files/";
		//	args = new String[] {
		//			inputBase+"haushalte.dat",
		//			inputBase+"haushaltspersonen.dat",
		//			inputBase+"fahrzeuge.dat",
		//			inputBase+"zielpersonen.dat",
		//			inputBase+"wege.dat",
		//			inputBase+"ausgaenge.dat",
		//			"D:/balmermi/documents/eclipse/output/mz/"
		//	};
		
		// from your directory
		String inputBase = "P:/Daten/Mikrozensen Verkehr Schweiz/2010/3_DB_SPSS/dat files/";
		args = new String[] {
				inputBase+"haushalte.dat",
				inputBase+"haushaltspersonen.dat",
				inputBase+"fahrzeuge.dat",
				inputBase+"zielpersonen.dat",
				inputBase+"wege.dat",
				inputBase+"ausgaenge.dat",
				inputBase+"etappen.dat",
				"C:/local/marmolea/output/MicroCensus2010/"
		};
		
		if (args.length != 8) {
			log.error("createMZ2Plans haushalteFile haushaltspersonenFile fahrzeugeFile zielpersonenFile wegeFile ausgaengeFile etappenFile outputBase");
			System.exit(-1);
		}

		Gbl.startMeasurement();
		
		// store input parameters
		String haushalteFile = args[0];
		String haushaltspersonenFile = args[1];
		String fahrzeugeFile = args[2];
		String zielpersonenFile = args[3];
		String wegeFile = args[4];
		String ausgaengeFile = args[5];
		String etappenFile = args[6];
		String outputBase = args[7];

		// print input parameters
		log.info("haushalteFile: "+haushalteFile);
		log.info("haushaltspersonenFile: "+haushaltspersonenFile);
		log.info("fahrzeugeFile: "+fahrzeugeFile);
		log.info("zielpersonenFile: "+zielpersonenFile);
		log.info("wegeFile: "+wegeFile);
		log.info("ausgaengeFile: "+ausgaengeFile);
		log.info("etappenFile: "+etappenFile);
		log.info("outputBase: "+outputBase);
		System.out.println("\n");
		
		
		// you will need to create households, persons and vehicles, incl. additional object attributes
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseHouseholds(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		Population population = scenario.getPopulation();
		ObjectAttributes populationAttributes = new ObjectAttributes();
		Households households = scenario.getHouseholds();
		ObjectAttributes householdAttributes = new ObjectAttributes();
		Vehicles vehicles = scenario.getVehicles();
		ObjectAttributes vehiclesAttributes = new ObjectAttributes();
		ObjectAttributes wegeAttributes = new ObjectAttributes();
				
		Gbl.printElapsedTime();
		
		// use the logger to print messages

//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing haushalteFile...");
		// first, parse the household file to create the household database
		// it will fill up the household and the attributes
		new MZHouseholdParser(households,householdAttributes).parse(haushalteFile);
		log.info("done. (parsing haushalteFile)");
				
		Gbl.printElapsedTime();

		// write intermediate results
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.00.xml");
		ObjectAttributesXmlWriter households_axmlw = new ObjectAttributesXmlWriter(householdAttributes);
		households_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		households_axmlw.writeFile(outputBase+"/householdAttributes.00.xml");
		log.info("done. (writing)");
				
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();

//////////////////////////////////////////////////////////////////////	
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing haushaltspersonenFile...");
		new MZHouseholdPersonParser(households,householdAttributes).parse(haushaltspersonenFile);
		log.info("done. (parsing haushaltspersonenFile)");
				
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();

		// write intermediate results
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.01.xml");
		households_axmlw.writeFile(outputBase+"/householdAttributes.01.xml");
		log.info("done. (writing)");
		
		Gbl.printElapsedTime();
		Gbl.printMemoryUsage();
		
//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing fahrzeugeFile...");
		// next fill up the vehicles. For that you need to doublecheck consistency with the households (as given in the MZ database structure)
		// and probably add additional data to the households too
		new MZVehicleParser(vehicles,vehiclesAttributes,households,householdAttributes).parse(fahrzeugeFile);
		log.info("done. (parsing fahrzeugeFile)");
				
		Gbl.printElapsedTime();

		// write intermediate results
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.02.xml");
		households_axmlw.writeFile(outputBase+"/householdAttributes.02.xml");
		new VehicleWriterV1(vehicles).writeFile(outputBase+"vehicles.02.xml");
		new ObjectAttributesXmlWriter(vehiclesAttributes).writeFile(outputBase+"/vehiclesAttributes.02.xml");
		log.info("done. (writing)");
		
		Gbl.printElapsedTime();
		
//////////////////////////////////////////////////////////////////////	
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing zielpersonenFile...");
		new MZZielPersonParser(population,populationAttributes,households,householdAttributes).parse(zielpersonenFile);
		log.info("done. (parsing zielpersonenFile)");
				
		Gbl.printElapsedTime();

		// write intermediate results
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.03.xml");
		households_axmlw.writeFile(outputBase+"/householdAttributes.03.xml.gz");
		new PopulationWriter(population, null).write(outputBase+"population.03.xml");
		ObjectAttributesXmlWriter population_axmlw = new ObjectAttributesXmlWriter(populationAttributes);
		population_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		population_axmlw.writeFile(outputBase+"/populationAttributes.03.xml");
		log.info("done. (writing)");
		
		int original_pop_size = population.getPersons().size();
		
		Gbl.printElapsedTime();
		
//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing wegeFile...");
		ArrayList<Set<Id>> pids = new MZWegeParser(population, wegeAttributes).parse(wegeFile);
		log.info("done. (parsing wegeFile)");
		
		Gbl.printElapsedTime();
		
		// write intermediate results
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.04.xml");
		households_axmlw.writeFile(outputBase+"/householdAttributes.04.xml.gz");
		new PopulationWriter(population, null).write(outputBase+"population.04.xml");
		population_axmlw.writeFile(outputBase+"/populationAttributes.04.xml");
		ObjectAttributesXmlWriter wege_axmlw = new ObjectAttributesXmlWriter(wegeAttributes);
		wege_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		wege_axmlw.putAttributeConverter(Etappe.class, new EtappeConverter());
		wege_axmlw.writeFile(outputBase+"/wegeAttributes.00.xml");

		log.info("done. (writing)");
		
		Gbl.printElapsedTime();
//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing etappenFile...");
		new MZEtappenParser(wegeAttributes).parse(etappenFile);
		wege_axmlw.writeFile(outputBase+"/wegeAttributes.01.xml");
		log.info("done. (parsing wegeFile)");
		
		Gbl.printElapsedTime();
//////////////////////////////////////////////////////////////////////
		
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("setting work locations...");
		MZPopulationUtils.setWorkLocations(population, populationAttributes);
		System.out.println("      done.");
		System.out.println("      Writing population with work coords set xml file \n");	
		new PopulationWriter(population, null).write(outputBase+"population.05.xml");
		System.out.println("  done.");

//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("setting work locations...");
		MZPopulationUtils.setWorkLocations(population, populationAttributes);
		System.out.println("      done.");
		System.out.println("      Writing population with work coords set xml file \n");	
		new PopulationWriter(population, null).write(outputBase+"population.05.xml");
		System.out.println("  done.");

		
		
//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("setting home locations...");
		MZPopulationUtils.setHomeLocations(population, householdAttributes, populationAttributes);
		System.out.println("      done.");
		System.out.println("      Writing population with home coords set xml file \n");
		new PopulationWriter(population, null).write(outputBase+"population.06.xml");
		System.out.println("  done.");
		

//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("removing persons with coord inconsistencies...");
		Set<Id> coord_err_pids = pids.get(0);
		if(coord_err_pids.size()>0){
			MZPopulationUtils.removePlans(population, coord_err_pids);
			System.out.println("      done.");
			System.out.println("      Total persons removed: " +  coord_err_pids.size());
			System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
			System.out.println("      Writing population without coord. inconsistencies xml file \n");	
			new PopulationWriter(population, null).write(outputBase+"population.07.xml");
			System.out.println("  done.");
			
			}else{System.out.println("      NO PEOPLE WITH COORD INCONSISTENCIES \n");} 
		
//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("removing persons with time inconsistencies...");
		Set<Id> time_err_pids = pids.get(1);
		if(time_err_pids.size()>0){
		MZPopulationUtils.removePlans(population, time_err_pids);
		System.out.println("      done.");
		System.out.println("      Total persons removed: " + time_err_pids.size());
		System.out.println("      Remaining population size: " + population.getPersons().size()+" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
		System.out.println("      Writing population without time  inconsistencies xml file \n");	
		new PopulationWriter(population, null).write(outputBase+"population.08.xml");
		System.out.println("  done.");
		
		}else{System.out.println("      NO PEOPLE WITH TIME INCONSISTENCIES \n");}

//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("handling border-crossing trips...");
		Set<Id> border_crossing_wids = pids.get(3);
		if(border_crossing_wids.size()>0){
		//MZPopulationUtils.HandleBorderCrossingTrips(population, wegeAttributes, border_crossing_wids);
		System.out.println("      done.");
		System.out.println("      Total trips handled: " + border_crossing_wids.size());
		System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
		System.out.println("      Writing population without undefined coords xml file \n");	
		new PopulationWriter(population, null).write(outputBase+"population.09.xml");
		System.out.println("  done.");
	
		}else{System.out.println("      NO BORDER CROSSING TRIPS \n");}

//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("removing persons with undefined negative coords...");
		Set<Id> undef_neg_pids = MZPopulationUtils.identifyPlansWithUndefinedNegCoords(population);
		if(undef_neg_pids.size()>0){
		MZPopulationUtils.removePlans(population, undef_neg_pids);
		System.out.println("      done.");
		System.out.println("      Total persons removed: " + undef_neg_pids.size());
		System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
		System.out.println("      Writing population without undefined coords xml file \n");	
		new PopulationWriter(population, null).write(outputBase+"population.10.xml");
			System.out.println("NUMBER OF UNDEFINED NEGATIVE COORDS "+undef_neg_pids.size());
		System.out.println("  done.");
	
		}else{System.out.println("      NO PEOPLE WITH TIME INCONSISTENCIES \n");}

//////////////////////////////////////////////////////////////////////

//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("removing persons with too long walk trips...");
//		Set<Id> long_walk_pids = MZPopulationUtils.identifyPlansWithTooLongWalkTrips(population);
//		if(long_walk_pids.size()>0){
//		MZPopulationUtils.removePlans(population, long_walk_pids);
//		System.out.println("      done.");
//		System.out.println("      Total persons removed: " + long_walk_pids.size());
//		System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
//		System.out.println("      Writing population without long walk trips xml file \n");	
//		new PopulationWriter(population, null).write(outputBase+"population.11.xml");
//		System.out.println("NUMBER OF PEOPLE WITH TOO LONG WALK COORDS "+long_walk_pids.size());
//		System.out.println("  done.");
//		
//		}else{System.out.println("      NO PEOPLE WITH TOO LONK WALK TRIPS \n");}


//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("Finished filtering population. Las population size = "+ population.getPersons().size());

		// and so on
				
		// and you do not need to instantiate this.
		// use it only for the main routine
//		 createMZ2Plans(config);

		System.out.println("#################################################################################\n" +
				   		   "################################FINISHED#########################################\n" +
						   "#################################################################################");	
		Gbl.printElapsedTime();
		
	System.out.println(border_crossing_wids.size());
	}//end main		


}

	
	
	
	
	
	
	

		

		
	

	
		

		

	
	

