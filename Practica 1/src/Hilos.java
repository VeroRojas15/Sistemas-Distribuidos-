import java.io.IOException;

public class Hilos {
    private static int contador = 0;
    private static boolean ejecutar = true; // Control para detener los hilos

    public static void main(String[] args) {
        // El hilo incrementa el contador cada segundo
        Thread hiloSum = new Thread(() -> {
            try {
                while (ejecutar) {
                    synchronized (Hilos.class) {
                        contador++;
                    }
                    Thread.sleep(1000); // Espera 1 segundo
                }
            } catch (InterruptedException e) {
                System.out.println("Hilo sumador interrumpido.");
            }
        });

        // El hilo imprime el contador cada 3 segundos
        Thread hiloRes = new Thread(() -> {
            try {
                while (ejecutar) {
                    Thread.sleep(3000); // Espera 3 segundos
                    synchronized (Hilos.class) {
                        System.out.println("Contador: " + contador);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Hilo de respuesta interrumpido.");
            }
        });
        

        // Hilo que detecta la tecla Enter para detener el programa
        Thread hiloStop = new Thread(() -> {
            System.out.println("Presiona 'Enter' para detener el programa.");
            try {
                System.in.read(); // Captura la entrada del teclado (Enter)
                ejecutar = false;
                System.out.println("Deteniendo hilos...");

                // Interrumpir los hilos manualmente
                hiloSum.interrupt();
                hiloRes.interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Iniciar los hilos
        hiloSum.start();
        hiloRes.start();
        hiloStop.start();

        // Espera a que el hilo de teclado termine antes de salir
        try {
            hiloStop.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Finalizando programa...");
    }
}
