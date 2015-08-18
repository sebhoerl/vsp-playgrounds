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

package playground.johannes.synpop.sim;

import playground.johannes.synpop.sim.data.CachedElement;

/**
 * @author johannes
 */
public class AttributeMutator {

    private Object dataKey;

    private ValueGenerator generator;

    private AttributeChangeListener listener;

    private Object oldValue;

    public void modify(CachedElement element) {
        oldValue = element.getData(dataKey);
        Object newValue = generator.newValue(element);
        element.setData(dataKey, newValue);

        if(listener != null) listener.onChange(dataKey, oldValue, newValue, element);
    }

    public void revert(CachedElement element) {
        Object newValue = element.getData(dataKey);
        element.setData(dataKey, oldValue);

        if(listener != null) listener.onChange(dataKey, newValue, oldValue, element);
    }

}
