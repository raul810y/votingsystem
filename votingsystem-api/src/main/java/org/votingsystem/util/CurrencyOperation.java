package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum CurrencyOperation implements SystemOperation {


    @JsonProperty("CLOSE_SESSION")
    CLOSE_SESSION(null),
    @JsonProperty("MSG_TO_DEVICE")
    MSG_TO_DEVICE(null),
    @JsonProperty("CURRENCY_WALLET_CHANGE")
    CURRENCY_WALLET_CHANGE(null),
    @JsonProperty("TRANSACTION_INFO")
    TRANSACTION_INFO(null),
    @JsonProperty("TRANSACTION_FROM_BANK")
    TRANSACTION_FROM_BANK(null),
    @JsonProperty("CURRENCY_REQUEST")
    CURRENCY_REQUEST("/api/currency/request"),
    @JsonProperty("GET_METADATA")
    GET_METADATA("/api/metadata"),
    @JsonProperty("CURRENCY_SEND")
    CURRENCY_SEND("/api/currency/send"),
    @JsonProperty("TRANSACTION_FROM_USER")
    TRANSACTION_FROM_USER(null),
    @JsonProperty("CURRENCY_CHANGE")
    CURRENCY_CHANGE(null),
    @JsonProperty("GET_TAG")
    GET_TAG("/api/tag"),
    @JsonProperty("CURRENCY_PERIOD_INIT")
    CURRENCY_PERIOD_INIT(null),
    @JsonProperty("GET_TRUSTED_CERTS")
    GET_TRUSTED_CERTS("/api/certs/trusted"),
    @JsonProperty("GET_CURRENCY_STATUS")
    GET_CURRENCY_STATUS("/api/currency/state"),
    @JsonProperty("GET_CURRENCY_BUNDLE_STATUS")
    GET_CURRENCY_BUNDLE_STATUS("/api/currency/bundle-state"),
    @JsonProperty("GET_TRANSACTION")
    GET_TRANSACTION("/api/transaction"),
    @JsonProperty("BROWSER_CERTIFICATION")
    BROWSER_CERTIFICATION("/api/cert-issuer/browser-csr"),
    @JsonProperty("INIT_BROWSER_SESSION")
    INIT_BROWSER_SESSION("/api/device/init-browser-session"),
    @JsonProperty("BANK_NEW")
    BANK_NEW("/api/user/new-bank");


    private String url;

    CurrencyOperation(String url) {
        this.url = url;
    }

    public String getUrl(String entityId) {
        return entityId + url;
    }

}