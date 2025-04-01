package rmi_interface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface AgendaInterface extends Remote {
    void agregarCita(String descripcion, String fecha) throws RemoteException;
    List<String> obtenerCitas() throws RemoteException;
    boolean eliminarCita(int indice) throws RemoteException;
}
