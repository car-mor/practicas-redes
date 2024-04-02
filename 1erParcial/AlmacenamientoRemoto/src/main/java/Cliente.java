import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.swing.JFileChooser;
/*
 * @authors Carlos Moreno y Vanessa Trejo
 */
public class Cliente {
    
    public static void main(String[] args) {
        try{
        while(true){
            int opc = 0;
            Socket cl = new Socket("127.0.0.1", 1234); //crear instancia de socket especifacndo direccion IP del socket al que nos conectaremos
            System.out.println("Conexion establecida..seleccione alguna de las siguientes opciones:");
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
                    
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    crearDirectorios(cl);
                    break;
                case 7:
                    cl.close();
                    System.out.println("Saliendo de la aplicación...");
                    return;
            }}
        }catch(Exception e){
            e.printStackTrace();
        }
    }
     public static void listarDirectorios(Socket cl) {
         var op = "";
         System.out.println("¿Desde listar los directorios de manera local o remota?");
         Scanner scanner = new Scanner(System.in);
            System.out.println("Escriba una l(local) o una r(remota) para elegir una opción: ");
             op = scanner.next();
             if(op.equals("l")){
                 try{
                     System.out.println("Seleccione la carpeta de donde desea enlistar los directorios");
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
                 }
                    cl.close();
                 }catch(Exception e){
                     e.printStackTrace();
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

//     private static void enlistarSubcarpetas(DataInputStream dis) throws IOException{
//       int cantidadCarpetas = dis.readInt();
//       int nivel = dis.readInt();
//       for (int i = 0; i < cantidadCarpetas; i++) {
//           for(int x=0; x<nivel;x++){
//               String identacion = dis.readUTF();
//               System.out.print(identacion);
//           }
//           String carpetas = dis.readUTF();
//           System.out.println(carpetas);
//            int cantidadSubcarpetas = dis.readInt();
//            if(cantidadSubcarpetas>0){
//                for(int j=0; j<=cantidadSubcarpetas;j++){
//                for(int y=0;y<nivel;y++){
//                    String identacion2 = dis.readUTF();
//                    System.out.print(identacion2);
//                }
//                String subcarpetas = dis.readUTF();
//                System.out.println(subcarpetas);
//                enlistarSubcarpetas(dis);
//            }
//            }
//        }  
//     }
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

    private static void crearDirectorios(Socket cl) {
    var op = "";
         System.out.println("¿Desde crear los directorios de manera local o remota?");
         Scanner scanner = new Scanner(System.in);
            System.out.println("Escriba una l(local) o una r(remota) para elegir una opción: ");
             op = scanner.next();
             if(op.equals("l")){
                 System.out.println("Seleccione la carpeta de donde desea enlistar los directorios");
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
                         crearDirectoriosLocal(f);
                    }//if
                 }
    
            }else if(op.equals("r")){
                 try{
                     String directiva = "mkdir";
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
                     System.out.println("¿En qué carpeta del servidor le gustaría crear un directorio?");
                     Scanner servdir = new Scanner(System.in);  
                     //String ruta = ruta+"\\"+servdir.nextLine();  
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
}

