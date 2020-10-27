# input-ftp-transport

The input-ftp-transport allows to download data from a (s)ftp server. It has been tested with Geoevent server 10.6.0.
It uses Apache Commons Net library to connect to ftp servers, and Jsch for sftp servers.

It has the following properties :

| Name | Description |
| ------ | ------ |
| Server type | Type of the server : ftp or sftp |
| Server | URL of the ftp server |
| User | Login user name |
| Password | User password |
| Server folder | Folder on the server where the file is stored |
| Private key | The private key used to authenticate into the ssh server |
| File name | Name of the file that will be downloaded |
| Local folder | Folder where the file will be downloaded |
| Frequency | This is the time in seconds between successive task executions |
| Number of lines to skip | Number of lines to skip |

# Build and deploy

In order to build the composant, you need the Geoevent sdk.

The GeoEvent Server SDK and documentation can be found in the ArcGIS Server installation directory. The default installation directories are below:

    Windows: <ArcGIS Server installation directory>\GeoEvent\sdk (for example, C:\Program Files\ArcGIS\Server\GeoEvent\sdk).
    Linux: <ArcGIS Server installation directory>/GeoEvent/sdk (for example, ~/arcgis/server/GeoEvent/sdk).

In these directories, you will find the document 'GeoEvent Developer Guide.pdf', which explains how to build and deploy the composant.