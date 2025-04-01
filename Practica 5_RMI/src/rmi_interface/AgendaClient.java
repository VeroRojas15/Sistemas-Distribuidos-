package rmi_interface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class AgendaClient {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            AgendaInterface stub = (AgendaInterface) registry.lookup("AgendaService");
            Scanner scanner = new Scanner(System.in);
            int opcion;

            do {
                System.out.println("\n--- Menu de la Agenda ---");
                System.out.println("1. Agregar cita");
                System.out.println("2. Ver citas");
                System.out.println("3. Eliminar cita");
                System.out.println("4. Salir");
                System.out.print("Seleccione una opcion: ");
                opcion = scanner.nextInt();
                scanner.nextLine();

                switch (opcion) {
                    case 1:
                        System.out.print("Ingrese la fecha (YYYY-MM-DD): ");
                        String fecha = scanner.nextLine();
                        System.out.print("Ingrese la descripcion de la cita: ");
                        String descripcion = scanner.nextLine();
                        stub.agregarCita(descripcion, fecha);
                        System.out.println("Cita agregada.");
                        break;
                    case 2:
                        List<String> citas = stub.obtenerCitas();
                        if (citas.isEmpty()) {
                            System.out.println("No hay citas agendadas.");
                        } else {
                            System.out.println("Citas agendadas:");
                            for (int i = 0; i < citas.size(); i++) {
                                System.out.println(i + ". " + citas.get(i));
                            }
                        }
                        break;
                    case 3:
                        System.out.print("Ingrese el numero de la cita a eliminar: ");
                        int indice = scanner.nextInt();
                        scanner.nextLine();
                        if (stub.eliminarCita(indice)) {
                            System.out.println("Cita eliminada.");
                        } else {
                            System.out.println("Índice invalido.");
                        }
                        break;
                    case 4:
                        System.out.println("Saliendo...");
                        break;
                    default:
                        System.out.println("Opcion invalida.");
                }
            } while (opcion != 4);

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
