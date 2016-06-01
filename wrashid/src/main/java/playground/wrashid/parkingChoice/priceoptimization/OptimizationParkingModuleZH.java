package playground.wrashid.parkingChoice.priceoptimization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.GeneralParkingModule;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.IOUtils;

import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.AverageWalkDistanceStatsZH;
import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.ParkingGroupOccupanciesZH;
import playground.wrashid.parkingChoice.priceoptimization.analysis.ArrivalDepartureParkingHandler;
import playground.wrashid.parkingChoice.priceoptimization.infrastracture.OptimizableParking;

public class OptimizationParkingModuleZH extends GeneralParkingModule implements IterationStartsListener, 
	IterationEndsListener{
	
	private AverageWalkDistanceStatsZH averageWalkDistanceStatsZH;
	private ParkingGroupOccupanciesZH parkingGroupOccupanciesZH;
	private ArrivalDepartureParkingHandler arrivalDepartureParkingHandler;
	private EventsManager eventsManager;
	private EventWriterXML eventsWriter;
	private Controler controler;
	
	public OptimizationParkingModuleZH(Controler controler) {
		super(controler);
		this.controler = controler;
		SetupParkingForOptimizationScenario.prepare(this,controler);

	}
	
		
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		
		/*for (ParkingArrivalEvent rA: rentableArrival) {
			for (ParkingDepartureEvent rD: rentableDeparture) {
				double rentedTime = 0;
				if (rA.getParkingId(rA.getAttributes()).equals(rD.getParkingId(rD.getAttributes())) && rD.getTime() > rA.getTime()){
					rentedTime = rD.getTime()-rA.getTime();
					double tempTime =0;
					if(rentableDur.containsKey(rA.getParkingId(rA.getAttributes()))){
						tempTime = rentableDur.get(rA.getParkingId(rA.getAttributes()));
						rentableDur.put(rA.getParkingId(rA.getAttributes()), rentedTime+tempTime);
					} else {
						rentableDur.put(rA.getParkingId(rA.getAttributes()), rentedTime);
					}
					break;
				}
			}
		}*/
		//Save rentable parking information
		/*HashMap<String, ArrayList<String>> rentableInfo = new HashMap<String, ArrayList<String>>();
		for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
			if (parking instanceof OptimizableParking){
				OptimizableParking par = (OptimizableParking) parking;
				ArrayList<String> info = new ArrayList<String>();
				
				double occup = Double.NaN;
				double rentedTime = Double.NaN;
				double pricePerHour=par.getCostPerHour();
				if (!(rentableDur.get(par.getId()) == null)){
					rentedTime = rentableDur.get(par.getId());
					occup = rentableDur.get(par.getId())/(24 * 60 * 60);
				}
				info.add(parking.getId().toString());
				info.add(Double.toString(rentedTime));
				info.add(Double.toString(occup));
				info.add(Double.toString(pricePerHour));
				info.add(Double.toString(parking.getCoordinate().getX()));
				info.add(Double.toString(parking.getCoordinate().getY()));
				rentableInfo.put(parking.getId().toString(), info);
			}
		}*/
		
		
		
		//String outputFolder = event.getControler().getControlerIO().getIterationPath(iter);
		//CsvFileWriter.writeCsvFile(outputFolder+"/rentableParkingInfo.csv", rentableInfo);
		
		
		
		//====================
	
		
		
		
		eventsManager = EventsUtils.createEventsManager();
		eventsWriter = new EventWriterXML(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		eventsManager.addHandler(eventsWriter);
		parkingGroupOccupanciesZH = new ParkingGroupOccupanciesZH(this.controler);
		eventsManager.addHandler(parkingGroupOccupanciesZH);
		averageWalkDistanceStatsZH = new AverageWalkDistanceStatsZH(getParkingInfrastructureManager().getAllParkings());
		eventsManager.addHandler(averageWalkDistanceStatsZH);
		
		arrivalDepartureParkingHandler = new ArrivalDepartureParkingHandler();
		eventsManager.addHandler(arrivalDepartureParkingHandler);

		eventsManager.resetHandlers(0);
		eventsWriter.init(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		
		getParkingInfrastructure().setEventsManager(eventsManager);
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		super.notifyStartup(event);
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		//TODO send output to file
		int iteration = event.getIteration();
		final BufferedWriter outLink = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(iteration, "prices.txt"));
		
		try {
			for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
				if (parking instanceof OptimizableParking){
					OptimizableParking par = (OptimizableParking) parking;
	
					double oldPrice = par.getCostPerHour();
					
						outLink.write(par.getId() + " " + Double.toString(oldPrice));
						outLink.newLine();
			}
								
			}
			outLink.flush();
			outLink.close();
		}
		 catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HashMap<Id<PC2Parking>, Double> rentableDur = this.arrivalDepartureParkingHandler.getRentableDur();
		//Get occupancy of rentable time in %, if higher than 70% => go up with the price
				//==================== Setting new prices for rentable parking dependig on the usage
				for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
					double occup = 0;
					if (parking instanceof OptimizableParking){
						OptimizableParking par = (OptimizableParking) parking;

						double oldPrice = par.getCostPerHour();
						if (!(rentableDur.get(par.getId()) == null)){
							//Check if rentable parking was used (is in list)
							occup = rentableDur.get(par.getId())/(24 * 60 * 60 * par.getMaximumParkingCapacity());
							if (occup > 1.0)
								System.out.println();
							if (occup > 0.85){
							//If occupancy was > 0.75, increase price by 0.5, else decrease price by 0.5
								if (oldPrice < 200){
									par.setCostPerHour(oldPrice + 0.25);
								}
							} else {
								if (oldPrice >= 0.25) {
									par.setCostPerHour(oldPrice - 0.25);	
								}
								else
									par.setCostPerHour(0.0);
							}
						}
						//rentable space was not used at all (is not in list) => also decrease price by 0.5
						else {
							if (oldPrice >= 0.25) {
								par.setCostPerHour(oldPrice - 0.25);	
							}
							else
								par.setCostPerHour(0.0);
						}
						
										
					}
				}
		
		
		
		parkingGroupOccupanciesZH.savePlot(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingGroupOccupancy.png"));
		averageWalkDistanceStatsZH.printStatistics();
		
		eventsManager.finishProcessing();
		eventsWriter.reset(0);		
	}


	
	

}
