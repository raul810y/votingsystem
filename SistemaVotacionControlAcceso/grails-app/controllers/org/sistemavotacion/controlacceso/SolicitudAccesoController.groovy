package org.sistemavotacion.controlacceso

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.sistemavotacion.controlacceso.modelo.*;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class SolicitudAccesoController {
    
    def solicitudAccesoService
	def firmaService
	def csrService

	def index = {}
	
    def obtener = {
        if (params.long('id')) {
			def solicitudAcceso
			SolicitudAcceso.withTransaction {
				solicitudAcceso = SolicitudAcceso.get(params.id)
			}
            if (solicitudAcceso) {
                    response.status = 200
                    response.contentLength = solicitudAcceso.mensajeSMIME?.contenido?.length
                    response.setContentType("text/plain")
                    response.outputStream <<  solicitudAcceso.mensajeSMIME?.contenido
                    response.outputStream.flush()
                    return false
            }
            response.status = 404
            render  message(code: 'anulacionVoto.errorSolicitudNoEncontrada')
            return false
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrecta')
        return false
    }

    def procesar = { 
        String nombreEntidadFirmada = grailsApplication.config.SistemaVotacion.nombreEntidadFirmada;
		SolicitudAcceso solicitudAcceso;
		Respuesta respuesta;
        if (request instanceof MultipartHttpServletRequest) {
			try {
				Map multipartFileMap = ((MultipartHttpServletRequest) request)?.getFileMap()
				MultipartFile solicitudAccesoMultipartFile = multipartFileMap.remove(nombreEntidadFirmada)                                
				respuesta = solicitudAccesoService.validarSolicitud(
					solicitudAccesoMultipartFile.getBytes(), request.getLocale())
				solicitudAcceso = respuesta.solicitudAcceso
				if (200 == respuesta.codigoEstado) {
					MultipartFile solicitudCsrFile = multipartFileMap.remove(
						grailsApplication.config.SistemaVotacion.nombreSolicitudCSR)
					Respuesta respuestaValidacionCSR = firmaService.firmarCertificadoVoto(solicitudCsrFile.getBytes(), 
						respuesta.evento, request.getLocale())
					respuesta = respuestaValidacionCSR
					if (200 == respuestaValidacionCSR.codigoEstado) {
						response.contentLength = respuestaValidacionCSR.firmaCSR.length
						response.setContentType("application/octet-stream")
						response.outputStream << respuestaValidacionCSR.firmaCSR
						response.outputStream.flush()
						return false
					} else {
						if (solicitudAcceso)
							solicitudAccesoService.rechazarSolicitud(solicitudAcceso)
					}
				}
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				if (solicitudAcceso)
					solicitudAccesoService.rechazarSolicitud(solicitudAcceso)
				String mensaje = ex.getMessage();
				if(!mensaje || "".equals(mensaje)) {
					mensaje = message(code: 'error.PeticionIncorrecta')
				}
				flash.respuesta = new Respuesta(mensaje:mensaje,
					codigoEstado:500, tipo: Tipo.PETICION_CON_ERRORES)
				forward controller: "error500", action: "procesar"
			}
			response.status = respuesta.codigoEstado
			render respuesta?.mensaje
			return false;
        }	
    }
    
    def encontrar = {
        if (params.hashSolicitudAccesoHex) {
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            String hashSolicitudAccesoBase64 = new String(
				hexConverter.unmarshal(params.hashSolicitudAccesoHex))
            log.debug "hashSolicitudAccesoBase64: ${hashSolicitudAccesoBase64}"
            SolicitudAcceso solicitudAcceso = SolicitudAcceso.findWhere(hashSolicitudAccesoBase64:
                hashSolicitudAccesoBase64)
            if (solicitudAcceso) {
                response.status = 200
                response.contentLength = solicitudAcceso.contenido.length
                response.setContentType("text/plain")
                response.outputStream <<  solicitudAcceso.contenido
                response.outputStream.flush()
                return false  
            }
            response.status = 404
            render message(code: 'error.solicitudAccesoNotFound', 
                args:[params.hashSolicitudAccesoHex])
            return false
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
	
	def encontrarPorNif = {
		if(params.nif && params.long('eventoId')) {
			EventoVotacion evento
			EventoVotacion.withTransaction {
				evento =  EventoVotacion.get(params.eventoId)
			}
			if(!evento) {
				response.status = 404 //Not Found
				render message(code: 'evento.eventoNotFound', args:[params.eventoId])
				return
			}
			Usuario usuario
			Usuario.withTransaction {
				usuario =  Usuario.findByNif(params.nif)
			}
			if(!usuario) {
				response.status = 404 //Not Found
				render message(code: 'usuario.nifNoEncontrado', args:[params.nif])
				return
			}
			SolicitudAcceso solicitudAcceso
			SolicitudAcceso.withTransaction {
				solicitudAcceso =  SolicitudAcceso.findWhere(
					usuario: usuario, eventoVotacion:evento)
			}
			if(!solicitudAcceso) {
				response.status = 404 //Not Found
				render message(code: 'error.nifSinSolicitudAcceso', args:[params.eventoId, params.nif])
				return
			}
			response.status = 200
			response.contentLength = solicitudAcceso.mensajeSMIME?.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  solicitudAcceso.mensajeSMIME?.contenido
			response.outputStream.flush()
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	
}