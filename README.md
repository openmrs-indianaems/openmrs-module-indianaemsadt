indianaemsadt module
==========================

Description
-----------
A module designed to send an ADT^A01 event to an HL7 v2 MLLP endpoint (i.e., HIE)
whenever a patient is registered in OpenMRS. This module was created as part of
Regenstrief, Indy EMS, and IHIE's collaboration on creating a system for Indy EMS
as part of their response to the COVID-19 pandemic in 2020.

Building from Source
--------------------
You will need to have Java 1.8+ and Maven 3.x+ installed.  Use the command 'mvn package' to 
compile and package the module. The .omod file will be in the omod/target folder.

Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Reference Application 2.10+ `Administration` > `Manage Modules` screen to 
upload and install the .omod file.

Environment
-----------
This module sends ADT^A01 messages to an HL7 server using MLLP and expects the following 
environment variables to be defined:

* `HL7_URL` (default "localhost") MLLP destination for ADT^A01 messages
* `HL7_PORT` (default "6661") port used at `HL7_URL` for recieving messages