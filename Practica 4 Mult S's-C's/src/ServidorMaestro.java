import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServidorMaestro {
    private static final int PUERTO_MAESTRO = 5000;
    private static final int PUERTO_INICIAL_SERVIDORES = 6000;
    private static Map<Integer, Tarea> listaTareas = new ConcurrentHashMap<>();
    private static List<ServidorSecundario> servidores = new CopyOnWriteArrayList<>();
    private static ExecutorService poolServidores = Executors.newCachedThreadPool();
    private static int contadorTareas = 0;
    private static int contadorServidores = 0;
    private static int puertoSiguiente = PUERTO_INICIAL_SERVIDORES;

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO_MAESTRO)) {
            System.out.println("Servidor Maestro iniciado en el puerto " + PUERTO_MAESTRO);
            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("Nuevo cliente conectado desde: " + cliente.getInetAddress());
                asignarClienteAServidor(cliente);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void asignarClienteAServidor(Socket cliente) {
        ServidorSecundario servidorDisponible = null;

        // Buscar un servidor con espacio
        for (ServidorSecundario servidor : servidores) {
            if (servidor.puedeAceptarCliente()) {
                servidorDisponible = servidor;
                break;
            }
        }

        // Si no hay servidores disponibles, crear uno nuevo con un nuevo puerto
        if (servidorDisponible == null) {
            servidorDisponible = new ServidorSecundario(++contadorServidores, puertoSiguiente++);
            servidores.add(servidorDisponible);
            poolServidores.execute(servidorDisponible);
        }

        servidorDisponible.asignarCliente(cliente);
    }

    static class Tarea {
        int id;
        String descripcion;
        String fechaHora;
        String prioridad;
        String areaResponsable;
        boolean completada;

        public Tarea(int id, String descripcion, String fechaHora, String prioridad, String areaResponsable) {
            this.id = id;
            this.descripcion = descripcion;
            this.fechaHora = fechaHora;
            this.prioridad = prioridad;
            this.areaResponsable = areaResponsable;
            this.completada = false;
        }

        public void marcarCompletada() {
            this.completada = true;
        }

        public String toString() {
            return "[" + id + "] " + descripcion + " - " + fechaHora + " - Prioridad: " + prioridad + " - Area: " + areaResponsable + " - Estado: " + (completada ? "Completada" : "Pendiente");
        }
    }

    static class ServidorSecundario extends Thread {
        private int idServidor;
        private int puerto;
        private List<ClienteHandler> clientes = new CopyOnWriteArrayList<>();
        private static final int MAX_CLIENTES = 2;
        private ServerSocket servidorSocket;

        public ServidorSecundario(int id, int puerto) {
            this.idServidor = id;
            this.puerto = puerto;
        }

        public boolean puedeAceptarCliente() {
            return clientes.size() < MAX_CLIENTES;
        }

        public void asignarCliente(Socket cliente) {
            if (puedeAceptarCliente()) {
                ClienteHandler nuevoCliente = new ClienteHandler(cliente, this);
                clientes.add(nuevoCliente);
                poolServidores.execute(nuevoCliente);
                System.out.println("Cliente asignado al Servidor Secundario " + idServidor + " en el puerto " + puerto);
            }
        }

        public void removerCliente(ClienteHandler cliente) {
            clientes.remove(cliente);
            System.out.println("Cliente eliminado del Servidor Secundario " + idServidor);
            verificarCierre();
        }

        private void verificarCierre() {
            if (clientes.isEmpty()) {
                System.out.println("Servidor Secundario " + idServidor + " sin clientes, cerrando...");
                servidores.remove(this);
                try {
                    if (servidorSocket != null) servidorSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void run() {
            System.out.println("Servidor Secundario " + idServidor + " iniciado en el puerto " + puerto);
        }

        public int getPuerto() {
            return puerto;
        }

        public int getIdServidor() {
            return idServidor;
        }
    }

    static class ClienteHandler extends Thread {
        private Socket socket;
        private PrintWriter salida;
        private String nombreUsuario;
        private ServidorSecundario servidor;

        public ClienteHandler(Socket socket, ServidorSecundario servidor) {
            this.socket = socket;
            this.servidor = servidor;
        }

        public void run() {
            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                salida = new PrintWriter(socket.getOutputStream(), true);

                salida.println("Conectado al Servidor Secundario " + servidor.getIdServidor() + " en el puerto " + servidor.getPuerto());

                nombreUsuario = entrada.readLine();
                System.out.println("Usuario conectado: " + nombreUsuario);

                sincronizarLista();

                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    procesarComando(mensaje);
                }
            } catch (IOException e) {
                System.out.println("Cliente desconectado: " + nombreUsuario);
            } finally {
                servidor.removerCliente(this);
                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }

        private void procesarComando(String comando) {
            String[] partes = comando.split("#");
            String accion = partes[0];

            switch (accion) {
                case "AGREGAR":
                    if (partes.length < 5) {
                        salida.println("ERROR: Formato incorrecto. Use: AGREGAR#Descripcion#FechaHora#Prioridad#Area");
                        salida.flush();
                        return;
                    }
                    Tarea nuevaTarea = new Tarea(++contadorTareas, partes[1], partes[2], partes[3], partes[4]);
                    listaTareas.put(nuevaTarea.id, nuevaTarea);

                    salida.println("OK... Tarea agregada con ID " + nuevaTarea.id);
                    salida.flush();
                    sincronizarTodos();
                    salida.println("FIN_LISTA");  
                    break;
                case "ELIMINAR":
                    try {
                        int idEliminar = Integer.parseInt(partes[1]);
                        if (listaTareas.remove(idEliminar) != null) {
                            sincronizarTodos();
                            salida.println("OK: Tarea eliminada");
                        } else {
                            salida.println("ERROR: Tarea no encontrada");
                        }
                    } catch (NumberFormatException e) {
                        salida.println("ERROR: ID inválido");
                    }
                    break;

                case "MODIFICAR":
                    try {
                        int idModificar = Integer.parseInt(partes[1]);
                        if (listaTareas.containsKey(idModificar)) {
                            listaTareas.get(idModificar).descripcion = partes[2];
                            listaTareas.get(idModificar).fechaHora = partes[3];
                            sincronizarTodos();
                            salida.println("OK: Tarea modificada");
                        } else {
                            salida.println("ERROR: Tarea no encontrada");
                        }
                    } catch (Exception e) {
                        salida.println("ERROR: Formato incorrecto");
                    }
                    break;

                case "COMPLETAR":
                    try {
                        int idCompletar = Integer.parseInt(partes[1]);
                        if (listaTareas.containsKey(idCompletar)) {
                            listaTareas.get(idCompletar).marcarCompletada();
                            sincronizarTodos();
                            salida.println("OK: Tarea completada");
                        } else {
                            salida.println("ERROR: Tarea no encontrada");
                        }
                    } catch (NumberFormatException e) {
                        salida.println("ERROR: ID invalido");
                    }
                    break;

                case "LISTA":
                    sincronizarLista();
                    break;

                default:
                    salida.println("ERROR: Comando no reconocido");
            }
        }
        private void sincronizarLista() {
            try {
                for (Tarea tarea : listaTareas.values()) {
                salida.println(tarea.toString());  
                }   
                Thread.sleep(50);  
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        private void sincronizarTodos() {
            for (ServidorSecundario servidor : servidores) {
                for (ClienteHandler cliente : servidor.clientes) {
                    cliente.sincronizarLista();  
                }
            }
        }
    }
}
