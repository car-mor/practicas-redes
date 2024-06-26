import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.swing.JFileChooser;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
                            recibeArchivosDeLocal(dis, dos, cl);
                            dis.close();
                            dos.close();
                            cl.close();
                            break;
                            
                        case "get": //caso 4
                            enviaArchivosACliente(dis, dos, cl);
                            dis.close();
                            dos.close();
                            cl.close();
                            break;
                            
                        case "cd": //caso 5
                            cambiarDirBaseRemoto(dis, dos, cl);
                            dis.close();
                            dos.close();
                            cl.close();
                            break;
                            
                        case "mkdir": //caso 6
                            crearRemoto(dis,dos,cl);
                            dis.close();
                            dos.close();
                            cl.close();
                            break;
                        
                        case "quit": //caso 7
                            salirAplicacion(dis, dos, cl, s);
                            dis.close();
                            dos.close();
                            cl.close();
                            s.close();
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
    private static void recibeArchivosDeLocal(DataInputStream dis, DataOutputStream dos, Socket cl) throws IOException{
        try{
          int pto = 1235;
          ServerSocket s2 = new ServerSocket(pto);
          //ServerSocket s_datos = new ServerSocket(pto+1);
          s2.setReuseAddress(true);
          System.out.println("Servidor iniciado esperando por archivos..");
          //dir base
            if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Documents/DocumentsRemoto");
            }
          File f = new File("");
          String ruta = baseDir.getAbsolutePath();
          String carpeta="archivos";
          String ruta_archivos = ruta+"\\"+carpeta+"\\";
          System.out.println("ruta:"+ruta_archivos);
          File f2 = new File(ruta_archivos);
          f2.mkdirs();
          f2.setWritable(true);
          boolean transferenceFinished = false;
          while (!transferenceFinished) {
              Socket cl2 = s2.accept();
              System.out.println("Cliente conectado desde "+cl2.getInetAddress()+":"+cl2.getPort());
              DataInputStream dis2 = new DataInputStream(cl2.getInputStream());
              String nombre = dis.readUTF();
              long tam = dis.readLong();
              if (nombre.endsWith(".zip")) {
                // Es un archivo zip, descomprimir en la carpeta destino
                File zipFile = new File(ruta_archivos + nombre);
                DataOutputStream dos2 = new DataOutputStream(new FileOutputStream(zipFile));

                long recibidos = 0;
                int l = 0, porcentaje = 0;
                while (recibidos < tam) {
                    byte[] b = new byte[3500];
                    l = dis2.read(b);
                    dos2.write(b, 0, l);
                    dos2.flush();
                    recibidos = recibidos + l;
                    porcentaje = (int) ((recibidos * 100) / tam);
                    System.out.print("\rRecibido el " + porcentaje + " % del archivo");
                }

                System.out.println("Archivo recibido y descomprimiendo...");
                dos2.close();
                dis2.close();
                cl2.close();
                
                transferenceFinished = true;
                
                

                // Descomprimir el archivo zip
                unzipFolder(zipFile, new File(ruta_archivos));

                // Eliminar el archivo zip
                zipFile.delete();
            } else {
              System.out.println("Comienza descarga del archivo "+nombre+" de "+tam+" bytes\n\n");
              DataOutputStream dos2 = new DataOutputStream(new FileOutputStream(ruta_archivos+nombre));
              long recibidos=0;
              int l=0, porcentaje=0;
              while(recibidos<tam){
                  byte[] b = new byte[3500];
                  l = dis2.read(b);
                  System.out.println("leidos: "+l);
                  dos2.write(b,0,l); //dos.write(b);
                  dos2.flush();
                  recibidos = recibidos + l;
                  porcentaje = (int)((recibidos*100)/tam);
                  System.out.print("\rRecibido el "+ porcentaje +" % del archivo");
              }//while
              System.out.println("Archivo recibido..");
              dos2.close();
              dis2.close();
              cl2.close();
              
              transferenceFinished = true;
              }
          }//for
          s2.close();
      }catch(Exception e){
          e.printStackTrace();
      }  
    }

   public static void unzipFolder(File zipFile, File destFolder) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipFile);
             org.apache.commons.compress.archivers.zip.ZipArchiveInputStream zis = new org.apache.commons.compress.archivers.zip.ZipArchiveInputStream(fis)) {
            org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (entry.isDirectory()) {
                    new File(destFolder, entry.getName()).mkdirs();
                } else {
                    File file = new File(destFolder, entry.getName());
                    file.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        IOUtils.copy(zis, fos);
                    }
                }
            }
        }
    }
 
    
