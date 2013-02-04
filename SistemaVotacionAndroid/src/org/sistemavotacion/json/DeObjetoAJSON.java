/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sistemavotacion.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.modelo.*;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.util.*;

import android.util.Log;

/**
 *
 * @author jgzornoza
 */
public class DeObjetoAJSON {

	public static final String TAG = "DeObjetoAJSON";

    public static JSONObject obtenerEventoJSON(Evento evento) throws JSONException{
    	Log.d(TAG + ".obtenerEventoJSON(...)", " - obtenerEventoJSON -");
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("asunto", evento.getAsunto());
    	jsonObject.put("contenido", evento.getContenido());
    	jsonObject.put("fechaInicio", DateUtils.getStringFromDate(evento.getFechaInicio()));
    	jsonObject.put("fechaFin", DateUtils.getStringFromDate(evento.getFechaFin()));
        if (evento.getTipo() != null) jsonObject.put("tipoEvento", evento.getTipo().toString()); 
        if (evento.getEventoId() != null) jsonObject.put("eventoId", evento.getEventoId()); 
        if (evento.getEtiquetas() != null) {
            String[] etiquetas = evento.getEtiquetas();
            JSONArray jsonArray = new JSONArray();
            for (String etiqueta : etiquetas) {
                jsonArray.put(etiqueta);
            }
            jsonObject.put("etiquetas", jsonArray);
        }
        if (evento.getCentroControl() != null) {
            Map<String, Object> centroControlMap = new HashMap<String, Object>(); 
            centroControlMap.put("id", evento.getCentroControl().getId());
            centroControlMap.put("nombre", evento.getCentroControl().getNombre());
            centroControlMap.put("serverURL", evento.getCentroControl().getServerURL());
            JSONObject centroControlJSON = new JSONObject(centroControlMap);
            jsonObject.put("centroControl", centroControlJSON);
        }        
        if (evento.getOpciones() != null) {
            Set<OpcionDeEvento> opciones = evento.getOpciones();
            JSONArray jsonArray = new JSONArray();
            Map<String, Object> opcionMap = new HashMap<String, Object>(); 
            for (OpcionDeEvento opcion : opciones) {
            	opcionMap.put("id", opcion.getId());
            	opcionMap.put("contenido", opcion.getContenido());
            	JSONObject opcionJSON = new JSONObject(opcionMap);
            	jsonArray.put(opcionJSON);
            }
            jsonObject.put("opciones", jsonArray);
        }
        if (evento.getCampos() != null) {
            Set<CampoDeEvento> campos = evento.getCampos();
            JSONArray jsonArray = new JSONArray();
            for (CampoDeEvento campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("contenido", campo.getContenido());
                campoMap.put("valor", campo.getValor());
                campoMap.put("id", campo.getId());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        if (evento.getTipoEleccion() != null) jsonObject.put(
        		"cardinalidad", evento.getTipoEleccion().toString()); 
        if (evento.getOpcionSeleccionada() != null) {
            Map<String, Object> opcionSeleccionadaMap = new HashMap<String, Object>(); 
            opcionSeleccionadaMap.put("id", evento.getOpcionSeleccionada().getId());
            opcionSeleccionadaMap.put("contenido", evento.getOpcionSeleccionada().getContenido());
            JSONObject opcionSeleccionadaJSON = new JSONObject(opcionSeleccionadaMap);
            jsonObject.put("opcionSeleccionada", opcionSeleccionadaJSON);
        } 
        if(evento.getHashSolicitudAccesoBase64() != null)
        	jsonObject.put("hashSolicitudAccesoBase64", evento.getHashSolicitudAccesoBase64()); 
        if(evento.getOrigenHashSolicitudAcceso() != null)
        	jsonObject.put("origenHashSolicitudAcceso", evento.getOrigenHashSolicitudAcceso()); 
        if(evento.getHashCertificadoVotoBase64() != null)
        	jsonObject.put("hashCertificadoVotoBase64", evento.getHashCertificadoVotoBase64());        
        if(evento.getOrigenHashCertificadoVoto() != null)
        	jsonObject.put("origenHashCertificadoVoto", evento.getOrigenHashCertificadoVoto());
        return jsonObject;    
    }

    public static String obtenerEventoJSONString(Evento evento) throws JSONException{
    	JSONObject eventoJSON = obtenerEventoJSON(evento);
    	if(eventoJSON == null) return null;
    	return eventoJSON.toString();
    }
    
    
    public static String obtenerDatosBusqueda(DatosBusqueda datosBusqueda) {
    	Log.d(TAG + ".obtenerDatosBusqueda(...)", " - obtenerDatosBusqueda");
    	Map<String, Object> map = new HashMap<String, Object>();
    	if(datosBusqueda.getTipo() != null)
    		map.put("tipo", datosBusqueda.getTipo().toString());
    	if(datosBusqueda.getEstadoEvento() != null)
    		map.put("estadoEvento",datosBusqueda.getEstadoEvento().toString());
    	if(datosBusqueda.getTextQuery() != null)
    		map.put("textQuery",datosBusqueda.getTextQuery());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }
    
   public static String obtenerVotoParaEventoJSON(Evento evento) {
   		Log.d(TAG + ".obtenerVotoParaEventoJSON(...)", " - obtenerVotoParaEventoJSON");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("eventoURL", ServerPaths.getURLEventoParaVotar(
        		Aplicacion.CONTROL_ACCESO_URL,
                evento.getEventoId().toString()));
        map.put("opcionSeleccionadaId", evento.getOpcionSeleccionada().getId());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }
    
   public static String obtenerFirmaParaEventoJSON(Evento evento) throws JSONException {
  		Log.d(TAG + ".obtenerFirmaParaEventoJSON(...)", "");
        Map<String, Object> map = new HashMap<String, Object>();
        if(evento.getControlAcceso() != null) {
        	Map<String, Object> controlAccesoMap = new HashMap<String, Object>();
            controlAccesoMap.put("serverURL", evento.getControlAcceso().getServerURL());
            controlAccesoMap.put("nombre", evento.getControlAcceso().getNombre());
            map.put("controlAcceso", controlAccesoMap);
        }
        map.put("eventoId", evento.getEventoId());
        map.put("asunto", evento.getAsunto());
        map.put("contenido", evento.getContenido());
        JSONObject jsonObject = new JSONObject(map);
        if (evento.getCampos() != null) {
            Set<CampoDeEvento> campos = evento.getCampos();
            JSONArray jsonArray = new JSONArray();
            for (CampoDeEvento campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("id", campo.getId());
                campoMap.put("contenido", campo.getContenido());
                campoMap.put("valor", campo.getValor());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        return jsonObject.toString();
    }
   
    public static String obtenerSolicitudAccesoJSON(Evento evento) {
  		Log.d(TAG + ".obtenerSolicitudAccesoJSON(...)", "");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("eventoURL", ServerPaths.getURLEventoParaVotar(
        		Aplicacion.CONTROL_ACCESO_URL, evento.getEventoId().toString()));
        map.put("hashSolicitudAccesoBase64", evento.getHashSolicitudAccesoBase64());
        JSONObject jsonObject = new JSONObject(map);        
         return jsonObject.toString();
    }
    
    public String obtenerComentarioJSON (Comentario comentario) {return null;}
    
    public String obtenerMensajeJSON (Mensaje mensaje) {return null;}
    
}
