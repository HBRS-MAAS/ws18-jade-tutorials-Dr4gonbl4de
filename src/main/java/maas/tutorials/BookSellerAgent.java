package maas.tutorials;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class BookSellerAgent extends Agent {
    private Hashtable catalogue;
    private int terminatedAgents = 0;

    protected void setup() {
        System.out.println("Hello! Seller-agent "+getAID().getName()+" is ready.");

        catalogue = new Hashtable();

        Object[] args = getArguments();
        System.out.println("args: " + args[0]);
        String[] tmpBook;

        for (int i = 0; i < args.length; ++i) {
            String s = (String)args[i];
            tmpBook = s.split("#");
            Book b = new Book(tmpBook[0], TYPE.valueOf(tmpBook[1].toUpperCase()), Integer.parseInt(tmpBook[2]), Integer.parseInt(tmpBook[3]));
            updateCatalogue(b.getTitle() + "_" + tmpBook[1].toUpperCase(), b);
            System.out.println(b.getTitle() + "_" + tmpBook[1].toUpperCase() + " registered!");
        }

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new informAboutTermination());
        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Seller-agent "+getAID().getName()+" terminating.");
    }

    public void updateCatalogue(final String title, final Book book) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                catalogue.put(title, book);
            }
        });
    }

    private class informAboutTermination extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE),
                                                         MessageTemplate.MatchConversationId("terminated"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                terminatedAgents++;
                System.out.println(terminatedAgents);
            }
            else {
                block();
            }
            if (terminatedAgents == 20) {
                myAgent.doDelete();
            }
        }
    }

    public class OfferRequestsServer extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                                                     MessageTemplate.MatchConversationId("book-trade"));
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                Book book = (Book)catalogue.get(title);
                System.out.println("asked for book: " + title);
                if (book != null) {
                    System.out.println("found book: " + title);
                    reply.setPerformative(ACLMessage.PROPOSE);
                    //reply.setConversationId("book-trade");
                    reply.setContent(String.valueOf(book.getPrice()));
                }
                else {
                    System.out.println("didn't able to find book: " + title);
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

    public class PurchaseOrdersServer extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                                     MessageTemplate.MatchConversationId("book-trade"));
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null) {
                System.out.println("order received");
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                Book book = (Book)catalogue.get(title);
                if (book != null) {
                    System.out.println("Book: " + book.getTitle() + " sold to: " + msg.getSender());
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setConversationId("book-trade");
                    if(!book.getType().equals(TYPE.EBOOK)) {
                        book.decreaseAmount();
                        int amount = book.getAmount();
                        if(amount == 0) {
                            catalogue.remove(title);
                        }

                    }
                }
                else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }
}
