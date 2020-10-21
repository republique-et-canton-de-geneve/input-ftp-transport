package ch.ge.geomatique.geoevent.transport.ftp;

import java.io.IOException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;


/**
* The SFtpClient class allows to connect to a sftp server and downloads a file
*
* @author  Philippe De Pol
* @version 1.0
* @since   21.10.2020
*/
public class SFtpClient
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
  // The private key used to authenticate into the ssh server
  private String privateKey = "";
  	
  public SFtpClient(String server,String user,String password,String serverFolder,String fileName,String localFolder,String privateKey)
  {
  	this.server = server;
  	this.user = user;
  	this.password = password;
  	this.serverFolder = serverFolder;
  	this.fileName = fileName;
  	this.localFolder = localFolder;
  	this.privateKey = privateKey;
  	
  }
  
  public void downloadFile() throws Exception
  {
    {
      Session session = null;
      ChannelSftp sftpChannel = null;
      
      try
      {
        JSch.setConfig("StrictHostKeyChecking", "no");
        JSch sshClient = new JSch();

        // Connect to the sftp server
        if (!privateKey.isEmpty())
          sshClient.addIdentity(privateKey);

        session = sshClient.getSession(user, server);

        if (!password.isEmpty())
          session.setPassword(password);

        session.connect();

        String remotefileName = serverFolder + fileName;

        sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        SftpATTRS attr = null;

        // Test if the file exists
        try
        {
          attr = sftpChannel.stat(remotefileName);
        } catch (Exception e)
        {;}

        // If the file doesn't exist, exit
        if (attr == null)
        	throw new Exception("SFTP Exception. File not found. (server:" + server + ",fileName:" + fileName+ ").");

        String localFilename = localFolder + fileName;

        // download the file
        sftpChannel.get(remotefileName, localFilename);
        
        }
        catch (Exception e)
        {
        	throw new Exception("SFTP Transport Exception error. (server:" + server + ").",  e);
        }
        finally
        {
          if (sftpChannel != null)
          {
            sftpChannel.disconnect();
          }
          
          if (session != null)
          	session.disconnect();
        }
    }
  }
}