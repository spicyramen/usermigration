usermigration
=============

User Migration tool
Read a CSV file in order to convert userid in Cisco CallManager 9.X
This program uses CUCM AXL API updateUser method

java -jar UserMigrationTool.jar -username=<CUCM Admin user> -password=<CUCM Admin password> -host=<CUCM IP Address> -userFile=<CSV file>
