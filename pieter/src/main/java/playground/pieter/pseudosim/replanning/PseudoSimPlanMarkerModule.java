package playground.pieter.pseudosim.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

import playground.pieter.pseudosim.controler.PseudoSimControler;

public class PseudoSimPlanMarkerModule implements PlanStrategyModule {

	private PseudoSimControler controler;

	public PseudoSimPlanMarkerModule(PseudoSimControler controler) {
		this.controler = controler;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {

	}

	@Override
	public void handlePlan(Plan plan) {
//		plan.setScore(0.0);
		controler.addPlanForPseudoSimulation(plan);
	}

	@Override
	public void finishReplanning() {

	}

}
