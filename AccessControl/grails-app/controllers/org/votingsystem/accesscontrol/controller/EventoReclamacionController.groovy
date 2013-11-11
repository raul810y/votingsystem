package org.votingsystem.accesscontrol.controller

import org.votingsystem.accesscontrol.model.*

import grails.converters.JSON

import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.*
/**
 * @infoController Reclamaciones
 * @descController Servicios relacionados con la publicación de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventoReclamacionController {

    def eventoReclamacionService
	def reclamacionService
    def eventoService
	def firmaService
	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de reclamaciones.
	 */
	def mainPage() {
		render(view:"mainPage" , model:[selectedSubsystem:Subsystem.CLAIMS.toString()])
	}
	
	/**
	 * @httpMethod [GET]
	 * @param [id] Opcional. El identificador de la reclamación en la base de datos. Si no se pasa ningún 
	 *        id la consulta se hará entre todos las reclamaciones.
	 * @param [max] Opcional (por defecto 20). Número máximo de reclamaciones que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @responseContentType [application/json]
	 * @return Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
    def index () {
        def eventoList = []
        def eventosMap = new HashMap()
        eventosMap.eventos = new HashMap()
        eventosMap.eventos.reclamaciones = []
        if (params.long('id')) {
			EventoReclamacion evento = null
			EventoReclamacion.withTransaction {
				evento = EventoReclamacion.get(params.long('id'))
				if(!(evento.estado == Evento.Estado.ACTIVO || evento.estado == Evento.Estado.PENDIENTE_COMIENZO ||
						evento.estado == Evento.Estado.CANCELADO || evento.estado == Evento.Estado.FINALIZADO)) evento = null
			}
			if(!evento) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'eventNotFound', args:[params.id])
				return false
			} else {
				if(request.contentType?.contains("application/json")) {
					render eventoService.optenerEventoMap(evento) as JSON
					return false
				} else {
					render(view:"eventoReclamacion", model: [
						selectedSubsystem:Subsystem.CLAIMS.toString(),
						eventMap: eventoService.optenerEventoMap(evento)])
					return
				}
			}
        } else {
			params.sort = "fechaInicio"
			log.debug " -Params: " + params
			Evento.Estado estadoEvento
			if(params.estadoEvento) estadoEvento = Evento.Estado.valueOf(params.estadoEvento)
			if(estadoEvento) {
				if(estadoEvento == Evento.Estado.FINALIZADO) {
					eventoList =  EventoReclamacion.findAllByEstadoOrEstado(
						Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, params)
					eventosMap.numeroTotalEventosReclamacionEnSistema = 
							EventoReclamacion.countByEstadoOrEstado(
							Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
				} else {
					eventoList =  EventoReclamacion.findAllByEstado(estadoEvento, params)
					eventosMap.numeroTotalEventosReclamacionEnSistema = 
						EventoReclamacion.countByEstado(estadoEvento)
				}
			} else {
				eventoList =  EventoReclamacion.findAllByEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
					   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, params)
				eventosMap.numeroTotalEventosReclamacionEnSistema = 
						EventoReclamacion.countByEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
						Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
			}
            eventosMap.offset = params.long('offset')
        }
		eventosMap.numeroEventosReclamacionEnPeticion = eventoList.size()
        eventoList.each {eventoItem ->
                eventosMap.eventos.reclamaciones.add(
				eventoService.optenerEventoReclamacionMap(eventoItem))
        }
        response.setContentType("application/json")
        render eventosMap as JSON
    }
    
	/**
	 * Servicio que proporciona el recibo con el que el sistema
	 * respondió a una solicitud de publicación de reclamación.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/eventoReclamacion/${id}/validado]
	 * @param [id] Obligatorio. El identificador de la reclamación en la base de datos.
	 * @return Documento SMIME con el recibo.
	 */
    def validado () {
        def evento
		Evento.withTransaction {
			evento = EventoReclamacion.get(params.long('id'))
		}
        MessageSMIME messageSMIME
        if (evento?.id) {
			MessageSMIME.withTransaction {
				List results = MessageSMIME.withCriteria {
					createAlias("smimePadre", "smimePadre")
					eq("smimePadre.evento", evento)
					eq("smimePadre.type", TypeVS.EVENTO_RECLAMACION)
				}
				messageSMIME = results?.iterator()?.next()
			}
            if (messageSMIME) {
                    response.status = ResponseVS.SC_OK
                    response.contentLength = messageSMIME.contenido.length
                    //response.setContentType("text/plain")
                    response.outputStream <<  messageSMIME.contenido
                    response.outputStream.flush()
                    return false
            }
        }
        if (!evento || !messageSMIME) {
            response.status = ResponseVS.SC_NOT_FOUND
            render message(code: 'eventNotFound', args:[params.id])
            return false
        }
    }
    
	/**
	 * Servicio que proporciona una copia de la reclamación publicada.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoReclamacion/${id}/firmado]
	 * @param [id] Obligatorio. El identificador de la reclamación en la base de datos.
	 * @return Documento SMIME con la solicitud de publicación de la reclamación.
	 */
	def firmado () {
		def evento
		Evento.withTransaction {
			evento = EventoReclamacion.get(params.long('id'))
		}
		if (evento) {
			MessageSMIME messageSMIME
			MessageSMIME.withTransaction {
				messageSMIME = MessageSMIME.findWhere(evento:evento, type:
					TypeVS.EVENTO_RECLAMACION)
			}
			if (messageSMIME) {
				response.status = ResponseVS.SC_OK
				response.contentLength = messageSMIME.contenido.length
				response.setContentType("text/plain")
				response.outputStream <<  messageSMIME.contenido
				response.outputStream.flush()
				return false
			}
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'eventNotFound', args:[params.id])
		return false
	}
    
	/**
	 * Servicio para publicar reclamaciones.
	 *
	 * @httpMethod [POST]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 *                     Documento en formato SMIME  en cuyo contenido se
	 *        encuentra la reclamación que se desea publicar en formato HTML.
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * @return Recibo que consiste en el documento SMIME recibido con la firma añadida del servidor.
	 */
    def save () {
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
        try {
			ResponseVS respuesta = eventoReclamacionService.saveEvent(
				messageSMIMEReq, request.getLocale())
			if(ResponseVS.SC_OK == respuesta.statusCode) {
				response.setHeader('eventURL',
					"${grailsApplication.config.grails.serverURL}/eventoReclamacion/${respuesta.eventVS.id}")
				response.setContentType("application/x-pkcs7-signature")
			} 
			params.respuesta = respuesta
        } catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			params.respuesta = new ResponseVS(
				statusCode:ResponseVS.SC_ERROR_PETICION,
				message:message(code:'publishClaimErrorMessage'), 
				type:TypeVS.EVENTO_RECLAMACION_ERROR)
        }
    }
	
	/**
	 * Servicio que devuelve estadísticas asociadas a una reclamación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoReclamacion/$id/estadisticas]
	 * @param [id] Obligatorio. Identificador en la base de datos de la reclamación que se desea consultar.
	 * @responseContentType [application/json]
	 * @return Documento JSON con las estadísticas asociadas a la reclamación solicitada.
	 */
    def estadisticas () {
        EventoReclamacion eventoReclamacion
		if (!params.evento) {
			EventoReclamacion.withTransaction {
				eventoReclamacion = EventoReclamacion.get(params.long('id'))
			}
		} else eventoReclamacion = params.evento
        if (eventoReclamacion) {
            def statisticsMap = reclamacionService.getStatisticsMap(eventoReclamacion, request.getLocale())
			if(request.contentType?.contains("application/json")) {
				if (params.callback) render "${params.callback}(${statisticsMap as JSON})"
				else render statisticsMap as JSON
				return false
			} else {
				render(view:"statistics", model: [statisticsMap:statisticsMap])
				return
			}
        }
        response.status = ResponseVS.SC_NOT_FOUND
        render message(code: 'eventNotFound', args:[params.id])
        return false
    }

	
	/**
	 * Servicio que devuelve información sobre la actividad de una acción de reclamación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoReclamacion/$id/informacionFirmas]
	 * @param [id] Obligatorio. El identificador de la reclamación la base de datos.
	 * @responseContentType [application/json]
	 * @return Documento JSON con información sobre las firmas recibidas por la reclamación solicitada.
	 */
	def informacionFirmas () {
		EventoReclamacion evento = EventoReclamacion.get(params.id)
		if (!evento) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.id])
			return false
		}
		def informacionReclamacionMap = new HashMap()
		List<Firma> firmas
		Firma.withTransaction {
			firmas = Firma.findAllWhere(evento:evento, type:TypeVS.FIRMA_EVENTO_RECLAMACION)
		}
		log.debug("count: " + Firma.findAllWhere(
			evento:evento, type:TypeVS.FIRMA_EVENTO_RECLAMACION).size())
		informacionReclamacionMap.numeroFirmas = firmas.size()
		informacionReclamacionMap.asuntoEvento = evento.asunto
		informacionReclamacionMap.eventoURL =
			"${grailsApplication.config.grails.serverURL}/evento/${evento.id}"
		informacionReclamacionMap.firmas = []
		firmas.each { firma ->
			def firmaMap = [id:firma.id, fechaCreacion:firma.dateCreated,
			usuario:firma.usuario.nif,
			firmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
				"/obtener?id=${firma.messageSMIME.id}",
			reciboFirmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
				"/recibo/${firma.messageSMIME?.id}"]
			def valoresCampos = ValorCampoDeEvento.findAllWhere(firma:firma)
			firmaMap.campos = []
			valoresCampos.each { valorCampo ->
				firmaMap.campos.add([campo:valorCampo.campoDeEvento.contenido, valor:valorCampo.valor])
			}
			informacionReclamacionMap.firmas.add(firmaMap)
		}
		response.status = ResponseVS.SC_OK
		response.setContentType("application/json")
		render informacionReclamacionMap as JSON
	}
}