import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.swing.JFileChooser;
import java.util.List;
import java.util.ArrayList;
/*
 * @authors Carlos Moreno y Vanessa Trejo
 */
public class Servidor {
    private static File baseDir;
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
                            String instr1="A continuación se enlistará la carpta base actual";
                            dos.writeUTF(instr1);
                            dos.flush();
                            
                            //enlista la carpeta o dir actual
                            if (baseDir == null) {
                                baseDir = new File(System.getProperty("user.home"), "Documents/DocumentsRemoto");
                            }
                                String tipo = (baseDir.isDirectory())?"Carpeta":"Archivo";
                                String carpeta = "\033[32m Elegiste: "+baseDir.getAbsolutePath();
                                dos.writeUTF(carpeta);
                                dos.flush();
                                String Tipo = "Tipo: "+tipo;
                                dos.writeUTF(Tipo);
                                dos.flush();
                                File[]listado = baseDir.listFiles();
                                    String permisos="";
                                if(baseDir.canRead())
                                   permisos = permisos+"r";
                                if(baseDir.canWrite())
                                   permisos = permisos+"w";
                                if(baseDir.canExecute())
                                   permisos = permisos+"x";
                                dos.writeUTF(permisos);
                                dos.flush();
                                String Seleccionada = baseDir.getName();
                                dos.writeUTF(Seleccionada);
                                dos.flush();
                                enlistarRemoto(baseDir, dos, baseDir.getName());
                            dis.close();
                            dos.close();
                            cl.close();
                            break;
                        
                        case "rmdir": //caso 2
                            eliminarRemoto(dis,dos,cl);
                            dis.close();
                            dos.close();
                            cl.close();
                            break;    
                        case "put": //caso 3
                            recibeArchivosLocal(dis, dos, cl);
                            break;
                            
                        case "get": //caso 4
                            enviaArchivosLocal(dis, dos, cl);
                            break;
                            
