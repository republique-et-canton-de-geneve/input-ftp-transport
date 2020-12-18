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
    int port;
    
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
    port = 2000;
    
		fakeFtpServer = new FakeFtpServer();
		fakeFtpServer.addUserAccount(new UserAccount(user, password, remoteFolder));

		FileSystem fileSystem = new WindowsFakeFileSystem();
		fileSystem.add(new DirectoryEntry(remoteFolder));
		fileSystem.add(new FileEntry(remoteFolder + "\\" + fileName, "abcdef 1234567890"));
		fakeFtpServer.setFileSystem(fileSystem);
		fakeFtpServer.setServerControlPort(port);
		
		fakeFtpServer.start();
		
    File directory = new File(localFolder);
		if (!directory.exists())
			directory.mkdir();
		
    ftpClient = new FtpClient(server, user, password, serverFolder, fileName, localFolder, port);

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
