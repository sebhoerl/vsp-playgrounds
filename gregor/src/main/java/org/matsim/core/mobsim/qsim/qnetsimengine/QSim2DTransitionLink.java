/* *********************************************************************** *
 * project: kai
 * KaiHiResLink.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

import playground.gregor.sim2d_v4.simulation.Sim2DEngine;

public class QSim2DTransitionLink extends QLinkInternalI {

	private static final Logger log = Logger.getLogger(QSim2DTransitionLink.class);

	private final QNode toQueueNode;
	private final QNetwork qNetwork;
	private final Link link;
	private final Sim2DEngine hybridEngine;

	private final QLinkImpl qLinkDelegate;

	private final boolean transferToSim2D = true;

	QSim2DTransitionLink(Link link, QNetwork network, QNode toQueueNode, Sim2DEngine hybridEngine, QLinkImpl qLinkImpl) {
		this.link = link;
		this.qNetwork = network;
		this.toQueueNode = toQueueNode;
		this.hybridEngine = hybridEngine;
		this.qLinkDelegate = qLinkImpl;
		
		init();
	}

	private void init() {
		//TODO 
		//departure boxes based on flow capacity 
		//2D Agent initialization??
		//departure box section implementation 
		//section coupling 
	}

	@Override
	void addFromIntersection(QVehicle veh) {
		log.info("add from intersection was called");
		if (!this.transferToSim2D) {
			this.qLinkDelegate.addFromIntersection(veh);
		} else {
			throw new UnsupportedOperationException() ;			
		}
	}

	@Override
	void addParkedVehicle(MobsimVehicle vehicle) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.addParkedVehicle(vehicle);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void clearVehicles() {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.clearVehicles();
		} else {
//			throw new UnsupportedOperationException() ;

		}
	}

	@Override
	boolean doSimStep(double now) {
		log.info("do sim step was called");
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.doSimStep(now);
		} else {
			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	QNode getToNode() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getToNode();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getAdditionalAgentsOnLink();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getVehicle(vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void letVehicleDepart(QVehicle vehicle, double now) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.letVehicleDepart(vehicle, now);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerAdditionalAgentOnLink(planAgent);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void registerDriverAgentWaitingForCar(MobsimDriverAgent agent) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerDriverAgentWaitingForCar(agent);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	QVehicle removeParkedVehicle(Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.removeParkedVehicle(vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.unregisterAdditionalAgentOnLink(mobsimAgentId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	boolean isNotOfferingVehicle() {
		log.info("isNotOfferingVehicle was called");
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.isNotOfferingVehicle();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getLastMovementTimeOfFirstVehicle();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	QVehicle getFirstVehicle() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getFirstVehicle();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	boolean hasGreenForToLink(Id toLinkId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.hasGreenForToLink(toLinkId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	boolean hasSpace() {
		log.info("hasSpace was called");
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.hasSpace();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	QVehicle popFirstVehicle() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.popFirstVehicle();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getAllNonParkedVehicles();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	public Collection<MobsimVehicle> getAllVehicles() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getAllVehicles();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	public Link getLink() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getLink();
		} else {
			return this.link;
		}
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.recalcTimeVariantAttributes(time);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getCustomAttributes();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	public VisData getVisData() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getVisData();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	QVehicle getParkedVehicle(Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getParkedVehicle(vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id vehicleId,
			double now) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.insertPassengerIntoVehicle(passenger, vehicleId, now);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerDriverAgentWaitingForPassengers(agent);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	MobsimAgent unregisterDriverAgentWaitingForPassengers(Id agentId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.unregisterDriverAgentWaitingForPassengers(agentId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerPassengerAgentWaitingForCar(agent, vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent,
			Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.unregisterPassengerAgentWaitingForCar(agent, vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	Set<MobsimAgent> getAgentsWaitingForCar(Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getAgentsWaitingForCar(vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

}
