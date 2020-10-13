# input-ftp-transport

The input-ftp-transport allows to download data from a (s)ftp server. It has been tested with Geoevent server 10.6.0

It has the following properties :

| name | description |
| ------ | ------ |
| Server type | Type of the server : ftp or sftp |
| Server | Name of the ftp server |
| User | Login user name |
| Password | User password |
| Server folder | Folder on the server where the file is stored |
| Private key | The private key used to authenticate into the ssh server |
| File name | Name of the file that will be downloaded |
| Local folder | Folder where the file will be downloaded |
| Frequency | This is the time in seconds between successive task executions |
| Number of lines to skip | Number of lines to skip |
