package org.votingsystem.client.webextension.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.bouncycastle.util.encoders.Hex;
import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.dialog.DialogVS;
import org.votingsystem.client.webextension.pane.DocumentVSBrowserPane;
import org.votingsystem.client.webextension.util.InboxMessage;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.*;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.dto.voting.VoteVSCancelerDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.currency.MapUtils;
import org.votingsystem.util.currency.Wallet;

import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;
import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGN_MECHANISM;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationService extends Service<ResponseVS> {

    private static Logger log = Logger.getLogger(OperationService.class.getSimpleName());

    private OperationVS operationVS = null;
    private char[] password = null;

    @Override protected Task createTask() {
        return new SignatureTask();
    }

    public void processOperationVS(char[] password, OperationVS operationVS) {
        this.operationVS = operationVS;
        this.password = password;
        restart();
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    class SignatureTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            log.info("OperationService.SignatureTask - call:" + operationVS.getType());
            ResponseVS responseVS = null;
            updateProgress(5, 100);
            try {
                switch (operationVS.getType()) {
                    case SEND_VOTE:
                        operationVS.setTargetServer(ContextVS.getInstance().getAccessControl());
                        String controlCenterURL = ContextVS.getInstance().getAccessControl().getControlCenter().getServerURL();
                        responseVS = Utils.checkServer(controlCenterURL);
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setControlCenter((ControlCenterVS) responseVS.getData());
                        }
                        break;
                    case CURRENCY_DELETE:
                        responseVS = new ResponseVS(ResponseVS.SC_OK);
                        break;
                    default:
                        responseVS = Utils.checkServer(operationVS.getServerURL().trim());
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setServer((ActorVS) responseVS.getData());
                            operationVS.setTargetServer((ActorVS) responseVS.getData());
                        }
                }
                updateProgress(25, 100);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    updateMessage(getOperationMessage(operationVS));
                    updateProgress(40, 100);
                    switch(operationVS.getType()) {
                        case SEND_VOTE:
                            responseVS = sendVote(operationVS);
                            break;
                        case CANCEL_VOTE:
                            responseVS = cancelVote(operationVS);
                            break;
                        case CERT_USER_NEW:
                            responseVS = sendCSRRequest(operationVS);
                            break;
                        case VOTING_PUBLISHING:
                            responseVS = publishSMIME(operationVS);
                            break;
                        case MESSAGEVS:
                            responseVS = WebSocketAuthenticatedService.getInstance().sendMessageVS(operationVS);
                            break;
                        case OPEN_SMIME_FROM_URL:
                            responseVS = openReceiptFromURL(operationVS);
                            break;
                        case FILE_FROM_URL:
                            responseVS = openFileFromURL(operationVS);
                            break;
                        case CURRENCY_REQUEST:
                            responseVS = sendCurrencyRequest(operationVS);
                            break;
                        case CURRENCY_DELETE:
                            responseVS = deleteCurrency(operationVS);
                            break;
                        case REPRESENTATIVE_SELECTION:
                            RepresentativeDelegationDto delegationDto = operationVS.getDocumentToSign(RepresentativeDelegationDto.class);
                            delegationDto.setUUID(UUID.randomUUID().toString());
                            responseVS = sendSMIME(JSON.getMapper().writeValueAsString(delegationDto), operationVS);
                            break;
                        case NEW_REPRESENTATIVE:
                            responseVS = sendRepresentativeData(operationVS);
                            break;
                        case CURRENCY_GROUP_EDIT:
                        case CURRENCY_GROUP_NEW:
                            responseVS = sendGroupVSData(operationVS);
                            break;
                        case ANONYMOUS_REPRESENTATIVE_SELECTION:
                            responseVS = processAnonymousRepresentativeSelection(operationVS);
                            break;
                        case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:
                            responseVS = processAnonymousRepresentativeSelectionCancelation(operationVS);
                            break;
                        default:
                            responseVS = sendSMIME(operationVS.updateUUID());
                    }
                    updateProgress(100, 100);
                    return responseVS;
                } else return responseVS;
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        private String getOperationMessage(OperationVS operationVS) {
            if(CryptoTokenVS.MOBILE == BrowserSessionService.getCryptoTokenType()) {
                return operationVS.getSignedMessageSubject() + " - " + ContextVS.getMessage("messageToDeviceProgressMsg",
                        BrowserSessionService.getInstance().getCryptoToken().getDeviceName());
            } else return operationVS.getSignedMessageSubject();
        }

        private ResponseVS openFileFromURL(final OperationVS operationVS) throws Exception {
            final ResponseVS responseVS = HttpHelper.getInstance().getData(operationVS.getDocumentURL(), null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                PlatformImpl.runLater(() -> {
                    try {
                        DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(null,
                                FileUtils.getFileFromBytes(responseVS.getMessageBytes()));
                        new DialogVS(documentVSBrowserPane, null).setCaption(documentVSBrowserPane.getCaption()).show();
                    } catch (Exception ex) {

                    }
                });
                return new ResponseVS(ResponseVS.SC_OK);
            }
            return responseVS;
        }

        private ResponseVS openReceiptFromURL(final OperationVS operationVS) throws Exception {
            ResponseVS responseVS = null;
            if(BrowserHost.getInstance().getSMIME(operationVS.getServiceURL()) != null) {
                responseVS = new ResponseVS(ResponseVS.SC_OK,
                        BrowserHost.getInstance().getSMIME(operationVS.getServiceURL()));
            } else {
                responseVS = HttpHelper.getInstance().getData(operationVS.getServiceURL(), ContentTypeVS.TEXT);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    BrowserHost.getInstance().setSMIME(operationVS.getServiceURL(),
                            responseVS.getMessage());
                }
            }
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_INITIALIZED);
                operationVS.setMessage(responseVS.getMessage());
                PlatformImpl.runLater(() -> {
                    DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(operationVS.getMessage(), null);
                    new DialogVS(documentVSBrowserPane, null).setCaption(documentVSBrowserPane.getCaption()).show();
                });
            }
            return responseVS;
        }

        private ResponseVS sendCSRRequest(OperationVS operationVS) throws Exception {
            CertExtensionDto certExtensionDto = operationVS.getData(CertExtensionDto.class);
            certExtensionDto.setDeviceId(HttpHelper.getMAC());
            certExtensionDto.setDeviceType(DeviceVS.Type.PC);
            certExtensionDto.setDeviceName(InetAddress.getLocalHost().getHostName());
            CertificationRequestVS certificationRequest = CertificationRequestVS.getUserRequest(
                    SIGN_MECHANISM, PROVIDER, certExtensionDto);
            byte[] csrBytes = certificationRequest.getCsrPEM();
            ResponseVS responseVS = HttpHelper.getInstance().sendData(csrBytes, null,
                    ((AccessControlVS) operationVS.getTargetServer()).getUserCSRServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Long requestId = Long.valueOf(responseVS.getMessage());
                byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
                EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, serializedCertificationRequest);
                BrowserSessionService.getInstance().setCSRRequest(requestId, bundle);
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS sendVote(OperationVS operationVS) throws Exception {
            log.info("sendVote");
            String fromUser = ContextVS.getInstance().getMessage("electorLbl");
            VoteVSHelper voteVSHelper = VoteVSHelper.load(operationVS.getVoteVS());
            VoteVSDto voteVS = voteVSHelper.getVote();
            String toUser = voteVS.getEventURL();
            String msgSubject = ContextVS.getInstance().getMessage("accessRequestMsgSubject")  + voteVS.getEventVSId();
            AccessRequestDto accessRequestDto = voteVSHelper.getAccessRequest();
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(fromUser, toUser,
                    JSON.getMapper().writeValueAsString(accessRequestDto), password, msgSubject);
            updateMessage(operationVS.getSignedMessageSubject());
            ResponseVS responseVS = new AccessRequestDataSender(smimeMessage,
                    accessRequestDto, voteVS.getHashCertVSBase64()).call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            updateProgress(60, 100);
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            String textToSign = JSON.getMapper().writeValueAsString(voteVS); ;
            fromUser = voteVS.getHashCertVSBase64();
            msgSubject = ContextVS.getInstance().getMessage("voteVSSubject");
            smimeMessage = certificationRequest.getSMIME(fromUser, toUser, textToSign, msgSubject, null);
            updateProgress(70, 100);
            smimeMessage = new MessageTimeStamper(smimeMessage, 
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
            responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.VOTE,
                    ContextVS.getInstance().getControlCenter().getVoteServiceURL());
            updateProgress(90, 100);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                voteVSHelper.setValidatedVote(responseVS.getSMIME());
                ResponseVS voteResponse = new ResponseVS(ResponseVS.SC_OK);
                voteResponse.setData(voteVSHelper);
                ContextVS.getInstance().addHashCertVSData(voteVS.getHashCertVSBase64(), voteResponse);
                String hashCertVSHex = new String(Hex.encode(voteVS.getHashCertVSBase64().getBytes()));
                Map responseMap = new HashMap<>();
                responseMap.put("statusCode", ResponseVS.SC_OK);
                responseMap.put("hashCertVSBase64", voteVS.getHashCertVSBase64());
                responseMap.put("hashCertVSHex", hashCertVSHex);
                responseMap.put("voteURL", ContextVS.getInstance().getAccessControl().getVoteStateServiceURL(hashCertVSHex));
                responseMap.put("voteVSReceipt", Base64.getEncoder().encodeToString(voteVSHelper.getValidatedVote().getBytes()));
                responseVS.setContentType(ContentTypeVS.JSON);
                responseVS.setMessage(JSON.getMapper().writeValueAsString(responseMap));
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS cancelVote(OperationVS operationVS) throws Exception {
            log.info("cancelVote");
            ResponseVS voteResponse = ContextVS.getInstance().getHashCertVSData(operationVS.getMessage());
            VoteVSHelper voteVSHelper = (VoteVSHelper) voteResponse.getData();
            VoteVSCancelerDto cancelerDto = voteVSHelper.getVoteCanceler();
            ResponseVS responseVS = sendSMIME(JSON.getMapper().writeValueAsString(cancelerDto), operationVS);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setMessage(Base64.getEncoder().encodeToString(responseVS.getSMIME().getBytes()));
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS processNewRepresentative(OperationVS operationVS) throws Exception {
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, operationVS.getReceiverName(),
                    operationVS.getJsonStr(), password, operationVS.getSignedMessageSubject());
            updateMessage(operationVS.getSignedMessageSubject());
            return HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    operationVS.getServiceURL());
        }

        private ResponseVS sendCurrencyRequest(OperationVS operationVS) throws Exception {
            log.info("sendCurrencyRequest");
            TransactionVSDto transactionVSDto = operationVS.getDocumentToSign(TransactionVSDto.class);
            CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionVSDto,
                    transactionVSDto.getAmount(), operationVS.getTargetServer().getServerURL());
            Map<String, Object> mapToSend = new HashMap<>();
            byte[] requestBytes = JSON.getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
            mapToSend.put(ContextVS.CSR_FILE_NAME, requestBytes);
            String textToSign =  JSON.getMapper().writeValueAsString(requestDto);
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, operationVS.getReceiverName(), textToSign,
                    password, operationVS.getSignedMessageSubject());
            updateMessage(operationVS.getSignedMessageSubject());
            mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
            ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ((CurrencyServer)operationVS.getTargetServer()).getCurrencyRequestServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                ResultListDto<String> resultListDto = (ResultListDto<String>) responseVS.getMessage(
                        new TypeReference<ResultListDto<String>>(){});
                requestDto.loadCurrencyCerts(resultListDto.getResultList());

                Wallet.saveToPlainWallet(requestDto.getCurrencyMap().values());
                responseVS = new ResponseVS(responseVS.getStatusCode(), resultListDto.getMessage());
                InboxMessage inboxMessage = new InboxMessage(ContextVS.getMessage("systemLbl"), new Date());
                inboxMessage.setMessage(MsgUtils.getPlainWalletNotEmptyMsg(MapUtils.getCurrencyMap(
                        requestDto.getCurrencyMap().values()))).setTypeVS(TypeVS.CURRENCY_IMPORT);
                InboxService.getInstance().newMessage(inboxMessage, false);
            }
            return responseVS;
        }

        private ResponseVS deleteCurrency(OperationVS operationVS) throws Exception {
            log.info("deleteCurrency");
            try {
                Set<Currency> wallet = Wallet.getWallet(password);
                wallet = wallet.stream().filter(currency -> {
                    if (currency.getHashCertVS().equals(operationVS.getMessage())) {
                        log.info("deleted currency with hashCertVS: " + operationVS.getMessage());
                        return false;
                    } else return true;
                }).collect(toSet());
                Wallet.saveWallet(CurrencyDto.serializeCollection(wallet), password);
                ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK).setType(TypeVS.CURRENCY_DELETE);
                EventBusService.getInstance().post(responseVS);
                return new ResponseVS(ResponseVS.SC_OK).setType(TypeVS.CURRENCY_DELETE);
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        //we know this is done in a background thread
        private  ResponseVS processAnonymousRepresentativeSelectionCancelation(OperationVS operationVS) throws Exception {
            RepresentativeDelegationDto delegation = BrowserSessionService.getInstance().getAnonymousDelegationDto();
            if(delegation == null) return new ResponseVS(ResponseVS.SC_ERROR,
                    ContextVS.getMessage("anonymousDelegationDataMissingMsg"));
            RepresentativeDelegationDto anonymousCancelationRequest = delegation.getAnonymousCancelationRequest();
            RepresentativeDelegationDto anonymousRepresentationDocumentCancelationRequest =
                    delegation.getAnonymousRepresentationDocumentCancelationRequest();
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null,
                    operationVS.getReceiverName(), JSON.getMapper().writeValueAsString(anonymousCancelationRequest),
                    password, operationVS.getSignedMessageSubject());
            SMIMEMessage anonymousSmimeMessage = delegation.getCertificationRequest().getSMIME(delegation.getHashCertVSBase64(),
                    ContextVS.getInstance().getAccessControl().getName(),
                    JSON.getMapper().writeValueAsString(anonymousRepresentationDocumentCancelationRequest),
                    operationVS.getSignedMessageSubject(), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(anonymousSmimeMessage,
                    ContextVS.getInstance().getDefaultServer().getTimeStampServiceURL());
            anonymousSmimeMessage = timeStamper.call();

            Map<String, Object> mapToSend = new HashMap<>();
            mapToSend.put(ContextVS.SMIME_FILE_NAME, smimeMessage.getBytes());
            mapToSend.put(ContextVS.SMIME_ANONYMOUS_FILE_NAME, anonymousSmimeMessage.getBytes());
            updateMessage(operationVS.getSignedMessageSubject());
            ResponseVS responseVS =  HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ContextVS.getInstance().getAccessControl().getAnonymousDelegationCancelerServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage delegationReceipt = responseVS.getSMIME();
                Collection matches = delegationReceipt.checkSignerCert(
                        ContextVS.getInstance().getAccessControl().getX509Certificate());
                if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
                responseVS.setSMIME(delegationReceipt);
                responseVS.setMessage(ContextVS.getMessage("cancelAnonymousRepresentationOkMsg"));
                return responseVS;
            }
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("errorLbl"));
        }

        //we know this is done in a background thread
        private ResponseVS processAnonymousRepresentativeSelection(OperationVS operationVS) throws Exception {
            String caption = operationVS.getCaption();
            if(caption.length() > 50) caption = caption.substring(0, 50) + "...";
            RepresentativeDelegationDto anonymousDelegation = operationVS.getData(RepresentativeDelegationDto.class);
            anonymousDelegation.setServerURL(ContextVS.getInstance().getAccessControl().getServerURL());
            RepresentativeDelegationDto anonymousCertRequest = anonymousDelegation.getAnonymousCertRequest();
            RepresentativeDelegationDto anonymousDelegationRequest = anonymousDelegation.getDelegation();
            try {
                SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, ContextVS.getInstance().getAccessControl().
                                getName(), JSON.getMapper().writeValueAsString(anonymousCertRequest), password,
                        operationVS.getSignedMessageSubject());
                anonymousDelegation.setAnonymousDelegationRequestBase64ContentDigest(smimeMessage.getContentDigestStr());
                updateMessage(operationVS.getSignedMessageSubject());
                //byte[] encryptedCSRBytes = Encryptor.encryptMessage(certificationRequest.getCsrPEM(),destinationCert);
                //byte[] delegationEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
                Map<String, Object> mapToSend = new HashMap<>();
                mapToSend.put(ContextVS.CSR_FILE_NAME, anonymousDelegation.getCertificationRequest().getCsrPEM());
                mapToSend.put(ContextVS.SMIME_FILE_NAME, smimeMessage.getBytes());
                ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                        ContextVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
                if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                //byte[] decryptedData = Encryptor.decryptFile(responseVS.getMessageBytes(),
                // certificationRequest.getPublicKey(), certificationRequest.getPrivateKey());
                anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
                updateProgress(60, 100);
                //this is the delegation request signed with anonymous cert
                smimeMessage = anonymousDelegation.getCertificationRequest().getSMIME(
                        anonymousDelegation.getHashCertVSBase64(),
                        ContextVS.getInstance().getAccessControl().getName(),
                        JSON.getMapper().writeValueAsString(anonymousDelegationRequest),
                        operationVS.getSignedMessageSubject(), null);
                smimeMessage = new MessageTimeStamper(
                        smimeMessage, ContextVS.getInstance().getAccessControl().getTimeStampServiceURL()).call();
                responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                        ContextVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    anonymousDelegation.setDelegationReceipt(responseVS.getSMIME(),
                            ContextVS.getInstance().getAccessControl().getX509Certificate());
                    BrowserSessionService.getInstance().setAnonymousDelegationDto(anonymousDelegation);
                    return ResponseVS.OK();
                } else return responseVS;
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        //we know this is done in a background thread
        private ResponseVS sendSMIME(String documentToSign, OperationVS operationVS, String... headers) throws Exception {
            log.info("sendSMIME");
            SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, operationVS.getReceiverName(),
                    documentToSign, password, operationVS.getSignedMessageSubject());
            updateMessage(operationVS.getSignedMessageSubject());
            return HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    operationVS.getServiceURL(), headers);
        }

        //we know this is done in a background thread
        private ResponseVS sendRepresentativeData(OperationVS operationVS, String... header) throws Exception {
            ResponseVS responseVS = sendSMIME(operationVS.getJsonStr(), operationVS);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                UserVSDto representativeDto = (UserVSDto) responseVS.getMessage(UserVSDto.class);
                String representativeURL =  ContextVS.getInstance().getAccessControl()
                        .getRepresentativeByNifServiceURL(representativeDto.getNIF());
                BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(representativeURL));
                responseVS.setMessage(ContextVS.getMessage("representativeDataSendOKMsg"));
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS sendGroupVSData(OperationVS operationVS) throws Exception {
            ResponseVS responseVS = sendSMIME(operationVS.getJsonStr(), operationVS);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                GroupVSDto dto = (GroupVSDto) responseVS.getMessage(GroupVSDto.class);
                String groupVSURL =  ContextVS.getInstance().getCurrencyServer().getGroupURL(dto.getId());
                BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(groupVSURL));
                responseVS.setMessage(ContextVS.getMessage("groupVSDataSendOKMsg"));
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS sendSMIME(OperationVS operationVS, String... header) throws Exception {
            return sendSMIME(operationVS.getJsonStr(), operationVS, header);
        }

        //we know this is done in a background thread
        private ResponseVS<ActorVS> publishSMIME(OperationVS operationVS) throws Exception {
            log.info("publishSMIME");
            ResponseVS responseVS = sendSMIME(operationVS.getJsonStr(), operationVS, "eventURL");
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                String eventURL = ((List<String>)responseVS.getData()).iterator().next() +"?menu=admin";
                log.info("publishSMIME - new event URL: " + eventURL);
                BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(eventURL));
                //String receipt = responseVS.getMessage();
                responseVS.setMessage(ContextVS.getMessage("eventVSPublishedOKMsg"));
            }
            return responseVS;
        }
    }

}
