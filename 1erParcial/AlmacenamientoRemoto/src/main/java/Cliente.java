import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.swing.JFileChooser;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.*;
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
public class Cliente {
    private static File baseDir;
    public static void main(String[] args) {
        try{
            
        while(true){
            
            //segun yo va adentro del while, sino no se cierra bien en la opcion 7
            Socket cl = new Socket("127.0.0.1", 1234); //crear instancia de socket especifacndo direccion IP del socket al que nos conectaremos
            //Socket cl2 = new Socket("127.0.0.1", 1234); //segundo socket para enviar bytes de archivos
            System.out.println("Conexion establecida..seleccione alguna de las siguientes opciones:");
            //************************************************************************
           
            int opc = 0;
            System.out.println("1. Listado de directorios (local/remoto) mediante la primitiva “list”.");
            System.out.println("2. Borrado de archivos/carpetas (local/remoto) mediante la primitiva “rmdir”.");
            System.out.println("3. Envío de archivos/carpetas desde el sistema de archivos local hacia el remoto (primitiva put)");
            System.out.println("4. Envío de archivos/carpetas desde el sistema de archivos remoto hacia el local (primitiva get)");
            System.out.println("5. Cambio de carpeta base (local/remoto) mediante primitiva “cd”");
            System.out.println("6. Creación de carpetas (local/remoto) mediante primitiva “mkdir”");
            System.out.println("7. Salir de la aplicación (primitiva “quit”)");
            Scanner scanner = new Scanner(System.in);
            System.out.println("Ingrese una opción: ");
            opc = scanner.nextInt();
            switch(opc){
                case 1:
                    listarDirectorios(cl);//ya se corrigió el remoto->listo
                    break;
                case 2:
                    borrarCarpetas(cl); //ya se corrigió el remoto->listo
                    break;
                case 3:
                    enviaArchivosAServidor(cl);
                    break;
                case 4:
                    
                    break;
                case 5:
                    cambioCarpetaBase(cl);//falta remoto 
                    break;
                case 6:
                    crearDirectorios(cl);//ya se corrigió el remoto->listo 
                    break;
                case 7:
                    salirAplicacion(cl);//listo
                    return;
            }}
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    //caso 1. enlistar local y remoto______________________________________________________________________
     public static void listarDirectorios(Socket cl) {
         var op = "";
         System.out.println("¿Desde listar los directorios de manera local o remota?");
         Scanner scanner = new Scanner(System.in);
            System.out.println("Escriba una l(local) o una r(remota) para elegir una opción: ");
             op = scanner.next();
             if(op.equals("l")){  
                System.out.println("Seleccione la carpeta de donde desea enlistar los directorios");
                if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Desktop");
                }
                JFileChooser jf = new JFileChooser();
                jf.setCurrentDirectory(baseDir);
                jf.setRequestFocusEnabled(true);
                jf.requestFocus();
                jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int r = jf.showDialog(null, "Elegir");
                if(r==JFileChooser.APPROVE_OPTION){
                    File f = jf.getSelectedFile();
                    String tipo = (f.isDirectory())?"Carpeta":"Archivo";
                    System.out.println("\033[32m Elegiste: "+f.getAbsolutePath());
                    System.out.println("Tipo: "+tipo);
                    if(tipo.compareTo("Carpeta")==0){
                        File[]listado = f.listFiles();
                         String permisos="";
                    if(f.canRead())
                        permisos = permisos+"r";
                    if(f.canWrite())
                        permisos = permisos+"w";
                    if(f.canExecute())
                        permisos = permisos+"x";
                    System.out.println("Permisos:"+permisos);
                        System.out.println("Contenido:");
                         listarSubdirectoriosLocal(f, 1);
                    }//if
                    try {
                            cl.close();
                    } catch (IOException e) {
                            e.printStackTrace();
                    }
                 }
             }else if(op.equals("r")){
                 try{
                     String directiva = "list";
                     DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                     dos.writeUTF(directiva);
                     dos.flush();
                     
                     DataInputStream dis = new DataInputStream(cl.getInputStream()); 
                     String instr1 = dis.readUTF();
                     System.out.println(instr1);
                     String carpeta = dis.readUTF();
                     System.out.println(carpeta);
                     String tipo = dis.readUTF();
                     System.out.println(tipo);
                     String permisos = dis.readUTF();
                     System.out.println("Permisos: "+permisos);
                     System.out.println("Carpetas enlistadas desde el servidor de su carpeta: ");
                     String carpetaSeleccionada = dis.readUTF();
                     System.out.println("Carpeta seleccionada: " + carpetaSeleccionada);
                     enlistarSubcarpetas(dis);
                     dis.close();
                     dos.close();
                     cl.close();
                 }catch(Exception e){
                   e.printStackTrace();
                 }
                 
             }else{
                 System.out.println("Opción no válida");
                 
             }           
}
     //parte del caso 1 para enlistar remoto
    private static void enlistarSubcarpetas(DataInputStream dis) throws IOException {
    String ruta = dis.readUTF();
    String[] partes = ruta.split("/");
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < partes.length; i++) {
        sb.append("|__");
    }
    System.out.println(sb.toString() + partes[partes.length - 1]);

    int cantidadSubcarpetas = dis.readInt();

    for (int i = 0; i < cantidadSubcarpetas; i++) {
        enlistarSubcarpetas(dis);
    }
}
     //parte del caso 1 para enlistar local
    private static void listarSubdirectoriosLocal(File directorio, int nivel) {
    File[] archivos = directorio.listFiles();
    if (archivos != null) {
        for (int x = 0; x < archivos.length; x++) {
            if (archivos[x].isDirectory()) {
                for (int j=0;j<nivel;j++){
                System.out.print("\t|__");
                }
                System.out.println(archivos[x].getName());
                File subDirectorio = archivos[x];
                File[] subArchivos = subDirectorio.listFiles();
                    for (int y = 0; y < subArchivos.length; y++) {
                        if(subArchivos[y].isDirectory()){
                            for (int k = 0; k < nivel; k++) {
                            System.out.print("\t|__");
                            }
                            System.out.println("\t"+subArchivos[y].getName());
                            listarSubdirectoriosLocal(subArchivos[y], nivel+1);
                        }
                    }
            }
        }
    }
}
    
    
    //caso 2. borrar archivos/carpetas local y remoto__________________________________________________________
    public static void borrarCarpetas(Socket cl) {
        var op = "";
        System.out.println("¿Desea borrar archivos/carpetas de manera local o remota?");
        Scanner scanner = new Scanner(System.in);
        System.out.println("Escriba una l(local) o una r(remota) para elegir una opción: ");
        op = scanner.next();
        
        //borrar de forma local archivos/carpetas
        if(op.equals("l")){
            System.out.println("Seleccione la carpeta o el archivo que desea eliminar");
            System.out.println("NOTA. Si elimina una carpeta con archivos se eliminarán también los archivos");
            
            //usuario selecciona la carpeta o archivo en documentos local para borrar
            if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Desktop");
                }
            JFileChooser jf = new JFileChooser();
            jf.setCurrentDirectory(baseDir);
            jf.setRequestFocusEnabled(true);
            jf.requestFocus();
            jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int r = jf.showDialog(null, "Elegir");
            if(r==JFileChooser.APPROVE_OPTION){
                File f = jf.getSelectedFile();
                String tipo = (f.isDirectory())?"Carpeta":"Archivo";
                System.out.println("\033[32m Elegiste: "+f.getAbsolutePath());
                System.out.println("Tipo: "+tipo);
                
                //si es carpeta
                if(tipo.compareTo("Carpeta")==0){
                    File[]listado = f.listFiles();
                    String permisos="";
                    if(f.canRead())
                        permisos = permisos+"r";
                    if(f.canWrite())
                        permisos = permisos+"w";
                    if(f.canExecute())
                        permisos = permisos+"x";
                    System.out.println("Permisos:"+permisos);
                    //se borra la carpeta y su contenido
                    borrarDirectorio(f);
                //si es archivo     
                } else {
                    //se borra el archivo (no es carpeta)
                    borrarArchivo(f);
                }
            }//if
            
        //borrar de forma remota archivos/carpetas
        }else if(op.equals("r")){
            try{
                //envia la directiva rmdir
                String directiva = "rmdir";
                DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                dos.writeUTF(directiva);
                dos.flush();
                
                //recibe la instruccion
                DataInputStream dis = new DataInputStream(cl.getInputStream());
                String instr = dis.readUTF();
                System.out.println(instr);
                
                //recibe ubicacion del dir actual
                String instr2 = dis.readUTF();
                System.out.println(instr2);
                
                //recibe tipo del dir actual
                String tipo = dis.readUTF();
                System.out.println(tipo);
                
                //recibe permisos
                String permisos = dis.readUTF();
                System.out.println(permisos);
                
                //recibe archivos
                List<String> listaArchivos = new ArrayList<>();
                while(true){
                    String nombreArchivo = dis.readUTF();
                    if(nombreArchivo.equals("-")){
                        break;
                    }
                    listaArchivos.add(nombreArchivo);
                }
                System.out.println("Los archivos y dir. remotos actuales del primer nivel de la carpeta son:");
                for(String nombreArchivo: listaArchivos){
                    System.out.println(nombreArchivo);
                }
                
                //se envia el nombre de la carpeta o archivo a borrar
                var fileBorrar = "";
                System.out.println("Ingrese el nombre completo de la carpeta o archivo que desea borrar ");
                Scanner scanner2 = new Scanner(System.in);
                fileBorrar = scanner2.nextLine();
                dos.writeUTF(fileBorrar);
                dos.flush();
                
                //recibe el tipo
                String tipoBorrar = dis.readUTF();
                System.out.println(tipoBorrar);
                
                //recibe permisos
                String permisosBorrar = dis.readUTF();
                System.out.println(permisosBorrar);
                
                //recibe si se borró correctamente o no
                String resultadoBorrar = dis.readUTF();
                System.out.println(resultadoBorrar);
                
                //se cierra
                dis.close();
                dos.close();
                cl.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
  //parte del caso 2. borrar dir recursivo forma local 
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
    
    //parte del caso 2. borrar solo archivos forma local
    public static void borrarArchivo(File directorio) {
        boolean archivoBorrado = directorio.delete(); 
        if (archivoBorrado) {
            System.out.println("El archivo fue eliminado correctamente");
        } else {
            System.out.println("Hubo un error al eliminar el archivo");
        }   
    }
    
//caso 3. Enviar archivos/directorios de cliente(local) a servidor(remoto)______________________________________________________
    private static void enviaArchivosAServidor(Socket cl){
    try{
            //envio directiva socket principal
             String directiva = "put";
                DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                dos.writeUTF(directiva);
                dos.flush();
            //Flujo de lectura socket principal
                DataInputStream dis = new DataInputStream(cl.getInputStream());
            //dir base
            if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Desktop");
            }    
            int pto = 1235;
            String dir = "127.0.0.2";
            Socket cl2 = new Socket(dir,pto);
            System.out.println("Conexion con servidor establecida para envio de archivos.. lanzando FileChooser..");
            JFileChooser jf = new JFileChooser();
            jf.setCurrentDirectory(baseDir);
            jf.setRequestFocusEnabled(true);
            jf.requestFocus();
            jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int r = jf.showDialog(null, "Enviar Archivo");
            if(r==JFileChooser.APPROVE_OPTION){
                File f = jf.getSelectedFile();
                if(f.isFile()){
                String nombre = f.getName();
                String path = f.getAbsolutePath();
                long tam = f.length();
                System.out.println("Preparandose pare enviar archivo "+path+" de "+tam+" bytes\n\n");
                DataOutputStream dos2 = new DataOutputStream(cl2.getOutputStream());
                DataInputStream dis2 = new DataInputStream(new FileInputStream(path));
                dos.writeUTF(nombre);
                dos.flush();
                dos.writeLong(tam);
                dos.flush();
                long enviados = 0;
                int l=0,porcentaje=0;
                while(enviados<tam){
                    byte[] b = new byte[3500];
                    l=dis2.read(b);
                    System.out.println("enviados: "+l);
                    dos2.write(b,0,l);// dos.write(b);
                    dos2.flush();
                    enviados = enviados + l;
                    porcentaje = (int)((enviados*100)/tam);
                    System.out.print("\rEnviado el "+porcentaje+" % del archivo");
                }//while
                System.out.println("\nArchivo enviado..");
                dis2.close();
                dos2.close();
                cl2.close();
                dis.close();
                dos.close();
                cl.close();
                }else if(f.isDirectory()){
                            // Es una carpeta, proceder a comprimir y enviar
                File folder = jf.getSelectedFile();
                File zipFile = new File(folder.getAbsolutePath() + ".zip");

                // Comprimir la carpeta en el archivo zip
                try (FileOutputStream fos = new FileOutputStream(zipFile);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    compressDirectoryToZipStream(folder, "", zos);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                long tam = zipFile.length();

                // Enviar el archivo zip al servidor
                DataOutputStream dos2 = new DataOutputStream(cl2.getOutputStream());
                DataInputStream dis2 = new DataInputStream(new FileInputStream(zipFile.getAbsolutePath()));
                dos.writeUTF(zipFile.getName());
                dos.flush();
                dos.writeLong(tam);
                dos.flush();

                long enviados = 0;
                int l = 0, porcentaje = 0;
                while (enviados < tam) {
                    byte[] b = new byte[3500];
                    l = dis2.read(b);
                    dos2.write(b, 0, l);
                    dos2.flush();
                    enviados = enviados + l;
                    porcentaje = (int) ((enviados * 100) / tam);
                    System.out.print("\rEnviado el " + porcentaje + " % del archivo");
                }

                // Cerrar los flujos y sockets
                dis2.close();
                dos2.close();
                cl2.close();
                dis.close();
                dos.close();
                cl.close();
                }
            }//if
        }catch(Exception e){
            e.printStackTrace();
        }//catch
    }
    
    private static void compressDirectoryToZipStream(File folder, String basePath, ZipOutputStream zos) throws IOException {
    File[] files = folder.listFiles();
    for (File file : files) {
        if (file.isDirectory()) {
            compressDirectoryToZipStream(file, basePath + file.getName() + "/", zos);
        } else {
            String filePath = basePath + file.getName();
            zos.putNextEntry(new ZipEntry(filePath));
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
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
    
    //caso 4. Recibir archivos/directorios de servidor(remoto)a cliente(local)
    private static void recibeArchivosDeRemoto(Socket cl){
        try{
    DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
    System.out.println("Seleccione el archivo que desea mandar al servidor");
                if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Desktop");
                }
                JFileChooser jf = new JFileChooser();
                jf.setCurrentDirectory(baseDir);
                jf.setRequestFocusEnabled(true);
                jf.requestFocus();
                jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int r = jf.showDialog(null, "Elegir");
                if(r==JFileChooser.APPROVE_OPTION){
                    File f = jf.getSelectedFile();
                    String tipo = (f.isDirectory())?"Carpeta":"Archivo";
                    String nombre = f.getName();
                    System.out.println("Elegiste: "+nombre);
                    String ruta = f.getAbsolutePath();
                    System.out.println("\033[32m Ruta: "+ruta);
                    DataInputStream dis = new DataInputStream(new FileInputStream(ruta));
                    System.out.println("Tipo: "+tipo);
                    String permisos = "";
                     if(f.canRead())
                        permisos = permisos+"r";
                    if(f.canWrite())
                        permisos = permisos+"w";
                    if(f.canExecute())
                        permisos = permisos+"x";
                    System.out.println("Permisos:"+permisos);
                    long tam = f.length();
                    System.out.println("Preparandose pare enviar archivo "+nombre+" de "+tam+" bytes\n\n");
                    dos.writeUTF(nombre);
                    dos.flush();
                    dos.writeLong(tam);
                    dos.flush();
                     long enviados = 0;
                int l=0,porcentaje=0;
                while(enviados<tam){
                    byte[] b = new byte[3500];
                    l=dis.read(b);
                    System.out.println("enviados: "+l);
                    dos.write(b,0,l);// dos.write(b);
                    dos.flush();
                    enviados = enviados + l;
                    porcentaje = (int)((enviados*100)/tam);
                    System.out.print("\rEnviado el "+porcentaje+" % del archivo");
                }//while
                System.out.println("\nArchivo enviado..");
                dis.close();
                dos.close();
                cl.close();
     }
   
}catch(Exception e){
            e.printStackTrace();
        }//catch
}
      

    
//caso 5. cambiar de carpeta base de manera local/remota______________________________________________________
    private static void cambioCarpetaBase(Socket cl){
        var op = "";
        
         System.out.println("¿Desde cambiar de directorio de manera local o remota?");
         Scanner scanner = new Scanner(System.in);
            System.out.println("Escriba una l(local) o una r(remota) para elegir una opción: ");
             op = scanner.next();
             if(op.equals("l")){
                String userHome = System.getProperty("user.home");
                File dir = new File(userHome, "Desktop");
                System.out.println("Actualmente su carpeta base local es: "+dir);
                System.out.println("Ruta: "+dir.getAbsolutePath());
                String tipo = (dir.isDirectory())?"Carpeta":"Archivo";
                System.out.println("Tipo: "+tipo);
                if(tipo.compareTo("Carpeta")==0){
                         String permisos="";
                    if(dir.canRead())
                        permisos = permisos+"r";
                    if(dir.canWrite())
                        permisos = permisos+"w";
                    if(dir.canExecute())
                        permisos = permisos+"x";
                    System.out.println("Permisos:"+permisos);
                }
                 System.out.println("Desea cambiar a otra carpeta base? (si/no)");
                 var op2 ="";
                 Scanner scanner2 = new Scanner(System.in);
                 op2 = scanner2.nextLine();
                 if(op2.equals("si")){
                     baseDir = cambioBaseFileChooser(dir);
                    System.out.println("Ha cambiado la carpeta base local de "+dir.getName()+" a "+baseDir.getName());
                    System.out.println("Ruta: "+baseDir.getAbsolutePath());
                    String tipo2 = (baseDir.isDirectory())?"Carpeta":"Archivo";
                    System.out.println("Tipo: "+tipo2);
                    if(tipo.compareTo("Carpeta")==0){
                         String permisos="";
                    if(baseDir.canRead())
                        permisos = permisos+"r";
                    if(baseDir.canWrite())
                        permisos = permisos+"w";
                    if(baseDir.canExecute())
                        permisos = permisos+"x";
                    System.out.println("Permisos:"+permisos);
                }
                 }else if(op2.equals("no")){
                     System.out.println("Su carpeta base sigue siendo: "+ dir.getName());
                 }else{
                     System.out.println("Opción no valida");
                 }
                
               //forma remota     
            }else if(op.equals("r")){
                 try{
                     //envia directiva cd
                     String directiva = "cd";
                     DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                     dos.writeUTF(directiva);
                     dos.flush();
                     
                     //recibe ubicacion del dir actual
                    DataInputStream dis = new DataInputStream(cl.getInputStream());
                    String dirBaseRemoto = dis.readUTF();
                    System.out.println(dirBaseRemoto);

                    //recibe tipo del dir actual
                    String tipo = dis.readUTF();
                    System.out.println(tipo);

                    //recibe permisos del dir actual
                    String permisos = dis.readUTF();
                    System.out.println(permisos);
                    
                    //recibe la pregunta para confirmar cambiar de dir
                    String pregunta = dis.readUTF();
                    System.out.println(pregunta);
                    
                    //envia respuesta de confirmacion
                    var op2 ="";
                    Scanner scanner2 = new Scanner(System.in);
                    op2 = scanner2.nextLine();
                    dos.writeUTF(op2);
                    dos.flush();
                    
                    //codigo para la confirmacion
                    if(op2.equals("si")){
                        System.out.println("Las carpeta actual de tu directorio base remoto y sus subcarpetas son: ");
                        
                        //recibe listado de la carpeta base actual y lo muestra en pantalla
                        //recibe archivos
                        List<String> listaArchivos = new ArrayList<>();
                        while(true){
                            String nombreArchivo = dis.readUTF();
                            if(nombreArchivo.equals("-")){
                                break;
                            }
                            listaArchivos.add(nombreArchivo);
                        }
                        System.out.println("Los archivos y dir. remotos actuales del primer nivel de la carpeta son:");
                        for(String nombreArchivo: listaArchivos){
                        System.out.println(nombreArchivo);
                        }
                        
                        //recibe instruccion
                        String instr = dis.readUTF();
                        System.out.println(instr);
                        
                        //envia el nombre de la nueva carpeta
                        var nuevoDirBase = "";
                        nuevoDirBase = scanner2.nextLine();
                        dos.writeUTF(nuevoDirBase);
                        dos.flush();
                        
//                        int sinDirectorios = dis.readInt();
//                        if(sinDirectorios==0){
//                            System.out.println("No tiene directorios la carpeta base remota");
//                        }
                        //recibe confirmacion
                        String confirmacion = dis.readUTF();
                        System.out.println(confirmacion);
                        
                        dis.close();
                        dos.close();
                        cl.close();
                    }else if(op2.equals("no")){
                       dos.writeUTF("no");
                       dos.flush();
                       String cancelacion = dis.readUTF();
                       System.out.println(cancelacion);
                       dis.close();
                       dos.close();
                       cl.close();
                    }else{
                        String incorrecto = dis.readUTF();
                        System.out.println(incorrecto);
                        dis.close();
                        dos.close();
                        cl.close();
                    }
                     dis.close();
                     dos.close();
                     cl.close();
                 }catch(Exception e){
                   e.printStackTrace();
                 }
            }else{
                 System.out.println("Opción no válida");
                 
             }
    }
    
    private static void envioDeNuevaBase(DataOutputStream dos) throws IOException{
        
    }
//metodo para carpeta base con FileChooser
   private static File cambioBaseFileChooser(File directorio){
    JFileChooser jf = new JFileChooser();
    jf.setCurrentDirectory(directorio);
    jf.setRequestFocusEnabled(true);
    jf.requestFocus();
    jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    int r = jf.showDialog(null, "Elegir");
    File f = null; // Declara la variable f aquí
    if(r==JFileChooser.APPROVE_OPTION){
        f = jf.getSelectedFile();
    }
    return f;
}
   
   
   
//caso 6. crear directorios de manera local o remota_________________________________________________________
    private static void crearDirectorios(Socket cl) {
    var op = "";
         System.out.println("¿Desde crear los directorios de manera local o remota?");
         Scanner scanner = new Scanner(System.in);
            System.out.println("Escriba una l(local) o una r(remota) para elegir una opción: ");
             op = scanner.next();
             if(op.equals("l")){
                System.out.println("Seleccione la carpeta de donde desea crear el directorio");
                if (baseDir == null) {
                baseDir = new File(System.getProperty("user.home"), "Desktop");
                }
                JFileChooser jf = new JFileChooser();
                jf.setCurrentDirectory(baseDir);
                jf.setRequestFocusEnabled(true);
                jf.requestFocus();
                jf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int r = jf.showDialog(null, "Elegir");
                if(r==JFileChooser.APPROVE_OPTION){
                    File f = jf.getSelectedFile();
                    String tipo = (f.isDirectory())?"Carpeta":"Archivo";
                    System.out.println("\033[32m Elegiste: "+f.getAbsolutePath());
                    System.out.println("Tipo: "+tipo);
                    if(tipo.compareTo("Carpeta")==0){
                        File[]listado = f.listFiles();
                         String permisos="";
                    if(f.canRead())
                        permisos = permisos+"r";
                    if(f.canWrite())
                        permisos = permisos+"w";
                    if(f.canExecute())
                        permisos = permisos+"x";
                    System.out.println("Permisos:"+permisos);
                        //System.out.println("Contenido:");
                         crearDirectoriosLocal(f);
                    }//if
                 }
            }else if(op.equals("r")){
                 try{
                     String directiva = "mkdir";
                     DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                     
                     //envia directiva mkdir
                     dos.writeUTF(directiva);
                     dos.flush();
                     
                     //recibe instrucciones para el cliente
                     DataInputStream dis = new DataInputStream(cl.getInputStream()); 
                     String instrMKDIR = dis.readUTF();
                     System.out.println(instrMKDIR);
                     
                     //recibe ubicacion de la dir de la carpeta actual
                     String ubicacion = dis.readUTF();
                     System.out.println(ubicacion);
                     
                     //recibe tipo
                     String tipo = dis.readUTF();
                     System.out.println(tipo);
                     
                     //recibe permisos
                     String permisos = dis.readUTF();
                     System.out.println("Permisos: "+permisos);
                     
                     //se envia el nombre de la nueva carpeta
                     System.out.println("Escriba el nombre de la nueva carpeta");
                     Scanner scMKDIR = new Scanner(System.in);
                     String entradaMKDIR = scMKDIR.nextLine(); // lee la entrada del usuario como una cadena y no como objeto
                     dos.writeUTF(entradaMKDIR); 
                     dos.flush(); 
                     
                     //recibe instruccion para saber si se creó o no el dir.
                     String instr3MKDIR = dis.readUTF();
                     System.out.println(instr3MKDIR);
                     
                     //se cierra
                     dis.close();
                     dos.close();
                     cl.close();
                 }catch(Exception e){
                   e.printStackTrace();
                 }
            }else{
                 System.out.println("Opción no válida");
                 
             }
    }
    //parte del caso 6 crear dir local
    private static void crearDirectoriosLocal(File f){
      String path = f.getAbsolutePath();
        System.out.println("Ingresa el nombre del nuevo directorio: ");  
      Scanner sc = new Scanner(System.in);  
      path = path+"\\"+sc.nextLine();  
      File f1 = new File(path); 
      boolean bool = f1.mkdir();  
      if(bool){  
         System.out.println("El directorio se creó satisfactoriamente");  
      }else{  
         System.out.println("Hubo un error al crear el directorio");  
      }  
        
    }

    //caso 7. salir de la app_______________________________________________________________________________
    public static void salirAplicacion(Socket cl) {
        try {
            //envía la directiva quit
            String directiva = "quit";
            DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
            dos.writeUTF(directiva);
            dos.flush();
            
            //recibe inst
            DataInputStream dis = new DataInputStream(cl.getInputStream());
            String instr = dis.readUTF();
            System.out.println(instr);
         
        System.out.println("Conexión cerrada. Saliendo de la aplicación...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    
}//public class cliente    