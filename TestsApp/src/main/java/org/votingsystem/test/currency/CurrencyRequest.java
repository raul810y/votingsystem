package org.votingsystem.test.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyRequestBatch;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.currency.Wallet;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyRequest {

    private static Logger log =  Logger.getLogger(CurrencyRequest.class.getName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        UserVS fromUserVS = signatureService.getUserVS();
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        BigDecimal totalAmount = new BigDecimal(10);
        String curencyCode = "EUR";
        TagVS tag = new TagVS("HIDROGENO");
        TransactionVSDto transactionVSDto = new TransactionVSDto();
        transactionVSDto.setAmount(totalAmount);
        transactionVSDto.setCurrencyCode(curencyCode);
        transactionVSDto.setTag(tag);
        transactionVSDto.setTimeLimited(true);
        CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionVSDto, totalAmount,
                ContextVS.getInstance().getCurrencyServer().getServerURL());
        String messageSubject = "TEST_CURRENCY_REQUEST_DATA_MSG_SUBJECT";
        Map<String, Object> mapToSend = new HashMap<>();
        byte[] requestBytes = JSON.getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
        mapToSend.put(ContextVS.CSR_FILE_NAME, requestBytes);
        String textToSign =  JSON.getMapper().writeValueAsString(requestDto);
        SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                currencyServer.getName(), textToSign, messageSubject);
        mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
        ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, currencyServer.getCurrencyRequestServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            ResultListDto<String> currencyCertsDto = (ResultListDto<String>) responseVS.getMessage(
                    new TypeReference<ResultListDto<String>>(){});
            requestDto.loadCurrencyCerts(currencyCertsDto.getResultList());
            Wallet.saveCurrencyToDir(requestDto.getCurrencyMap().values(), ContextVS.getInstance().getProperty("walletDir"));
        } else {
            log.log(Level.SEVERE," --- ERROR --- " + responseVS.getMessage());
        }
        System.exit(0);
    }

}

