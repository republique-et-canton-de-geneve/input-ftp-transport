package ch.ge.geomatique.geoevent.transport.ftp;

import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.InboundTransportBase;
import com.esri.ges.transport.TransportDefinition;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;


/**
 * The FTPInboundTransport class implements an ftp transport component which
 * connects to a ftp server and downloads a specific file
 */
public class FTPInboundTransport extends InboundTransportBase implements Runnable
{
  public FTPInboundTransport(TransportDefinition definition) throws ComponentException
  {
    super(definition);
  }

  static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(FTPInboundTransport.class);

  // Server type : ftp or sftp
  private String serverType = "";
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
  // The private key used to authenticate into the ssh server
  private String privateKey = "";
  // Folder where the file will be downloaded
  private String localFolder = "";
  // This is the time in seconds between successive task executions
  private int frequency = 0;
  // Number of lines to skip in the downloaded file
  private int numberOfLinesToSkip = 0;
	static final String SQL_EXCEPTION_DELETE_FILE = "FTP connector. Impossible to delete file : ";

	static final int RETURN_CHAR = 10;

	
  Timer timer;

  @Override
  public synchronized void start()
  {
    Thread thread = null;
    
    try
    {
      switch (getRunningState())
      {
      case STARTING:
      case STARTED:
      case STOPPING:
        return;
			default:
				break;
      }
      setRunningState(RunningState.STARTING);
      thread = new Thread(this);
      thread.start();
    } catch (Exception e)
    {
      LOGGER.error("UNEXPECTED_ERROR_STARTING", e);
      stop();
    }
  }

  @Override
  public synchronized void stop()
  {
    try
    {
      if (this.timer != null)
      {
        timer.cancel();
        timer.purge();
      }
    } catch (Exception ex)
    {
      LOGGER.error("UNABLE_TO_CLOSE", ex);
    }
    setRunningState(RunningState.STOPPED);
    LOGGER.debug("INBOUND_STOP");
  }

  @Override
  public void validate()
  {
    LOGGER.debug("INBOUND_SKIP_VALIDATION");
  }

  // Read and store properties
  public void applyProperties()
  {
    LOGGER.info("reading properties");
    
    serverType = properties.get("serverType").getValueAsString();
    server = properties.get("server").getValueAsString();
    
    if (properties.get("user").getValueAsString() != null)
    	user = properties.get("user").getValueAsString();

    if (properties.get("password").getValueAsString() != null)
    	password = properties.get("password").getValueAsString();

    if (properties.get("serverFolder").getValueAsString() != null)
    	serverFolder = properties.get("serverFolder").getValueAsString();
    
    if (properties.get("privateKey").getValueAsString() != null)
    	privateKey = properties.get("privateKey").getValueAsString();

    if (properties.get("fileName").getValueAsString() != null)
    	fileName = properties.get("fileName").getValueAsString();
    
    if (properties.get("localFolder").getValueAsString() != null)
    	localFolder = properties.get("localFolder").getValueAsString();
    
    if (properties.get("frequency").getValue() != null)
    	frequency = (Integer) properties.get("frequency").getValue();
    
    if (properties.get("numberOfLinesToSkip").getValue() != null)
    	numberOfLinesToSkip = (Integer) properties.get("numberOfLinesToSkip").getValue();
    
  }

  @Override
  public void run()
  {
    try
    {
      applyProperties();
      setRunningState(RunningState.STARTED);

      // Timer task which connects to a ftp server and downloads a file
      TimerTask running = new TimerTask()
      {
        public void run()
        {
          downloadFile();
        }
      };

      timer = new Timer();

      // Get the current time
      LocalDateTime date = LocalDateTime.now();
      
      timer.scheduleAtFixedRate(running, Date.from(date.atZone(ZoneId.systemDefault()).toInstant()), frequency * 1000L);

    } catch (Exception ex)
    {
      LOGGER.error(ex.getMessage(), ex);
      setRunningState(RunningState.ERROR);
    }
  }

