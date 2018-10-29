package maas.tutorials;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


@SuppressWarnings("serial")
public class BookBuyerAgent extends Agent {
	private String targetBookTitle;
	private AID[] sellerAgents;

	protected void setup() {
	// Printout a welcome message
		System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");

		Object[] args = getArguments();
		if(args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Trying to buy "+targetBookTitle);

			addBehaviour(new TickerBehaviour(this, 60000) {
				@Override
				protected void onTick() {
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						sellerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
						}
					}
					catch (FIPAException fe) {
						System.out.println("Error searching seller Agents");
						fe.printStackTrace();
					}
					System.out.println(sellerAgents.length + " seller Agents found!");
					myAgent.addBehaviour(new RequestPerformer());
				}
			});
		}
		else {
			System.out.println("No book title specified");
			doDelete();
		}
	}
	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
			    myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
			    myAgent.send(shutdownMessage);
			}
			catch (Exception e) {
			    //LOGGER.error(e);
			}

		}
	}

	private class informAboutTermination extends OneShotBehaviour{
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.PROPAGATE);
			msg.setConversationId("terminated");
			for (int i = 0; i < sellerAgents.length; ++i) {
				msg.addReceiver(sellerAgents[i]);
			}
			myAgent.send(msg);
			System.out.println("terminatingmessage send");
		}
	}

	private class RequestPerformer extends Behaviour {
		private AID bestSeller;
		private int bestPrice;
		private int repliesCnt = 0;
		private MessageTemplate mt;
		private int step = 0;

		@Override
		public void action() {
			switch (step) {
				case 0:
					ACLMessage cfp = new ACLMessage((ACLMessage.CFP));
					for (int i = 0; i < sellerAgents.length; ++i) {
						cfp.addReceiver(sellerAgents[i]);
					}
					cfp.setContent(targetBookTitle);
					cfp.setConversationId("book-trade");
					cfp.setReplyWith("cfp" + System.currentTimeMillis());
					myAgent.send(cfp);
					System.out.println(getAID() + " send Call For Proposal");
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
							MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					step = 1;
					break;
				case 1:
					ACLMessage reply = myAgent.receive(mt);
					if (reply != null) {
						if (reply.getPerformative() == ACLMessage.PROPOSE) {
							int price = Integer.parseInt(reply.getContent());
							if (bestSeller == null || price < bestPrice) {
								bestPrice = price;
								bestSeller = reply.getSender();
							}
						}
						repliesCnt++;
						if (repliesCnt >= sellerAgents.length) {
							if (bestSeller == null) {
								System.out.println("book not available: "+targetBookTitle+" for agent: "+getAID());
								myAgent.doDelete();
							}
							step = 2;
						}
					} else {
						block();
					}
					break;
				case 2:
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(bestSeller);
					order.setContent(targetBookTitle);
					order.setConversationId("book-trade");
					order.setReplyWith("order" + System.currentTimeMillis());
					myAgent.send(order);
					System.out.println(getAID() + " accepted Proposal");
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
											 MessageTemplate.MatchInReplyTo(order.getReplyWith()));
					step = 3;
					break;
				case 3:
					reply = myAgent.receive(mt);
					System.out.println(getAID() + " case 3");
					if (reply != null) {
						if (reply.getPerformative() == ACLMessage.INFORM) {
							System.out.println(targetBookTitle + " successfully purchased.");
							System.out.println("Price = " + bestPrice);
						}
						else {
							System.out.println(getAID() + " Order failed");
							step = 0;
						}
						step = 4;
						myAgent.addBehaviour(new informAboutTermination());
						myAgent.addBehaviour(new shutdown());
					} else {
						block();
					}
					break;
			}
		}

		@Override
		public boolean done() {
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}
}
