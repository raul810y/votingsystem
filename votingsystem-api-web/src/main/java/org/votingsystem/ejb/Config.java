package org.votingsystem.ejb;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateSource;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface Config {

    public String getApplicationDirPath();
    public String getEntityId();
    public void addTrustedTimeStampIssuer(X509Certificate trustedTimeStampIssuer);
    public Set<TrustAnchor> getTrustedCertAnchors();
    public Certificate getCACertificate(Long certificateId);
    public String getTimestampServiceURL();
    public AbstractSignatureTokenConnection getSigningToken();
    public CertificateSource getTrustedCertSource();
    public Map<Long, X509Certificate> getTrustedTimeStampServers();
    public MetadataDto getMetadata();
    public boolean isAdmin(User user) throws ValidationException;
    public X509Certificate getSigningCert();
    public String getIdProviderEntityId();
    public void setIdProviderEntityId(String idProviderEntityId);

}
