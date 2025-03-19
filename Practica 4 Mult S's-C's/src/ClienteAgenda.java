import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteAgenda {
    private static final String HOST = "localhost";
    private static final int PUERTO_MAESTRO = 5000;
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreUsuario;

    public static void main(String[] args) {
        ClienteAgenda cliente = new ClienteAgenda();
        cliente.iniciar();
    }

    public void iniciar() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Ingrese su nombre de usuario: ");
            nombreUsuario = scanner.nextLine();

            // Conectar con el servidor maestro
            socket = new Socket(HOST, PUERTO_MAESTRO);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            salida.println(nombreUsuario);
            salida.flush();  

            // Recibir asignación del servidor
            String servidorAsignado = entrada.readLine();
            System.out.println("Conectado a: " + servidorAsignado);

            while (true) {
                mostrarMenu();
                String opcion = scanner.nextLine();
                procesarOpcion(opcion, scanner);
            }
        } catch (IOException e) {
            System.out.println("Error al conectar con el servidor.");
            e.printStackTrace();
        }
    }

    private void mostrarMenu() {
        System.out.println("\n--- MENU AGENDA DIGITAL ---");
        System.out.println("1. Agregar una tarea");
        System.out.println("2. Eliminar una tarea");
        System.out.println("3. Modificar una tarea");
        System.out.println("4. Marcar tarea como completada");
        System.out.println("5. Ver lista de tareas");
        System.out.println("6. Salir");
        System.out.print("Seleccione una opcion: ");
    }

    private void procesarOpcion(String opcion, Scanner scanner) {
        switch (opcion) {
            case "1":
                System.out.print("\nIngrese el Nombre: ");
                String descripcion = scanner.nextLine();
                System.out.print("Ingrese la fecha y hora (YYYY-MM-DD HH:MM): ");
                String fechaHora = scanner.nextLine();
                System.out.print("Ingrese la prioridad (Alta/Media/Baja): ");
                String prioridad = scanner.nextLine();
                System.out.print("Ingrese el area responsable: ");
                String area = scanner.nextLine();
                
                salida.println("AGREGAR#" + descripcion + "#" + fechaHora + "#" + prioridad + "#" + area);
                salida.flush();
                recibirRespuesta();
                break;

            case "2":
                solicitarListaTareas();
                System.out.print("\nIngrese el ID de la tarea a eliminar: ");
                String idEliminar = scanner.nextLine();
                salida.println("ELIMINAR:" + idEliminar);
                salida.flush();
                recibirRespuesta();
                break;

            case "3":
                solicitarListaTareas();
                System.out.print("\nIngrese el ID de la tarea a modificar: ");
                String idModificar = scanner.nextLine();
                System.out.print("Ingrese el nuevo nombre: ");
                String nuevaDescripcion = scanner.nextLine();
                System.out.print("Ingrese la nueva fecha y hora (YYYY-MM-DD HH:MM): ");
                String nuevaFechaHora = scanner.nextLine();
                
                salida.println("MODIFICAR#" + idModificar + "#" + nuevaDescripcion + "#" + nuevaFechaHora);
                salida.flush();
                recibirRespuesta();
                break;

            case "4":
                solicitarListaTareas();
                System.out.print("\nIngrese el ID de la tarea a marcar como completada: ");
                String idCompletar = scanner.nextLine();
                
                salida.println("COMPLETAR#" + idCompletar);
                salida.flush();
                recibirRespuesta();
                break;

            case "5":
                solicitarListaTareas();
                break;

            case "6":
                System.out.println("Saliendo del cliente...");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
                break;

            default:
                System.out.println("Opcion no valida, intente de nuevo.");
        }
    }

    private void recibirRespuesta() {
        try {
            String respuesta = entrada.readLine();
            if (respuesta != null) {
                System.out.println("Servidor: " + respuesta);
            } else {
                System.out.println("Servidor no envio respuesta.");
            }
        } catch (IOException e) {
            System.out.println("Error al recibir respuesta del servidor.");
        }
    }

    private void solicitarListaTareas() {
        System.out.println("\nLISTA DE TAREAS: ");
        salida.println("LISTA");
        salida.flush(); 
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                System.out.println(mensaje);
                if (mensaje.equals("FIN_LISTA")) 
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error al recibir la lista de tareas.");
        }
    }
}