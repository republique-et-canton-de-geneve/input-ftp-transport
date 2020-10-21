package ch.ge.geomatique.geoevent.transport.ftp;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.stefanbirkner.fakesftpserver.rule.FakeSftpServerRule;

/**
* The SFtpClientTest class is a Junit test case which connects to a sftp server and downloads a file
*
* @author  Philippe De Pol
* @version 1.0
* @since   21.10.2020
*/
public class SFtpClientTest
{
		final static Logger logger = LoggerFactory.getLogger(SFtpClientTest.class);
	
		@Rule
		public final FakeSftpServerRule sftpServer = new FakeSftpServerRule().addUser("user", "password").setPort(22);
	
    private SFtpClient SFtpClient;
    
    String server;
    String user;
    String password;
    String serverFolder;
    String fileName;
    String localFolder;
    String privateKey;
    
	@Before
	// Set up the fake sftp server and create an instance of the ftp client
	public void setUp() throws Exception
	{
        server = "localhost";
        user = "user";
        password = "password";
        serverFolder = "";
        fileName = "file.txt";
        localFolder = "D:\\tmp\\";
        privateKey = "";
		
        sftpServer.putFile("/" + fileName, "content of file", UTF_8);
        
        File directory = new File(localFolder);
		if (!directory.exists())
			directory.mkdir();
		 
        SFtpClient = new SFtpClient(server, user, password, serverFolder, fileName, localFolder, privateKey);
	}

	@After
	public void tearDown() throws Exception
	{
		// Delete the downloaded file
		File file = new File(localFolder + fileName);
		file.delete();
	}

	@Test
	public void testDownloadFile()
	{
		try
		{
			SFtpClient.downloadFile();
		}
		catch (Exception e)
		{
			logger.error("Exception when downloading file:", e);
		}

		File file = new File(localFolder + fileName);
		assertTrue(file.exists());
	}

}
