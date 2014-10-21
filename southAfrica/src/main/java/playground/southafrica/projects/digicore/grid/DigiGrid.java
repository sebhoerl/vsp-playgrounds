/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.digicore.grid;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Quad;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.lights.Light;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * Class that acts as the container for the three-dimensional grid containing
 * the centroids of the polyhedra that is used for the Digicore accelerometer
 * research. Associated with the grid is the number of observations in each
 * dodecahedron (cell) and also the rating of each cell.
 *
 * @author jwjoubert
 */
public class DigiGrid  extends AbstractAnalysis {
	final private Logger LOG = Logger.getLogger(DigiGrid.class);

	private OcTree<Coord3d> ot;

	private Map<Coord3d, Double> map;
	private Map<Coord3d, Integer> mapRating;

	private List<Double> riskThresholds;
	
	private final double scale;
	private double pointsConsidered;
	private Visual visual = Visual.NONE;
	private boolean isPopulated = false;
	private boolean isRanked = false;
	
	/* Specify colours */
	final static Color DIGI_GREEN = new Color(147, 214, 83, 255);
	final static Color DIGI_YELLOW = new Color(248, 215, 85, 255);
	final static Color DIGI_ORANGE = new Color(250, 164, 54, 255);
	final static Color DIGI_RED = new Color(246, 43, 32, 255);
	final static Color DIGI_GRAY = new Color(100, 100, 100, 255);
	


	public DigiGrid(final double scale) {
		this.scale = scale;
	}
	

	public void setupGrid(String filename){
		/* Check that risk zone thresholds have been set. */
		if(riskThresholds == null){
			LOG.error("Cannot build the grid without risk thresholds.");
			throw new RuntimeException("First set thresholds with setRiskThresholds() method.");
		}
		
		LOG.info("Calculating the data extent...");
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				double z = Double.parseDouble(sa[7]);
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				minZ = Math.min(minZ, z);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				maxZ = Math.max(maxZ, z);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();

		/* Establish the centroid grid given the point extent. */
		FCCGrid fccg = new FCCGrid(minX, maxX, minY, maxY, minZ, maxZ, scale);
		GridPoint[] ga = fccg.getFcGrid();
		for(GridPoint gp : ga){
			minX = Math.min(minX, gp.getX());
			minY = Math.min(minY, gp.getY());
			minZ = Math.min(minZ, gp.getZ());
			maxX = Math.max(maxX, gp.getX());
			maxY = Math.max(maxY, gp.getY());
			maxZ = Math.max(maxZ, gp.getZ());
		}

		/* Establish and populate the OcTree given the centroid extent. */
		map = new HashMap<Coord3d, Double>(ga.length);
		LOG.info("Building OcTree with dodecahedron centroids...");
		ot = new OcTree<Coord3d>(minX, minY, minZ, maxX, maxY, maxZ);
		for(GridPoint gp : ga){
			Coord3d c = new Coord3d(gp.getX(), gp.getY(), gp.getZ());
			ot.put(gp.getX(), gp.getY(), gp.getZ(), c);
			map.put(c, new Double(0.0));
		}
		LOG.info("Done populating centroid grid: " + ga.length + " points.");
	}
	
	/* Cells are ranked based on the number of records associated with them */  
	private Comparator<Coord3d> getGridComparator(){
		return new Comparator<Coord3d>() {
			@Override
			public int compare(Coord3d o1, Coord3d o2) {
				return map.get(o2).compareTo(map.get(o1));
			}
		};
	}
	
	public double getCount(Coord3d c){
		return this.map.get(c);
	}
	
	
	public void incrementCount(Coord3d c, double weight){
		this.map.put(c, this.map.get(c)+weight);
		this.pointsConsidered++;
		this.isPopulated = true;
	}
	
	
	public double getScale(){
		return this.scale;
	}
	
	public Coord3d getClosest(double x, double y, double z){
		return this.ot.get(x, y, z);
	}
	
	public int getCellRisk(Coord3d c){
		return this.mapRating.get(c);
	}
	
