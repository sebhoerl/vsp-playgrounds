package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CarsUsedLocationsffSHP {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
	        
		Set<String> a1 = new TreeSet<String>();
		Set<String> a2 = new TreeSet<String>();
	        Network network = scenario.getNetwork();
	        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system
	       
	    	
			final BufferedReader readLink1 = IOUtils.getBufferedReader(args[1]);
			final BufferedReader readLink2 = IOUtils.getBufferedReader(args[2]);

			

			HashMap<String, String> mapa = new HashMap<String, String>();

			
			 Collection featuresMovedIncrease = new ArrayList();
		        featuresMovedIncrease = new ArrayList<SimpleFeature>();
		        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
		                setCrs(crs).
		                setName("nodes").
		                addAttribute("ID", String.class).
		               //addAttribute("Customers", Integer.class).
		                //addAttribute("Be Af Mo", String.class).
		                
		                create();
		        
		      
			String s = readLink1.readLine();
			s = readLink1.readLine();
			
			while (s != null) {
				
				String[] arr = s.split("\\s");
				mapa.put(arr[0].substring(4), arr[3]);
				s = readLink1.readLine();
				
				
			}
			s = readLink2.readLine();
			s = readLink2.readLine();
			int i = 0;
			while( s != null) {
				String[] arr = s.split("\t");
			
				if (arr[1].contains("W")) {
					String b = arr[1].substring(0,4);
					
					SimpleFeature ft = nodeFactory.createPoint(network.getLinks().get(new IdImpl(arr[4])).getCoord(), new Object[] {Integer.toString(i)}, null);
					featuresMovedIncrease.add(ft);
					
				
					i++;
					}
				
				s = readLink2.readLine();
			}
	        
	        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/used_ff_nofreedist_all.shp");
	     

	}

}
