package br.com.fingerPrint;

import java.sql.*;
import javax.swing.JOptionPane;

public class ConexionBD {
    
    public String porta="3306";
    public String nomeServidor="localhost";
    public String db="finger";
    public String user="root";
    public String pass="";
    
    Connection conn;
    
    public void ConexionBD(){
         conn=null;    
    }
    
    public Connection connectar(){
        try{
            String ruta = "jdbc:mysql://";
            String servidor = nomeServidor+":"+porta+"/";
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn=DriverManager.getConnection(ruta+servidor+db,user,pass);
            if(conn!=null){
                System.out.println("Conexão com a lista de banco de dados...");
            } else if (conn == null){
                throw new SQLException();
            }
        }catch(SQLException e){
            JOptionPane.showMessageDialog(null,"Error: " + e.getMessage());
        }
        catch(NullPointerException e){
            JOptionPane.showMessageDialog(null,"Error: "+e.getMessage() );
        }
        catch (Exception E) {
                    System.err.println("Não foi possível carregar a unidade.");
                    E.printStackTrace();
        }
        finally{
            return conn;
        }
    }
    
    public void deconectar(){
        conn = null;
        System.out.println("Desconexão com a lista de banco de dados...");            
    }
        
}