import java.io.*;
import java.net.*;
/*
 * @authors Carlos Moreno y Vanessa Trejo
 */
public class Servidor {
    public static void main(String[] args) {
        try{
           ServerSocket s = new ServerSocket(1234); //asociacion al puerto 1234
            s.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            System.out.println("Servidor iniciado en el puerto "+s.getLocalPort()); //si se creó, va a imprimir el puerto al que está asociado, si está filtrado o bloqueado por firewall mandaría excepción
            for(;;){ //while(true)
                Socket cl = s.accept(); //en cada interacción aceptamos cliente, y devuelve referencia de tipo socket.
                System.out.println("Cliente conectado desde-> "+cl.getInetAddress()+":"+cl.getPort());//registro de quien fue ultimo que se conecto
                
            }    
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
