package playground.balac.utils;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public class ExtractFacilities {

	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(scenario);
	
	ScenarioImpl scenario_new = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary");
	String outputPath = "C:/Users/balacm/Desktop/EIRASS_Paper/10pc_input_files/facilities_land_price.xml";
	//final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Avignon/teleatlas_1pc/out/retailers_300_speedfactor_1/RetailersSummary");
	//String outputPath = "C:/Users/balacm/Desktop/Retailers/OldFrancescosResults/facilities_extraceted_retailers_old.xml";
	private int numberOfFirstRetailer = 28;
	private int numberOfSecondRetailer = 18;
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
	
	
	public void extractFacilities(String facilitiesPath, String networkFilePath) throws IOException {
		networkReader.readFile(networkFilePath);
		//populationReader.readFile(plansFilePath);
		fr.readFile(facilitiesPath);
		int insideCust = 0;
		int outsideCust = 0;
		int inside = 0;
		int outside = 0;
		double centerX = 683217.0; 
	      double centerY = 247300.0;
	      CoordImpl coord = new CoordImpl(centerX, centerY);
	      double landPrice = 0.0;
		//readLink.readLine();
		
			for(int i = 0; i < numberOfFirstRetailer; i++) {
			
				String s = readLink.readLine();
				String[] arr = s.split("\t");
				ActivityFacility f = scenario.getActivityFacilities().getFacilities().get(new IdImpl(arr[1]));
				
				landPrice += CoordUtils.calcDistance(f.getCoord(), coord) * (-0.00001) + 1.0;
				if (CoordUtils.calcDistance(coord, f.getCoord()) < 5000) {
					
					insideCust +=  Integer.parseInt(arr[5]);
					
					inside++;
				}
				else {
					outsideCust +=  Integer.parseInt(arr[5]);
					outside++;
					
				}
				//scenario_new.getActivityFacilities().getFacilities().put(new IdImpl(arr[1]), f);
				scenario_new.getActivityFacilities().addActivityFacility(f);
				
			}
			
			for(int i = 0; i < numberOfSecondRetailer; i++) {
				
				String s = readLink.readLine();
				String[] arr = s.split("\t");
				ActivityFacility f = scenario.getActivityFacilities().getFacilities().get(new IdImpl(arr[1]));
				landPrice += CoordUtils.calcDistance(f.getCoord(), coord) * (-0.00001) + 1.0;
				//scenario_new.getActivityFacilities().getFacilities().put(new IdImpl(arr[1]), f);
				if (CoordUtils.calcDistance(coord, f.getCoord()) < 5000) {
					
					insideCust +=  Integer.parseInt(arr[5]);
					inside++;
				}
				else {
					outsideCust +=  Integer.parseInt(arr[5]);
					outside++;
					
				}
				scenario_new.getActivityFacilities().addActivityFacility(f);
			}
		
			new FacilitiesWriter(scenario_new.getActivityFacilities()).write(outputPath);
		
		System.out.println(inside);
		System.out.println(outside);
		System.out.println(insideCust);
		System.out.println(outsideCust);
		System.out.println(landPrice);
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		ExtractFacilities ef = new ExtractFacilities();
		ef.extractFacilities("C:/Users/balacm/Desktop/FreeSpeedFactor1.output_facilities.xml.gz", args[0]);
		//ef.extractFacilities("C:/Users/balacm/Desktop/Avignon/teleatlas_1pc/out/retailers_300_speedfactor_1/FreeSpeedFactor1.output_facilities.xml.gz");
	}

}
