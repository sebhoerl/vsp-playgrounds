/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.analysis.tripstats;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacility;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author thibautd
 */
@Singleton
public class TripStatisticsCollectingEventHandler implements LegHandler, ActivityHandler, BeforeMobsimListener, AfterMobsimListener {
	private static final Logger log = Logger.getLogger(TripStatisticsCollectingEventHandler.class);
	private boolean collectStatistics = true;

	private final OutputDirectoryHierarchy io;
	private BufferedWriter writer = null;

	private final StageActivityTypes stages;
	private final MainModeIdentifier modeIdentifier;

	private final Map<Id<Person>, Record> currentTrips = new HashMap<>();

	private final Set<Id<Person>> personsToTrack;
	private final Scenario scenario;

	@Inject
	public TripStatisticsCollectingEventHandler(
			OutputDirectoryHierarchy io,
			TripRouter router,
			Scenario scenario) {
		this(
				io,
				router.getStageActivityTypes(),
				router.getMainModeIdentifier(),
				scenario.getPopulation().getPersons().keySet(),
				scenario);
	}

	public TripStatisticsCollectingEventHandler(
			OutputDirectoryHierarchy io,
			StageActivityTypes stages,
			MainModeIdentifier modeIdentifier,
			Set<Id<Person>> personsToTrack,
			Scenario scenario) {
		this.io = io;
		this.stages = stages;
		this.modeIdentifier = modeIdentifier;
		this.personsToTrack = personsToTrack;
		this.scenario = scenario;
	}

	@Override
	public void handleActivity(Id<Person> agentId, Activity activity) {
		if ( !collectStatistics || !personsToTrack.contains( agentId ) ) return;

		if ( log.isTraceEnabled() ) {
			log.trace( "handle activity "+activity+" for agent "+agentId );
		}

		if ( stages.isStageActivity( activity.getType() ) ) {
			currentTrips.get( agentId ).trip.add( activity );
			return;
		}

		final Record record = currentTrips.put( agentId, new Record( activity ) );
		if ( record == null )  return;
		record.destination = activity;

		final String mode = modeIdentifier.identifyMainMode( record.trip );
		try {
			writer.newLine();
			writer.write(agentId.toString());
			writer.write( "\t" );
			writer.write( ""+record.origin.getEndTime() );
			writer.write( "\t" );
			writer.write( mode );
			writer.write( "\t" );
			writer.write( record.origin.getType() );
			writer.write( "\t" );
			writer.write("" + getCoord( record.origin  ).getX());
			writer.write("\t");
			writer.write("" + getCoord( record.origin  ).getY());
			writer.write("\t");
			writer.write( ""+record.destination.getType() );
			writer.write("\t");
			writer.write("" + getCoord(record.destination).getX());
			writer.write("\t");
			writer.write("" + getCoord(record.destination).getY());
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * gets the coordinate of an activity, infering it from facilities or link if it is not defined.
	 *
	 */
	private Coord getCoord( final Activity act ) {
		if ( act.getCoord() != null ) return act.getCoord();

		final Id<ActivityFacility> facilityId = act.getFacilityId();
		if ( facilityId != null ) return scenario.getActivityFacilities().getFacilities().get( facilityId ).getCoord();

		// Where should the activity be? start, end, middle?
		return scenario.getNetwork().getLinks().get( act.getLinkId() ).getCoord();
	}

	@Override
	public void handleLeg(Id<Person> agentId, Leg leg) {
		if ( !collectStatistics || !personsToTrack.contains( agentId ) ) return;
		if ( log.isTraceEnabled() ) {
			log.trace( "handle leg "+leg+" for agent "+agentId );
		}
		currentTrips.get( agentId ).trip.add(leg);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		try {
			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		writer = null;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		writer = IOUtils.getBufferedWriter( io.getIterationFilename( event.getIteration() , "trip-stats.dat" ) );
		try {
			writer.write( "personId" );
			writer.write( "\t" );
			writer.write( "departureTime" );
			writer.write( "\t" );
			writer.write( "mode" );
			writer.write( "\t" );
			writer.write( "origType" );
			writer.write( "\t" );
			writer.write( "xOrig" );
			writer.write( "\t" );
			writer.write( "yOrig" );
			writer.write( "\t" );
			writer.write( "destType" );
			writer.write( "\t" );
			writer.write( "xDest" );
			writer.write( "\t" );
			writer.write( "yDest" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static class Record {
		Activity origin, destination;
		final List<PlanElement> trip = new ArrayList<>();

		Record( final Activity origin )  {
			this.origin = origin;
		}
	}
}