                        case "cd": //caso 5
                            cambiarDirBaseRemoto(dis, dos, cl);
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
    //caso 1. enlistar remoto----------------------------------------------------------------------------
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
    }
    }
    
    //caso 2. eliminar archivos/directorios remoto----------------------------------------------------------
    private static void eliminarRemoto(DataInputStream dis, DataOutputStream dos, Socket cl) {
        try{
            //se envia la instrucción al cliente
            String instr="Seleccione la carpeta/archivo que desea eliminar. "
                    + "NOTA. Si elimina una carpeta con archivos se eliminarán también los archivos";
            dos.writeUTF(instr);
            dos.flush();
            
            //dir base
            if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Documents/DocumentsRemoto");
            }
            
            //se envia al cliente la dir del dir base actual
            String tipo = (baseDir.isDirectory())?"Carpeta":"Archivo";
            String carpeta = "\033[32m Tu directorio base actual es: "+baseDir.getAbsolutePath();
            dos.writeUTF(carpeta);
            dos.flush();
            
            //se envia tipo
            String Tipo = "Tipo: "+tipo;
            dos.writeUTF(Tipo);
            dos.flush();
            
            //se envian los permisos
            String permisos="";
            if(baseDir.canRead())
                permisos = permisos+"r";
            if(baseDir.canWrite())
                permisos = permisos+"w";
            if(baseDir.canExecute())
                permisos = permisos+"x";
            dos.writeUTF(permisos);
            dos.flush();
            
            //enlistamos de manera remota los archivos y carpetas de nivel 1
            File[]archivos = baseDir.listFiles();
            List<String> listaArchivos = new ArrayList<>(); //lista String para enviar con 'dos'
            for(File archivo: archivos){
                listaArchivos.add(archivo.getName());
            }   
            
            //se envia el listado al cliente
            for(String nombreArchivo : listaArchivos){
                dos.writeUTF(nombreArchivo);
            }
            dos.writeUTF("-");
            dos.flush();
            
            //recibe el nombre de la carpeta o archivo a borrar
            String fileBorrar = dis.readUTF();
            
            //se envía al cliente el tipo y nombre de lo que elegió borrar iterando y comparando
            for(File archivo: archivos){
                
                //si es carpeta/directorio
                if(archivo.isDirectory() && archivo.getName().equals(fileBorrar)){
                    //se envia que se eligió una carpeta 
                    dos.writeUTF("Elegiste borrar: " + fileBorrar + "de tipo: carpeta");
                    dos.flush();
                    //se envían los permisos
                    String permisosBorrar="";
                    if(archivo.canRead())
                        permisosBorrar = permisosBorrar+"r";
                    if(archivo.canWrite())
                        permisosBorrar = permisosBorrar+"w";
                    if(archivo.canExecute())
                        permisosBorrar = permisosBorrar+"x";
                    dos.writeUTF(permisosBorrar);
                    dos.flush();
                    
                    //se borra la carpeta (incluido lo que tenga adentro)
                    borrarDirectorio(archivo);
                    
                    //se envia al usuario si se borró correctamente o no
                    if(!archivo.exists()){
                       dos.writeUTF("Carpeta fue borrada correctamente");
                       dos.flush(); 
                    }else{
                        dos.writeUTF("Hubo un error al borrar la carpeta");
                        dos.flush();
                    }
                }
                //si es solo un archivo
                if(!archivo.isDirectory() && archivo.getName().equals(fileBorrar)){
                    //se envia que se eligió un archivo
                    dos.writeUTF("Elegiste borrar: " + fileBorrar + "de tipo: archivo");
                    dos.flush();
                    //se envían los permisos
                    String permisosBorrar="";
                    if(archivo.canRead())
                        permisosBorrar = permisosBorrar+"r";
                    if(archivo.canWrite())
                        permisosBorrar = permisosBorrar+"w";
                    if(archivo.canExecute())
                        permisosBorrar = permisosBorrar+"x";
                    dos.writeUTF(permisosBorrar);
                    dos.flush();
                    
                    //se borra el archivo
                    boolean bol = archivo.delete(); 
                    if (bol) {
                        //se envia al usuario si se borró correctamente o no
                        dos.writeUTF("Archivo borrado correctamente");
                        dos.flush(); 
                    } else {
                        //se envia al usuario si se borró correctamente o no
                        dos.writeUTF("Hubo un error al borrar el archivo");
                        dos.flush();
                    } 
                    
                }//if archivo   
                
            }//for     
        
        }catch (IOException e) {
            e.printStackTrace();
        }//try catch
    }//termina metodo eliminar remoto
    
    
    //caso2 de eliminar recursivo si es directorio
    public static void borrarDirectorio(File directorio) {
        //si es carpeta 
        if (directorio.isDirectory()) {
            File[] archivos = directorio.listFiles();
            if (archivos != null) {
                //borra todo el contenido antes de borrar la propia carpeta je
                for (File archivo : archivos) {
                    //si es carpeta
                    if (archivo.isDirectory()){
                        borrarDirectorio(archivo);
                    }
                    //si solo es archivo
                    if (!archivo.isDirectory()){
                        boolean archivoBorrado = archivo.delete(); 
                        if (archivoBorrado) {
                            System.out.println("El archivo fue eliminado correctamente");
                        } else {
                            System.out.println("Hubo un error al eliminar el archivo");
                        } 
                    }
                }
            }
        }
        boolean dirBorrado = directorio.delete();
        if (dirBorrado) {
            System.out.println("La carpeta/directorio fue eliminado correctamente");
        } else {
            System.out.println("Hubo un error al eliminar la carpeta");
        }
    }
    
    //caso 3.Recibir archivos/directorios de cliente(local) a servidor(remoto) --------------------------------------------------------------------------
    private static void recibeArchivosLocal(DataInputStream dis, DataOutputStream dos, Socket cl){
            try{
            ServerSocket s2 = new ServerSocket(1234);
            s2.setReuseAddress(true);
            String ruta = baseDir.getAbsolutePath();
            baseDir.setWritable(true);
            for(;;){
                Socket cl2 = s2.accept();
                String nombre = dis.readUTF();
                long tam = dis.readLong();
                System.out.println("Comienza descarga del archivo "+nombre+" de "+tam+" bytes\n\n");
                DataOutputStream dos2 = new DataOutputStream(new FileOutputStream(ruta+nombre));
                long recibidos=0;
                  int l=0, porcentaje=0;
                  while(recibidos<tam){
                      byte[] b = new byte[3500];
                      l = dis.read(b);
                      System.out.println("leidos: "+l);
                      dos2.write(b,0,l); //dos.write(b);
                      dos2.flush();
                      recibidos = recibidos + l;
                      porcentaje = (int)((recibidos*100)/tam);
                      System.out.print("\rRecibido el "+ porcentaje +" % del archivo");
                  }//while
                  System.out.println("Archivo recibido..");
                  dos2.close();
                  dis.close();
                  cl2.close();
                  cl.close();
            }
            }catch (IOException e) {
            e.printStackTrace();
        }//try catch
    }
    //caso 4. Recibir archivos/directorios de servidor(remoto)a cliente(local)______________________________________________________
    private static void enviaArchivosLocal(DataInputStream dis, DataOutputStream dos, Socket cl) throws IOException{

    }
    
    //caso 5. cambiar dir. remoto--------------------------------------------------------------------------
    private static void cambiarDirBaseRemoto(DataInputStream dis, DataOutputStream dos, Socket cl) throws IOException{
        if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Documents/DocumentsRemoto");
            }
            //se envia al cliente la dir del dir base actual
            String tipo = (baseDir.isDirectory())?"Carpeta":"Archivo";
            String carpeta = "\033[32m Tu directorio base actual remoto es: "+baseDir.getAbsolutePath();
            dos.writeUTF(carpeta);
            dos.flush();
            //se envia tipo
            String Tipo = "Tipo: "+tipo;
            dos.writeUTF(Tipo);
            dos.flush();
            //se envian los permisos
            String permisos="";
            if(baseDir.canRead())
                permisos = permisos+"r";
            if(baseDir.canWrite())
                permisos = permisos+"w";
            if(baseDir.canExecute())
                permisos = permisos+"x";
            dos.writeUTF(permisos);
            dos.flush();
            String pregunta = "Desea cambiar a otra carpeta base? (si/no)";
            dos.writeUTF(pregunta);
            dos.flush();
            String confirmacion = dis.readUTF();
            if(confirmacion.equals("si")){
                dos.writeUTF("Las carpeta actual de tu directorio base remoto y sus subcarpetas son: ");
                dos.flush();
                enlistarRemoto(baseDir, dos, baseDir.getName());
                dos.writeUTF("Escriba el nombre del directorio al que le gustaría establecer como base remoto: ");
                dos.flush();
                String nuevoBase = dis.readUTF();
                recorrerCambioDirBase(baseDir,dos, nuevoBase);
            }else if(confirmacion.equals("no")){
                dos.writeUTF("Su carpeta base sigue siendo: "+ baseDir.getName());
            }else{
                dos.writeUTF("Opción no válida");
            }
            
            
    }
    private static void recorrerCambioDirBase(File f, DataOutputStream dos, String nuevoBase) throws IOException {
    File[] listado = f.listFiles();
    boolean directorioEncontrado = false;
    if (listado != null) {
        for (int x = 0; x < listado.length; x++) {
            if (listado[x].getName().equals(nuevoBase)) {
                File nuevoDir = new File(listado[x].getAbsolutePath());
                baseDir = nuevoDir;
                directorioEncontrado = true;
                break;
            } else {
                File subDirectorio = listado[x];
                File[] subArchivos = subDirectorio.listFiles();
                for (int y = 0; y < subArchivos.length; y++) {
                    if (subArchivos[y].getName().equals(nuevoBase)) {
                        File nuevoDir2 = new File(subArchivos[y].getAbsolutePath());
                        baseDir = nuevoDir2;
                        directorioEncontrado = true;
                        break;
                    } else {
                        recorrerCambioDirBase(subArchivos[y], dos, nuevoBase);
                    }
                }
            }
        }
    }else{
        dos.writeInt(0);
        dos.flush();
    }
    
    if (directorioEncontrado) {
        dos.writeUTF("Se ha cambiado la carpeta base remota a: " + baseDir.getName());
        dos.flush();
    } else {
        dos.writeUTF("No se encontró el directorio solicitado: " + nuevoBase);
        dos.flush();
    }
}
    


    
    //caso 6. crear dir. remoto----------------------------------------------------------------------------
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
    
    //caso 7. salir de la aplicacion ja-------------------------------------------------------------------
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