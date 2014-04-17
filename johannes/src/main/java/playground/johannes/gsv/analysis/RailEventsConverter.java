package playground.johannes.gsv.analysis;

import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.johannes.gsv.sim.TransitAlightEvent;
import playground.johannes.gsv.sim.TransitBoardEvent;
import playground.johannes.gsv.sim.TransitLineEvent;

public class RailEventsConverter implements GenericEventHandler {

	private final EventsManager events;
	
	private final Population population;
	
	private final TransitSchedule schedule;
	
	public RailEventsConverter(EventsManager events, Population population, TransitSchedule schedule) {
		this.events = events;
		this.population = population;
		this.schedule = schedule;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(GenericEvent event) {
		if(event.getEventType().equalsIgnoreCase(TransitBoardEvent.TYPE)) {
			Person person = population.getPersons().get(new IdImpl(event.getAttributes().get(TransitLineEvent.PERSON_KEY)));
			TransitLine line = schedule.getTransitLines().get(new IdImpl(event.getAttributes().get(TransitLineEvent.LINE_KEY)));
			TransitRoute route = line.getRoutes().get(TransitLineEvent.ROUTE_KEY);
			TransitStopFacility facility = schedule.getFacilities().get(new IdImpl(event.getAttributes().get(TransitLineEvent.STOP_KEY)));
			
			TransitBoardEvent bEvent = new TransitBoardEvent(event.getTime(), person, line, route, facility);
			events.processEvent(bEvent);
		} else if(event.getEventType().equalsIgnoreCase(TransitAlightEvent.TYPE)) {
			Person person = population.getPersons().get(new IdImpl(event.getAttributes().get(TransitLineEvent.PERSON_KEY)));
			TransitLine line = schedule.getTransitLines().get(new IdImpl(event.getAttributes().get(TransitLineEvent.LINE_KEY)));
			TransitRoute route = line.getRoutes().get(TransitLineEvent.ROUTE_KEY);
			TransitStopFacility facility = schedule.getFacilities().get(new IdImpl(event.getAttributes().get(TransitLineEvent.STOP_KEY)));
			
			TransitAlightEvent bEvent = new TransitAlightEvent(event.getTime(), person, line, route, facility);
			events.processEvent(bEvent);
		}
	}

}
