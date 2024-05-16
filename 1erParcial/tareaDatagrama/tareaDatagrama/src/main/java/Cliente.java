import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import javax.swing.JFileChooser;
import java.util.Random;//para simular la perdida del paquete

public class Cliente {
    // ctespara el temporizador y el número máximo de intentos
    private static final int TIMEOUT = 1000; // 5 segundos de espera
    //private static final int MAX_ATTEMPTS = 3; // max de intentos
    private static int NACK = 0; // cambia a 1 cuando se recibió el acuse de retransmisión
    private static int NACK_paquete = 0; // que paquete se perdió?
    private static File ARCHIVO; // que paquete se perdió?
    private static Random random = new Random(); //instancia "random" para perdida de paquete aleatoria

    
    public static void main(String[] args) {
        try {
            // nuevo socket datagrama socket cliente
            String host="127.0.0.1";
            int pto=8888;
            InetAddress dst = InetAddress.getByName(host);
            DatagramSocket cl = new DatagramSocket();
            System.out.println("Cliente iniciado para enviar achivo a  "+host+":"+pto);

            // tamaño de ventana dado por usuario en bytes
            Scanner scanner = new Scanner(System.in);
            System.out.print("Ingrese el tamaño de ventana en bytes: ");
            int TamañoVentana = scanner.nextInt();

            // tamaño de paquete dado por usuario bytes
            System.out.print("Ingrese el tamaño del paquete en bytes: ");
            int TamañoPaquete = scanner.nextInt();
            
            //calcular número de paquetes
            int NumPaquetes = TamañoVentana / TamañoPaquete;
            System.out.println("Número de paquetes: " + NumPaquetes);
            
            //solicitar al usuario elegir el archivo 
            System.out.println("Seleccione el archivo para enviar ");
            JFileChooser jf = new JFileChooser();
            jf.setCurrentDirectory(new File("C:/users/adria/documents"));
            jf.setRequestFocusEnabled(true);
            jf.requestFocus();
            jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int r = jf.showDialog(null, "Elegir");
            if(r==JFileChooser.APPROVE_OPTION){
                File f = jf.getSelectedFile();
                String path = f.getAbsolutePath();
                long tam = f.length();
                String nom = f.getName();
                System.out.println("Preparandose pare enviar archivo "+path+" de "+tam+" bytes" + "archivo: " + nom + "\n\n");
                ARCHIVO = f; //variable global
                
                //envia tamaño
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(bos);
                dos.writeLong(tam);
                byte[] fileSizeBytes = bos.toByteArray();
                DatagramPacket fileSizePacket = new DatagramPacket(fileSizeBytes, fileSizeBytes.length, dst, pto);
                cl.send(fileSizePacket);
                
                //envia nombre
                byte[] nombreArchivoBytes = nom.getBytes();
                DatagramPacket nombreArchivoPacket = new DatagramPacket(nombreArchivoBytes, nombreArchivoBytes.length, dst, pto);
                cl.send(nombreArchivoPacket);
               
                // objeto para leer por bytes el archivo
                FileInputStream fileInputStream = new FileInputStream(f);
                
                // bytes restantes por leer del archivo (inicia con el tamaño del file)
                int bytesRestantes = (int) tam;
                
                //contador
                int ii=0;
                boolean pasar = true;
                //enviar datagramas
                while(ii<NumPaquetes && bytesRestantes > 0) {
                    //20% probabilidad de que se pierda un paquete
                    if(random.nextDouble() <0.2 && pasar == true){
                        System.out.println("\nSimulación del paquete: #" + ii + " perdido\n");
                        ii++;
                    }
                    //buffer del tamaño de la ventana y se le agrega 1 byte para el num de paquete
                    byte[] packetData = new byte[TamañoPaquete+1];
                    
                    //se agrega el num de paquete al inicio del datagrama 
                    packetData[0] = (byte)ii;
                    
                    // Leer el archivo en bytes empezando desde el segundo byte y se guarda en bytesRead
                    int bytesRead = fileInputStream.read(packetData, 1, TamañoPaquete);
                    
                    // ver si se llegó al final del archivo (leer) y break si terminó y no hay bytes restantes por leer
                    //se evita agregar 00 al final
                    if (bytesRead == -1 && bytesRestantes == 0) {
                        break; 
                    }
                    //se va restando al tamaño actual los que ya se leyeron
                    bytesRestantes -= bytesRead;
                    
                    // Enviar el datagrama (PAQUETE)cuando haya bytes leidos
                    if (bytesRead > 0) {
                        //se crea datagrama
                        DatagramPacket packet = new DatagramPacket(packetData, bytesRead, dst, pto);
                        //se envía al servidor
                        cl.send(packet);
                        System.out.println("Enviado paquete #" + ii + " con: " + bytesRead + " bytes");
                        //incrementa el contador
                        ii++;
                        
                        // CONFIRMACION o ACUSE NACK+++++++++++++++++++++++++++++++
                        boolean confirmacion = false;
                        while (!confirmacion){
                            try {
                                // temporizador de espera
                                cl.setSoTimeout(TIMEOUT);
            
                                //obtener confirmación del servidor
                                DatagramPacket confirmationPacket = new DatagramPacket(new byte[2], 2);
                                cl.receive(confirmationPacket);
            
                                // saber si es confirmacion 1 o acuse NACK 0
                                int acuses = confirmationPacket.getData()[0] & 0xFF;
                                
                                //es acuse NACK
                                if(acuses == 0){
                                    /*se solicita reetransmisión, entonces obtenemos el num de paquete
                                    que se esperaba recibir*/
                                    int paquetePerdido = confirmationPacket.getData()[1] & 0xFF;
                                    NACK_paquete = paquetePerdido;
                                    //se activa bandera de acuse NACK
                                    NACK = 1;
                                    System.out.println("Se recibió acuse NACK para retrasnmisión del paquete: #" + paquetePerdido +". Bandera NACK activada");
                                    //se sale del while para terminar la transmisión
                                    //todos los siguientes paquetes serán descartados por el servidor
                                    confirmacion = true;
                                    
                                //es confirmación
                                }else if (acuses == 1){
                                    // obtener num de paquete confirmado
                                    int paqueteC = confirmationPacket.getData()[1] & 0xFF;
                                    if (paqueteC == ii - 1) {
                                    System.out.println("El paquete #" + paqueteC + " fue confirmado por el servidor");
                                    //se sale de este while
                                    confirmacion = true;
                                    }  
                                }  
                            } catch (SocketTimeoutException e) {
                            //  reenviar el paquete si paso el tiempo
                            System.out.println("No se recibió la confirmación del paquete #" + (ii - 1) + " dentro del tiempo límite");
                            confirmacion = true;
                            }
                        }
                    }
                }
                //termina el while y se enviaron todos los paquetes
                if(NACK == 1){
                    /*ahora se va a reetransmitir el archivo desde donde se 
                    quedó esperando el servidor*/
                    System.out.println("conenzamos RETROCEDER -N");
                    NACK(cl, dst, pto, TamañoVentana, NumPaquetes, TamañoPaquete);
                    //se termina de enviar todo
                    System.out.println("Archivo enviado con exito");
                }else{
                    //archivo enviado correctamente, nunca se recibio el NACK
                    System.out.println("Archivo enviado con éxito");
                }
                //cerrar socket
                cl.close();
            }   
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    //********************* RETROCEDER N *****************************
    // cuando el acuse NACK se activa
    private static void NACK(DatagramSocket cl, InetAddress dst, int pto, int TamañoVentana, int NumPaquetes, int TamañoPaquete) throws IOException {
        //se va a reenviar desde el archivo que se perdió
        //primero se lee el archivo hasta llegar al num de paquete deseado
        int NumPaquetePerdido = NACK_paquete; //num del paquete para el for
        File f = ARCHIVO; //archivo a reenviar
        String path = f.getAbsolutePath();
        long tam = f.length(); //tamaño
        String nom = f.getName();//nombre
        
        // objeto para leer por bytes el archivo
        FileInputStream fileInputStream = new FileInputStream(f);
                
        // bytes restantes por leer del archivo (inicia con el tamaño del file)
        int bytesRestantes = (int) tam;
                
        //contador
        int ii=0;
        
        while(ii<NumPaquetes && bytesRestantes > 0) {
            //buffer del tamaño de la ventana y se le agrega 1 byte para el num de paquete
            byte[] packetData = new byte[TamañoPaquete+1];
                    
            //se agrega el num de paquete al inicio del datagrama 
            packetData[0] = (byte)ii;
                    
            // Leer el archivo en bytes empezando desde el segundo byte y se guarda en bytesRead
            int bytesRead = fileInputStream.read(packetData, 1, TamañoPaquete);
                    
            // ver si se llegó al final del archivo (leer) y break si terminó y no hay bytes restantes por leer
            //se evita agregar 00 al final
            if (bytesRead == -1 && bytesRestantes == 0) {
                break; 
            }
            //se va restando al tamaño actual los que ya se leyeron
            bytesRestantes -= bytesRead;
                    
            // Enviar el datagrama (PAQUETE)cuando haya bytes leidos y COINCIDA
            //con el paquete perdido
            if (bytesRead > 0 && ii == NumPaquetePerdido) {
                //se crea datagrama
                DatagramPacket packet = new DatagramPacket(packetData, bytesRead, dst, pto);
                //se envía al servidor
                cl.send(packet);
                System.out.println("Enviado paquete #" + ii + " con: " + bytesRead + " bytes");
                //incrementa el contador
                
                ii++;
                NumPaquetePerdido = ii; //para seguir entrando al ciclo y mandando los datagramas
                
                
                // CONFIRMACION o ACUSE NACK+++++++++++++++++++++++++++++++
                boolean confirmacion = false;
                int intentos = 0;
                while (!confirmacion){
                try {
                    // temporizador de espera
                    cl.setSoTimeout(TIMEOUT);
            
                    //obtener confirmación del servidor
                    DatagramPacket confirmationPacket = new DatagramPacket(new byte[2], 2);
                    cl.receive(confirmationPacket);
            
                    // saber si es confirmacion 1 o acuse NACK 0
                    int acuses = confirmationPacket.getData()[0] & 0xFF;
                                
                    //es confirmación
                    if(acuses == 1){
                        // obtener num de paquete confirmado
                        int paqueteC = confirmationPacket.getData()[1] & 0xFF;
                        if (paqueteC == ii - 1) {
                            System.out.println("El paquete #" + paqueteC + " fue confirmado por el servidor");
                            //se sale de este while
                            confirmacion = true;
                        }  
                    }  
                    } catch (SocketTimeoutException e) {
                        //  reenviar el paquete si paso el tiempo
                        cl.send(packet);
                        intentos++;
                        }
                }
            
            //aun no se llega al num de paquete perdido
            }else if(bytesRead > 0 && ii != NumPaquetePerdido){
                ii ++;
            }
        
        }
        //se cierran 
        cl.close();
        fileInputStream.close();
    }//end funcion
    
}//main