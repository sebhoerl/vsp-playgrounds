/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.demandde.cemdap.input;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.demandde.cemdap.LogToOutputSaver;

/**
 * This class is derived from "playground.dziemke.cemdapMatsimCadyts.oneperson.DemandGeneratorOnePersonV2.java"
 * In contrast to its predecessors, it creates a full population based on the Zensus 2001. People are assigned
 * commuter relations based on the Pendlerstatistik 2009.
 * 
 * @author dziemke
 */
public class DemandGeneratorCensus {
	private static final Logger LOG = Logger.getLogger(DemandGeneratorCensus.class);

	// Storage objects
	private Population population;
	private Map<Id<Household>, Household> households;
	private ObjectAttributes municipalities;
	private Map<String, Map<String, CommuterRelationV2>> relationsMap;
	private String outputBase;
	
	// Optionally used storage objects
	private List<String> spatialRefinementZoneIds;
	
	// Parameters
	private List<String> idsOfFederalStatesIncluded;
	private int numberOfPlansPerPerson;
	private double defaultAdultsToEmployeesRatio;
	private double defaultEmployeesToCommutersRatio;
	boolean includeChildren = false;
	boolean writeMatsimPlanFiles = false;
	
	// Optionally used parameters
	private String shapeFileForSpatialRefinement;
	private String idOfMunicipailityForSpatialRefinement;
	private String featureKeyInShapeFile;

	// Counters
	private int counterMissingComRel = 0;
	private int counterExternalCommuters = 0;
	private int counterComRelUnassigned = 0;
	private int allEmployees = 0;
	private int allPersons = 0;
	private int allStudents = 0;
	

	public static void main(String[] args) {
		// Input and output files
		String commuterFileOutgoing1 = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Berlin_2009/B2009Ga.txt";
		String commuterFileOutgoing2 = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga.txt";
		String commuterFileOutgoing3 = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil2BR2009Ga.txt";
		String commuterFileOutgoing4 = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil3BR2009Ga.txt";
		String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4};
		String censusFile = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung_BE_BB.csv";
		String outputBase = "../../shared-svn/studies/countries/de/berlin_scenario_2016/syn_pop/100_test/";
		
		// Parameters
		int numberOfPlansPerPerson = 5;
		List<String> idsOfFederalStatesIncluded = Arrays.asList("12");
		// Default ratios are used for cases where information is missing, which is the case for smaller municipalities.
		double defaultAdultsToEmployeesRatio = 1.23;  // Calibrated based on sum value from Zensus 2011.
		double defaultCensusEmployeesToCommutersRatio = 2.5;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.
		// Choosing this too high effects that too many commuter relations are created, which is uncritical as relative shares will still be correct.
		// Choosing this too low effects that employed people (according to the census) are left without workplace. Minimize this number!

		DemandGeneratorCensus demandGeneratorCensus = new DemandGeneratorCensus(commuterFilesOutgoing, censusFile, outputBase, numberOfPlansPerPerson,
				idsOfFederalStatesIncluded, defaultAdultsToEmployeesRatio, defaultCensusEmployeesToCommutersRatio);
		
		demandGeneratorCensus.setWriteMatsimPlanFiles(true);
		
		demandGeneratorCensus.setShapeFileForSpatialRefinement("../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/Bezirksregion_EPSG_25833.shp");
		demandGeneratorCensus.setIdOfMunicipailityForSpatialRefinement("11000000"); // "Amtliche Gemeindeschlüssel (AGS)" of Berlin is "11000000"
		demandGeneratorCensus.setFeatureKeyInShapeFile("SCHLUESSEL");
		
