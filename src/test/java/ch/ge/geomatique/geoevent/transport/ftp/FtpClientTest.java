package ch.ge.geomatique.geoevent.transport.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* The FtpClientTest class is a Junit test case which connects to a ftp server and downloads a file
*
* @author  Philippe De Pol
* @version 1.0
* @since   21.10.2020
*/
public class FtpClientTest
{
		final static Logger logger = LoggerFactory.getLogger(FtpClientTest.class);
    private FakeFtpServer fakeFtpServer;
    
    private FtpClient ftpClient;
    
    String server;
    String user;
    String password;
    String serverFolder;
    String fileName;
    String localFolder;
    String remoteFolder;
    
	@Before
	// Set up the fake ftp server and create an instance of the ftp client
	public void setUp() throws Exception
	{
        server = "localhost";
        user = "user";
        password = "password";
        serverFolder = "";
        fileName = "file1.txt";
        localFolder = "D:\\tmp\\";
        remoteFolder = "C:\\data";
        
		fakeFtpServer = new FakeFtpServer();
		fakeFtpServer.addUserAccount(new UserAccount(user, password, remoteFolder));

		FileSystem fileSystem = new WindowsFakeFileSystem();
		fileSystem.add(new DirectoryEntry(remoteFolder));
		fileSystem.add(new FileEntry(remoteFolder + "\\" + fileName, "abcdef 1234567890"));
		fakeFtpServer.setFileSystem(fileSystem);

		fakeFtpServer.start();
		
        File directory = new File(localFolder);
		if (!directory.exists())
			directory.mkdir();
		
    	ftpClient = new FtpClient(server, user, password, serverFolder, fileName, localFolder);

	}

	@After
	public void tearDown() throws Exception
	{
		// Delete the downloaded file
		File file = new File(localFolder + fileName);
		file.delete();
	}

	@Test
	public void testDownloadFile() {
		try
		{
			ftpClient.downloadFile();
		}
		catch (IOException e)
		{
			logger.error("Exception when downloading file:", e);
		}

		File file = new File(localFolder + fileName);
		assertTrue("File not downloaded with FTP server", file.exists());
	}

}
