import java.io.*;
import java.net.*;
import javax.swing.JFileChooser;
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
                try{
                    DataInputStream dis = new DataInputStream(cl.getInputStream());
                    String directiva = dis.readUTF();
                    DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                    switch(directiva){
                        case "list":
                            String instr1="Seleccione la carpeta de donde desea enlistar los directorios";
                            dos.writeUTF(instr1);
                            dos.flush();
                            JFileChooser jf = new JFileChooser();
                            File dir = new File("d:\\Documentos\\");
                            jf.setCurrentDirectory(dir);
                            jf.setRequestFocusEnabled(true);
                            jf.requestFocus();
                            jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                            int r = jf.showDialog(null, "Elegir");
                            if(r==JFileChooser.APPROVE_OPTION){
                                File f = jf.getSelectedFile();
                                String tipo = (f.isDirectory())?"Carpeta":"Archivo";
                                String carpeta = "\033[32m Elegiste: "+f.getAbsolutePath();
                                dos.writeUTF(carpeta);
                                dos.flush();
                                String Tipo = "Tipo: "+tipo;
                                dos.writeUTF(Tipo);
                                dos.flush();
                                if(tipo.compareTo("Carpeta")==0){
                                File[]listado = f.listFiles();
                                    String permisos="";
                                if(f.canRead())
                                   permisos = permisos+"r";
                                if(f.canWrite())
                                   permisos = permisos+"w";
                                if(f.canExecute())
                                   permisos = permisos+"x";
                                dos.writeUTF(permisos);
                                dos.flush();
                                String Seleccionada = f.getName();
                                dos.writeUTF(Seleccionada);
                                dos.flush();
                                enlistarRemoto(f, dos);
                               }//if
                            }
                            dos.close();
                            dis.close();
                            cl.close();
                            break;
                    }          
                }catch(Exception e){
                   e.printStackTrace();
                }
            }    
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private static void enlistarRemoto(File f, DataOutputStream dos) throws IOException {
    File[]listado = f.listFiles();
    int cantidadCarpetas = listado.length;
    dos.writeInt(cantidadCarpetas);
        for (int x = 0; x < listado.length; x++) {
        if (listado[x].isDirectory()) {
            dos.writeUTF(listado[x].getName());
            dos.flush();
            File f2 = listado[x];
            File[] sublistado = f2.listFiles();
            dos.writeInt(sublistado.length);
            dos.flush();
            for (int y = 0; y < sublistado.length; y++) {
                if (sublistado[y].isDirectory()) {
                    dos.writeUTF(sublistado[y].getName());
                    dos.flush();
                    enlistarRemoto(sublistado[y], dos);
                }
            }
        }
    }
}
}