		demandGeneratorCensus.generateDemand();
	}

	
	public DemandGeneratorCensus(String[] commuterFilesOutgoing, String censusFile, String outputBase, int numberOfPlansPerPerson, 
			List<String> idsOfFederalStatesIncluded, double defaultAdultsToEmployeesRatio, double defaultEmployeesToCommutersRatio) {
		LogToOutputSaver.setOutputDirectory(outputBase);
		
		this.outputBase = outputBase;
		this.numberOfPlansPerPerson = numberOfPlansPerPerson;
		
		this.idsOfFederalStatesIncluded = idsOfFederalStatesIncluded;
		// adding a check for each id (length==2); since, that's what we are using in the end. Amit Aug'17
		this.idsOfFederalStatesIncluded.stream().forEach(e -> {
			if (e.length()!=2) throw new RuntimeException("Length of the id for each Federal State must be equal to 2. This is not the case for "+ e);
		});

		this.defaultAdultsToEmployeesRatio = defaultAdultsToEmployeesRatio;
		this.defaultEmployeesToCommutersRatio = defaultEmployeesToCommutersRatio;
		
		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		this.households = new HashMap<>();

		// Read census
		CensusReader censusReader = new CensusReader(censusFile, ";");
		this.municipalities = censusReader.getMunicipalities();

		// Read commuter relations
		this.relationsMap = new HashMap<>();
		for (String commuterFileOutgoing : commuterFilesOutgoing) {
			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing, "\t");
			Map<String, Map<String, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
			this.relationsMap.putAll(currentRelationMap);
		}
	}
	
	
	public void generateDemand() {
		if (this.shapeFileForSpatialRefinement != null && this.featureKeyInShapeFile != null) {
			this.spatialRefinementZoneIds = readShape(this.shapeFileForSpatialRefinement, this.featureKeyInShapeFile);
		} else {
			LOG.info("A shape file and/or and the attribute that contains the keys has not ben provided.");
		}
				
		int counter = 1;
		
		for (String munId : relationsMap.keySet()) { // Loop over municipalities from commuter file
			Map<String, CommuterRelationV2> relationsFromMunicipality = relationsMap.get(munId);

			// Employees in census are all employees, not only socially-secured employees
			if (this.municipalities.getAttribute(munId, "employedMale") == null || this.municipalities.getAttribute(munId, "employedFemale") == null) {
				LOG.warn("Employed male (and possibly other) information is not available in the census data for munId "+ munId + ". Skipping this municipality.");
				continue;
			}

			int employeesMale = (int) this.municipalities.getAttribute(munId, "employedMale");
			int employeesFemale = (int) this.municipalities.getAttribute(munId, "employedFemale");

			scaleRelations(relationsFromMunicipality, employeesMale, employeesFemale, this.defaultEmployeesToCommutersRatio);
			List<String> commuterRelationListMale = createRelationList(relationsFromMunicipality, "male");
			List<String> commuterRelationListFemale = createRelationList(relationsFromMunicipality, "female");

			int pop0_2Male = (int) this.municipalities.getAttribute(munId, "pop0_2Male");
			int pop3_5Male = (int) this.municipalities.getAttribute(munId, "pop3_5Male");
			int pop6_14Male = (int) this.municipalities.getAttribute(munId, "pop6_14Male");
			int pop15_17Male = (int) this.municipalities.getAttribute(munId, "pop15_17Male");
			int pop18_24Male = (int) this.municipalities.getAttribute(munId, "pop18_24Male");
			int pop25_29Male = (int) this.municipalities.getAttribute(munId, "pop25_29Male");
			int pop30_39Male = (int) this.municipalities.getAttribute(munId, "pop30_39Male");
			int pop40_49Male = (int) this.municipalities.getAttribute(munId, "pop40_49Male");
			int pop50_64Male = (int) this.municipalities.getAttribute(munId, "pop50_64Male");
			int pop65_74Male = (int) this.municipalities.getAttribute(munId, "pop65_74Male");
			int pop75PlusMale = (int) this.municipalities.getAttribute(munId, "pop75PlusMale");

			int pop0_2Female = (int) this.municipalities.getAttribute(munId, "pop0_2Female");
			int pop3_5Female = (int) this.municipalities.getAttribute(munId, "pop3_5Female");
			int pop6_14Female = (int) this.municipalities.getAttribute(munId, "pop6_14Female");
			int pop15_17Female = (int) this.municipalities.getAttribute(munId, "pop15_17Female");
			int pop18_24Female = (int) this.municipalities.getAttribute(munId, "pop18_24Female");
			int pop25_29Female = (int) this.municipalities.getAttribute(munId, "pop25_29Female");
			int pop30_39Female = (int) this.municipalities.getAttribute(munId, "pop30_39Female");
			int pop40_49Female = (int) this.municipalities.getAttribute(munId, "pop40_49Female");
			int pop50_64Female = (int) this.municipalities.getAttribute(munId, "pop50_64Female");
			int pop65_74Female = (int) this.municipalities.getAttribute(munId, "pop65_74Female");
			int pop75PlusFemale = (int) this.municipalities.getAttribute(munId, "pop75PlusFemale");

			int adultsMale = pop18_24Male + pop25_29Male + pop30_39Male + pop40_49Male + pop50_64Male;
			int adultsFemale = pop18_24Female + pop25_29Female + pop30_39Female + pop40_49Female + pop50_64Female;

			// The adults-to-employees ratio is needed to determine if a given person has a job
			double adultsToEmployeesMaleRatio;
			double adultsToEmployeesFemaleRatio;
			if (employeesMale != 0) { // Avoid dividing by zero
				adultsToEmployeesMaleRatio = (double) adultsMale / (double) employeesMale;
			} else {
				adultsToEmployeesMaleRatio = this.defaultAdultsToEmployeesRatio;
			}
			if (employeesFemale != 0) { // Avoid dividing by zero
				adultsToEmployeesFemaleRatio = (double) adultsFemale / (double) employeesFemale;
			} else {
				adultsToEmployeesFemaleRatio = this.defaultAdultsToEmployeesRatio;
			}

			if (includeChildren) {
				createHouseholdsAndPersons(counter, munId, pop0_2Male, 0, 0, 2, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop0_2Male;
				createHouseholdsAndPersons(counter, munId, pop0_2Female, 1, 0, 2, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop0_2Female;
				createHouseholdsAndPersons(counter, munId, pop3_5Male, 0, 3, 5, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop3_5Male;
				createHouseholdsAndPersons(counter, munId, pop3_5Female, 1, 3, 5, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop3_5Female;
				createHouseholdsAndPersons(counter, munId, pop6_14Male, 0, 6, 14, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop6_14Male;
				createHouseholdsAndPersons(counter, munId, pop6_14Female, 1, 6, 14, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop6_14Female;
				createHouseholdsAndPersons(counter, munId, pop15_17Male, 0, 15, 17, adultsToEmployeesMaleRatio, commuterRelationListMale);
				counter += pop15_17Male;
				createHouseholdsAndPersons(counter, munId, pop15_17Female, 1, 15, 17, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
				counter += pop15_17Female;
			}
			createHouseholdsAndPersons(counter, munId, pop18_24Male, 0, 18, 24, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop18_24Male;
			createHouseholdsAndPersons(counter, munId, pop18_24Female, 1, 18, 24, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop18_24Female;
			createHouseholdsAndPersons(counter, munId, pop25_29Male, 0, 25, 29, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop25_29Male;
			createHouseholdsAndPersons(counter, munId, pop25_29Female, 1, 25, 29, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop25_29Female;
			createHouseholdsAndPersons(counter, munId, pop30_39Male, 0, 30, 39, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop30_39Male;
			createHouseholdsAndPersons(counter, munId, pop30_39Female, 1, 30, 39, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop30_39Female;
			createHouseholdsAndPersons(counter, munId, pop40_49Male, 0, 40, 49, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop40_49Male;
			createHouseholdsAndPersons(counter, munId, pop40_49Female, 1, 40, 49, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop40_49Female;
			createHouseholdsAndPersons(counter, munId, pop50_64Male, 0, 50, 64, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop50_64Male;
			createHouseholdsAndPersons(counter, munId, pop50_64Female, 1, 50, 64, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop50_64Female;
			createHouseholdsAndPersons(counter, munId, pop65_74Male, 0, 65, 74, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop65_74Male;
			createHouseholdsAndPersons(counter, munId, pop65_74Female, 1, 65, 74, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop65_74Female;
			// 90 years as the upper bound is a simplifying assumption!
			createHouseholdsAndPersons(counter, munId, pop75PlusMale, 0, 75, 90, adultsToEmployeesMaleRatio, commuterRelationListMale);
			counter += pop75PlusMale;
			createHouseholdsAndPersons(counter, munId, pop75PlusFemale, 1, 75, 90, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			counter += pop75PlusFemale;

			// Information on unassigned commuter relations
			this.counterComRelUnassigned += commuterRelationListMale.size();
			if (commuterRelationListMale.size() > 100) {
				LOG.info(commuterRelationListMale.size() + " male commuter relations from " + munId +
						" remain unassigned; based on census, there are " + employeesMale + " male employees.");
			}
			this.counterComRelUnassigned += commuterRelationListFemale.size();
			if (commuterRelationListFemale.size() > 100) {
				LOG.info(commuterRelationListFemale.size() + " female commuter relations from " + munId +
						" remain unassigned; based on census, there are " + employeesFemale + " female employees.");
			}
		}
		
		
		// Write some relevant information on console
		LOG.warn("There are " + this.counterMissingComRel + " employees who have been set to unemployed since no commuter relation could be assigned to them.");
		LOG.warn("Share of employees that had to be set to unemployed due to lack of commuter relations: " + ((double) this.counterMissingComRel / (double) this.allEmployees));
		LOG.warn("Altogether " + this.counterComRelUnassigned + " commuter relations remain unassigned.");
		LOG.warn("There are " + this.counterExternalCommuters + " people who commute outside of Berlin and Brandenburg.");
		LOG.warn("Total number of employees: " + this.allEmployees);
		LOG.warn("Total population: " + this.allPersons);
		LOG.warn("Total number of students: " + this.allStudents);
		
		// Write output files
		writeHouseholdsFile(this.households, this.outputBase + "households.dat");
		writePersonsFile(this.population, this.outputBase + "persons.dat");
		if (this.writeMatsimPlanFiles) {
			writeMatsimPlansFile(this.population, this.outputBase + "plans.xml.gz");
		}

		// Create copies of population, but with different work locations
		for (int i = 1; i < numberOfPlansPerPerson; i++) { // "less than" because the plan consists already in the original
			Population population2 = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

			for (Person person : this.population.getPersons().values()) {
				// Choose new location of work, if applicable
				if ((boolean) person.getAttributes().getAttribute("employed")) {
					String locationOfWork = (String) person.getAttributes().getAttribute("locationOfWork");
					if (locationOfWork.equals("-99")) {
						throw new RuntimeException("This combination of attribute values is implaubible.");
					} else {
						if (locationOfWork.length() == 6) { // An LOR, i.e. a location inside Berlin // TODO generalize
							person.getAttributes().putAttribute("locationOfWork", getSpatiallyRefinedZone());
						} else if (locationOfWork.length() == 8) { // An "Amtliche Gemeindeschlüssel (AGS)", i.e. a location outside Berlin
							// Do nothing; leave it as it is
						} else {
							throw new RuntimeException("The identifier of the work location cannot have a length other than 6 or 8.");
						}
					}
				}
				// Choose new location of school, if applicable
				if ((boolean) person.getAttributes().getAttribute("student")) {
					String locationOfSchool = (String) person.getAttributes().getAttribute("locationOfSchool");
					if (locationOfSchool.equals("-99")) {
						throw new RuntimeException("This combination of attribute values is implaubible.");
					} else {
						if (locationOfSchool.length() == 6) { // An LOR, i.e. a location inside Berlin // TODO generalize
							person.getAttributes().putAttribute("locationOfSchool", getSpatiallyRefinedZone());
						} else if (locationOfSchool.length() == 8) { // An "Amtliche Gemeindeschlüssel (AGS)", i.e. a location outside Berlin
							// Do nothing; leave it as it is
						} else {
							throw new RuntimeException("The identifier of the work location cannot have a length other than 6 or 8.");
						}
					}					
				}
				population2.addPerson(person);
			}
			writePersonsFile(population2, this.outputBase + "persons" + (i+1) + ".dat");
			if (this.writeMatsimPlanFiles) {
				writeMatsimPlansFile(population2, this.outputBase + "plans" + (i+1) + ".xml.gz");
			}
		}
	}


	private void createHouseholdsAndPersons(int counter, String municipalityId, int numberOfPersons, int gender, int lowerAgeBound, int upperAgeBound, 
			double adultsToEmployeesRatio, List<String> commuterRelationList) {
		
		for (int i = 0; i < numberOfPersons; i++) {
			this.allPersons++;
			Id<Household> householdId = Id.create((counter + i), Household.class);
			HouseholdImpl household = new HouseholdImpl(householdId); // TODO Or use factory?
			household.getAttributes().putAttribute("numberOfAdults", 1); // Always 1; no household structure
			household.getAttributes().putAttribute("totalNumberOfHouseholdVehicles", 1);
			household.getAttributes().putAttribute("homeTSZLocation", getLocation(municipalityId));
			household.getAttributes().putAttribute("numberOfChildren", 0); // None, ignore them in this version
			household.getAttributes().putAttribute("householdStructure", 1); // 1 = single, no children
			
			Id<Person> personId = Id.create(householdId + "01", Person.class);
			Person person = this.population.getFactory().createPerson(personId);
			// The following attribute names inspired by "PersonUtils.java": "sex", "hasLicense", "carAvail", "employed", "age", "travelcards"
			person.getAttributes().putAttribute("householdId", householdId);
			boolean employed = false;
			if (lowerAgeBound < 65 && upperAgeBound > 17) { // younger and older people are never employed
				employed = getEmployed(adultsToEmployeesRatio);
			}
			person.getAttributes().putAttribute("employed", employed);
			
			boolean student = false;
			if (lowerAgeBound < 30 && upperAgeBound > 17 && !employed) { // younger and older people are never student; employed people neither
				student = true; // TODO quite simple assumption, which may be improved later
				allStudents++;
			}			
			person.getAttributes().putAttribute("student", student);
			
			if (employed) {
				allEmployees++;
				if (commuterRelationList.size() == 0) { // No relations left in list, which employee could choose from
					counterMissingComRel++;
					person.getAttributes().putAttribute("locationOfWork", "-99");
					person.getAttributes().putAttribute("employed", false);
				} else {
					String locationOfWork = getRandomWorkLocation(commuterRelationList);
					if (locationOfWork.length() == 8 && ! this.idsOfFederalStatesIncluded.contains(locationOfWork.substring(0,2))) { // TODO external commuter are currently treated as non workers
						counterExternalCommuters++;
						person.getAttributes().putAttribute("locationOfWork", "-99");
						person.getAttributes().putAttribute("employed", false);
					} else {
						person.getAttributes().putAttribute("locationOfWork", locationOfWork);
					}
				}
			} else {
				person.getAttributes().putAttribute("locationOfWork", "-99");
			}

			if (student) {
				// TODO quite simple assumption, which may be improved later
				person.getAttributes().putAttribute("locationOfSchool", getLocation(municipalityId));
			} else {
				person.getAttributes().putAttribute("locationOfSchool", "-99");
			}
			
			person.getAttributes().putAttribute("hasLicense", true); // for CEMDAP's "driversLicence" variable
			person.getAttributes().putAttribute("gender", gender); // for CEMDAP's "female" variable
			person.getAttributes().putAttribute("age", getAgeInBounds(lowerAgeBound, upperAgeBound));
			person.getAttributes().putAttribute("parent", false);
			
			this.population.addPerson(person);
			
			List<Id<Person>> personIds = new ArrayList<>(); // does in current implementation (only 1 p/hh) not make much sense
			personIds.add(personId);
			household.setMemberIds(personIds);
			this.households.put(householdId, household);
		}
	}	
	
			
	private static void scaleRelations(Map<String, CommuterRelationV2> relationsFromMunicipality, int employeesMale,
			int employeesFemale, double defaultEmployeesToCommutersRatio) {
		// Count all commuters starting in the given municipality
		int commutersMale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsMale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == null || relation.getTrips() == 0) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsMale((relation.getTrips() / 2));
				}
			}
			commutersMale += relation.getTripsMale();
		}
		int commutersFemale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsFemale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == null || relation.getTrips() == 0) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsFemale((relation.getTrips() / 2));
				}
			}
			commutersFemale += relation.getTripsFemale();
		}
		
		// Compute ratios
		double employeesToCommutersMaleRatio;
		double employeesToCommutersFemaleRatio;
		if (employeesMale != 0) {
			employeesToCommutersMaleRatio = (double) employeesMale / (double) commutersMale;
		} else {
			employeesToCommutersMaleRatio = defaultEmployeesToCommutersRatio;
		}
		if (employeesFemale != 0) {
			employeesToCommutersFemaleRatio = (double) employeesFemale / (double) commutersFemale;
		} else {
			employeesToCommutersFemaleRatio = defaultEmployeesToCommutersRatio;
		}
		
		// Scale
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsMale((int) Math.ceil(relation.getTripsMale() * employeesToCommutersMaleRatio));
		}
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsFemale((int) Math.ceil(relation.getTripsFemale() * employeesToCommutersFemaleRatio));
		}
	}


	private static List<String> createRelationList(Map<String, CommuterRelationV2> relationsFromMunicipality, String gender) {
		List<String> commuterRealtionsList = new ArrayList<>();
		for (String destination : relationsFromMunicipality.keySet()) {
			int trips;
			switch (gender) {
				case "male":
					trips = relationsFromMunicipality.get(destination).getTripsMale();
					break;
				case "female":
					trips = relationsFromMunicipality.get(destination).getTripsFemale();
					break;
				default:
					throw new IllegalArgumentException("Must either be male or female.");
			}
			for (int i = 0; i < trips ; i++) {
				commuterRealtionsList.add(destination);
			}
		}
		return commuterRealtionsList;
	}


	private String getRandomWorkLocation(List<String> commuterRelationList) {
		Random random = new Random();
		int position = random.nextInt(commuterRelationList.size());
		String workMunicipalityId = commuterRelationList.get(position);
		commuterRelationList.remove(position);
		return getLocation(workMunicipalityId);
	}


	private String getLocation(String municipalityId) {
		String locationId;
		if (municipalityId.equals(this.idOfMunicipailityForSpatialRefinement)){
			locationId = getSpatiallyRefinedZone();
		} else {
			locationId = municipalityId;
		}
		return locationId;
	}


	private String getSpatiallyRefinedZone() {
		Random random = new Random();
		return this.spatialRefinementZoneIds.get(random.nextInt(this.spatialRefinementZoneIds.size()));
	}


	private static boolean getEmployed(double adultsToEmployeesRatio) {
		return Math.random() * adultsToEmployeesRatio < 1;
	}
	
	
	private static int getAgeInBounds(int lowerBound, int upperBound) {
		return (int) (lowerBound + Math.random() * (upperBound - lowerBound + 1));
	}


	private static List<String> readShape(String shapeFile, String attributeKey) {
		List<String> lors = new ArrayList<>();
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

		for (SimpleFeature feature : features) {
			String key = (String) feature.getAttribute(attributeKey);
			lors.add(key);
		}
		return lors;
	}
	
	
	private static void writeHouseholdsFile(Map<Id<Household>, Household> households, String fileName) {
		BufferedWriter bufferedWriterHouseholds = null;
		
		try {
            File householdsFile = new File(fileName);
    		FileWriter fileWriterHouseholds = new FileWriter(householdsFile);
    		bufferedWriterHouseholds = new BufferedWriter(fileWriterHouseholds);
    		
    		for (Household household : households.values()) {
    			int householdId = Integer.parseInt(household.getId().toString());
    			int numberOfAdults = (Integer) household.getAttributes().getAttribute("numberOfAdults");
    			int totalNumberOfHouseholdVehicles = (Integer) household.getAttributes().getAttribute("totalNumberOfHouseholdVehicles");
    			int homeTSZLocation = Integer.parseInt(household.getAttributes().getAttribute("homeTSZLocation").toString());
    			int numberOfChildren = (Integer) household.getAttributes().getAttribute("numberOfChildren");
    			int householdStructure = (Integer) household.getAttributes().getAttribute("householdStructure");

    			// Altogether this creates 32 columns = number in query file
    			bufferedWriterHouseholds.write(householdId + "\t" + numberOfAdults + "\t" + totalNumberOfHouseholdVehicles
    					+ "\t" + homeTSZLocation + "\t" + numberOfChildren + "\t" + householdStructure + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0);
    			bufferedWriterHouseholds.newLine();
    		}
    		
    	} catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriterHouseholds != null) {
                    bufferedWriterHouseholds.flush();
                    bufferedWriterHouseholds.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		LOG.info("Households file " + fileName + " written.");
    }
	
	
	private static void writePersonsFile(Population population, String fileName) {
		BufferedWriter bufferedWriterPersons = null;
		
		try {
			File personFile = new File(fileName);
			FileWriter fileWriterPersons = new FileWriter(personFile);
			bufferedWriterPersons = new BufferedWriter(fileWriterPersons);
			    		    		
			for (Person person : population.getPersons().values()) {
				int householdId = Integer.parseInt(person.getAttributes().getAttribute("householdId").toString());
				int personId = Integer.parseInt(person.getId().toString());
				
				int employed;
				if ((boolean) person.getAttributes().getAttribute("employed")) {
					employed = 1;
				} else {
					employed = 0;
				}
				
				int student;
				if ((boolean) person.getAttributes().getAttribute("student")) {
					student = 1;
				} else {
					student = 0;
				}
				
				int driversLicence;
				if ((boolean) person.getAttributes().getAttribute("hasLicense")) {
					driversLicence = 1;
				} else {
					driversLicence = 0;
				}
				
				int locationOfWork = Integer.parseInt(person.getAttributes().getAttribute("locationOfWork").toString());
				int locationOfSchool = Integer.parseInt(person.getAttributes().getAttribute("locationOfSchool").toString());
				
				int female = (Integer) person.getAttributes().getAttribute("gender"); // assumes that female = 1
				int age = (Integer) person.getAttributes().getAttribute("age");
				
				int parent;
				if ((boolean) person.getAttributes().getAttribute("parent")) {
					parent = 1;
				} else {
					parent = 0;
				}
				
				// Altogether this creates 59 columns = number in query file
				bufferedWriterPersons.write(householdId + "\t" + personId + "\t" + employed  + "\t" + student
						+ "\t" + driversLicence + "\t" + locationOfWork + "\t" + locationOfSchool
						+ "\t" + female + "\t" + age + "\t" + parent + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
						+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 );
				bufferedWriterPersons.newLine();
			}
		} catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        //Close the BufferedWriter
	        try {
	            if (bufferedWriterPersons != null) {
	                bufferedWriterPersons.flush();
	                bufferedWriterPersons.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
		LOG.info("Persons file " + fileName + " written.");
	}


	private static void writeMatsimPlansFile(Population population, String fileName) {
	    MatsimWriter popWriter = new PopulationWriter(population);
	    popWriter.write(fileName);
	}
	

    public Population getPopulation() {
    	return this.population;
	}
    
    
    public void setShapeFileForSpatialRefinement(String shapeFileForSpatialRefinement) {
    	this.shapeFileForSpatialRefinement = shapeFileForSpatialRefinement;
    }
    
    
    public void setIdOfMunicipailityForSpatialRefinement(String idOfMunicipailityForSpatialRefinement) {
    	this.idOfMunicipailityForSpatialRefinement = idOfMunicipailityForSpatialRefinement;
    }
    
    
    public void setFeatureKeyInShapeFile(String featureKeyInShapeFile) {
    	this.featureKeyInShapeFile = featureKeyInShapeFile;
    }
    
    
    public void setWriteMatsimPlanFiles(boolean writeMatsimPlanFiles) {
    	this.writeMatsimPlanFiles = writeMatsimPlanFiles;
    }
    
    
    public void setIncludeChildren(boolean includeChildren) {
    	this.includeChildren = includeChildren;
    }    
}