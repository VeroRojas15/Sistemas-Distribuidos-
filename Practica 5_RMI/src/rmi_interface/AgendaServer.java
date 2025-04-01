package rmi_interface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AgendaServer {
    public static void main(String[] args) {
        try {
            AgendaImplementation obj = new AgendaImplementation();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("AgendaService", obj);
            System.out.println("Servidor de Agenda RMI listo...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
