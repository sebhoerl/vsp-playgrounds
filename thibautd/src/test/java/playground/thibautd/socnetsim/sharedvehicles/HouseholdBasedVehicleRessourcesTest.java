/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdBasedVehicleRessourcesTest.java
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
package playground.thibautd.socnetsim.sharedvehicles;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;

/**
 * @author thibautd
 */
public class HouseholdBasedVehicleRessourcesTest {

	@Test
	public void testVehiclesIdsAreCorrect() throws Exception {
		final Households households = createHouseholds();
		final VehicleRessources testee = new HouseholdBasedVehicleRessources( households );

		for (Household household : households.getHouseholds().values()) {
			final Set<Id> expected = new HashSet<Id>( household.getVehicleIds() );
			for (Id person : household.getMemberIds()) {
				final Set<Id> actual = testee.identifyVehiclesUsableForAgent( person );
				assertEquals(
						"unexpected vehicles for agent "+person,
						expected,
						actual);
			}
		}
	}

	private static Households createHouseholds() {
		final HouseholdsImpl hhs = new HouseholdsImpl();

		int c = 0;
		int v = 0;
		// NO INTERFACE BASED WAY TO ADD MEMBERS ???????
		HouseholdImpl hh = (HouseholdImpl) hhs.getFactory().createHousehold( new IdImpl( "small" ) );
		hh.setMemberIds( Arrays.<Id>asList( new IdImpl( c++ ) ) );
		hh.setVehicleIds( Collections.<Id>emptyList() );
		hhs.addHousehold( hh );

		hh = (HouseholdImpl) hhs.getFactory().createHousehold( new IdImpl( "big" ) );
		hh.setMemberIds( Arrays.<Id>asList(
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ )) );
		hh.setVehicleIds( Arrays.<Id>asList( new IdImpl( v++ ) ) );
		hhs.addHousehold( hh );

		hh = (HouseholdImpl) hhs.getFactory().createHousehold( new IdImpl( "lots of vehicles" ) );
		hh.setMemberIds( Arrays.<Id>asList(
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ ),
					new IdImpl( c++ )) );
		hh.setVehicleIds( Arrays.<Id>asList(
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ ),
					new IdImpl( v++ )) );
		hhs.addHousehold( hh );

		return hhs;
	}
}

