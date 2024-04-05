import java.io.*;
import java.net.*;
import java.util.Scanner;
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
            System.out.println("Esperando conexiones del cliente... ");
            for(;;){ //while(true)
                Socket cl = s.accept(); //en cada interacción aceptamos cliente, y devuelve referencia de tipo socket.
                System.out.println("Cliente conectado desde-> "+cl.getInetAddress()+":"+cl.getPort());//registro de quien fue ultimo que se conecto
                try{
                    DataInputStream dis = new DataInputStream(cl.getInputStream());
                    String directiva = dis.readUTF();
                    DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                    
                    //empieza switch
                    switch(directiva){
                        
                        case "list": //caso 1
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
                                enlistarRemoto(f, dos, f.getName());
                               }//if
                            }
                            dis.close();
                            dos.close();
                            cl.close();
                            break;
                        
                        case "rmdir": //caso 2
                            eliminarRemoto(dis,dos,cl);   
                            break;    
                            
                        case "mkdir": //caso 6
                            crearRemoto(dis,dos,cl);   
                            break;
                        
                        case "quit": //caso 7
                            salirAplicacion(dis, dos, cl);
                            //s.close();
                            break;
                    }          
                }catch(Exception e){
                   e.printStackTrace();
                }
            }    
        }catch(Exception e){
            e.printStackTrace();
        }
    }//main
    
//********************************************** EMPIEZAN MÉTODOS ******************************
    //caso 1. enlistar remoto
    private static void enlistarRemoto(File f, DataOutputStream dos, String ruta) throws IOException {
    dos.writeUTF(ruta);
    dos.flush();

    File[] listado = f.listFiles();
    if (listado != null) {
        dos.writeInt(listado.length);
        dos.flush();

        for (File subdir : listado) {
            if (subdir.isDirectory()) {
                enlistarRemoto(subdir, dos, ruta + "/" + subdir.getName());
            }
        }
    } else {
        dos.writeInt(0);
        dos.flush();
    }
    }
    
    //caso 2. eliminar archivos/directorios remoto
    private static void eliminarRemoto(DataInputStream dis, DataOutputStream dos, Socket cl) {
        try{
            //se envia la instrucción al cliente
            String instr="Seleccione la carpeta/archivo que desea eliminar. "
                    + "NOTA. Si elimina una carpeta con archivos se eliminarán también los archivos";
            dos.writeUTF(instr);
            dos.flush();
            
        }catch (IOException e) {
            e.printStackTrace();
        }//try catch
    }//termina metodo eliminar remoto
    
    //caso 6. crear dir. remoto
    private static void crearRemoto(DataInputStream dis, DataOutputStream dos, Socket cl) {
        try{   
            //se envia la instrucción al cliente
            String instrMKDIR="Seleccione la carpeta de donde desea crear el directorio";
            dos.writeUTF(instrMKDIR);
            dos.flush();
                            
            //se selecciona la carpeta remota
            JFileChooser jfMKDIR = new JFileChooser();
            File dirMKDIR = new File("d:\\Documentos\\");
            jfMKDIR.setCurrentDirectory(dirMKDIR);
            jfMKDIR.setRequestFocusEnabled(true);
            jfMKDIR.requestFocus();
            jfMKDIR.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int rMKDIR = jfMKDIR.showDialog(null, "Elegir");
            if(rMKDIR==JFileChooser.APPROVE_OPTION){
                File fMKDIR = jfMKDIR.getSelectedFile();
                String tipoMKDIR = (fMKDIR.isDirectory())?"Carpeta":"Archivo";
                String carpetaMKDIR = "\033[32m Elegiste: "+fMKDIR.getAbsolutePath();
                                
                //envia carpeta remota absolutePath
                dos.writeUTF(carpetaMKDIR);
                dos.flush();
                String TipoMKDIR = "Tipo: "+tipoMKDIR;
                                
                //envia tipo
                dos.writeUTF(TipoMKDIR);
                dos.flush();
                                
                //envía permisos
                if(tipoMKDIR.compareTo("Carpeta")==0){
                    File[]listadoMKDIR = fMKDIR.listFiles();
                    String permisosMKDIR="";
                    if(fMKDIR.canRead())
                        permisosMKDIR = permisosMKDIR+"r";
                    if(fMKDIR.canWrite())
                        permisosMKDIR = permisosMKDIR+"w";
                    if(fMKDIR.canExecute())
                        permisosMKDIR = permisosMKDIR+"x";
                    dos.writeUTF(permisosMKDIR);
                    dos.flush();
                                
                    //envia la segunda instruccion
                    String instr2MKDIR="Ingresa el nombre del nuevo directorio: ";
                    dos.writeUTF(instr2MKDIR);
                    dos.flush();
                                
                    //recibe el nombre de la carpeta
                    String entradaMKDIR = dis.readUTF();
                                
                    //se crea la nueva carpeta remota
                    String pathMKDIR = fMKDIR.getAbsolutePath() + "\\" + entradaMKDIR;
                    File f1MKDIR = new File(pathMKDIR);
                    boolean boolMKDIR = f1MKDIR.mkdirs();  //crea la carpeta
                    f1MKDIR.setWritable(true); //le damos permisos de escritura o habilitamos
                    f1MKDIR.setReadable(true); // habilita los permisos de lectura
                                
                    //se envía al cliente si la carpeta fue creada correctamente o no
                    if(boolMKDIR){  
                        String instr3MKDIR="El directorio se creó satisfactoriamente ";
                        dos.writeUTF(instr3MKDIR);
                        dos.flush();  
                    }else{ 
                        String instr4MKDIR="Hubo un error al crear el directorio";
                        dos.writeUTF(instr4MKDIR);
                        dos.flush();
                    } //if
                }//if
            }//if
        }catch (IOException e) {
            e.printStackTrace();
        }//try catch
    }//termina metodo crear remoto
    
    //caso 7. salir de la aplicacion ja
    private static void salirAplicacion(DataInputStream dis, DataOutputStream dos, Socket cl) {
        try{
            //se envia la instrucción al cliente
            String instr="Cerrando flujos de salida, entrada y socket server...";
            dos.writeUTF(instr);
            dos.flush();
            dis.close();
            dos.close();
            cl.close();
            System.out.println("Cliente desconectado desde-> "+cl.getInetAddress()+":"+cl.getPort());
        }catch (IOException e) {
            e.printStackTrace();
        
    }
    }//termina metodo salir app
      
}//servidor