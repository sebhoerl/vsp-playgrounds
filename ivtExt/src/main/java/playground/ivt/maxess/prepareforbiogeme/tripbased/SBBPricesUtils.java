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
package playground.ivt.maxess.prepareforbiogeme.tripbased;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;

/**
 * @author thibautd
 */
public class SBBPricesUtils {
	private static final Logger log = Logger.getLogger( SBBPricesUtils.class );
	public enum SBBClass {first, second;}

	/**
	 * Prices according to SBB official fare system
	 * http://voev.ch/T600_f
	 */
	public static double computeSBBTripPrice(
			final SBBClass klasse,
			final boolean halbTax,
			final Trip trip ) {
		return computeSBBTripPrice( klasse , halbTax , getDistance( trip ) );
	}

	public static double computeSBBTripPrice(
			final SBBClass klasse,
			final boolean halbTax,
			final double distance_m ) {
		if ( log.isTraceEnabled() ) log.trace( "start calculation" );
		if ( log.isTraceEnabled() ) log.trace( "distance_m="+distance_m );
		final double roundedDistance_km = roundDistance( distance_m / 1000 );

		if ( log.isTraceEnabled() ) log.trace( "roundedDistance_km="+roundedDistance_km );

		final double unroundedPrice = calcUnroundedPrice( roundedDistance_km );

		if ( log.isTraceEnabled() ) log.trace( "roundedPrice="+unroundedPrice );

		final double basePrice = roundPrice( roundedDistance_km , unroundedPrice );

		if ( log.isTraceEnabled() ) log.trace( "basePrice="+basePrice );

		final double multiplicator = getMultiplicator( klasse , halbTax );
		final double minimumPrice = getMinimumPrice( klasse , halbTax );

		return Math.max( multiplicator * basePrice , minimumPrice );
	}

	private static double getMultiplicator( SBBClass klasse, boolean halbTax ) {
		double m = 1;

		if ( klasse == SBBClass.first ) m *= 1.75;
		if ( halbTax ) m /= 2;

		return m;
	}

	private static double getMinimumPrice( SBBClass klasse, boolean halbTax ) {
		switch ( klasse ) {
			case first:
				return halbTax ? 2.7 : 5.40;
			case second:
				return halbTax ? 2.2 : 3;
		}
		throw new RuntimeException();
	}

	private static double roundDistance( double distance_km ) {
		if ( distance_km <= 8 ) return round( distance_km , 4 );
		if ( distance_km <= 30 ) return round( distance_km , 2 );
		if ( distance_km <= 60 ) return round( distance_km , 3 );
		if ( distance_km <= 100 ) return round( distance_km , 4 );
		if ( distance_km <= 150 ) return round( distance_km , 5 );
		if ( distance_km <= 300 ) return round( distance_km , 10 );
		return round( distance_km , 20 );
	}

	private static double roundPrice( double distance_km, double price ) {
		return round( price , distance_km < 70 ? 0.2 : 1 );
	}

	private static double round( double d , double binSize ) {
		final double bin = Math.ceil( d / binSize );
		return bin * binSize;
	}

	private static double calcUnroundedPrice( double distance ) {
		double p = 0;
		p += calcPriceInterval( 0 , 4 , distance , 0.4451 );
		p += calcPriceInterval( 4 , 14 , distance , 0.4230 );
		p += calcPriceInterval( 14 , 48 , distance , 0.3564 );
		p += calcPriceInterval( 48 , 150 , distance , 0.2581 );
		p += calcPriceInterval( 150 , 200 , distance , 0.2508 );
		p += calcPriceInterval( 200 , 250 , distance , 0.2229 );
		p += calcPriceInterval( 250 , 300 , distance , 0.2013 );
		p += calcPriceInterval( 300 , 480 , distance , 0.1960 );
		p += calcPriceInterval( 480 , 1500 , distance , 0.1936 );
		return p;
	}

	private static double calcPriceInterval( double min, double max, double distance, double rate ) {
		if ( distance <= min ) return 0;
		if ( distance > max ) return (max - min) * rate;
		return (distance - min) * rate;
	}

	public static double getDistance( Trip trip ) {
		double d = 0;

		for ( Leg l : trip.getLegsOnly() ) {
			// defined?
			d += l.getRoute().getDistance();
		}

		return d;
	}
}