	/** 
	 * Sort the polyhedra based on their counts only.
	 */
	public void rankGridCells(){
		if(map.size() == 0){
			throw new RuntimeException("Cannot rank zero cells. Grid has possibly not been populated yet.");
		}
		LOG.info("Ranking polyhedra cells based on point-counts only.");

		List<Coord3d> sortedCoords = new ArrayList<Coord3d>(map.keySet());
		Collections.sort(sortedCoords, getGridComparator());
		
		/* Report the top 20 cell values. */
		LOG.info("   20 polyhedra with largest number of observations:");
		for(int i = 0; i < 20; i++){
			LOG.info(String.format("      %d: %.1f observations", i+1, map.get(sortedCoords.get(i))));
		}
		
		double totalAdded = 0.0;
		double cumulative = 0.0;
		mapRating = new TreeMap<Coord3d, Integer>(getGridComparator());
		
		List<Coord3d> centroidsToRemove = new ArrayList<Coord3d>();

		double maxValue = 0.0;
		for(int i = 0; i < sortedCoords.size(); i++){
			Coord3d c = sortedCoords.get(i);
			double obs = map.get(c);
			if(obs > 0){
				maxValue = Math.max(maxValue, (double)obs);
				totalAdded += (double)obs;
				cumulative = totalAdded / pointsConsidered;
				
				/* Get the rating class for this value. */
				Integer ratingZone = null;
				int zoneIndex = 0;
				while(ratingZone == null && zoneIndex < this.riskThresholds.size()){
					if(cumulative <= this.riskThresholds.get(zoneIndex)){
						ratingZone = new Integer(zoneIndex);
					} else{
						zoneIndex++;
					}
				}
				mapRating.put(c, ratingZone);
			} else{
				centroidsToRemove.add(c);
			}
		}
		
		/* Remove zero-count dodecahedra. */
		for(Coord3d c : centroidsToRemove){
			map.remove(c);
		}
		
		this.isRanked = true;
		LOG.info("Done ranking polyhedra cells.");
		LOG.info("A total of " + map.size() + " dodecahedra contains points (max value: " + maxValue + ")");
	}
	
	public void setRiskThresholds(List<Double> riskThresholds){
		this.riskThresholds = riskThresholds;
	}
	
	
	public void visualiseGrid(){
		LOG.info("Visualising results...");
		if(this.visual == Visual.NONE){
			LOG.error("Visualisation is switched off! First set using setVisualisation()");
		} else{
			try {
				AnalysisLauncher.open(this);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot visualise!!");
			}
		}
	}
	
	public void visualiseGrid(Visual visual){
		if(visual != this.visual){
			LOG.warn("Requested visualisation is different than set visualisation.");
			LOG.warn("Set visualisation will be changed.");
		}
		setVisual(visual);
		visualiseGrid();
	}

	public void setVisual(Visual visual){
		this.visual = visual;
	}
	
	
	/**
	 * Currently four visualisations are supported:
	 * <ul>
	 * 	<li><b>NONE</b>: no visualisation;
	 * 	<li><b>CENTROID</b>: only draws the centroids of each cell;
	 * 	<li><b>SLICE</b>: takes a horizontal slice at z=? (currently this z-value is hard-coded);
	 * 	<li><b>POLYHEDRA</b>: draws all the dodecahedra.
	 * </ul>
	 * 
	 * @author jwjoubert
	 */
	public static enum Visual{NONE,CENTROID,SLICE,POLYHEDRA}
	
