/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.avparking.optimizer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author jbischoff
 *
 */
public class PrivateAVScheduler extends TaxiScheduler {

	/**
	 * @param taxiCfg
	 * @param network
	 * @param fleet
	 * @param timer
	 * @param params
	 * @param travelTime
	 * @param travelDisutility
	 */
	@Inject
	public PrivateAVScheduler(TaxiConfigGroup taxiCfg, Fleet fleet, @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network,
			MobsimTimer timer, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			@Named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER) TravelDisutility travelDisutility) {
		super(taxiCfg, fleet, network, timer, travelTime, travelDisutility);
	}

	public void moveIdleVehicle(Vehicle vehicle, VrpPathWithTravelData vrpPath) {
		Schedule schedule = vehicle.getSchedule();
		divertOrAppendDrive(schedule, vrpPath);
		appendStayTask(vehicle);
	}

	public void stopCruisingVehicle(Vehicle vehicle) {
		if (!taxiCfg.isVehicleDiversion()) {
			throw new RuntimeException("Diversion must be on");
		}

		Schedule schedule = vehicle.getSchedule();
		TaxiEmptyDriveTask driveTask = (TaxiEmptyDriveTask)Schedules.getNextToLastTask(schedule);
		schedule.removeLastTask();
		OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)driveTask.getTaskTracker();
		LinkTimePair stopPoint = tracker.getDiversionPoint();
		tracker.divertPath(
				new VrpPathWithTravelDataImpl(stopPoint.time, 0, new Link[] { stopPoint.link }, new double[] { 0 }));

		appendStayTask(vehicle);
	}
}
