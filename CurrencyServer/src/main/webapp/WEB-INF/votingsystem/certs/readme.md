#### Server certificates

**DO NOT USE 'CurrencyServer.jks' CERTIFICATES IN PRODUCTION**. You should get a real certificate signed by a trusted certificate authority.

[**Generación y configuración del almacén de claves de la aplicación**](https://github.com/votingsystem/votingsystem/wiki/Almacenes-de-claves).

#### Authority certificates

To add a new trusted authority copy its cert (PEM files) in the dir with the 'AC_' prefix in the file name

- [AC_DNIE_001_SHA2.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/PortalDNIe/PRF1_Cons02.action?pag=REF_076)
- [AC_DNIE_002_SHA2.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/PortalDNIe/PRF1_Cons02.action?pag=REF_076)
- [AC_DNIE_003_SHA2.pem' certificado intermedio del DNI electrónico emitido en España]
(http://www.dnielectronico.es/PortalDNIe/PRF1_Cons02.action?pag=REF_076)

#### Admin certificates
To add a new admin copy its cert (PEM files) in the dir with the 'ADMIN_' prefix in the file name

