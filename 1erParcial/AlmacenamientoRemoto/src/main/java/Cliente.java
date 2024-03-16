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
            int opc = 0;
            Socket cl = new Socket("127.0.0.1", 1234); //crear instancia de socket especifacndo direccion IP del socket al que nos conectaremos
            System.out.println("Conexion establecida..seleccione alguna de las siguientes opciones:");
            System.out.println("1. Listado de directorios (local/remoto) mediante la primitiva “list”.");
            System.out.println("2. Borrado de archivos/carpetas (local/remoto) mediante la primitiva “rmdir”.");
            System.out.println("3. Envío de archivos/carpetas desde el sistema de archivos local hacia el remoto (primitiva put)");
            System.out.println("4. Envío de archivos/carpetas desde el sistema de archivos remoto hacia el local (primitiva get)");
            System.out.println("5. Cambio de carpeta base (local/remoto) mediante primitiva “cd”");
            System.out.println("6. Creación de carpetas (local/remoto) mediante primitiva “mkdir”");
            System.out.println("7. Salir de la aplicación (primitva “quit”)");
            Scanner scanner = new Scanner(System.in);
            System.out.println("Ingrese una opción: ");
            opc = scanner.nextInt();
            switch(opc){
                case 1:
                    listarDirectoriosLocal(cl);
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
                    break;
                case 7:
                    break;
            }
            cl.close(); //Cierra socket principal luego de todas operaciones
        }catch(Exception e){
            e.printStackTrace();
        }
    }
     public static void listarDirectoriosLocal(Socket cl) {
         var op = "";
         System.out.println("¿Desde listar los directorios de manera local o remota?");
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
                        for(int x =0;x<listado.length;x++){
                            if (listado[x].isDirectory()) {
                            System.out.println("\033[33m ->" + listado[x]);
                            }
                        }//for
                    }//if
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
                     int cantidadCarpetas = dis.readInt();
                     System.out.println("Carpetas recibidas desde el servidor: ");
                     for (int i = 0; i < cantidadCarpetas; i++) {
                        String nombreCarpeta = dis.readUTF(); // Leer el nombre de la carpeta
                        System.out.println("- " + nombreCarpeta);
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
            
         
                /*if(tipo.compareTo("Archivo")==0){
                    System.out.println("Tamaño:"+f.length()+" bytes");
                    String permisos="";
                    if(f.canRead())
                        permisos = permisos+"r";
                    if(f.canWrite())
                        permisos = permisos+"w";
                    if(f.canExecute())
                        permisos = permisos+"x";
                    System.out.println("Permisos:"+permisos);
                    
                }else if(tipo.compareTo("Carpeta")==0){
                    File[]listado = f.listFiles();
                    System.out.println("Contenido:");
                    for(int x =0;x<listado.length;x++){
                        System.out.println("\033[33m ->"+listado[x]);
                      
                    }//for
                }//else if
            }//if*/

       
}
}

