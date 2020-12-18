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
		public final FakeSftpServerRule sftpServer = new FakeSftpServerRule();
	
    private SFtpClient SFtpClient;
    
    String server;
    String user;
    String password;
    String serverFolder;
    String fileName;
    String localFolder;
    String privateKey;
    int port;
    
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
        port = 2001;
        
        sftpServer.putFile("/" + fileName, "content of file", UTF_8);
        sftpServer.addUser(user, password);
        sftpServer.setPort(port);
        
        File directory = new File(localFolder);
    		if (!directory.exists())
    			directory.mkdir();
		 
        SFtpClient = new SFtpClient(server, user, password, serverFolder, fileName, localFolder, privateKey, port);
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
		catch (IOException e)
		{
			logger.error("Exception when downloading file:", e);
		}

		File file = new File(localFolder + fileName);
		assertTrue("File not downloaded with SFTP server", file.exists());
	}

}
