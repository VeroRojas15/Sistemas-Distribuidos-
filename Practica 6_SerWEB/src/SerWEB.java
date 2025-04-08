import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;

class SerWEB {
    public static final int PUERTO=5600;
    ServerSocket ss;
    ExecutorService ejecutorSubprocesos;
   
    class Receptor implements Runnable {
        protected Socket socket;
        DataOutputStream dos;
        DataInputStream dis;
        protected String FileName;
        int contBytes;
        List<String> parametro;
        List<String> valor;


        public Receptor(Socket _socket) throws Exception {
            this.socket=_socket;
        }
        
        // Método principal que maneja la solicitud del cliente
        public void run() {
            try {
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
                valor = new ArrayList<>();
                parametro = new ArrayList<>();
                contBytes=0;

                byte[] b = new byte[50000*2]; // Buffer para leer datos
                int t = dis.read(b);
                
                String peticion=null;
                if(t>0) {
                    peticion = new String(b,0,t);//Convierte los bytes leídos en cadena que contiene la solicitud HTTP.
                }

                System.out.println("t: "+t);

                if(peticion==null) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("<html><head><title>Servidor WEB\n");
                    sb.append("</title><body bgcolor=\"#AACCFF\"<br>Linea Vacia</br>\n");
                    sb.append("</body></html>\n");
                    dos.write(sb.toString().getBytes());
                    dos.flush();
                    socket.close();
                    return;
                }
                
                // Procesa la primera línea de la petición
                System.out.println("\nCliente Conectado desde: "+socket.getInetAddress());
                System.out.println("Puerto: "+socket.getPort());
                System.out.println("Datos: "+peticion+"\r\n\r\n");

                StringTokenizer st1= new StringTokenizer(peticion,"\n");
                String line = st1.nextToken();
                contBytes+=line.getBytes().length;

                // Manejo de diferentes tipos de solicitudes HTTP
                if(line.toUpperCase().startsWith("GET")) {
                    if(line.indexOf("?")==-1) {    						
                        getArch(line); 
                        if(FileName.compareTo("")==0) 
                        {
                            SendA("index.htm",dos);
                        }else {
                            SendA(FileName,dos);
                        }  												
                    }else {
                        StringTokenizer tokens=new StringTokenizer(line,"?");
                        String req_a=tokens.nextToken();
                        String req=tokens.nextToken();
                        System.out.println("Token1: "+req_a);
                        System.out.println("Token2: "+req);
                        String parametros = req.substring(0, req.indexOf(" "))+"\n"; 
                        System.out.println("parametros: "+parametros);
                        StringBuffer respuesta= new StringBuffer(); 

                        respuesta.append("HTTP/1.0 200 Okay \n");
                        String fecha= "Date: " + new Date()+" \n";
                        respuesta.append(fecha);
                        String tipo_mime = "Content-Type: text/html \n\n";
                        respuesta.append(tipo_mime);
                        respuesta.append("<html><head><title>SERVIDOR WEB</title></head>\n");
                        respuesta.append("<body bgcolor=\"#AACCFF\"><center><h1><br>Parametros Obtenidos...</br></h1><h3><b>\n");
                        respuesta.append(parametros);
                        respuesta.append("</b></h3>\n");
                        respuesta.append("</center></body></html>\n\n");
                        System.out.println("Respuesta: "+respuesta);
                        dos.write(respuesta.toString().getBytes()); //Manda la respuesta
                        dos.flush();
                        dos.close();
                        socket.close();
                    }

                } else if(line.toUpperCase().startsWith("PUT")) {

                    String archExt=null;
                    do {
                        line = st1.nextToken();
                        contBytes+=line.getBytes().length;
                        if(line.startsWith("Content-Disposition")) {
                            archExt=line.substring(line.lastIndexOf("=")+2, line.lastIndexOf("\""));
                            break;
                        }				
                    }while(st1.hasMoreTokens());

                    System.out.println("Archivo: " + archExt);

                    line = st1.nextToken();
                    contBytes+=line.getBytes().length;
                    line = st1.nextToken();
                    contBytes+=line.getBytes().length;

                    System.out.println("Se leyeron: " + contBytes);
                    System.out.println("Tamaño Buffer: " + b.length);
                    System.out.println("Ocupado: " + t);

                    byte[] array=Arrays.copyOfRange(b, contBytes+14,t-58);

                    File archivoReconstruido = new File("C:\\Users\\angie\\OneDrive\\Escritorio\\SerWEB\\"+archExt);
                    Path rutaArchivoReconstruido = archivoReconstruido.toPath();
                    Files.write(rutaArchivoReconstruido, array, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    String respuesta = "HTTP/1.1 200 OK\r\n" +
                       "Content-Type: text/html\r\n\r\n" +
                       "<html><head><title>SERVIDOR WEB</title></head>\r\n"
                       + "<body bgcolor=\"#AACCFF\"><center><h1><br>ARCHIVO SUBIDO CORRECTAMENTE</br></h1>\r\n"
                       + "</center></body></html>";
                    dos.write(respuesta.getBytes());
                    dos.flush();
                    dos.close();
                    socket.close();

                } else if(line.toUpperCase().startsWith("DELETE")) {

                    getArch(line);

                    String archEl="C:\\Users\\angie\\OneDrive\\Escritorio\\SerWEB\\"+FileName;
                    System.out.println("Archivo a eliminar: " + archEl);

                    File archivo = new File(archEl);
                    String respuesta;

                    if (archivo.exists()) {
                        if (archivo.delete()) {
                            respuesta = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html\r\n\r\n" +
                                "<html><head><title>SERVIDOR WEB</title></head>\r\n"
                                + "<body bgcolor=\"#AACCFF\"><center><h1><br>ARCHIVO ELIMINADO CORRECTAMENTE</br></h1>\r\n"
                                + "</center></body></html>";
                        } else{
                            respuesta = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html\r\n\r\n" +
                                "<html><head><title>SERVIDOR WEB</title></head>\r\n"
                                + "<body bgcolor=\"#AACCFF\"><center><h1><br>NO SE PUDO ELIMINAR EL ARCHIVO</br></h1>\r\n"
                                + "</center></body></html>";
                        }    
                    } else{
                        respuesta = "HTTP/1.1 200 OK\r\n" +
                           "Content-Type: text/html\r\n\r\n" +
                           "<html><head><title>SERVIDOR WEB</title></head>\r\n"
                           + "<body bgcolor=\"#AACCFF\"><center><h1><br>EL ARCHIVO NO EXISTE</br></h1>\r\n"
                           + "</center></body></html>";
                    }

                    dos.write(respuesta.getBytes());
                    dos.flush();
                    dos.close();
                    socket.close();

                } else if(line.toUpperCase().startsWith("HEAD")) {
                    if(line.indexOf("?")==-1) {    						
                        getArch(line); //Obtiene el nombre del archivo
                        //System.out.println("Valor archivo: "+FileName);
                        if(FileName.compareTo("")==0) {
                            SendASB("index.htm",dos);
                        }else{
                            SendASB(FileName,dos);
                        }  												
                    }

                    socket.close();

                } else if(line.toUpperCase().startsWith("POST")) {
                    if(line.indexOf("?")==1) {
                        System.out.println("SOLICITUD INCORRECTA");
                    }else {
                        do {
                            line = st1.nextToken();
                            if(line.startsWith("Content-Length")) {
                                line = st1.nextToken();
                                break;
                            }				
                        }while(st1.hasMoreTokens());

                        if(!st1.hasMoreTokens()) {
                            StringBuffer respuesta= new StringBuffer(); //Crea un buffer para ir agregando los elementos 

                            respuesta.append("HTTP/1.0 200 Okay \n");
                            String fecha= "Date: " + new Date()+" \n";
                            respuesta.append(fecha);
                            String tipo_mime = "Content-Type: text/html \n\n";
                            respuesta.append(tipo_mime);
                            respuesta.append("<html><head><title>SERVIDOR WEB</title></head>\n");
                            respuesta.append("<body bgcolor=\"#AACCFF\"><center><h1><br>Parametros Obtenidos...</br></h1><h3><b>\n");
                            respuesta.append("SE RECIBIO UNA SOLICITUD SIN PARAMETROS");
                            respuesta.append("</b></h3>\n");
                            respuesta.append("</center></body></html>\n\n");

                            System.out.println("Respuesta: "+respuesta);
                            dos.write(respuesta.toString().getBytes());
                            dos.flush();
                            dos.close();
                            socket.close();	  
                        }else {
                            do {
                                line = st1.nextToken();
                                if(line.startsWith("----------------------------")) {
                                    if(!st1.hasMoreTokens()) {
                                        break;
                                    }
                                    line = st1.nextToken();
                                    parametro.add(line.substring(line.lastIndexOf("=")+2, line.lastIndexOf("\"")));
                                    line = st1.nextToken();
                                    line = st1.nextToken();
                                    valor.add(line);
                                }				
                            }while(st1.hasMoreTokens());

                            System.out.println("Parametros: \n"+parametro.toString()+"   "+valor.toString());

                            StringBuffer respuesta= new StringBuffer(); //Crea un buffer para ir agregando los elementos 

                            respuesta.append("HTTP/1.0 200 Okay \n");
                            String fecha= "Date: " + new Date()+" \n";
                            respuesta.append(fecha);
                            String tipo_mime = "Content-Type: text/html \n\n";
                            respuesta.append(tipo_mime);
                            respuesta.append("<html><head><title>SERVIDOR WEB</title></head>\n");
                            respuesta.append("<body bgcolor=\"#AACCFF\"><center><h1><br>Parametros Obtenidos...</br></h1><h3><b>\n");
                            respuesta.append("Parametros: "+parametro.toString()+" Valores: "+valor.toString());
                            respuesta.append("</b></h3>\n");
                            respuesta.append("</center></body></html>\n\n");
                            System.out.println("Respuesta: "+respuesta);
                            dos.write(respuesta.toString().getBytes()); //Manda la respuesta
                            dos.flush();
                            dos.close();
                            socket.close();
                        }
                    }
                }else {
                    dos.write("HTTP/1.0 501 Not Implemented\r\n".getBytes());
                    dos.flush();
                    dos.close();
                    socket.close();
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
            contBytes=0;
        }//run
        
        
        public void getArch(String line) { // Obtiene el nombre del archivo solicitado en la petición
            int i;
            int f;
            
            if(line.toUpperCase().startsWith("GET") || line.toUpperCase().startsWith("DELETE") || line.toUpperCase().startsWith("HEAD")) {
                i=line.indexOf("/");
                f=line.indexOf(" ",i);
                FileName=line.substring(i+1,f);
            }
        }
        
        //
        public void SendA(String arg, DataOutputStream dos1) {
            try{
                int b_leidos=0;
                DataInputStream dis2 = new DataInputStream(new FileInputStream("C:\\Users\\angie\\OneDrive\\Escritorio\\SerWEB\\"+arg));

                byte[] buf=new byte[1024];
                int x=0;
                File ff = new File("C:\\Users\\angie\\OneDrive\\Escritorio\\SerWEB\\"+arg);			
                long tam_archivo=ff.length(),cont=0;

                // Determinar el Content-Type basado en la extensión del archivo
                String mimeType = "application/octet-stream";  // Predeterminado
                if (arg.endsWith(".jpg") || arg.endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                }else if (arg.endsWith(".png")) {
                    mimeType = "image/png";
                }else if (arg.endsWith(".gif")) {
                    mimeType = "image/gif";
                }else if (arg.endsWith(".html") || arg.endsWith(".htm")) {
                    mimeType = "text/html";
                }else if (arg.endsWith(".css")) {
                   mimeType = "text/css";
                }else if (arg.endsWith(".js")) {
                    mimeType = "application/javascript";
                }else if (arg.endsWith(".pdf")) {
                    mimeType = "application/pdf";
                }else if (arg.endsWith(".mp4")) {
                    mimeType = "video/mp4";
                }else if (arg.endsWith(".mp3")) {
                    mimeType = "audio/mpeg";
                }else if (arg.endsWith(".wav")) {
                    mimeType = "audio/wav";
                }else if (arg.endsWith(".avi")) {
                    mimeType = "video/x-msvideo";
                }else if (arg.endsWith(".zip")) {
                   mimeType = "application/zip";
                }else if (arg.endsWith(".json")) {
                    mimeType = "application/json";
                }else if (arg.endsWith(".xml")) {
                   mimeType = "application/xml";
                }else if (arg.endsWith(".txt")) {
                    mimeType = "text/plain";
                }else if (arg.endsWith(".ico")) {
                    mimeType = "image/x-icon";
                }else if (arg.endsWith(".svg")) {
                    mimeType = "image/svg+xml";
                }

                String sb = "";
                sb = sb+"HTTP/1.0 200 ok\n";
                sb = sb +"Server: Angie Server/1.0\n";
                sb = sb +"Date: " + new Date()+" \n";
                sb = sb +"Content-Type: " + mimeType + "\n";
                sb = sb +"Content-Length: "+tam_archivo+" \n";
                sb = sb +"\n";
                dos1.write(sb.getBytes());
                dos1.flush();

                while(cont<tam_archivo) {
                    x = dis2.read(buf);
                    dos1.write(buf,0,x);
                    cont=cont+x;
                    dos1.flush();
                }

                dis2.close();
                dos1.close();
            }catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }

        public void SendASB(String arg, DataOutputStream dos1) { //Especificmente para solictudes HEAD
            try{
                int b_leidos=0;
                DataInputStream dis2 = new DataInputStream(new FileInputStream("C:\\Users\\angie\\OneDrive\\Escritorio\\SerWEB\\"+arg));

                byte[] buf=new byte[1024];
                int x=0;
                File ff = new File("C:\\Users\\angie\\OneDrive\\Escritorio\\SerWEB\\"+arg);			
                long tam_archivo=ff.length(),cont=0;

                // Determinar el Content-Type basado en la extensión del archivo
                String mimeType = "application/octet-stream";  // Predeterminado
                if (arg.endsWith(".jpg") || arg.endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                }else if (arg.endsWith(".png")) {
                    mimeType = "image/png";
                }else if (arg.endsWith(".gif")) {
                    mimeType = "image/gif";
                }else if (arg.endsWith(".html") || arg.endsWith(".htm")) {
                    mimeType = "text/html";
                }else if (arg.endsWith(".css")) {
                    mimeType = "text/css";
                }else if (arg.endsWith(".js")) {
                    mimeType = "application/javascript";
                }else if (arg.endsWith(".pdf")) {
                    mimeType = "application/pdf";
                }else if (arg.endsWith(".mp4")) {
                    mimeType = "video/mp4";
                }else if (arg.endsWith(".mp3")) {
                    mimeType = "audio/mpeg";
                }else if (arg.endsWith(".wav")) {
                    mimeType = "audio/wav";
                }else if (arg.endsWith(".avi")) {
                    mimeType = "video/x-msvideo";
                }else if (arg.endsWith(".zip")) {
                    mimeType = "application/zip";
                }else if (arg.endsWith(".json")) {
                    mimeType = "application/json";
                }else if (arg.endsWith(".xml")) {
                    mimeType = "application/xml";
                }else if (arg.endsWith(".txt")) {
                    mimeType = "text/plain";
                }else if (arg.endsWith(".ico")) {
                    mimeType = "image/x-icon";
                }else if (arg.endsWith(".svg")) {
                    mimeType = "image/svg+xml";
                }
                
                String sb = "";
                sb = sb+"HTTP/1.0 200 ok\n";
                sb = sb +"Server: Angie Server/1.0\n";
                sb = sb +"Date: " + new Date()+" \n";
                sb = sb +"Content-Type: " + mimeType + "\n";
                sb = sb +"Content-Length: "+tam_archivo+" \n";
                sb = sb +"\n";
                
                dos1.write(sb.getBytes());
                dos1.flush();
                dis2.close();
                dos1.close();
            }catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
    
    public SerWEB() throws Exception {
        System.out.println("Iniciando Servidor ....");
        this.ss=new ServerSocket(PUERTO);
        this.ejecutorSubprocesos= Executors.newCachedThreadPool(); //crea la piscina de hilos 

        System.out.println("Servidor Iniciado Correctamente\n");
        System.out.println("Esperando Cliente ....");

        try {
            for(;;) {
                Socket accept=ss.accept();
                this.ejecutorSubprocesos.execute(new Receptor(accept));
            }
        }catch(Exception e) {
            this.ejecutorSubprocesos.shutdown();
        }
    }
    
    public static void main(String[] args) throws Exception{
        SerWEB sWEB=new SerWEB();
    }
}