package playground.gregor.sim2d_v3.events;

import org.matsim.core.events.handler.EventHandler;

public interface DoubleValueStringKeyAtCoordinateEventHandler extends EventHandler{

	public void handleEvent(DoubleValueStringKeyAtCoordinateEvent e);
}
