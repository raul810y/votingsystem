package org.votingsystem.model;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public enum TypeVS {

	CONTROL_CENTER_VALIDATED_VOTE,
    ACCESS_CONTROL_VALIDATED_VOTE,
	   
    REQUEST_WITH_ERRORS,
    REQUEST_WITHOUT_FILE,
    EVENT_WITH_ERRORS,
    OK,
    TEST,
    SIGNATURE_ERROR,
    ERROR,
    CANCELLED,
    USER_ERROR,
    RECEIPT,
    RECEIPT_VIEW,
    VOTING_EVENT,
    VOTING_EVENT_ERROR,
    VOTEVS,
    VOTE_ERROR,
    CANCEL_VOTE,
    CANCEL_VOTE_ERROR,
    CONTROL_CENTER_ASSOCIATION,
    CONTROL_CENTER_ASSOCIATION_ERROR,
    BACKUP,
    MANIFEST_EVENT,
    MANIFEST_EVENT_ERROR,
    CLAIM_EVENT_SIGN,
    CLAIM_EVENT, 
    CLAIM_EVENT_ERROR,
    CLAIM_EVENT_SIGNATURE_ERROR,
    VOTE_RECEIPT_ERROR,
    RECEIPT_ERROR,
    VOTE_RECEIPT,
    INDEX_REQUEST,
    SELECT_IMAGE,
    INDEX_REQUEST_ERROR,
    ACCESS_REQUEST_ERROR,
    SMIME_CLAIM_SIGNATURE,
    SEND_SMIME_VOTE,
	REPRESENTATIVE_SELECTION,

    ANONYMOUS_REPRESENTATIVE_REQUEST_ERROR,
    ANONYMOUS_REPRESENTATIVE_REQUEST,
    ANONYMOUS_REPRESENTATIVE_REQUEST_USED,
    ANONYMOUS_REPRESENTATIVE_SELECTION,
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED,

    CONTROL_CENTER_STATE_CHANGE_SMIME, 
    BACKUP_REQUEST, 
    MANIFEST_PUBLISHING, 
    MANIFEST_SIGN, 
    CLAIM_PUBLISHING,
    VOTING_PUBLISHING,
    ACCESS_REQUEST,
    ACCESS_REQUEST_CANCELLATION,
    EVENT_CANCELLATION,
    SAVE_RECEIPT,
    SAVE_RECEIPT_ANONYMOUS_DELEGATION,
    OPEN_RECEIPT,
    NEW_REPRESENTATIVE,
    REPRESENTATIVE_VOTING_HISTORY_REQUEST,
    REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR,
    REPRESENTATIVE_REVOKE_ERROR,
    REPRESENTATIVE_ACCREDITATIONS_REQUEST,
    REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR,
    REPRESENTATIVE_SELECTION_ERROR,
    REPRESENTATIVE_DATA_ERROR,
    REPRESENTATIVE_DATA_OLD,
    REPRESENTATIVE_DATA,
    REPRESENTATIVE_REVOKE,
    TERMINATED,

    MESSAGEVS,
    MESSAGEVS_GET,
    MESSAGEVS_EDIT,
    MESSAGEVS_DECRYPT,

    LISTEN_TRANSACTIONS,
    INIT_VALIDATED_SESSION,

    CERT_USER_NEW,
    CERT_CA_NEW,
    CERT_EDIT,

    VICKET,
    VICKET_USER_INFO,
    VICKET_CANCEL,
    VICKET_REQUEST,
    VICKET_REQUEST_ERROR,
    VICKET_REQUEST_WITH_ITEMS_REPEATED,
    VICKET_BATCH_ERROR,
    VICKET_DEPOSIT_FROM_VICKET_SOURCE,
    VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER,
    VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP,
    VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS,
    VICKET_DEPOSIT_ERROR,
    VICKET_GROUP_NEW,
    VICKET_GROUP_EDIT,
    VICKET_GROUP_CANCEL,
    VICKET_GROUP_SUBSCRIBE,
    VICKET_GROUP_UPDATE_SUBSCRIPTION,
    VICKET_GROUP_USER_ACTIVATE,
    VICKET_GROUP_USER_DEACTIVATE,
    VICKET_GROUP_USER_DEPOSIT,
    VICKET_INIT_PERIOD,
    VICKET_SOURCE,
    VICKET_SOURCE_NEW,

    WEB_SOCKET_INIT,
    WEB_SOCKET_MESSAGE,
    WEB_SOCKET_ADD_SESSION;
    
}
