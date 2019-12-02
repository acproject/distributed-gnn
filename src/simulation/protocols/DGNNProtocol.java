package simulation.protocols;

import java.util.Map;

import contextualegonetwork.ContextualEgoNetwork;
import contextualegonetwork.Interaction;
import contextualegonetwork.Node;
import contextualegonetwork.Utils;
import models.Model;
import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.Network;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import simulation.managers.ContextualEgoNetworkManager;
import simulation.messages.Message;

public class DGNNProtocol implements EDProtocol, CDProtocol {

//	id number of the protocol
	public static int dgnnProtocolId;
//	artificial thing to know on which node runs a particular instance of the protocol
	public static Map<String, Integer> idTranslator;
//	id string of the protocol
	public static final String DGNN_PROTOCOL_ID="dgnn";
	public static final String SEPARATOR = "$";
//	prefix for getting stuff from the configuration file
	public static String prefix=null;
//	a reference to the peersim node
//	private Node peersimNode;
	
//	maybe the CEN? or maybe in another class/package?
	public ContextualEgoNetworkManager cenManager;
//	a reference to the model
	public Model model;
	
	public DGNNProtocol(String prefix) {}
	
	/**
	 * initialize this object with all the modules running on it (the cen manager, and the gnn model at least)
	 * @return true if something went wrong and the simulation must be stopped, false otherwise
	 */
	public boolean initialize() {
		cenManager = new ContextualEgoNetworkManager();
		if(cenManager.initialize()) return true;
		model = Model.create(cenManager.getContextualEgoNetwork());
//		peersimNode=node;
		return false;
	}
	
	@Override
	public void nextCycle(peersim.core.Node node, int protocolId) {
//		also let the model do some periodic stuff if needed
		model.doPeriodicStuff(Utils.getCurrentTimestamp());
	}

	@Override
	public void processEvent(peersim.core.Node node, int protocolId, Object msg) {
		Message message=(Message) msg;
		switch(message.type){
			case EGO_NETWORK_QUERY: //can replay right away
				cenManager.handleENQ(node, message);
				break;
			case EGO_NETWORK_REPLY: //update the CEN
				cenManager.handleENR(message);			
				break;
			case MODEL_PUSH: //pass to the learner
				break;
			case NEW_INTERACTION: //a new interaction happened!
				ContextualEgoNetwork cen = cenManager.getContextualEgoNetwork();
				contextualegonetwork.Node alter = cen.getOrCreateNode(message.senderId, null);
				Interaction interaction = cen.getCurrentContext().getOrAddEdge(alter, cen.getEgo()).addDetectedInteraction(message.body);
				model.newInteraction(interaction, message.parameters);
				debugprint(message.type, message.senderId, message.recipientId);
				break;
			default:
				break;
		}

	}

	public String getMessageBodyAndRegisterInteraction(String alterId, String iteractionType) {
		ContextualEgoNetwork cen = cenManager.getContextualEgoNetwork();
		Node alter = cen.getOrCreateNode(alterId, null);
		Interaction interaction = cen.getCurrentContext().getOrAddEdge(cen.getEgo(), alter).addDetectedInteraction(iteractionType);
		model.newInteraction(interaction, null);
		return model.getModelParameters(alter);
	}
	
	/**
	 * utility method to broadcast something to all the neighbours of the sender node
	 * @param message
	 * @param senderNode
	 * @deprecated =D
	 */
	/*public void sendToAllAlters(Message message, peersim.core.Node senderNode) {
//		get all neighbours
		for(contextualegonetwork.Node neighbour : cenManager.getContextualEgoNetwork().getCurrentContext().getNodes()) {
//			copy the message and modify the recipient
			Message copy=message.clone();
			copy.recipientId=neighbour.getId();
			sendMessage(copy, senderNode);
		}
	}*/
	
	/**
	 * utility method to send a message
	 * @param message the message to be sent (its fields must be already filled in)
	 * @param senderNode the peersim node sending the message
	 */
	public static void sendMessage(Message message, peersim.core.Node senderNode) {
		//UnreliableTransport transport = (UnreliableTransport) (Network.prototype).getProtocol(transportid);
		peersim.core.Node recipient;
		try{
			recipient=Network.get(DGNNProtocol.idTranslator.get(message.recipientId));
		} catch(NullPointerException e){
//			skipping non existent node in the network
			return;
		}
		Transport transport=(Transport)senderNode.getProtocol(FastConfig.getTransport(dgnnProtocolId));
        transport.send(senderNode, recipient, message, dgnnProtocolId);
	}
	
	@Override
	public Object clone() {
		return new DGNNProtocol(prefix);
	}
	
	public static void debugprint(Object... messages){
		System.out.print("DEBUG|||");
		for(Object message : messages){
			System.out.print(" " + message);
		}
		System.out.println();
	}

}
