package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSClaim
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
/**
 * @infoController Eventos
 * @descController Servicios relacionados con los eventos del sistema.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventVSController {

    def eventVSElectionService
    def eventVSService

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/eventVS/$id?]
	 * @param [id] Opcional. El identificador del eventVS en la base de datos. Si no se pasa ningún id
	 *        la consulta se hará entre todos los eventos.
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de creación.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
	def index() { 
        def eventVSList = []
        def eventsVSMap = new HashMap()
        eventsVSMap.eventsVS = new HashMap()
        eventsVSMap.eventsVS.manifests = []
        eventsVSMap.eventsVS.elections = []
        eventsVSMap.eventsVS.claims = []
        if (params.long('id')) {
			EventVS eventVS = null
			EventVS.withTransaction {
				eventVS = EventVS.get(params.long('id'))
			}
			if(!eventVS) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'eventVSNotFound', args:[params.id])
				return false
			} else {
				render eventVSService.getEventVSMap(eventVS) as JSON
				return false
			} 
        } else {
			EventVS.withTransaction {
				eventVSList =  EventVS.findAllByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
				   EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING, params)
			}
            eventsVSMap.offset = params.long('offset')
        }
		log.debug "index - params: ${params}"
        eventsVSMap.numEventVSInSystem = EventVS.countByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
				   EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING)
        eventsVSMap.numEventsVSManifestInSystem = EventVSManifest.countByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
				   EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING)
        eventsVSMap.numEventsVSClaimInSystem = EventVSClaim.countByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
				   EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING)
        eventsVSMap.numEventsVSElectionInSystem = EventVSElection.countByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
				   EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING)
        eventsVSMap.numEventVSInRequest = eventVSList.size()
        eventVSList.each {eventVSItem ->
                if (eventVSItem instanceof EventVSElection) {
					eventsVSMap.eventsVS.elections.add(eventVSService.getEventVSElectionMap(eventVSItem))
                } else if (eventVSItem instanceof EventVSManifest) {
					if(eventVSItem.state == EventVS.State.PENDING_SIGNATURE ) return
                    eventsVSMap.eventsVS.manifests.add(eventVSService.getEventVSManifestMap(eventVSItem))
                } else if (eventVSItem instanceof EventVSClaim) {
                    eventsVSMap.eventsVS.claims.add(eventVSService.getEventVSClaimMap(eventVSItem))
                }
        }
		eventsVSMap.numEventsVSElection = eventsVSMap.eventsVS.elections.size()
		eventsVSMap.numEventsVSManifest = eventsVSMap.eventsVS.manifests.size()
		eventsVSMap.numEventsVSClaim = eventsVSMap.eventsVS.claims.size()
        render eventsVSMap as JSON
	}
	    
	/**
	 * Servicio que devuelve estadísticas asociadas a un eventVS.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVS/$id/statistics]
	 * @param [id] Identificador en la base de datos del eventVS que se desea consultar.
	 * @return PDFDocumentVS JSON con estadísticas asociadas al eventVS consultado.
	 */
    def statistics () {
		EventVS eventVS
		EventVS.withTransaction {
			eventVS = EventVS.get(params.id)
		}
		if (eventVS) {
			params.eventVS = eventVS
			if (eventVS instanceof EventVSManifest) forward(controller:"eventVSManifest",action:"statistics")
			if (eventVS instanceof EventVSClaim) forward(controller:"eventVSClaim",action:"statistics")
			if (eventVS instanceof EventVSElection) forward(controller:"eventVSElection",action:"statistics")
			return false
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'eventVSNotFound', args:[params.id])
		return false
    }
  
	/**
	 * Servicio que cancela eventos
	 *
	 * @httpMethod [POST]
	 * @requestContentType application/x-pkcs7-signature Obligatorio. Archivo con los datos del eventVS que se desea
	 * 				cancelar firmado por el userVS que publicó o un administrador de sistema.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
   def cancelled() {
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS responseVS = eventVSService.cancelEvent(
			messageSMIMEReq, request.getLocale());
		if(ResponseVS.SC_OK == responseVS.statusCode) {
			response.status = ResponseVS.SC_OK
			response.setContentType(org.votingsystem.model.ContentTypeVS.SIGNED)
	    }
	    params.responseVS = responseVS
   }

   /**
	* Servicio que comprueba las fechas de un eventVS
	*
	* @param id  Obligatorio. El identificador del eventVS en la base de datos.
	* @httpMethod [GET]
	* @serviceURL [/eventVS/$id/checkDates]
	* @return Si todo va bien devuelve un código de estado HTTP 200.
	*/
   def checkDates () {
	   EventVS eventVS
	   if (params.long('id')) {
		   EventVS.withTransaction {
			   eventVS = EventVS.get(params.id)
		   }
	   }
	   if(!eventVS) {
		   response.status = ResponseVS.SC_NOT_FOUND
		   render message(code: 'eventVSNotFound', args:[params.id])
		   return false
	   }
	   ResponseVS responseVS = eventVSService.checkDatesEventVS(eventVS, request.getLocale())
	   response.status = responseVS.statusCode
	   render responseVS?.eventVS?.state?.toString()
	   return false
   }
	   
}