//caso 4. Enviar archivos/directorios de servidor(remoto)a cliente(local)______________________________________________________
private static void enviaArchivosACliente(DataInputStream dis, DataOutputStream dos, Socket cl) {
    try {
        
        //dir base
            if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Documents/DocumentsRemoto");
            }
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
            dos.flush();  //enlistar
            //Recibir nombre de carpeta o archivo
            String nombre = dis.readUTF();
            System.out.println("Servidor recibió solicitud de envío de: " + nombre);
          
          //Obtiene direccion o ruta de carpeta base
          String ruta_archivos = baseDir.getAbsolutePath();
        
        // Verificar si es una carpeta o un archivo
        File file = new File(ruta_archivos+"\\"+nombre);
        if (file.isDirectory()) {
            // Es una carpeta, comprimir y enviar
            File zipFile = new File(ruta_archivos + file.getName() + ".zip");
            zipFolder(file, zipFile);
            long tam = zipFile.length();
            dos.writeUTF(zipFile.getName());
            dos.writeLong(tam);

            // Crear un socket secundario para enviar los datos
            int pto = 1235;
            ServerSocket s2 = new ServerSocket(pto);
            s2.setReuseAddress(true);
            Socket cl2 = s2.accept();
            DataOutputStream dos2 = new DataOutputStream(cl2.getOutputStream());

            try (FileInputStream fis = new FileInputStream(zipFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                long enviados = 0;
                int l = 0, porcentaje = 0;
                while (enviados < tam) {
                    byte[] b = new byte[3500];
                    l = bis.read(b);
                    dos2.write(b, 0, l);
                    dos2.flush();
                    enviados = enviados + l;
                    porcentaje = (int) ((enviados * 100) / tam);
                    System.out.print("\rEnviado el " + porcentaje + " % del archivo");
                }
                System.out.println("\nArchivo enviado.");
            }
            zipFile.delete();
            dis.close();
            dos.close();
            cl2.close();
            s2.close();
            cl.close();
        } else {
            // Es un archivo, enviar
            long tam = file.length();
            dos.writeUTF(file.getName());
            dos.writeLong(tam);

            // Crear un socket secundario para enviar los datos
            int pto = 1235;
            ServerSocket s2 = new ServerSocket(pto);
            s2.setReuseAddress(true);
            Socket cl2 = s2.accept();
            DataOutputStream dos2 = new DataOutputStream(cl2.getOutputStream());

            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                long enviados = 0;
                int l = 0, porcentaje = 0;
                while (enviados < tam) {
                    byte[] b = new byte[3500];
                    l = bis.read(b);
                    dos2.write(b, 0, l);
                    dos2.flush();
                    enviados = enviados + l;
                    porcentaje = (int) ((enviados * 100) / tam);
                    System.out.print("\rEnviado el " + porcentaje + " % del archivo");
                }
                System.out.println("\nArchivo enviado.");
            }
            dis.close();
            dos.close();
            cl2.close();
            s2.close();
            cl.close();
        }
            dis.close();
            dos.close();
            cl.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

public static void zipFolder(File folder, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos)) {
            compressDirectory(folder, "", zos);
        }
    }

