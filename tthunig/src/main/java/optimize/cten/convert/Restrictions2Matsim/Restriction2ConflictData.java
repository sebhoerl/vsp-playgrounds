package optimize.cten.convert.Restrictions2Matsim;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.ConflictingDirections;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.data.TtRestriction;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;

public class Restriction2ConflictData {
	
	private DgIdConverter idConverter;

	private Network networkSimplifiedAndSpatiallyExpanded;
	private Network fullNetwork;
	
	private Map<Id<Node>, Id<SignalSystem>> nodeId2signalSystemId = new HashMap<>();
	
	public Restriction2ConflictData(DgIdPool idPool, Network networkSimplifiedAndSpatiallyExpanded, Network fullNetwork, SignalSystemsData signalSystemsFullNetwork){
		this.idConverter = new DgIdConverter(idPool);
		this.networkSimplifiedAndSpatiallyExpanded = networkSimplifiedAndSpatiallyExpanded;
		this.fullNetwork = fullNetwork;
		
		// initialize nodeId2signalSystemId map:
		for (SignalSystemData system : signalSystemsFullNetwork.getSignalSystemData().values()) {
			for (SignalData signal : system.getSignalData().values()) {
				nodeId2signalSystemId.put(fullNetwork.getLinks().get(signal.getLinkId()).getToNode().getId(), system.getId());
				break; // all signalized links of a signal system have the same to-node id
			}
		}
	}

	public void convertConflicts(ConflictData conflictData, Map<Id<DgCrossing>, DgCrossing> crossings) {
		for (DgCrossing crossing : crossings.values()) {
			// identify signal system id
			Id<Node> nodeIdFullNetwork = idConverter.convertCrossingId2NodeId(crossing.getId());
			Id<SignalSystem> signalSystemId = nodeId2signalSystemId.get(nodeIdFullNetwork); 
			
			// create ConflictingDirections for system and add to conflictData
			ConflictingDirections conflictingDirections = conflictData.getFactory().createConflictingDirectionsContainerForSignalSystem(signalSystemId);
			conflictData.addConflictingDirectionsForSignalSystem(signalSystemId, conflictingDirections);
			
			// fill the restrictions
			for (TtRestriction restriction : crossing.getRestrictions().values()) {
				DgStreet light = crossing.getLights().get(restriction.getLightId());
				// identify from and to link for this direction/light
				Tuple<Id<Link>, Id<Link>> fromToLinkIdTupleFullNetwork = identifyFromAndToLinkForThisDirection(light, nodeIdFullNetwork);
				// create direction object for this light
				Direction direction = conflictData.getFactory().createDirection(signalSystemId, 
						fromToLinkIdTupleFullNetwork.getFirst(), fromToLinkIdTupleFullNetwork.getSecond(), 
						Id.create(restriction.getLightId(), Direction.class));
				conflictingDirections.addDirection(direction);
				
				// add all directions as conflicting or non-conflicting
				for (Id<DgStreet> rlightId : restriction.getRlightsAllowed()) {
					if (restriction.isAllowed()) {
						addAsNonConflicting(direction, rlightId);
					} else {
						addAsConflict(direction, rlightId);
					}
				}
				for (Id<DgStreet> lightId : restriction.getRlightsOff()) {
					addAsConflict(direction, lightId);
				}
				// add all remaining lights as conflicting (if 'allowed') or as non-conflicting (if '!allowed')
				for (Id<DgStreet> lightId : crossing.getLights().keySet()) {
					if (!direction.getConflictingDirections().contains(lightId)
							&& !direction.getNonConflictingDirections().contains(lightId)) {
						if (restriction.isAllowed()) {
							addAsConflict(direction, lightId);
						} else {
							addAsNonConflicting(direction, lightId);
						}
					}
				}
				if (!restriction.getRlightsOn().isEmpty()) {
					throw new RuntimeException("not yet implemented. would need to be in the same signals group.");
				}
			}
		}
	}

	private void addAsNonConflicting(Direction direction, Id<DgStreet> lightId) {
		// identify direction from light id
		Id<Direction> nonConflictingDirectionId = Id.create(lightId, Direction.class);
		direction.addNonConflictingDirection(nonConflictingDirectionId);
	}

	private void addAsConflict(Direction direction, Id<DgStreet> lightId) {
		// identify direction from light id
		Id<Direction> conflictingDirectionId = Id.create(lightId, Direction.class);
		direction.addConflictingDirection(conflictingDirectionId);
	}

	private Tuple<Id<Link>, Id<Link>> identifyFromAndToLinkForThisDirection(DgStreet light, Id<Node> nodeId) {
		Id<Link> lightLinkId = idConverter.convertStreetId2LinkId(light.getId()); // alternatively: idConverter.convertToCrossingNodeId2LinkId(toCrossingNodeId)
		Link lightLink = networkSimplifiedAndSpatiallyExpanded.getLinks().get(lightLinkId);
		// find corresponding from and to link in the spatially expanded network
		Id<Link> fromLinkIdExpandedNetwork = null;
		Id<Link> toLinkIdExpandedNetwork = null;
		if (lightLink.getFromNode().getInLinks().size() > 1) {
			throw new RuntimeException("There should always only be one ingoing link for every ingoing crossing node in the spatially expanded network.");
		}
		for (Link inLink : lightLink.getFromNode().getInLinks().values()) {
			fromLinkIdExpandedNetwork = inLink.getId();
			break;
		}
		if (lightLink.getToNode().getOutLinks().size() > 1) {
			throw new RuntimeException("There should always only be one outgoing link for every outgoing crossing node in the spatially expanded network.");
		}
		for (Link inLink : lightLink.getToNode().getOutLinks().values()) {
			toLinkIdExpandedNetwork = inLink.getId();
			break;
		}
		// find corresponding link in the full network 
		Id<Link> fromLinkIdFullNetwork = null;
		Id<Link> toLinkIdFullNetwork = null;
		if (fullNetwork.getLinks().containsKey(fromLinkIdExpandedNetwork)) {
			fromLinkIdFullNetwork = fromLinkIdExpandedNetwork;
		} else {
			for (Id<Link> inLinkIdFullNetwork : fullNetwork.getNodes().get(nodeId).getInLinks().keySet()) {
				// look for the id corresponding to the end of the expanded network id (link concatenation merges ids via '-')
				if (fromLinkIdExpandedNetwork.toString().endsWith(inLinkIdFullNetwork.toString())) {
					fromLinkIdFullNetwork = inLinkIdFullNetwork;
				}
			}
		}
		if (fullNetwork.getLinks().containsKey(toLinkIdExpandedNetwork)) {
			toLinkIdFullNetwork = toLinkIdExpandedNetwork;
		} else {
			for (Id<Link> outLinkIdFullNetwork : fullNetwork.getNodes().get(nodeId).getOutLinks().keySet()) {
				// look for the id corresponding to the beginning of the expanded network id (link concatenation merges ids via '-')
				if (toLinkIdExpandedNetwork.toString().startsWith(outLinkIdFullNetwork.toString())) {
					toLinkIdFullNetwork = outLinkIdFullNetwork;
				}
			}
		}
		return new Tuple<Id<Link>, Id<Link>>(fromLinkIdFullNetwork, toLinkIdFullNetwork);
	}

}