  // Download a file from a (s)ftp server
  private void downloadFile()
  {
    if (password.isEmpty())
      password = "PASS";

    // Make sure paths ends with / character
    if (!localFolder.endsWith("/"))
      localFolder += "/";

    if (serverFolder.length() > 0 && !serverFolder.endsWith("/"))
      serverFolder += "/";

    if (serverType.equals("ftp"))
      downloadFTPFile();
    else
      downloadSFTPFile();
  }

  // Download the file from a classic FTP server
  private void downloadFTPFile()
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
        LOGGER.error("FTP server refused connection. (server:" + server + ").");
        stop();
        setRunningState(RunningState.ERROR);
        return;
      }

      ftp.setFileType(FTP.ASCII_FILE_TYPE);
  
      String remoteFile = serverFolder + fileName;
        
      // Test if the file exists on the ftp server
      FTPFile[] files = ftp.listFiles(remoteFile);
        
      if (files.length == 0)
      		return;
      
      
      File downloadFile = new File(localFolder  + fileName);
      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
      boolean success = ftp.retrieveFile(remoteFile, outputStream);
      outputStream.close();
      
      if (success)
      {
        receive(localFolder + fileName);
        
        if (!downloadFile.delete())
          LOGGER.error("FTP Transport Exception error. Impossible to delete local file.");
      }

      ftp.logout();

    }
  	catch (IOException e)
    {
      LOGGER.error("FTP Transport Exception error. (server:" + server + ").", e);
      stop();
      setRunningState(RunningState.ERROR);
    }
    finally
    {
      if (ftp.isConnected())
      {
        try
        {
          ftp.disconnect();
        }
        catch (IOException ioe)
        {
          LOGGER.error("FTP Transport Exception error. Impossible to disconnect");
        }
      }
    }
  }

  
//Download the file from a SFTP server
 private void downloadSFTPFile()
 {
   Session session = null;
   ChannelSftp sftpChannel = null;
   
   LOGGER.info("Downloading SFTP file");
   
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
       return;

     String localFilename = localFolder + fileName;
     
     // download and remove the file
     sftpChannel.get(remotefileName, localFilename);
     receive(localFilename);
     File downloadFile = new File(localFilename);
     
     if (!downloadFile.delete())
       LOGGER.error("SFTP Transport Exception error. Impossible to delete local file.");

     }
     catch (Exception e)
     {
       LOGGER.error("SFTP Transport Exception error (server:" + server + ").", e);
       stop();
       setRunningState(RunningState.ERROR);
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


  // Convert the downloaded file as bytes and send them into the geoevent
  // workflow
  private void receive(String localFile)
  {
    ByteBuffer bb = null;

    try
    {
      Path filePath = Paths.get(localFile);
      byte[] data = Files.readAllBytes(filePath);
      
      int returnCharfound = 0;
      int lastReturnPosition = 0;
      
      // If we must skip lines in the downloaded file
      if (numberOfLinesToSkip > 0)
      {
        // Scan each byte
        for(int i=0; i< data.length; i++)
        {
          // If it's a return character
          if(data[i] == RETURN_CHAR)
          {
              returnCharfound++;
              // If the number of return characters is equal to the number of lines to skip, save the current position
              if (returnCharfound == numberOfLinesToSkip)
              {
                lastReturnPosition = i;
                break;
              }
    
          }
        }
      }
      
      // skip first lines
      if (returnCharfound > 0)
        data = Arrays.copyOfRange(data, lastReturnPosition + 1, data.length);
      
      bb = ByteBuffer.allocate(data.length);

      bb.put(data);
      bb.flip();
      byteListener.receive(bb, "");
      bb.clear();
    } catch (BufferOverflowException boe)
    {
      LOGGER.error("BUFFER_OVERFLOW_ERROR", boe);
      setRunningState(RunningState.ERROR);
    } catch (Exception e)
    {
      LOGGER.error("UNEXPECTED_ERROR2", e);
      stop();
      setRunningState(RunningState.ERROR);
    }

  }

}

