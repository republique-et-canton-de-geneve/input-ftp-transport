package ch.ge.geomatique.geoevent.transport.ftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
* The FtpClient class allows to connect to a ftp server and downloads a file
*
* @author  Philippe De Pol
* @version 1.0
* @since   21.10.2020
*/
public class FtpClient
{
  // Server name
  private String server = "";
  // User name
  private String user = "";
  // User password
  private String password = "";
  // Folder on the server where the file is stored
  private String serverFolder = "";
  // Name of the file that will be downloaded
  private String fileName = "";
  // Folder where the file will be downloaded
  private String localFolder = "";
  
  	
  public FtpClient(String server,String user,String password,String serverFolder,String fileName,String localFolder)
  {
  	this.server = server;
  	this.user = user;
  	this.password = password;
  	this.serverFolder = serverFolder;
  	this.fileName = fileName;
  	this.localFolder = localFolder;
  	
  }
  
  public boolean downloadFile() throws IOException
  {
    int reply;
    FTPClient ftp = new FTPClient();
    	
    try 
    {
      // Connect to the server
      ftp.connect(server);
      ftp.enterLocalPassiveMode();
      ftp.login(user, password);

      reply = ftp.getReplyCode();

      // Test if we are connected to the server
      if (!FTPReply.isPositiveCompletion(reply))
      {
        ftp.disconnect();
        throw new IOException("FTP server refused connection. (server:" + server + ").");
      }

      ftp.setFileType(FTP.ASCII_FILE_TYPE);
  
      String remoteFile = serverFolder + fileName;

      // Test if the file exists on the ftp server
      FTPFile[] files = ftp.listFiles(remoteFile);

      if (files.length == 0)
      	throw new IOException("FTP Exception. File does not exist. (server:" + server + ",fileName:" + fileName+ ").");
      
      File downloadFile = new File(localFolder  + fileName);
      boolean success;
      
      // Download the file
      try
      (
      		FileOutputStream fr = new FileOutputStream(downloadFile);
      		OutputStream outputStream = new BufferedOutputStream(fr);
      )
      {
      	success = ftp.retrieveFile(remoteFile, outputStream);
      }

      ftp.logout();
      
      return success;

    }
    catch (IOException e)
    {
  		throw new IOException("FTP Transport Exception error. (server:" + server + ").",  e);
    }
    finally
    {
      if (ftp.isConnected())
      {
          ftp.disconnect();
      }
    }
  }
}