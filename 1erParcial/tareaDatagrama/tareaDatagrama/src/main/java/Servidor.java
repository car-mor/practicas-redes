import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Servidor {
    private static int NACK = 0; //bandera acuse NACK inicia desactivada
    public static void main(String[] args) {
        try {
            // socket
            DatagramSocket socket = new DatagramSocket(8888);
            socket.setReuseAddress(true);
            System.out.println("Servidor iniciado. Esperando paquetes...");
            
            //contador para los paquetes
            int paqueteEsperado = 0;
            //contador bytes para guardar el archivo mas adelante
            int bytesRecibidos = 0;
            
            //Obtener tamaño en Bytes del archivo a recibir
            byte[] fileSizeBytes = new byte[8];
            DatagramPacket fileSizePacket = new DatagramPacket(fileSizeBytes, fileSizeBytes.length);
            socket.receive(fileSizePacket);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(fileSizePacket.getData()));
            long tamañoArchivo = dis.readLong();
        
            //Obtener nombre en Bytes del archivo a recibir
            byte[] nombreArchivoBytes = new byte[64];
            DatagramPacket nombreArchivoPacket = new DatagramPacket(nombreArchivoBytes, nombreArchivoBytes.length);
            socket.receive(nombreArchivoPacket);
            String nombreArchivo = new String(nombreArchivoPacket.getData(), 0, nombreArchivoPacket.getLength());
                
            //para guardar el archivo en bytes mas adelante
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            
            //EMPEZAR A recibir paquetes
            while (true) {
                            
                //num max que puede recibir datagramas -> 65535
                DatagramPacket p = new DatagramPacket(new byte[65535],65535); 
                // Recibir el datagrama
                socket.receive(p);
                
                //obtenemos el valor del contador (primer byte del datagrama)
                int NumPaquete = p.getData()[0] & 0xFF; //pasarlo a num entero
                
                //se confirma que es el paquete que se esperaba
                if(NumPaquete == paqueteEsperado){
                    
                    //variable lectura
                    int bytesRead = p.getLength();
                
                    //datos del paquete
                    byte[] packetData = Arrays.copyOfRange(p.getData(), 1, 1 + bytesRead);
                
                    byteArrayOutputStream.write(packetData);
                    bytesRecibidos += bytesRead; // Incrementar el contador de bytes recibidos
                    
                    System.out.println("Paquete #" + NumPaquete + " recibido de " + p.getAddress()+":"+p.getPort() +" con: "+ bytesRead +" bytes");
                
                    //vamos a enviar confirmacion de recibido
                    enviarConfirmacion(socket, p.getAddress(), p.getPort(), NumPaquete);
                    
                    //contador de paquete esperado
                    paqueteEsperado = NumPaquete + 1;
                    System.out.println(bytesRecibidos + "/" +tamañoArchivo);
                    
                    //se escribe el archivo en bytes cuando se tengan tosos los bytes del archivo
                    if (bytesRecibidos >= tamañoArchivo) {
                        guardarArchivo(byteArrayOutputStream.toByteArray(), nombreArchivo);
                        byteArrayOutputStream.reset();
                        break;
                    }
                //NO se recibió el paquete que se esperaba
                }else{
                    //vamos a enviar acuse NACK para solicitar la reetransmisión
                    System.out.println("Paquete #" + NumPaquete + " descartado, se esperaba el paquete #" + paqueteEsperado);
                    if(NACK == 0){
                        //se hcae 1 no envia mas de 1 acuse
                        NACK = 1;
                        NACK(socket, p.getAddress(), p.getPort(), paqueteEsperado);
                        //se regresa al while
                    }
                }
             
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    // enviar una confirmacion al cliente
    private static void enviarConfirmacion(DatagramSocket socket, InetAddress address, int port, int numPaquete) throws IOException {
        // byte array con la confirmación (2bytes)
        byte[] confirmacionData = new byte[2];

        // en el primer byte agregamos 1 que significa CONFIRMACION
        confirmacionData[0] = 1;
        
        // en el segundo byte agregamos el numero de paquete 
        confirmacionData[1] = (byte) numPaquete;
        
        // creacion datagrama
        DatagramPacket confirmacionPacket = new DatagramPacket(confirmacionData, confirmacionData.length, address, port);

        // se envia
        socket.send(confirmacionPacket);
        System.out.println("Confirmación para paquete #" + numPaquete + " enviada al cliente");
    }
    
    
    // enviar acuse NACK
    private static void NACK(DatagramSocket socket, InetAddress address, int port, int paqueteEsperado) throws IOException {
        
        // byte array para el acuse NACK (2bytes)
        byte[] acuseNACK = new byte[2];
        
        // en el primer byte agregamos 0 que significa un acuse NACK
        acuseNACK[0] = 0;
        
        // en el segundo byte agregamos el numero de paquete que se necesita volver a transmitir
        acuseNACK[1] = (byte) paqueteEsperado;
        
        // creacion datagrama
        DatagramPacket confirmacionPacket = new DatagramPacket(acuseNACK, acuseNACK.length, address, port);

        // se envia
        socket.send(confirmacionPacket);
        System.out.println("Acuse NACK para el paquete #" + paqueteEsperado + " enviada al cliente");
    }

    
    private static void guardarArchivo(byte[] data, String nombreArchivo) {
        try {
            String desktopPath = System.getProperty("user.home") + "\\Desktop\\";
            System.out.println("Directorio de escritorio: " + desktopPath);
            FileOutputStream fileOutputStream = new FileOutputStream(desktopPath + nombreArchivo);
            fileOutputStream.write(data);
            fileOutputStream.close();
            System.out.println("Archivo guardado en: " + desktopPath + nombreArchivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}//class servidor