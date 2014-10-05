package org.votingsystem.vicket.controller

import grails.converters.JSON
import groovy.io.FileType
import org.apache.log4j.Logger
import org.apache.log4j.RollingFileAppender
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.DateUtils

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * @infoController Reports
 * @descController Informes de la aplicación en formato JSON
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ReportsController {

    def filesService

    private static Logger reportslog = Logger.getLogger("reportsLog");
    private static Logger transactionslog = Logger.getLogger("transactionsLog");


    def index() {
        String weekReportsBaseDir = "${grailsApplication.config.VotingSystem.backupCopyPath}/weekReports"
        def dir = new File(weekReportsBaseDir)
        List<DateUtils.TimePeriod> periods = []
        if(dir.exists()) {
            DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            dir.eachFileRecurse (FileType.FILES) { file ->
                if("balances.json".equals(file.getName())) {
                    String[] period = file.getParentFile().getName().split("_")
                    DateUtils.TimePeriod timePeriod = new DateUtils.TimePeriod(formatter.parse(period[0]), formatter.parse(period[1]));
                    periods.add(timePeriod)
                }
            }
        }
        render(view:'index', model: [periods:periods])
        return false
    }

    def logs() {
        if(request.contentType?.contains("json")) {
            RollingFileAppender appender = reportslog.getAppender("VicketServerReports")
            File reportsFile = new File(appender.file)
            def messageJSON = JSON.parse("{\"records\":[" + reportsFile.text + "]}")
            messageJSON.numTotalRecords = messageJSON.records.length()
            render messageJSON as JSON
            return false
        } else {
            render(view:'logs')
        }
    }

    def week() {
        Calendar calendar = RequestUtils.getCalendar(params)
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar)
        Map<String, File> reportFiles
        reportFiles = filesService.getWeekReportFiles(timePeriod)
        if(request.contentType?.contains("json")) {
            if(reportFiles.reportsFile.exists()) {
                render JSON.parse(reportFiles.reportsFile.text) as JSON
            } else {
                response.status = ResponseVS.SC_NOT_FOUND
                render [:] as JSON
            }
            return false
        } else {
            if(!reportFiles.reportsFile.exists()) {
                response.status = ResponseVS.SC_PRECONDITION_FAILED
                render(view:'/error412',  model: [message:message(code:'reportsForPeriodMissingMsg',
                        args:[timePeriod.toString()])])
            } else render(view:'week',  model: [date:params.date])
        }
    }

    def transactionvs() {
        if(request.contentType?.contains("json")) {
            RollingFileAppender appender = transactionslog.getAppender("VicketTransactionsReports")
            File reportsFile = new File(appender.file)
            //testfile.eachLine{ line ->}
            def messageJSON = JSON.parse("{\"transactionRecords\":[" + reportsFile.text + "]}")
            if(params.transactionvsType) {
                messageJSON.transactionRecords = messageJSON["transactionRecords"].findAll() { item ->
                    if(item.type.equals(params.transactionvsType)) { return item }
                }
            }
            messageJSON.numTotalTransactions = messageJSON.transactionRecords.size()
            messageJSON.queryRecordCount = messageJSON.transactionRecords.size()
            render messageJSON as JSON
            return false
        } else {
            render(view:'transactionvs')
        }
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }
}