package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.FileUtils;
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import com.itextpdf.text.pdf.AcroFields
import com.itextpdf.text.pdf.PdfReader
import grails.converters.JSON
import grails.util.*

/**
 * @infoController Solicitud de copias de seguridad
 * @descController Servicios que gestiona solicitudes de copias de seguridad.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class SolicitudCopiaController {
	
	def eventoFirmaService
	def eventoReclamacionService
	def eventoVotacionService
	def mailSenderService

	/**
	 * Servicio que recibe solicitudes de copias de seguridad
	 *
	 * @httpMethod [POST]
     * @serviceURL [/solicitudCopia]
     * @requestContentType [application/pdf,application/x-pkcs7-signature] Obligatorio. 
     *              El archivo PDF con los datos de la copia de seguridad.
	 * @return Si los datos son correctos el solicitante recibirá un
	 *         email con información para poder obtener la copia de seguridad.
	 */
	def index() { 
		
		try {
			
			final Documento documento = params.pdfDocument
			if (documento && documento.estado == Documento.Estado.VALIDADO) {
				PdfReader reader = new PdfReader(documento.pdf);
				AcroFields form = reader.getAcroFields();
				String eventoId = form.getField("eventoId");
				String msg = null
				
				def evento
				if(eventoId) {
					Evento.withTransaction {
						evento = Evento.get(new Long(eventoId))
					}
				} else msg = message(code: 'backupRequestEventWithoutIdErrorMsg')

				if(!evento)
					msg = message(code:'backupRequestEventIdErrorMsg', [eventoId])
				if(!evento.copiaSeguridadDisponible) 
					msg = message(code:'eventWithoutBackup', args:[evento.asunto])
				String asunto = form.getField("asunto");
				String email = form.getField("email");
				if(!email) 
					msg =  message(code:'backupRequestEmailMissingErrorMsg')
				
				if(msg) {
					documento.estado = Documento.Estado.SOLICITUD_COPIA_ERROR 
				} else documento.estado = Documento.Estado.SOLICITUD_COPIA 
			
				Documento.withTransaction {
					documento.evento = evento
					documento.save(flush:true)
				}
				if(msg) {
					params.respuesta = new Respuesta(
						codigoEstado:Respuesta.SC_ERROR_PETICION,
						mensaje:msg)
					return false
				}
				
				log.debug "backup request - eventoId: ${eventoId} - asunto: ${asunto} - email: ${email}"
				Respuesta respuestaGeneracionBackup = null
				if(params.sync && Environment.DEVELOPMENT.equals(Environment.current)) {
					respuestaGeneracionBackup = requestBackup(evento, request.locale)
					if(Respuesta.SC_OK == respuestaGeneracionBackup?.codigoEstado) {
						File archivoCopias = respuestaGeneracionBackup.file
						SolicitudCopia solicitudCopia = new SolicitudCopia(
							filePath:archivoCopias.getAbsolutePath(), 
							type:respuestaGeneracionBackup.datos.type,
							documento:documento, email:email, 
							numeroCopias:respuestaGeneracionBackup.datos.cantidad)
						SolicitudCopia.withTransaction {
							solicitudCopia.save()
						}
						params.id = solicitudCopia.id
						flash.forwarded = Boolean.TRUE
						forward action: "download"
					} else {
						log.error("DEVELOPMENT - error generating backup");
						params.respuesta = respuestaGeneracionBackup
						return false
					} 
				} else {
					final Evento event = evento
					final Locale locale = request.locale
					final String emailRequest = email
					runAsync {
						Respuesta backupResponse = requestBackup(event, locale)
						if(Respuesta.SC_OK == backupResponse?.codigoEstado) {
							File archivoCopias = backupResponse.file
							SolicitudCopia solicitudCopia = new SolicitudCopia(
								filePath:archivoCopias.getAbsolutePath(), 
								type:backupResponse.datos.type,
								documento:documento, email:emailRequest, 
								numeroCopias:backupResponse.datos.cantidad)
							SolicitudCopia.withTransaction {
								solicitudCopia.save()
							}
							mailSenderService.sendInstruccionesDescargaCopiaSeguridad(solicitudCopia, locale)
						} else log.error("Error generando archivo de copias de respaldo");
					}
					response.status = Respuesta.SC_OK
					render message(code:'backupRequestOKMsg', args:[email])
					return false
				}
				


			} else {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'error.PeticionIncorrectaHTML', args:[
					"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
				return false
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_PETICION
			render(ex.getMessage())
			return false
		}
	}
	
	private void este() {
		log.debug ("este")
	}
	
	private Respuesta requestBackup(Evento evento, Locale locale) {
		log.debug ("requestBackup")
		Respuesta respuestaGeneracionBackup
		if(evento instanceof EventoFirma) {
			respuestaGeneracionBackup = eventoFirmaService.generarCopiaRespaldo(
				(EventoFirma)evento, locale)
			log.debug("---> EventoFirma")
		} else if(evento instanceof EventoReclamacion) {
			log.debug("---> EventoReclamacion")
			respuestaGeneracionBackup = eventoReclamacionService.generarCopiaRespaldo(
				(EventoReclamacion)evento,locale)
		} else if(evento instanceof EventoVotacion) {
			log.debug("---> EventoVotacion")
			respuestaGeneracionBackup = eventoVotacionService.generarCopiaRespaldo(
				(EventoVotacion)evento, locale)
		}
		return respuestaGeneracionBackup
	}
	
	/**
	 * Servicio que proporciona la copia de seguridad a partir de la URL que se envía
	 * al solicitante en el mail de confirmación que recibe al enviar la solicitud.
	 * 
	 * @httpMethod [GET]
     * @serviceURL [/solicitudCopia/download/$id]
	 * @param [id] Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.
	 * @return Archivo zip con la copia de seguridad.
	 */
	def download() {
		SolicitudCopia solicitud
		SolicitudCopia.withTransaction {
			solicitud = SolicitudCopia.get(params.long('id'))
		}
		if(!solicitud) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'solicitudCopia.noEncontrada', args:[params.id]) 
			return false
		}
		if(!solicitud.filePath) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'backupDownloadedMsg', args:[params.id]) 
			return false
		}
		File copiaRespaldo = new File(solicitud.filePath)
		if (copiaRespaldo != null) {
			def bytesCopiaRespaldo = copiaRespaldo.getBytes()
			response.contentLength = bytesCopiaRespaldo.length
			response.setHeader("Content-disposition", "filename=${copiaRespaldo.getName()}")
			response.setHeader("NombreArchivo", "${copiaRespaldo.getName()}")
			response.setContentType("application/octet-stream")
			response.outputStream << bytesCopiaRespaldo
			response.outputStream.flush()
			solicitud.filePath = null
			solicitud.save()
			copiaRespaldo.delete()
			return false
		} else {
			log.error (message(code: 'error.SinCopiaRespaldo'))
			response.status = Respuesta.SC_ERROR
			render message(code: 'error.SinCopiaRespaldo')
			return false
		}
	}
	
	/**
	 * Servicio que proporciona copias de las solicitudes de copias de seguridad recibidas.
	 *
	 * @httpMethod [GET]
     * @serviceURL [/solicitudCopia/$id]
	 * @param [id] Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.
	 * @return El PDF en el que se solicita la copia de seguridad.
	 */
	def get() {
		SolicitudCopia solicitud
		byte[] solicitudBytes
		SolicitudCopia.withTransaction {
			solicitud = SolicitudCopia.get(params.long('id'))
			if(solicitud) {
				if(solicitud.documento) {//has PDF
					//response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
					response.contentType = "application/pdf"
					solicitudBytes = solicitud.documento?.pdf
				} else {//has SMIME
					response.contentType = "text/plain"
					solicitudBytes = solicitud.mensajeSMIME?.contenido
				}
			} 
		}
		if(!solicitud) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'solicitudCopia.noEncontrada', args:[params.id]) 
			return false
		}

		response.setHeader("Content-Length", "${solicitudBytes.length}")
		response.outputStream << solicitudBytes // Performing a binary stream copy
		response.outputStream.flush()
		return false
	}

}