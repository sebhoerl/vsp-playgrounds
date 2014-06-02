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

package playground.jbischoff.taxi.optimizer.rank;

import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.contrib.dvrp.util.DistanceUtils;

import playground.jbischoff.energy.charging.ElectricTaxiChargingHandler;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal;
import playground.michalm.taxi.optimizer.fifo.NOSTaxiOptimizer;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;


/**
 * @author jbischoff
 */

public class NOSRankTaxiOptimizer
    extends NOSTaxiOptimizer
{
    private final TaxiOptimizerConfiguration optimConfig;
    private final IdleRankVehicleFinder idleVehicleFinder;
    private  ElectricTaxiChargingHandler ecabhandler;
    private final static double NEEDSTOCHARGESOC = 0.2;
    
    private boolean idleRankMode;

    //    private final List<Id> shortTimeIdlers;
    
    private Map<Id, Link> nearestChargers;
    private static Logger log = Logger.getLogger(NOSRankTaxiOptimizer.class);


    public static NOSRankTaxiOptimizer createNOSRankTaxiOptimizer(MatsimVrpContext context,
            VrpPathCalculator calculator, TaxiSchedulerParams params,
            TravelDisutilitySource tdisSource, String workingDir)
    {
        TaxiScheduler scheduler = new RankModeTaxiScheduler(context, calculator, params);

        VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(calculator, scheduler);

        TaxiOptimizerConfiguration optimConfig = new TaxiOptimizerConfiguration(context,
                calculator, scheduler, vrpFinder, Goal.MIN_WAIT_TIME, workingDir);

        return new NOSRankTaxiOptimizer(optimConfig, new IdleRankVehicleFinder(context, scheduler));
    }


    private NOSRankTaxiOptimizer(TaxiOptimizerConfiguration optimConfig,
            IdleRankVehicleFinder vehicleFinder)
    {
        super(optimConfig, vehicleFinder, null);
        this.optimConfig = optimConfig;
        this.idleVehicleFinder = vehicleFinder;
        //        this.shortTimeIdlers = new ArrayList<Id>();

    }


    public void setRankMode(boolean rankMode)
    {
        ((RankModeTaxiScheduler)optimConfig.scheduler).rankmode = rankMode;
    }


    


    private static class RankModeTaxiScheduler
        extends TaxiScheduler
    {
        private boolean rankmode;
        private VrpPathCalculator calculator;


        public RankModeTaxiScheduler(MatsimVrpContext context, VrpPathCalculator calculator,
                TaxiSchedulerParams params)
        {
            super(context, calculator, params);
            this.calculator = calculator;
        }


        @Override
        public void appendWaitAfterDropoff(Schedule<TaxiTask> schedule)
        {
            if (rankmode) {
                TaxiDropoffStayTask dropoffStayTask = (TaxiDropoffStayTask)Schedules
                        .getLastTask(schedule);

                Link link = dropoffStayTask.getLink();
                Link startLink = schedule.getVehicle().getStartLink();

                if (link != startLink) {
                    double t5 = dropoffStayTask.getEndTime();
                    VrpPathWithTravelData path = calculator.calcPath(link, startLink, t5);
                    schedule.addTask(new TaxiCruiseDriveTask(path));

                    double t6 = path.getArrivalTime();
                    double tEnd = Math.max(t6, schedule.getVehicle().getT1());
                    schedule.addTask(new TaxiWaitStayTask(t6, tEnd, schedule.getVehicle()
                            .getStartLink()));
                }
                else {
                    super.appendWaitAfterDropoff(schedule);
                }
            }
            else {
                super.appendWaitAfterDropoff(schedule);
            }
        }
    }
    
    

    public void setEcabhandler(ElectricTaxiChargingHandler ecabhandler)
    {
        this.ecabhandler = ecabhandler;
        this.idleVehicleFinder.addEcabHandler(ecabhandler);
    }


    public void setIdleRankMode(boolean b)
    {
        this.idleRankMode = b;
    }


    public void doSimStep(double time)
    {
        if (time % 60. == 0.) {
            checkWaitingVehiclesBatteryState(time);
            //            if (this.idleRankMode)
            //                updateIdlers();
            //            	sendIdlingTaxisBackToRank(time);
        }
        if (this.idleRankMode){
            if (time % 60. == 5.)
            {
            	
                sendIdlingTaxisBackToRank(time);
                
            }}
    }


    private void checkWaitingVehiclesBatteryState(double time)
    {
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles()) {
            if (veh.getSchedule().getStatus() != ScheduleStatus.STARTED)
                continue;
            if (! (Schedules.getLastTask(veh.getSchedule()).getTaskIdx() == veh.getSchedule()
                    .getCurrentTask().getTaskIdx()))
                continue;
            if (veh.getSchedule().getCurrentTask().getType().equals(TaxiTaskType.WAIT_STAY)) {
                TaxiWaitStayTask twst = (TaxiWaitStayTask)veh.getSchedule().getCurrentTask();
                if (!this.ecabhandler.isAtCharger(twst.getLink().getId())) {
                    if (this.needsToCharge(veh.getId())) {
                        scheduleRankReturn(veh, time);
                    }

                }

            }

        }
    }


    protected void scheduleRankReturn(Vehicle veh, double time)
    {
        @SuppressWarnings("unchecked")
        Schedule<Task> sched = (Schedule<Task>)veh.getSchedule();
        TaxiWaitStayTask last = (TaxiWaitStayTask)Schedules.getLastTask(veh.getSchedule());
        if (last.getStatus() != TaskStatus.STARTED)
            throw new IllegalStateException();

        last.setEndTime(time);
        Link currentLink = last.getLink();
        Link nearestRank = this.nearestChargers.get(currentLink.getId());

        VrpPathWithTravelData path = optimConfig.calculator
                .calcPath(currentLink, nearestRank, time);
        if (path.getArrivalTime()>veh.getT1()) return; // no rank return if vehicle is going out of service anyway
        sched.addTask(new TaxiCruiseDriveTask(path));
        sched.addTask(new TaxiWaitStayTask(path.getArrivalTime(), veh.getT1(), nearestRank));

    }


    private Link findNearestCharger(Link positionLink)
    {
        Link nearestChargerLink = null;
        double bestTravelCost = Double.MAX_VALUE;
        for (Id cid : ecabhandler.getChargers().keySet()) {
            Link currentLink = optimConfig.context.getScenario().getNetwork().getLinks()
                    .get(cid);
            //            double currentCost = optimConfig.calculator.calcPath(positionLink, currentLink,     optimConfig.context.getTime()).getTravelCost();
            double currentCost = DistanceUtils.calculateSquaredDistance(positionLink, currentLink);
            if (currentCost < bestTravelCost) {
                bestTravelCost = currentCost;
                nearestChargerLink = currentLink;
            }
        }
        return nearestChargerLink;
    }


    public void sendIdlingTaxisBackToRank(double time)
    {
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles()) {
        	if (!optimConfig.scheduler.isIdle(veh)) continue;
            if (veh.getSchedule().getStatus() != ScheduleStatus.STARTED)
                continue;
            if (! (Schedules.getLastTask(veh.getSchedule()).getTaskIdx() == veh.getSchedule()
                    .getCurrentTask().getTaskIdx()))
                continue;
            
            if (veh.getSchedule().getCurrentTask().getType().equals(TaskType.STAY)) {
            	
                TaxiWaitStayTask twst = (TaxiWaitStayTask)veh.getSchedule().getCurrentTask();
                if (!this.ecabhandler.isAtCharger(twst.getLink().getId())) {
                    if (time - twst.getBeginTime() > 60.) {
                    	scheduleRankReturn(veh, time);
                        
                    }

                }

            }

        }
    }


    public void createNearestChargerDb()
    {
        this.nearestChargers = new HashMap<Id, Link>();

        for (Link l : optimConfig.context.getScenario().getNetwork().getLinks().values()) {
            Link chargerLink = findNearestCharger(l);
            this.nearestChargers.put(l.getId(), chargerLink);

        }

        log.info("...done");

    }

    private boolean needsToCharge(Id vid){
        return (this.ecabhandler.getRelativeTaxiSoC(vid)<=NEEDSTOCHARGESOC);
    }
    
}