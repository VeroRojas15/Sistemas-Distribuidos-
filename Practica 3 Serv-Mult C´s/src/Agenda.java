import com.github.lgooddatepicker.components.TimePicker;
import com.toedter.calendar.JDateChooser; 
import java.io.*;
import java.net.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Agenda {
    private static final String HOST = "localhost";
    private static final int Port = 5050;
    private Socket socket;
    private ObjectOutputStream write;
    private ObjectInputStream listen;

    private JFrame frame;
    private JTextArea areaTexto;
    private JTextField textDescription;
    private JTextField campoID; 
    private JButton btnAgregar, btnCompletar, btnPosponer, btnSalir;
    private JTextArea textProximas;
    private JDateChooser selectDate;
    private TimePicker selectTime;

    private String nombreUsuario;

    public Agenda() {
        solicitarNombre();
        iniciarInterfaz();
        conectServer();
    }

    private void solicitarNombre() {
        nombreUsuario = JOptionPane.showInputDialog(null, "Introduce tu nombre:", "Nombre de usuario", JOptionPane.PLAIN_MESSAGE);
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            nombreUsuario = "Desconocido";
        }
    }

    private void conectServer() {
        try {
            socket = new Socket(HOST, Port);
            write = new ObjectOutputStream(socket.getOutputStream());
            listen = new ObjectInputStream(socket.getInputStream());

            // Enviar el nombre al servidor
            write.writeObject(nombreUsuario);

            areaTexto.append("Conectado al servidor como: " + nombreUsuario + "\n");
        } catch (IOException e) {
            areaTexto.append("Error al conectar con el servidor: " + e.getMessage() + "\n");
        }
    }

    private void agregarTarea() {
        try {
            String description = textDescription.getText();
            Date date = selectDate.getDate();
            LocalTime time = selectTime.getTime();
            if (date == null || time == null) {
                areaTexto.append("⚠ Debe seleccionar fecha y hora.\n");
                return;
            }

            SimpleDateFormat formDate = new SimpleDateFormat("dd-MM-yyyy");
            String strDate = formDate.format(date);
            DateTimeFormatter formTime = DateTimeFormatter.ofPattern("HH:mm:ss");
            String strTime = time.format(formTime);
            SimpleDateFormat formDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date dateTime = formDateTime.parse(strDate + " " + strTime);

            write.writeObject("ADD");
            write.writeObject(description);
            write.writeObject(dateTime);

            String res = (String) listen.readObject();
            areaTexto.append("Servidor: " + res + "\n");
            String listTasks = (String) listen.readObject();
            areaTexto.append(listTasks + "\n");
            String listReminders = (String) listen.readObject();
            textProximas.setText("");
            textProximas.append(listReminders + "\n");

        } catch (Exception e) {
            areaTexto.append("⚠ Error al agregar tarea: " + e.getMessage() + "\n");
        }
    }

    private void tasksCompleted() {
        try {
            int id = Integer.parseInt(campoID.getText());
            write.writeObject("COMPLETE");
            write.writeObject(id);

            String res = (String) listen.readObject();
            areaTexto.append("Servidor: " + res + "\n");
            String listTasks = (String) listen.readObject();
            areaTexto.append(listTasks + "\n");
            String listReminders = (String) listen.readObject();
            textProximas.setText("");
            textProximas.append(listReminders + "\n");
        } catch (Exception e) {
            areaTexto.append("⚠ Error al completar tarea: " + e.getMessage() + "\n");
        }
    }

    private void Post() {
        try {
            int id = Integer.parseInt(campoID.getText());
            write.writeObject("POST");
            write.writeObject(id);

            String res = (String) listen.readObject();
            areaTexto.append("Servidor: " + res + "\n");
            String listTasks = (String) listen.readObject();
            areaTexto.append(listTasks + "\n");
            String listReminders = (String) listen.readObject();
            textProximas.setText("");
            textProximas.append(listReminders + "\n");
        } catch (Exception e) {
            areaTexto.append("⚠ Error al posponer tarea: " + e.getMessage() + "\n");
        }
    }

    private void salir() {
        try {
            if (write != null) {
                write.writeObject("EXIT");
                write.flush();
            }
            if (listen != null) listen.close();
            if (write != null) write.close();
            if (socket != null) socket.close();

            System.out.println("Cliente desconectado del servidor.");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("⚠ Error al cerrar la conexión: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Agenda::new);
    }

    private void iniciarInterfaz() {
        frame = new JFrame("User Digital Agenda - " + nombreUsuario);
        frame.setSize(900, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 20));

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        JPanel panelRecordatorios = new JPanel(new BorderLayout());
        panelRecordatorios.setBorder(BorderFactory.createTitledBorder("Recordatorios"));
        textProximas = new JTextArea();
        textProximas.setEditable(false);
        textProximas.setLineWrap(true);
        textProximas.setWrapStyleWord(true);
        textProximas.setPreferredSize(new Dimension(300, 0));
        panelRecordatorios.add(new JScrollPane(textProximas), BorderLayout.CENTER);

        JPanel panelTareas = new JPanel(new BorderLayout());
        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        areaTexto.setLineWrap(true);
        areaTexto.setWrapStyleWord(true);
        panelTareas.add(new JScrollPane(areaTexto), BorderLayout.CENTER);

        panelPrincipal.add(panelRecordatorios, BorderLayout.WEST);
        panelPrincipal.add(panelTareas, BorderLayout.CENTER);

        frame.add(panelPrincipal, BorderLayout.CENTER);

        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new GridLayout(4, 500, 5, 5));
        panelSuperior.add(new JLabel("Descripción:"));
        textDescription = new JTextField();
        panelSuperior.add(textDescription);
        panelSuperior.add(new JLabel("Fecha:"));
        selectDate = new JDateChooser();
        panelSuperior.add(selectDate);
        panelSuperior.add(new JLabel("Hora:"));
        selectTime = new TimePicker();
        panelSuperior.add(selectTime);
        panelSuperior.add(new JLabel("ID de tarea:"));
        campoID = new JTextField();
        panelSuperior.add(campoID);

        frame.add(panelSuperior, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayout(1, 15, 10, 10));
        btnAgregar = new JButton("Agregar");
        btnCompletar = new JButton("Completar");
        btnPosponer = new JButton("Posponer");
        btnSalir = new JButton("Salir");
        panelBotones.add(btnAgregar);
        panelBotones.add(btnCompletar);
        panelBotones.add(btnPosponer);
        panelBotones.add(btnSalir);
        frame.add(panelBotones, BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> agregarTarea());
        btnCompletar.addActionListener(e -> tasksCompleted());
        btnPosponer.addActionListener(e -> Post());
        btnSalir.addActionListener(e -> salir());

        frame.setVisible(true);
    }
}
