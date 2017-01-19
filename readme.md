To build the project and install javascript libraries

    mvn clean install -Pinstall-web-templates


## TimeStamp server

All the signatures made by the system incorporates a TimeStamp, the system 
needs a TimeStampServer to add TimeStamps to the signatures.
You need to install the one provided in the proyect and update the
param **timestampServerURL** on **config.properties** with the value of your
installation.

 
### Discrete timestamps

A discrete timestamp is a timestamp that has a discrete value, f.e:
all request between 01:00 and 02:00 will have as TimeStamp time 02:00, all
between 04:00 and 05:00 will have 05:00 ...

Discrete timestamps are created to make it more difficult to trace 
electors analyzing the TimeStamp of the signed votes (the results of the 
elections are freely available online for anyone that could want to validate them).


#### Server certificates

**DO NOT USE THE PROJECT PROVIDED CERTIFICATES IN PRODUCTION**. You should get a 
real certificate signed by a trusted certificate authority.

[**Generación y configuración del almacén de claves de la aplicación**](https://github.com/votingsystem/votingsystem/wiki/Almacenes-de-claves).
