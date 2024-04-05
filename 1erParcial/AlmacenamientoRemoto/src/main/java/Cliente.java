import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.swing.JFileChooser;
/*
 * @authors Carlos Moreno y Vanessa Trejo
 */
public class Cliente {
    private static File baseDir;
    public static void main(String[] args) {
        try{
            Socket cl = new Socket("127.0.0.1", 1234); //crear instancia de socket especifacndo direccion IP del socket al que nos conectaremos
            System.out.println("Conexion establecida..seleccione alguna de las siguientes opciones:");
        while(true){
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
                    listarDirectorios(cl);
                    break;
                case 2:
                    borrarCarpetas(cl);
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    cambioCarpetaBase(cl);
                    break;
                case 6:
                    crearDirectorios(cl);
                    break;
                case 7:
                    salirAplicacion(cl);
                    return;
            }}
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    //1
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
                     System.out.println(carpetaSeleccionada);
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
//caso 5. cambiar de carpeta base de manera local/remota
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
                
                    
            }else if(op.equals("r")){
                 try{
                     String directiva = "cd";
                     DataOutputStream dos = new DataOutputStream(cl.getOutputStream());
                     //envia directiva cd
                     dos.writeUTF(directiva);
                     dos.flush();
                     
                     //recibe instrucciones para el cliente
                     DataInputStream dis = new DataInputStream(cl.getInputStream()); 
                     String instrCDDIR = dis.readUTF();
                     System.out.println(instrCDDIR);
                     
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
   
   
   
//caso 6. crear directorios de manera local o remota
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
                     
                     //recibe carpeta
                     String carpeta = dis.readUTF();
                     System.out.println(carpeta);
                     
                     //recibe tipo
                     String tipo = dis.readUTF();
                     System.out.println(tipo);
                     
                     //recibe permisos
                     String permisos = dis.readUTF();
                     System.out.println("Permisos: "+permisos);
                     
                     //recibe la instrucción 2
                     String instr2MKDIR = dis.readUTF();
                     System.out.println(instr2MKDIR);
                     
                     //envía el nombre de la nueva carpeta
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

    //caso 2. borrar archivos/carpetas
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
                  
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
    //caso 2. borrar dir recursivo forma local
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
    
    //caso 2. borrar solo archivos forma local
    public static void borrarArchivo(File directorio) {
        boolean archivoBorrado = directorio.delete(); 
        if (archivoBorrado) {
            System.out.println("El archivo fue eliminado correctamente");
        } else {
            System.out.println("Hubo un error al eliminar el archivo");
        }   
    }
    
    //caso 7. salir de la app
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