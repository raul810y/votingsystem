package org.sistemavotacion.controlacceso

import java.security.cert.X509Certificate;

import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.sistemavotacion.controlacceso.modelo.*;
import grails.converters.JSON
import org.bouncycastle.util.encoders.Base64;

/**
 * @infoController Solicitudes de acceso
 * @descController Servicios relacionados con las solicitudes de acceso recibidas en una votación.
 * 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class SolicitudAccesoController {
    
    def solicitudAccesoService
	def firmaService
	def csrService
	def encryptionService

	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/solicitudAcceso/$id]
	 * @param [id] Obligatorio. El identificador de la solicitud de acceso en la base de datos.
	 * @return <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	 * 			La solicitud de acceso</a> solicitada.
	 */
    def index () {
        if (params.long('id')) {
			def solicitudAcceso
			SolicitudAcceso.withTransaction {
				solicitudAcceso = SolicitudAcceso.get(params.id)
			}
            if (solicitudAcceso) {
                    response.status = Respuesta.SC_OK
                    response.contentLength = solicitudAcceso.mensajeSMIME?.contenido?.length
                    response.setContentType("text/plain")
                    response.outputStream <<  solicitudAcceso.mensajeSMIME?.contenido
                    response.outputStream.flush()
                    return false
            }
            response.status = Respuesta.SC_NOT_FOUND
            render  message(code: 'anulacionVoto.errorSolicitudNoEncontrada')
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrecta')
        return false
    }

	/**
	 * Servicio que valida las <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	 * solicitudes de acceso</a> recibidas en una votación.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/solicitudAcceso]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] La solicitud de acceso.
	 * @param [csr] Obligatorio. La solicitud de certificado de voto.
	 * @return La solicitud de certificado de voto firmada.
	 */
    def processFileMap () {
		MensajeSMIME mensajeSMIMEReq = params[
			grailsApplication.config.SistemaVotacion.accessRequestFileName]
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		params.mensajeSMIMEReq = mensajeSMIMEReq
		SolicitudAcceso solicitudAcceso;
		Respuesta respuesta = solicitudAccesoService.saveRequest(
			mensajeSMIMEReq, request.getLocale())
		EventoVotacion evento = respuesta.evento
		if (Respuesta.SC_OK == respuesta.codigoEstado) {
			solicitudAcceso = respuesta.solicitudAcceso
			byte[] csrRequest = params[
				grailsApplication.config.SistemaVotacion.nombreSolicitudCSR]
			Usuario representative = null
			if(solicitudAcceso.usuario.type == Usuario.Type.REPRESENTATIVE) {
				representative = solicitudAcceso.usuario
			}
			
			
			//log.debug("======== csrRequest: ${new String(csrRequest)}")
			
			
			
			Respuesta respuestaValidacionCSR = firmaService.
					firmarCertificadoVoto(csrRequest, 
					evento, representative, request.getLocale())
			if (Respuesta.SC_OK == respuestaValidacionCSR.codigoEstado) {
				respuesta.tipo = Tipo.SOLICITUD_ACCESO;
				params.respuesta = respuesta
				params.responseBytes = respuestaValidacionCSR.messageBytes
				params.receiverCert = respuestaValidacionCSR.certificado
				params.receiverPublicKey = respuestaValidacionCSR.data
				response.setContentType("multipart/encrypted")
				return false
			} else {
				respuestaValidacionCSR.tipo = Tipo.SOLICITUD_ACCESO_ERROR;
				params.respuesta = respuestaValidacionCSR
				if (solicitudAcceso) solicitudAccesoService.
					rechazarSolicitud(solicitudAcceso, respuesta.mensaje)
					
			}
		} else params.respuesta = respuesta
    }
    
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/solicitudAcceso/hashHex/$hashHex]
	 * @param [hashHex] Obligatorio. Hash en formato hexadecimal asociado
	 *        a la solicitud de acceso.
	 * @return La solicitud de acceso asociada al hash.
	 */
    def hashHex () {
        if (params.hashHex) {
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            String hashSolicitudAccesoBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
            log.debug "hashSolicitudAccesoBase64: ${hashSolicitudAccesoBase64}"
            SolicitudAcceso solicitudAcceso = SolicitudAcceso.findWhere(hashSolicitudAccesoBase64:
                hashSolicitudAccesoBase64)
            if (solicitudAcceso) {
                response.status = Respuesta.SC_OK
                response.contentLength = solicitudAcceso.contenido.length
                response.setContentType("text/plain")
                response.outputStream <<  solicitudAcceso.contenido
                response.outputStream.flush()
                return false  
            }
            response.status = Respuesta.SC_NOT_FOUND
            render message(code: 'error.solicitudAccesoNotFound', 
                args:[params.hashHex])
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/solicitudAcceso/evento/$eventoId/nif/$nif]
	 * @param [eventoId] Obligatorio. El identificador de la votación en la base de datos.
	 * @param [nif] Obligatorio. El nif del solicitante.
	 * @return La solicitud de acceso asociada al nif y el evento.
	 */
	def encontrarPorNif () {
		if(params.nif && params.long('eventoId')) {
			EventoVotacion evento
			EventoVotacion.withTransaction {
				evento =  EventoVotacion.get(params.eventoId)
			}
			if(!evento) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'eventNotFound', args:[params.eventoId])
				return
			}
			Usuario usuario
			Usuario.withTransaction {
				usuario =  Usuario.findByNif(params.nif)
			}
			if(!usuario) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'usuario.nifNoEncontrado', args:[params.nif])
				return
			}
			SolicitudAcceso solicitudAcceso
			SolicitudAcceso.withTransaction {
				solicitudAcceso =  SolicitudAcceso.findWhere(
					usuario: usuario, eventoVotacion:evento)
			}
			if(!solicitudAcceso) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'error.nifSinSolicitudAcceso', args:[params.eventoId, params.nif])
				return
			}
			response.status = Respuesta.SC_OK
			response.contentLength = solicitudAcceso.mensajeSMIME?.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  solicitudAcceso.mensajeSMIME?.contenido
			response.outputStream.flush()
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
		return false
	}

	
}