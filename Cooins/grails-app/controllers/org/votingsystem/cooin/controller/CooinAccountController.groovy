package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.dao.DataAccessException
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class CooinAccountController {

    def cooinAccountService

    def balance() {
        if(params.long('id')) {
            UserVS userVS = null
            Map resultMap = [:]
            UserVS.withTransaction { userVS = UserVS.get(params.long('id')) }
            if(!userVS) {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            def userAccountsDB
            CooinAccount.withTransaction { userAccountsDB = CooinAccount.createCriteria().list(sort:'dateCreated', order:'asc') {
                eq("userVS", userVS)
                eq("state", CooinAccount.State.ACTIVE)
            }}
            List userAccounts = []
            userAccountsDB.each { it ->
                userAccounts.add(cooinAccountService.getUserVSAccountMap(it))
            }
            resultMap = [name: userVS.name, id:userVS.id, accounts:userAccounts]
            render resultMap as JSON
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}