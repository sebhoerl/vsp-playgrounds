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

package playground.johannes.gsv.synPop.mid.run;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.DeleteMissingTimesTask;
import playground.johannes.gsv.synPop.DeleteNegativeDurationTask;
import playground.johannes.gsv.synPop.DeleteOverlappingLegsTask;
import playground.johannes.gsv.synPop.FixMissingActTimesTask;
import playground.johannes.gsv.synPop.InsertActivitiesTask;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTaskComposite;
import playground.johannes.gsv.synPop.ProxyPlanTaskComposite;
import playground.johannes.gsv.synPop.RoundTripTask;
import playground.johannes.gsv.synPop.SetActivityTimeTask;
import playground.johannes.gsv.synPop.SetActivityTypeTask;
import playground.johannes.gsv.synPop.SetFirstActivityTypeTask;
import playground.johannes.gsv.synPop.SortLegsTimeTask;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.LegDistanceHandler;
import playground.johannes.gsv.synPop.mid.LegEndTimeHandler;
import playground.johannes.gsv.synPop.mid.LegMainPurposeHandler;
import playground.johannes.gsv.synPop.mid.LegModeHandler;
import playground.johannes.gsv.synPop.mid.LegOriginHandler;
import playground.johannes.gsv.synPop.mid.LegRoundTrip;
import playground.johannes.gsv.synPop.mid.LegSortedIdHandler;
import playground.johannes.gsv.synPop.mid.LegStartTimeHandler;
import playground.johannes.gsv.synPop.mid.PersonDayHandler;
import playground.johannes.gsv.synPop.mid.PersonMonthHandler;
import playground.johannes.gsv.synPop.mid.PersonMunicipalityClassHandler;
import playground.johannes.gsv.synPop.mid.PersonStateHandler;
import playground.johannes.gsv.synPop.mid.PersonWeightHandler;
import playground.johannes.gsv.synPop.mid.TXTReader;

/**
 * @author johannes
 *
 */
public class PopulationGenerator {
	
	private static final Logger logger = Logger.getLogger(PopulationGenerator.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String personFile = "/home/johannes/gsv/mid2008/raw/MiD2008_PUF_Personen.txt";
		String legFile = "/home/johannes/gsv/mid2008/raw/MiD2008_PUF_Wege.txt";
		String outFile = "/home/johannes/gsv/mid2008/pop/pop.xml";
		/*
		 * setup text parser
		 */
		TXTReader reader = new TXTReader();
		reader.addPersonAttributeHandler(new PersonMunicipalityClassHandler());
		reader.addPersonAttributeHandler(new PersonWeightHandler());
		reader.addPersonAttributeHandler(new PersonDayHandler());
		reader.addPersonAttributeHandler(new PersonStateHandler());
		reader.addPersonAttributeHandler(new PersonMonthHandler());
		
		reader.addLegAttributeHandler(new LegSortedIdHandler());
		reader.addLegAttributeHandler(new LegMainPurposeHandler());
		reader.addLegAttributeHandler(new LegOriginHandler());
		reader.addLegAttributeHandler(new LegRoundTrip());
		reader.addLegAttributeHandler(new LegStartTimeHandler());
		reader.addLegAttributeHandler(new LegEndTimeHandler());
		reader.addLegAttributeHandler(new LegDistanceHandler());
		reader.addLegAttributeHandler(new LegModeHandler());
		/*
		 * read files
		 */
		logger.info("Reading persons...");
		Collection<ProxyPerson> persons = reader.read(personFile, legFile).values();
		logger.info(String.format("Read %s persons.", persons.size()));
		/*
		 * sort legs
		 */
		ProxyPlanTaskComposite composite = new ProxyPlanTaskComposite();
		composite.addComponent(new SortLegsTimeTask());
		logger.info("Sorting legs...");
		ProxyTaskRunner.run(composite, persons);
		/*
		 * filter legs		
		 */
		ProxyPersonTaskComposite pComposite = new ProxyPersonTaskComposite();
		pComposite.addComponent(new DeleteNegativeDurationTask());
		pComposite.addComponent(new DeleteMissingTimesTask());
		pComposite.addComponent(new DeleteOverlappingLegsTask());
		
		logger.info(String.format("Filtering %s legs...", persons.size()));
		persons = ProxyTaskRunner.runAndDelete(pComposite, persons);
		logger.info(String.format("After filter: %s persons.", persons.size()));
		/*
		 * generate activities
		 */
		composite = new ProxyPlanTaskComposite();
		composite.addComponent(new InsertActivitiesTask());
		composite.addComponent(new SetActivityTypeTask());
		composite.addComponent(new SetFirstActivityTypeTask());
		composite.addComponent(new RoundTripTask());
		composite.addComponent(new SetActivityTimeTask());
		composite.addComponent(new FixMissingActTimesTask());
		
		logger.info("Applying person tasks...");
		ProxyTaskRunner.run(composite, persons);
		logger.info("Done.");
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(outFile, persons);
		logger.info("Done.");
	}

}
