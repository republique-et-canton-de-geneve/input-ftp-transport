/*
 * input-ftp-transport
 *
 * Copyright (C) 2019 - 2020 République et Canton de Genève
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ge.geomatique.geoevent.transport.ftp;

import java.io.IOException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

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
  // SFTP port
  private int port;
  
  public SFtpClient(String server,String user,String password,String serverFolder,String fileName,String localFolder,String privateKey, int port)
  {
  	this.server = server;
  	this.user = user;
  	this.password = password;
  	this.serverFolder = serverFolder;
  	this.fileName = fileName;
  	this.localFolder = localFolder;
  	this.privateKey = privateKey;
  	this.port = port;
  }
  
  public void downloadFile() throws IOException
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

      session = sshClient.getSession(user, server, port);

      if (!password.isEmpty())
        session.setPassword(password);

      session.connect();

      String remotefileName = serverFolder + fileName;

      sftpChannel = (ChannelSftp) session.openChannel("sftp");
      sftpChannel.connect();

      SftpATTRS attr = null;

      // Test if the file exists
      attr = sftpChannel.stat(remotefileName);

      // If the file doesn't exist, exit
      if (attr == null)
      	throw new IOException("SFTP Exception. File not found. (server:" + server + ",fileName:" + fileName+ ").");

      String localFilename = localFolder + fileName;

      // download the file
      sftpChannel.get(remotefileName, localFilename);
      
      }
      catch (JSchException | SftpException e)
      {
      	throw new IOException("SFTP Transport Exception error. (server:" + server + ").",  e);
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