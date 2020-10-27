package ch.ge.geomatique.geoevent.transport.ftp;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FilenameUtils;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.core.property.Property;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.InboundTransportBase;
import com.esri.ges.transport.TransportDefinition;


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
  
  static final long SECOND_MILLISECONDS = 1000L;
	static final int FTP_DEFAULT_PORT = 21;
	static final String SQL_EXCEPTION_DELETE_FILE = "FTP connector. Impossible to delete file : ";
	static final int RETURN_CHAR = 10;
	
  // Server type : ftp or sftp
  private String serverType = "";
  // Server name
  private String server = "";
  // Server port
  private int port = FTP_DEFAULT_PORT;
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

  	Property passwordProperty = getProperty("password");
  	
  	try
  	{
  		if (passwordProperty.getValueAsString() != null)
  			password = passwordProperty.getDecryptedValue();
		}
  	catch (Exception e)
  	{
			LOGGER.error("FTP connector. Exception when decoding password : ", e);
  	}

    if (properties.get("serverFolder").getValueAsString() != null)
    	serverFolder = properties.get("serverFolder").getValueAsString();
    
    if (properties.get("privateKey").getValueAsString() != null)
    	privateKey = properties.get("privateKey").getValueAsString();

    // Use FileNameUtils getName to prevent 'Path Traversal security issue
    if (properties.get("fileName").getValueAsString() != null)
    	fileName = FilenameUtils.getName(properties.get("fileName").getValueAsString());
    
    if (properties.get("localFolder").getValueAsString() != null)
    	localFolder = properties.get("localFolder").getValueAsString();
    
    if (properties.get("frequency").getValue() != null)
    	frequency = (Integer) properties.get("frequency").getValue();
    
    if (properties.get("numberOfLinesToSkip").getValue() != null)
    	numberOfLinesToSkip = (Integer) properties.get("numberOfLinesToSkip").getValue();
    
    if (properties.get("port").getValue() != null)
    	port = (Integer) properties.get("port").getValue();
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
      
      timer.scheduleAtFixedRate(running, Date.from(date.atZone(ZoneId.systemDefault()).toInstant()), frequency * SECOND_MILLISECONDS);

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
    {
    	downloadFTPFile();
    }
    else
    {
      downloadSFTPFile();
    }
  }

  // Download the file from a classic FTP server
  private void downloadFTPFile()
  {
  	FtpClient ftpClient = new FtpClient(server, user, password, serverFolder, fileName, localFolder, port);
  	
		try
		{
			boolean success = ftpClient.downloadFile();
			
			String localFileName = localFolder + fileName;
			
      if (success)
      {
        receive(localFileName);
        Files.delete(Paths.get(localFileName));
      }
		}
		catch (IOException e)
		{
      LOGGER.error("FTP Transport Exception error. (server:" + server + ").", e);
      stop();
      setRunningState(RunningState.ERROR);
		}
  }

  
//Download the file from a SFTP server
 private void downloadSFTPFile()
 {
 	SFtpClient sFtpClient = new SFtpClient(server, user, password, serverFolder, fileName, localFolder, privateKey, port);

	try
	{
		sFtpClient.downloadFile();
		
		String localFileName = localFolder + fileName;
    receive(localFileName);
    
    Files.delete(Paths.get(localFileName));
	}
	catch (Exception e)
	{
    LOGGER.error("SFTP Transport Exception error. (server:" + server + ").", e);
    stop();
    setRunningState(RunningState.ERROR);
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
      LOGGER.error("UNEXPECTED_ERROR", e);
      stop();
      setRunningState(RunningState.ERROR);
    }

  }

}

