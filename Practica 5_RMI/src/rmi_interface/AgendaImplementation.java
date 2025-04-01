package rmi_interface;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class AgendaImplementation extends UnicastRemoteObject implements AgendaInterface {
    private List<String> citas;

    public AgendaImplementation() throws RemoteException {
        super();
        citas = new ArrayList<>();
    }

    @Override
    public void agregarCita(String descripcion, String fecha) throws RemoteException {
        citas.add(fecha + " - " + descripcion);
        System.out.println("Cita agregada: " + fecha + " - " + descripcion);
    }

    @Override
    public List<String> obtenerCitas() throws RemoteException {
        return citas;
    }

    @Override
    public boolean eliminarCita(int indice) throws RemoteException {
        if (indice >= 0 && indice < citas.size()) {
            System.out.println("Cita eliminada: " + citas.get(indice));
            citas.remove(indice);
            return true;
        }
        return false;
    }
}
