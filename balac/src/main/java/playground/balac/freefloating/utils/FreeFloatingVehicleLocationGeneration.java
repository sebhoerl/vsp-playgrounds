package playground.balac.freefloating.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

public class FreeFloatingVehicleLocationGeneration {
	public void run (String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
	    config.network().setInputFile("C:/Users/balacm/Desktop/Retailers_10pc/network.xml.gz");
	    Scenario scenario = ScenarioUtils.loadScenario(config);
		BufferedWriter output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/vehiclesFF_sameRBStations_3.txt")));
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);
	    String s = reader.readLine();
	    s = reader.readLine();
	    LinkUtils linkUtils = new LinkUtils(scenario.getNetwork());
	    while(s != null) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
			Link l = linkUtils.getClosestLink(coordStart);		    	
	    	
			output.write(l.getId() + "\t" + "3");
	    	output.newLine();
			
	    	s = reader.readLine();
	    	
	    }	 
	  /*  for (int i = 1; i < 1000; i++) {
	    	int rd = MatsimRandom.getRandom().nextInt(numberLinks);
	    	LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl)network.getLinks().values().toArray()[rd], (NetworkImpl)network, Double.valueOf(0.0D), Double.valueOf(0.0D));
	    	
	    	output.write(link.getId() + "\t" + "1");
	    	output.newLine();
	    }*/
	    
	    output.flush();
	    output.close();
		
	}
	public static void main(String[] args) throws IOException {
	
		FreeFloatingVehicleLocationGeneration ff = new FreeFloatingVehicleLocationGeneration();
		
		ff.run(args);
		
		
	}
	private class LinkUtils {
		
		Network network;
		public LinkUtils(Network network) {
			
			this.network = network;		}
		
		public LinkImpl getClosestLink(Coord coord) {
			
			double distance = (1.0D / 0.0D);
		    Id closestLinkId = new IdImpl(0L);
		    for (Link link : network.getLinks().values()) {
		      LinkImpl mylink = (LinkImpl)link;
		      Double newDistance = Double.valueOf(mylink.calcDistance(coord));
		      if (newDistance.doubleValue() < distance) {
		        distance = newDistance.doubleValue();
		        closestLinkId = link.getId();
		      }

		    }

		    return (LinkImpl)network.getLinks().get(closestLinkId);
			
			
		}
	}

}