private static void compressDirectory(File folder, String basePath, ZipArchiveOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String dirPath = basePath + file.getName() + "/";
                zos.putArchiveEntry(new ZipArchiveEntry(dirPath));
                compressDirectory(file, dirPath, zos);
                zos.closeArchiveEntry();
            } else {
                String filePath = basePath + file.getName();
                zos.putArchiveEntry(new ZipArchiveEntry(filePath));
                try (FileInputStream fis = new FileInputStream(file)) {
                    IOUtils.copy(fis, zos);
                }
                zos.closeArchiveEntry();
            }
        }
    }
    
    
    //caso 5. cambiar dir. remoto--------------------------------------------------------------------------
    private static void cambiarDirBaseRemoto(DataInputStream dis, DataOutputStream dos, Socket cl) throws IOException{
        //si aun no se definía el dir base
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
            
            //envia pregunta de confirmacion
            String pregunta = "Desea cambiar a otra carpeta base? (si/no)";
            dos.writeUTF(pregunta);
            dos.flush();
            
            //recibe confirmacion
            String confirmacion = dis.readUTF();
            
            //codigo para procesar confirmacion
            if(confirmacion.equals("si")){
                //manda a enlistar
                //enlistamos de manera remota los archivos y carpetas de nivel 1
                File[]archivos = baseDir.listFiles();
                List<String> listaArchivos = new ArrayList<>(); //lista String para enviar con 'dos'
                for(File archivo: archivos){
                    if(archivo.isDirectory()){
                        listaArchivos.add(archivo.getName());
                    }
            }   
            
            //se envia el listado al cliente
            for(String nombreArchivo : listaArchivos){
                dos.writeUTF(nombreArchivo);
            }
            dos.writeUTF("-");
            dos.flush();
                
                //envia instruccion
                dos.writeUTF("Escriba el nombre del directorio al que le gustaría establecer como base remoto: ");
                dos.flush();
                
                //recibe el nombre de la carpeta a la que se cambiará
                String nuevoBase = dis.readUTF();
                
                //llama a la funcion para el cambio
                recorrerCambioDirBase(baseDir,dos, nuevoBase);
                
            }else if(confirmacion.equals("no")){
                dos.writeUTF("Su carpeta base sigue siendo: "+ baseDir.getName());
            }else{
                dos.writeUTF("Opción no válida");
            }
             
    }
    private static void recorrerCambioDirBase(File f, DataOutputStream dos, String nuevoBase) throws IOException {
    //enlistamos de manera remota los archivos y carpetas de nivel 1 para comparar
    File[]archivos = baseDir.listFiles();
    List<String> listaArchivos = new ArrayList<>(); //lista String para enviar con 'dos'
    for(File archivo: archivos){
        listaArchivos.add(archivo.getName());
    }
    
    //buscamos el match de nombres
    for(File archivo: archivos){       
        //si es carpeta/directorio
        if(archivo.isDirectory() && archivo.getName().equals(nuevoBase)){
                    
            // Crea una referencia al nuevo directorio
            File nuevoDirectorio = new File(baseDir, nuevoBase);
            baseDir = nuevoDirectorio;
    
            //se envia al usuario si se borró correctamente o no
            if(nuevoDirectorio.exists()){
                dos.writeUTF("Se cambio de directorio correctamente");
                dos.flush(); 
            }else{
                dos.writeUTF("Hubo un error al cambiar de directorio");
                dos.flush();
            }
        }//if
               
    }//for  
    
}//ends
    


    
    //caso 6. crear dir. remoto----------------------------------------------------------------------------
    private static void crearRemoto(DataInputStream dis, DataOutputStream dos, Socket cl) {
        try{   
            //se envia la instrucción al cliente
            String instrMKDIR="La nueva carpeta se creará en el directorio base actual";
            dos.writeUTF(instrMKDIR);
            dos.flush();
            
            //directorio base                
            if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Documents/DocumentsRemoto");
            }
            String tipo = (baseDir.isDirectory())?"Carpeta":"Archivo";
            
            //se envia al cliente la direccion de la carpeta base actual
            String carpeta = "\033[32m Directorio base actual: "+baseDir.getAbsolutePath();
            dos.writeUTF(carpeta);
            dos.flush();
            
            //se envía al cliente tipo
            String Tipo = "Tipo: "+tipo;
            dos.writeUTF(Tipo);
            dos.flush();
                                
            //se envian permisos
            //File[]listado = baseDir.listFiles();
            String permisos="";
            if(baseDir.canRead())
                permisos = permisos+"r";
            if(baseDir.canWrite())
                permisos = permisos+"w";
            if(baseDir.canExecute())
                permisos = permisos+"x";
            dos.writeUTF(permisos);
            dos.flush();
            
            //recibe el nombre de la nueva carpeta
            String nuevoFile = dis.readUTF();
            
            //crea la nueva carpeta
            String path = baseDir.getAbsolutePath()+"\\"+nuevoFile;  
            File f1 = new File(path);
            boolean bool = f1.mkdir(); 
            f1.setWritable(true); //le damos permisos de escritura o habilitamos
            f1.setReadable(true); // habilita los permisos de lectura
                                
            //se envía al cliente si la carpeta fue creada correctamente o no
            if(bool){  
                String instr3MKDIR="El directorio se creó satisfactoriamente ";
                dos.writeUTF(instr3MKDIR);
                dos.flush(); 
            }else{  
                String instr4MKDIR="Hubo un error al crear el directorio";
                dos.writeUTF(instr4MKDIR);
                dos.flush();  
            }  
        }catch (IOException e) {
            e.printStackTrace();
        }//try catch
    }//termina metodo crear remoto
    
    //caso 7. salir de la aplicacion ja-------------------------------------------------------------------
    private static void salirAplicacion(DataInputStream dis, DataOutputStream dos, Socket cl, ServerSocket s) {
        try{
            //se envia la instrucción al cliente
            String instr="Cerrando flujos de salida, entrada y socket server...";
            dos.writeUTF(instr);
            dos.flush();
            s.close();
            dis.close();
            dos.close();
            cl.close();
            System.out.println("Cliente desconectado desde-> "+cl.getInetAddress()+":"+cl.getPort());
        }catch (IOException e) {
            e.printStackTrace();
        
    }
    }//termina metodo salir app
      
}//servidor