	@Override
	public void init() throws Exception {
		switch (this.visual) {
		case CENTROID:
			printCentroids();
			break;
		case SLICE:
			printPolyhedraSlice();
			break;
		case POLYHEDRA:
			printPolyhedra();
			break;
		default:
			throw new RuntimeException("Don't know how to visualise " + this.visual.toString());
		}
	}
	
	
	private void printCentroids(){
		/* Cool, now let's plot the buggers. First as spheres. */
		int index = 0;
		Coord3d[] coords = new Coord3d[map.size()];
		Color[] colors = new Color[map.size()];
		for(Coord3d c : map.keySet()){
			coords[index] = c;
			colors[index++] = new Color(0, 0, 0, 255);
		}
		Scatter scatter = new Scatter(coords, colors, 6f);
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
		chart.getView().updateBounds();
		chart.getScene().add(scatter);
	}
	
	
	private void printPolyhedra(){
		/* Set up the chart. */
		chart = AWTChartComponentFactory.chart(Quality.Nicest, "awt");
		Light light = chart.addLight(new Coord3d(-1000f, -300f, 12000f));
		light.setRepresentationRadius(100);
		light.setEnabled(true);
		light.setEnabled(false);

		chart.addKeyController();
		Coord3d oldViewPoint = chart.getCanvas().getView().getViewPoint();
		Coord3d newViewPoint = new Coord3d(oldViewPoint.x, oldViewPoint.y/3, 500);

		chart.getView().setSquared(false);
		chart.getView().setSquared(true);
		chart.getView().setMaximized(true);
		chart.getView().setViewPoint(newViewPoint, true);
		chart.getView().setViewPositionMode(ViewPositionMode.FREE);
		chart.getView().setBoundManual(new BoundingBox3d(-800f, 800f, -800f, 800f, 600f, 1500f));
		chart.getView().updateBounds();
		
		for(Coord3d c : map.keySet()){
			/* Set up polyhedra shapes. */
			ArrayList<org.jzy3d.plot3d.primitives.Polygon> body = new ArrayList<org.jzy3d.plot3d.primitives.Polygon>();
			
			/* Change colour based on rating zone. */
			Color fillColor = null;
			int zone = mapRating.get(c);
			switch (zone) {
			case 0:
				fillColor = DIGI_GREEN;
				break;
			case 1:
				fillColor = DIGI_YELLOW;
				break;
			case 2:
				fillColor = DIGI_ORANGE;
				break;
			case 3:
				fillColor = DIGI_RED;
				break;
			default:
			}
			if(fillColor == null){
				LOG.error("Something wrong!!");
			}
			
			/* TODO Change this as required. Limit faces to certain colours. */
			int highestRiskZoneToDraw = 3;
			if(zone <= highestRiskZoneToDraw){
				/* Create faces. */
				FCCPolyhedron poly = new FCCPolyhedron(c.x, c.y, c.z, scale);
				for(NDPolygon face : poly.getFcPolyhedron()){
					Quad q = new Quad();
					for(GridPoint point : face.getPolyFace()){
						q.add(new Point(new Coord3d(point.getX(), point.getY(), point.getZ())));
					}
					body.add((org.jzy3d.plot3d.primitives.Polygon)q);
				}
				
				Shape shape = new Shape(body);
				shape.setFaceDisplayed(true);
				shape.setColor(fillColor);
				shape.setFaceDisplayed(true);
				shape.setWireframeColor(DIGI_GRAY);
				chart.getScene().add(shape);
			}

			chart.getView().shoot();
		}
	}
	
	
	private void printPolyhedraSlice(){
		/* Set up the chart. */
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
		Light light = chart.addLight(new Coord3d(-1000f, -300f, 12000f));
		light.setRepresentationRadius(100);
		light.setEnabled(false);
		
		chart.addKeyController();
		Coord3d viewPoint = new Coord3d(2*Math.PI/2, Math.PI/2, 2000);
		
		chart.getView().setSquared(false);
		chart.getView().setSquared(true);
		chart.getView().setMaximized(true);
		chart.getView().setViewPoint(viewPoint, true);
		chart.getView().setViewPositionMode(ViewPositionMode.FREE);
		chart.getView().setBoundManual(new BoundingBox3d(-600f, 600f, -700f, 700f, 600f, 1500f));
		chart.getView().updateBounds();
		
		for(Coord3d c : map.keySet()){
			/* Change colour based on rating zone. */
			Color fillColor = null;
			int zone = mapRating.get(c);
			switch (zone) {
			case 0:
				fillColor = DIGI_GREEN;
				break;
			case 1:
				fillColor = DIGI_YELLOW;
				break;
			case 2:
				fillColor = DIGI_ORANGE;
				break;
			case 3:
				fillColor = DIGI_RED;
				break;
			default:
			}
			if(fillColor == null){
				LOG.error("Something wrong!!");
			}
			
			
			/* Create slice face. */
//			for(double depth = 800; depth <= 1200; depth+=10){
			{double depth = 1009.0;
				FCCPlanePolygon fccPoly = new FCCPlanePolygon(c.x, c.y, c.z, 3, depth, scale);
				Polygon poly = new Polygon();
				
				for(GridPoint point : fccPoly.getFcPlanePolygon().getPolyFace()){
					poly.add(new Point(new Coord3d(point.getX(), point.getY(), 0)));
				}
				
				poly.setFaceDisplayed(true);
				poly.setColor(fillColor);
				poly.setWireframeColor(DIGI_GRAY);
				chart.getScene().add(poly);
				
				/* FIXME Get a screenshot working!! */
				try {
					File temp = File.createTempFile("myTempImage", ".tmp");
					chart.screenshot(temp);
					BufferedImage img = ImageIO.read(temp);
					ImageIO.write( img, "png", new File("/User/jwjoubert/Desktop/png.png"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				chart.getView().shoot();
//				try {
//					chart.screenshot(new File("/Users/jwjoubert/Pictures/Digicore/Slice_" + depth + ".png"));
//				} catch (IOException e) {
//					e.printStackTrace();
//					LOG.error("Could not write image for depth " + depth);
//				}
			}
		}
	}
	
	/**
	 * Writes the accelerometer 'blob' results: the number of observations in
	 * each cell (only those with a value greater than zero), and the risk class
	 * of the cell. The output file with name <code>cellValuesAndRiskClasses.csv</code>
	 * will be created in the output folder.
	 * 
	 * @param outputFolder
	 */
	public void writeCellCountsAndRiskClasses(String outputFolder){
		if(map.size() == 0 || mapRating == null){
			throw new RuntimeException("Insufficient data to write. Either no grids, or no ranking.");
		}
		String filename = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "cellValuesAndRiskClasses.csv";
		LOG.info("Writing the cell values and risk classes to " + filename); 
		
		/* Report the risk thresholds for which this output holds. */
		LOG.info("  \\_ Accelerometer risk thresholds:");
		for(int i = 0; i < this.riskThresholds.size(); i++){
			LOG.info(String.format("      \\_ Risk %d: %.4f", i, this.riskThresholds.get(i)));
		}
		
		/* Write the cell values and their risk classes. */
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			/* Header. */
			bw.write("x,y,z,count,class");
			bw.newLine();
			
			for(Coord3d c : this.map.keySet()){
				bw.write(String.format("%.4f, %.4f,%.4f,%.1f,%d\n", c.x, c.y, c.z, this.map.get(c), this.mapRating.get(c)));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done writing cell values and risk classes.");
	}
	
	public boolean isRanked(){
		return this.isRanked;
	}
	
	public boolean isPopulated(){
		return this.isPopulated;
	}
	
	public void populateFromGridFile(String filename){
		LOG.info("Building grid from " + filename);
		double maxValue = Double.NEGATIVE_INFINITY;

		LOG.info("Calculating the data extent...");
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine();
			while( (line = br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[0]);
				double y = Double.parseDouble(sa[1]);
				double z = Double.parseDouble(sa[2]);
				minX = Math.min(minX, x-2*scale);
				minY = Math.min(minY, y-2*scale);
				minZ = Math.min(minZ, z-2*scale);
				maxX = Math.max(maxX, x+2*scale);
				maxY = Math.max(maxY, y+2*scale);
				maxZ = Math.max(maxZ, z+2*scale);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();

		/* Populate the grid. */
		LOG.info("Building and populating the OcTree with dodecahedron centroids...");
		map = new HashMap<Coord3d, Double>((int) counter.getCounter());
		mapRating = new TreeMap<Coord3d, Integer>(getGridComparator());
		ot = new OcTree<Coord3d>(minX, minY, minZ, maxX, maxY, maxZ);
		
		counter.reset();
		br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine();
			while( (line = br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[0]);
				double y = Double.parseDouble(sa[1]);
				double z = Double.parseDouble(sa[2]);
				double count = Double.parseDouble(sa[3]);
				int riskClass = Integer.parseInt(sa[4]);
				
				maxValue = Math.max(maxValue, count);
				Coord3d c = new Coord3d(x, y, z);
				ot.put(x, y,z, c);
				map.put(c, count);
				mapRating.put(c, riskClass);
				
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		
		LOG.info("Done building grid");
		this.isPopulated = true;
		this.isRanked = true;
		
		LOG.info("A total of " + map.size() + " dodecahedra contains points (max value: " + maxValue + ")");
	}
}