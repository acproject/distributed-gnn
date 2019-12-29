package models;

import java.util.Arrays;
import java.util.HashMap;

import contextualegonetwork.ContextualEgoNetwork;
import contextualegonetwork.Edge;
import contextualegonetwork.Interaction;
import contextualegonetwork.Node;

public class GNNModel implements Model {
	private ContextualEgoNetwork contextualEgoNetwork;
	private double[] egoParameters;
	private double[] egoId;
	private HashMap<Node, double[]> alterIds;
	
    protected GNNModel(ContextualEgoNetwork contextualEgoNetwork) {
    	this.contextualEgoNetwork = contextualEgoNetwork;
    	egoId = new double[10];
    	for(int i=0;i<egoId.length;i++)
    		egoId[i] = Math.random();
    	normalize(egoId);
    	egoParameters = new double[10];
    	for(int i=0;i<egoParameters.length;i++)
    		egoParameters[i] = Math.random();
    	alterIds = new HashMap<Node, double[]>();
    }

	@Override
	public void newInteraction(EdgeInteraction interaction) {
	}

	@Override
	public void newInteraction(EdgeInteraction interaction, String neighborModelParameters, boolean isReply) {
		double[] params = fromString(neighborModelParameters.split("\\|")[0]);
		double[] alterId = fromString(neighborModelParameters.split("\\|")[1]);
		Node alter = alter(interaction.getEdge());
		alterIds.put(alter, alterId);
		for(int i=0;i<egoParameters.length;i++)
			egoParameters[i] = egoParameters[i]*0.5 + params[i]*0.5;
		for(int i=0;i<egoId.length;i++)
			egoId[i] -= alterId[i]*0.15;
		normalize(egoId);
		
		double error = 1-dot(egoParameters, alterId, egoId);
		for(int i=0;i<egoParameters.length;i++)
			egoParameters[i] -= error*0.5*alterId[i]*egoId[i];
	}

	@Override
	public void doPeriodicStuff(long atTime) {
	}

	@Override
	public String getModelParameters(EdgeInteraction interaction) {
		return Arrays.toString(egoParameters)+"|"+Arrays.toString(egoId);
	}

	@Override
	public double evaluate(EdgeInteraction interaction) {
		if(!alterIds.containsKey(alter(interaction.getEdge())))
			return 0;
		HashMap<Edge, Double> evaluations = new HashMap<Edge, Double>();
		for(Edge edge : contextualEgoNetwork.getCurrentContext().getEdges()) {
			double[] alterId = alterIds.get(alter(edge));
			if(alterId!=null)
				evaluations.put(edge, dot(egoParameters, alterId, egoId));
		}
		double assignedEvaluation = evaluations.getOrDefault(alter(interaction.getEdge()), 0.);
		int topk = 0;
		for(double value : evaluations.values())
			if(value>=assignedEvaluation)
				topk += 1;
		return topk/evaluations.size();
	}
	
	protected Node alter(Edge edge) {
		if(edge.getDst()==contextualEgoNetwork.getEgo())
			return edge.getSrc();
		if(edge.getSrc()==contextualEgoNetwork.getEgo())
			return edge.getDst();
		throw new RuntimeException("Cannot retrieve alter");
	}
	
	protected static void normalize(double[] v) {
		double norm = Math.sqrt(dot(v, v));
		if(norm==0)
			return;
		for(int i=0;i<v.length;i++)
			v[i] /= norm;
	}
	
	protected static double[] fromString(String neighborModelParameters) {
		String[] unparsedParams = neighborModelParameters.substring(1,neighborModelParameters.length()-1).split(",");
		double[] params = new double[unparsedParams.length];
		for(int i=0;i<params.length;i++)
			params[i] = Double.parseDouble(unparsedParams[i]);
		return params;
	}
	
	protected static double dot(double[] v1, double[] v2) {
		double sum = 0;
		for(int i=0;i<v1.length && i<v2.length;i++)
			sum += v1[i]*v2[i];
		return sum;
	}
	protected static double dot(double[] v1, double[] v2, double[] v3) {
		double sum = 0;
		for(int i=0;i<v1.length && i<v2.length && i<v3.length;i++)
			sum += v1[i]*v2[i]*v3[i];
		return sum;
	}
}
