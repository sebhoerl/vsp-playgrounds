/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEndIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.replanning.identifiers;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.mobsim.framework.PersonAgent;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.tools.LinkReplanningMap;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

public class LeaveLinkIdentifier extends DuringLegIdentifier {

	protected LinkReplanningMap linkReplanningMap;
	
	// use the Factory!
	/*package*/ LeaveLinkIdentifier(LinkReplanningMap linkReplanningMap) {
		this.linkReplanningMap = linkReplanningMap;
	}
	
	@Override
	public List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner) {
		List<PersonAgent> agentsToReplanLeaveLink = linkReplanningMap.getReplanningAgents(time);
		List<PersonAgent> agentsToReplan = new ArrayList<PersonAgent>(); 

		for (PersonAgent personAgent : agentsToReplanLeaveLink) {
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) personAgent;
			if (withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner)) {
				agentsToReplan.add(withinDayPersonAgent);
			}
		}

		return agentsToReplan;
	}

}
