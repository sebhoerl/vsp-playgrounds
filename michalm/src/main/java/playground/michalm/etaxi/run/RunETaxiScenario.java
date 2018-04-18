/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.etaxi.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.EvModule;
import org.matsim.vsp.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.vsp.ev.dvrp.EvDvrpIntegrationModule;

public class RunETaxiScenario {
	private static final String CONFIG_FILE = "mielec_2014_02/mielec_etaxi_config.xml";
	private static final double CHARGING_SPEED_FACTOR = 1.; // full speed
	private static final double MAX_RELATIVE_SOC = 0.8;// up to 80% SOC
	private static final double TEMPERATURE = 20;// oC

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup(), new EvConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) {
		DvrpConfigGroup.get(config).setNetworkMode(null);// to switch off network filtering
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(ETaxiDvrpModules.create());
		controler.addOverridingModule(createEvDvrpIntegrationModule());

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	public static EvDvrpIntegrationModule createEvDvrpIntegrationModule() {
		return new EvDvrpIntegrationModule()//
				.setChargingStrategyFactory(charger -> new FixedSpeedChargingStrategy(
						charger.getPower() * CHARGING_SPEED_FACTOR, MAX_RELATIVE_SOC))//
				.setTemperatureProvider(() -> TEMPERATURE)//
				.setTurnedOnPredicate(vehicle -> vehicle.getSchedule().getStatus() == ScheduleStatus.STARTED)//
				.setVehicleFileUrlGetter(cfg -> TaxiConfigGroup.get(cfg).getTaxisFileUrl(cfg.getContext()));
	}

	public static void main(String[] args) {
		// String configFile = "./src/main/resources/one_etaxi/one_etaxi_config.xml";
		// String configFile =
		// "../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/mielec_etaxi_config.xml";
		RunETaxiScenario.run(CONFIG_FILE, false);
	}
}