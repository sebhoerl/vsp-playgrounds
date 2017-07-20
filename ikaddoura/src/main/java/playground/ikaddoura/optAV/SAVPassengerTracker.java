/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.optAV;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author ikaddoura
 */

public class SAVPassengerTracker implements ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private final Set<Id<Person>> taxiDrivers = new HashSet<>();
	private final Set<Id<Vehicle>> taxiVehicles = new HashSet<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicle2passenger = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicle2lastPassenger = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.vehicle2passenger.clear();
		this.vehicle2lastPassenger.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
	
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.taxiDrivers.add(event.getPersonId());
		}			
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (taxiDrivers.contains(event.getPersonId())) {
			
			// taxi driver
			taxiVehicles.add(event.getVehicleId());
			
		} else {
			
			// passenger
			
			if (taxiVehicles.contains(event.getVehicleId())) {
				
				// passenger getting into a taxi
				vehicle2lastPassenger.put(event.getVehicleId(), event.getPersonId());

				if (vehicle2passenger.get(event.getVehicleId()) != null) {
					throw new RuntimeException("More than one passenger in one SAV. Not (yet) considered. Aborting...");
					
				} else {
					vehicle2passenger.put(event.getVehicleId(), event.getPersonId());
				}
			}
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (taxiDrivers.contains(event.getPersonId())) {
			
			// taxi driver

		} else {
			
			// passenger
			
			if (taxiVehicles.contains(event.getVehicleId())) {
				
				// passenger getting into a taxi

				if (vehicle2passenger.get(event.getVehicleId()) != null) {
					vehicle2passenger.remove(event.getVehicleId());
				} else {
					throw new RuntimeException("The passenger " + event.getPersonId() + " should have entered the taxi vehicle " + event.getVehicleId() + " before time = " + event.getTime() + ". Aborting...");
				}
			}	
		}
	}

	public Set<Id<Person>> getTaxiDrivers() {
		return taxiDrivers;
	}

	public Set<Id<Vehicle>> getTaxiVehicles() {
		return taxiVehicles;
	}

	public Map<Id<Vehicle>, Id<Person>> getVehicle2passenger() {
		return vehicle2passenger;
	}

	public Map<Id<Vehicle>, Id<Person>> getVehicle2lastPassenger() {
		return vehicle2lastPassenger;
	}

}
