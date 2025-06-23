import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static final int Port = 5050;
    private static List<Tarea> task = new CopyOnWriteArrayList<>();
    private static ScheduledExecutorService reminder = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws ClassNotFoundException {
        try (ServerSocket servidor = new ServerSocket(Port)) {
            System.out.println("Servidor iniciado en el puerto " + Port);

            reminder.scheduleAtFixedRate(() -> { expiredTasks(); listReminders(); }, 0, 1, TimeUnit.MINUTES);

            while (true) {
                Socket user = servidor.accept();
                new Thread(() -> handleClient(user)).start();
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        } finally {
            System.out.println("Cerrando servidor...");
            reminder.shutdown();
            try {
                if (!reminder.awaitTermination(2, TimeUnit.SECONDS)) {
                    reminder.shutdownNow();
                }
            } catch (InterruptedException e) {
                reminder.shutdownNow();
            }
            System.out.println("Servidor cerrado correctamente.");
        }
    }

    private static void handleClient(Socket user) {
        try (ObjectOutputStream write = new ObjectOutputStream(user.getOutputStream());
             ObjectInputStream listen = new ObjectInputStream(user.getInputStream())) {

            String nombre = (String) listen.readObject();
            System.out.println("Cliente conectado: " + nombre + " desde " + user.getInetAddress());

            while (true) {
                try {
                    String action = (String) listen.readObject();

                    if ("ADD".equals(action)) {
                        String description = (String) listen.readObject();
                        Date date = (Date) listen.readObject();

                        if (date.before(new Date())) {
                            write.writeObject("Error: Fecha inválida.");
                        } else {
                            int id = task.size() + 1;
                            Tarea newTask = new Tarea(id, description, date, status(3));
                            task.add(newTask);
                            System.out.println("Tarea agregada: " + description);
                            write.writeObject("Tarea agregada.");
                            write.writeObject(listTasks());
                            write.writeObject(listReminders());

                        }
                    } else if ("COMPLETE".equals(action)){
                        int id = (Integer) listen.readObject(); 
                        if (id > 0 && id <= task.size()) {
                            Tarea tarea = task.get(id - 1);
                            tarea.status = status(1); 
                            System.out.println("Tarea: " + id + " |  " + tarea.status);
                            write.writeObject("Tarea " + id + "  |  " + tarea.status);
                            write.writeObject(listTasks());
                            write.writeObject(listReminders());
                        } else {
                            write.writeObject("Error: ID de tarea inválido.");
                        }
                    } else if ("POST".equals(action)){
                        int id = (Integer) listen.readObject();
                        if(id > 0 && id <= task.size()){
                            Tarea tarea = task.get(id - 1);
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(tarea.fecha);
                            calendar.add(Calendar.DATE, 1);
                            tarea.fecha = calendar.getTime();  
                            tarea.status = status(2);  
                            write.writeObject("Tarea " + id + " | " + tarea.status + " | Nueva fecha: " + 
                                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(tarea.fecha));
                            write.writeObject(listTasks());  
                            write.writeObject(listReminders());  
                        } else {
                            write.writeObject("Error: ID de tarea inválido.");
                        }
                    } else if ("EXIT".equals(action)) {
                        System.out.println("🔴 Cliente desconectado: " + nombre);
                        write.writeObject("Desconexión exitosa.");
                        break; 
                    }
                } catch (EOFException | SocketException e) {
                    System.out.println("Cliente desconectado abruptamente: " + nombre);
                    break;
                }
            }
            listen.close();
            write.close();
            user.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error en la comunicación con el cliente: " + e.getMessage());
        }
    }

    public static String status(int status) {
        switch (status) {
            case 1:
                return "✅ Completada";
            case 2:
                return "🔄 Pospuesta";
            case 3:
                return "⏳ En tiempo";
            case 4:
                return "❌ Vencida";
            default:
                return "Desconocido";
        }
    }

    private static String listTasks() {
        if (task.isEmpty()) {
            return "📌 Lista de tareas:\n\tNo hay tareas registradas.\n";
        }
        StringBuilder lista = new StringBuilder("\n📌 Lista de tareas:\n");
        for (Tarea t : task) {
            if (!t.status.equals("✅ Completada")) { 
                lista.append("   ID: ").append(t.id)  
                     .append(" | Descripción: ").append(t.descripcion)  
                     .append(" | Fecha: ").append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(t.fecha))  
                     .append(" | Estado: ").append(t.status)
                     .append("\n");
            }
        }
        lista.append("----------------------------------\n");
        return lista.toString();
    }

    private static String listReminders() {
        if (task.isEmpty()) {
            return "\n📌\t•\n\t•\n\t•\n\t•\n\t•\n";
        }

        StringBuilder list = new StringBuilder("\n📌 \n");
        Date ahora = new Date();
        Calendar limite = Calendar.getInstance();
        limite.setTime(ahora);
        limite.add(Calendar.HOUR, 48);
        boolean reminders = false;

        for (Tarea t : task) {
            if (!t.status.equals("✅ Completada")&& t.fecha.before(limite.getTime())) { 
                list.append(" 🔴 ID: ").append(t.id)  
                     .append(" | Descripción: ").append(t.descripcion)  
                     .append(" |\n Fecha: ").append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(t.fecha))  
                     .append(" | Estado: ").append(t.status)  
                     .append("\n");
                reminders = true;
            }
        }

        if (!reminders) {
            return "📌\t•\n\t•\n\t•\n\t•\n\t•\n";
        }
        list.append("----------------------------------\n");
        return list.toString();
    }

    private static void expiredTasks() {
        Date ahora = new Date();
        for (Tarea t : task) {
            if (!t.status.equals("✅ Completada") && t.fecha.before(ahora)) {
                t.status = status(4);
                System.out.println("⏳ Tarea vencida: " + t.descripcion);
            }
        }
    }
}

class Tarea implements Serializable {
    private static final long serialVersionUID = 1L;
    String descripcion;
    Date fecha;
    final int id;
    String status;
    public Tarea(int id, String descripcion, Date fecha, String status) {
        this.id = id;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.status  = status;
    }

    @Override
    public String toString() {
        return "Tarea: " + descripcion + " | Fecha: " + fecha + " | Estado: " + status;
    }
}
