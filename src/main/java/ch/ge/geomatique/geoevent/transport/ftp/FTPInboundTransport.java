package ch.ge.geomatique.geoevent.transport.ftp;

import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

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
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;


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
  // Number of lines to skip in the downloaded file
  private int numberOfLinesToSkip = 0;
	static final String CASTOR = "castor";
	static final String SIEMENS = "siemens";
	static final String SQL_EXCEPTION_DELETE_FILE = "FTP connector. Impossible to delete file : ";
	static final String FILE_NAME = "fileName";
	static final String LOCAL_FOLDER = "localFolder";
	static final String NUMBER_OF_LINES_TO_SKIP = "numberOfLinesToSkip";
	static final String PASSWORD_STRING = "password";
	static final String PRIVATE_KEY = "privateKey";
	static final String SENSOR_TYPE = "sensorType";
	static final String SERVER_STRING = "server";
	static final String SERVER_FOLDER = "serverFolder";
	static final String SERVER_TYPE = "serverType";
	static final int RETURN_CHAR = 10;
	static final int PERIOD = 60000;
	static final int THIRTY_SECONDS = 30;

	
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
    
    // Get server type property
    if (getProperty(SERVER_TYPE).isValid() && getProperty(SERVER_TYPE).getValue() != null)
    {
      String value = (String) getProperty(SERVER_TYPE).getValue();
      if (value.length() > 0 && !value.equals(serverType))
      {
        serverType = value;
      }
    }

    // Get server name property
    if (getProperty(SERVER_STRING).isValid() && getProperty(SERVER_STRING).getValue() != null)
    {
      String value = (String) getProperty(SERVER_STRING).getValue();
      if (value.length() > 0 && !value.equals(server))
      {
        server = value;
      }
    }

    // Get user name property
    if (getProperty("user").isValid() && getProperty("user").getValue() != null)
    {
      String value = (String) getProperty("user").getValue();
      if (value.length() > 0 && !value.equals(user))
      {
        user = value;
      }
    }

    // Get password property
    if (getProperty(PASSWORD_STRING).isValid() && getProperty(PASSWORD_STRING).getValue() != null)
    {
      String value = (String) getProperty(PASSWORD_STRING).getValue();
      if (value.length() > 0 && !value.equals(password))
      {
        password = value;
      }
    }

    // Get server folder property
    if (getProperty(SERVER_FOLDER).isValid() && getProperty(SERVER_FOLDER).getValue() != null)
    {
      String value = (String) getProperty(SERVER_FOLDER).getValue();
      if (value.length() > 0 && !value.equals(serverFolder))
      {
        serverFolder = value;
      }
    }

    // Get private key property
    if (getProperty(PRIVATE_KEY).isValid() && getProperty(PRIVATE_KEY).getValue() != null)
    {
      String value = (String) getProperty(PRIVATE_KEY).getValue();
      if (value.length() > 0 && !value.equals(privateKey))
      {
        privateKey = value;
      }
    }

    // Get file name property
    if (getProperty(FILE_NAME).isValid() && getProperty(FILE_NAME).getValue() != null)
    {
      String value = (String) getProperty(FILE_NAME).getValue();
      if (value.length() > 0 && !value.equals(fileName))
      {
        fileName = value;
      }
    }

    // Get local folder property
    if (getProperty(LOCAL_FOLDER).isValid() && getProperty(LOCAL_FOLDER).getValue() != null)
    {
      String value = (String) getProperty(LOCAL_FOLDER).getValue();
      if (value.length() > 0 && !value.equals(localFolder))
      {
        localFolder = value;
      }
    }

    // Get period property
    if (getProperty(NUMBER_OF_LINES_TO_SKIP).isValid() && getProperty(NUMBER_OF_LINES_TO_SKIP).getValue() != null)
    {
      int value = (Integer) getProperty(NUMBER_OF_LINES_TO_SKIP).getValue();
      if (value > 0 && value != numberOfLinesToSkip)
      {
        numberOfLinesToSkip = value;
      }
    }
    
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

      // Get the current time with 30 seconds
      LocalDateTime date = LocalDateTime.now().withSecond(THIRTY_SECONDS);
      
      timer.scheduleAtFixedRate(running, Date.from(date.atZone(ZoneId.systemDefault()).toInstant()), PERIOD);

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
        downloadFile.delete();
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

     ChannelSftp sftpChannel = null;
     try
     {
       String remotefileName = serverFolder + fileName;

       sftpChannel = (ChannelSftp) session.openChannel("sftp");
       sftpChannel.connect();

       SftpATTRS attr = null;

       // Test if the file exists
       try
       {
         attr = sftpChannel.stat(remotefileName);
       } catch (Exception e)
       {
         ;
       }

       // If the file doesn't exist, exit
       if (attr == null)
         return;

       // download and remove the file
       sftpChannel.get(remotefileName, localFolder + fileName);
       sftpChannel.rm(remotefileName);
       receive(localFolder + fileName);

     } catch (SftpException | JSchException ex)
     {
       LOGGER.error("SFTP Transport Exception error (server:" + server + ").", ex);
       stop();
       setRunningState(RunningState.ERROR);
     } finally
     {
       if (sftpChannel != null)
       {
         sftpChannel.disconnect();
       }
     }

   } catch (Exception e)
   {
     LOGGER.error("SFTP Transport Exception error (server:" + server + ").", e);
     stop();
     setRunningState(RunningState.ERROR);
   } finally
   {
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

