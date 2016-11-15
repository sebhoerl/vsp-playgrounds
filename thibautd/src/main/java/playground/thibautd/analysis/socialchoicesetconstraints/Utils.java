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
package playground.thibautd.analysis.socialchoicesetconstraints;

import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.utils.CsvParser;
import playground.thibautd.utils.RandomUtils;
import playground.thibautd.utils.spatialcollections.VPTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.vividsolutions.jts.geom.Dimension.A;
import static org.osgeo.proj4j.parser.Proj4Keyword.k;

/**
 * @author thibautd
 */
class Utils {
	public static VPTree<Coord,ActivityFacility> createTree( final ActivityFacilities activityFacilities ) {
		final Collection<ActivityFacility> facilities =
				activityFacilities.getFacilities().values().stream()
						// TODO: make predicate configurable
						.filter( a -> a.getActivityOptions().keySet().contains( "leisure" ) )
						.collect( Collectors.toList() );

		final VPTree<Coord,ActivityFacility> tree = new VPTree<>( CoordUtils::calcEuclideanDistance , ActivityFacility::getCoord );
		tree.add( facilities );

		return tree;
	}

	public static AllCliques readMaximalCliques( final String cliquesCsv ) {
		final AllCliques cliques = new AllCliques();

		try ( final CsvParser reader = new CsvParser( '\t' , '"' , cliquesCsv ) ) {
			String currentCliqueId = null;
			Set<Id<Person>> currentClique = null;
			while ( reader.nextLine() ) {
				final String cliqueId = reader.getField( "cliqueId" );
				final Id<Person> egoId = reader.getIdField( "egoId", Person.class );

				if ( !cliqueId.equals( currentCliqueId ) ) {
					currentCliqueId = cliqueId;
					currentClique = new LinkedHashSet<>();
					cliques.addClique( currentClique );
				}

				currentClique.add( egoId );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}

		return cliques;
	}

	public static class AllCliques {
		private final Collection<Set<Id<Person>>> maximalCliques = new ArrayList<>();

		public Collection<Set<Id<Person>>> getMaximalCliques() {
			return maximalCliques;
		}

		public int getMaxSize() {
			return maximalCliques.stream()
					.mapToInt( Set::size )
					.max()
					.orElseGet( () -> 0 );
		}

		public List<Set<Id<Person>>> getNCliquesOfSize(
				final Random random,
				final int nCombs,
				final int size ) {
			return combinations(
					random,
					nCombs,
					maximalCliques,
					size);
		}

		private static List<Set<Id<Person>>> combinations(
				final Random random,
				final int nCombs,
				final Collection<Set<Id<Person>>> cliques,
				final int size ) {
			final long totalNCombs =
					cliques.stream()
							.mapToLong( c -> nComb( c , size ) )
							.sum();

			final TLongSet permutationIndices =
					permutations(
							new RandomDataGenerator( RandomGeneratorFactory.createRandomGenerator( random ) ),
							nCombs ,
							totalNCombs );

			final List<Set<Id<Person>>> combs = new ArrayList<>( permutationIndices.size() );

			long currentCombIndex = 0;
			for ( Set<Id<Person>> maximalClique : cliques ) {
				if ( permutationIndices.isEmpty() ) break;
				if ( maximalClique.size() < size ) continue;
				for ( int[] comb : new Combinations( maximalClique.size() , size ) ) {
					if ( permutationIndices.remove( currentCombIndex++ ) ) {
						combs.add( buildComb( maximalClique , comb ) );
					}
				}
			}

			return combs;
		}

		private static Set<Id<Person>> buildComb( final Set<Id<Person>> maximalClique, final int[] comb ) {
			final Id[] arr = maximalClique.toArray( new Id[ maximalClique.size() ] );
			final Set<Id<Person>> clique = new LinkedHashSet<>();
			for ( int i : comb ) clique.add( arr[ i ] );
			return clique;
		}

		public void addClique( Set<Id<Person>> c ) {
			maximalCliques.add( c );
		}
	}

	private static long nComb( final Collection<?> coll , final int k ) {
		if ( coll.size() < k ) return 0;
		return CombinatoricsUtils.binomialCoefficient( coll.size() , k );
	}

	private static TLongSet permutations( final RandomDataGenerator random , final int k , final long n ) {
		if ( n <= k ) return new TLongHashSet( LongStream.iterate( 0 , i -> i + 1 ).limit( n ).toArray() );

		final SparseLongVector vect = new SparseLongVector();
		// do not iterate more than necessary (apache math seems to shuffle the whole array)
		for ( int i = 0; i < k; i++ ) {
			final long j = i + random.nextLong( 0 , n - i );
			vect.swap( i , j );
		}

		return new TLongHashSet( vect.toArray( k ) );
	}

	private static class SparseLongVector {
		private final TLongLongMap map = new TLongLongHashMap();

		public long get( final long i ) {
			return map.containsKey( i ) ? map.get( i ) : i;
		}

		public long remove( final long i ) {
			return map.containsKey( i ) ? map.get( i ) : i;
		}

		public long set( final long i , final long v ) {
			final long oldI = remove( i );
			if ( i != v ) map.put( i , v );
			return oldI;
		}

		public void swap( final long i , final long j ) {
			final long oldI = set( i , j );
			set( j , oldI );
		}

		public long[] toArray( final int until ) {
			final long[] result = new long[ until ];
			for ( int i=0; i < until; i++ ) result[ i ] = get( i );
			return result;
		}
	}
}
