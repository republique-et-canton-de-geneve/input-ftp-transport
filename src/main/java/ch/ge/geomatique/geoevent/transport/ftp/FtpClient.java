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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * The FtpClient class allows to connect to a ftp server and downloads files
 *
 * @author Philippe De Pol
 * @version 1.1
 * @since 25.03.2021
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
    // The file format of the files
    private String fileFormat = "";
    // Folder where the file will be downloaded
    private String localFolder = "";
    // FTP port
    private int port;

    public FtpClient(String server, String user, String password, String serverFolder, String fileFormat, String localFolder, int port)
    {
	this.server = server;
	this.user = user;
	this.password = password;
	this.serverFolder = serverFolder;
	this.fileFormat = fileFormat;
	this.localFolder = localFolder;
	this.port = port;
    }

    public List<String> downloadFiles() throws IOException
    {
	// The reply from the ftp server
	int reply;
	// List of all file names downloaded
	List<String> fileNames = new ArrayList<>();
	FTPClient ftp = new FTPClient();

	try
	{
	    // Connect to the server
	    ftp.connect(server, port);
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

	    String remoteFiles = serverFolder + fileFormat;

	    // Test if the file exists on the ftp server
	    FTPFile[] files = ftp.listFiles(remoteFiles);

	    if (files.length == 0)
		throw new IOException("FTP Exception. File does not exist. (server:" + server + ",fileName:" + fileFormat + ").");

	    // Download each file
	    for (FTPFile file : files)
	    {
		Path path = Paths.get(file.getName());
		// Get only the file name without the path
		String filename = path.getFileName().toString();
		File downloadFile = new File(localFolder + filename);

		try (FileOutputStream fr = new FileOutputStream(downloadFile); OutputStream outputStream = new BufferedOutputStream(fr);)
		{
		    boolean success = ftp.retrieveFile(serverFolder + filename, outputStream);

		    if (success)
			fileNames.add(filename);
		}
	    }

	    ftp.logout();

	    return fileNames;

	} catch (IOException e)
	{
	    throw new IOException("FTP Transport Exception error. (server:" + server + ").", e);
	} finally
	{
	    if (ftp.isConnected())
	    {
		ftp.disconnect();
	    }
	}
    }
}