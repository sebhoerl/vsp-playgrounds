package playground.jhackney.socialnetworks.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.PlanAlgorithm;

public class SNPickFacilityFromAlter extends AbstractMultithreadedModule {
	
	private final static Logger log = Logger.getLogger(SNPickFacilityFromAlter.class);
	private String[] factypes={"home","work","shop","education","leisure"};
	private Network network=null;
	private TravelCost tcost=null;
	private TravelTime ttime=null;
	private Knowledges knowledges;
	
	public SNPickFacilityFromAlter(Config config, Network network, TravelCost tcost, TravelTime ttime, Knowledges kn) {
		super(config.global());
		log.info("initializing SNPickFacility");
    	this.network=network;
    	this.tcost = tcost;
    	this.ttime = ttime;
    	this.knowledges = kn;
    }

    @Override
		public PlanAlgorithm getPlanAlgoInstance() {

    	return new SNPickFacility(factypes, (NetworkLayer) network, tcost, ttime, this.knowledges);
    }
}
