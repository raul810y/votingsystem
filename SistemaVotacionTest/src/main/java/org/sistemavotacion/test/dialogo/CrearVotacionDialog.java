package org.sistemavotacion.test.dialogo;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import net.miginfocom.swing.MigLayout;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.modelo.Tipo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CrearVotacionDialog extends JDialog implements KeyListener {

    private static Logger logger = LoggerFactory.getLogger(CrearVotacionDialog.class);
    
    private static BlockingQueue<Future<Respuesta>> queue = 
        new LinkedBlockingQueue<Future<Respuesta>>(3);
    
    private static final String IPADDRESS_PATTERN = 
		"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private JFrame parentFrame;
    private boolean mostrandoPantallaEnvio = false;
    private Border normalTextBorder;
    private Border opcionesBorder;
    private Evento evento;
    private static final String ASUNTO_VOTACION = "Nueva Votación";
    private Future<Respuesta> tareaEnEjecucion;
    private final AtomicBoolean done = new AtomicBoolean(false);
    
    /**
     * Creates new form CrearVotacionDialog
     */
    public CrearVotacionDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.parentFrame = (JFrame)parent;
        setLocationRelativeTo(null);
        initComponents();
        //DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        fechaInicioDatePicker.setDate(DateUtils.getNextDayRoundedDate(1));
        fechaFinalDatePicker.setDate(DateUtils.getNextDayRoundedDate(2));
        fechaInicioDatePicker.setFormats(formatter);
        fechaFinalDatePicker.setFormats(formatter);      
        opcionesPanel.setLayout(new MigLayout());
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                done.set(true);
                if (tareaEnEjecucion != null) {
                    tareaEnEjecucion.cancel(true);
                }
            }
            public void windowClosing(WindowEvent e) { 
                done.set(true);
            }
        });
        progressBar.setIndeterminate(true);
        progressBarPanel.setVisible(false);
        evento = new Evento();
        validacionPanel.setVisible(false);
        normalTextBorder = etiquetasTextField.getBorder();
        opcionesBorder = opcionesPanel.getBorder();
        asuntoTextField.addKeyListener(this);
        editorPane.addKeyListener(this);
        etiquetasTextField.addKeyListener(this);
        ContextoPruebas.INSTANCE.submit(new Runnable() {
            @Override public void run() {
                try {
                    readFutures();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        pack();
    }
        
    public void readFutures () {
        logger.debug(" - readFutures");
        while (!done.get()) {
            try {
                Future<Respuesta> future = queue.take();
                Respuesta respuesta = future.get();
                if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    try {
                        byte[] responseBytes = respuesta.getMessageBytes();
                        FileUtils.copyStreamToFile(new ByteArrayInputStream(responseBytes), 
                            new File(ContextoPruebas.DEFAULTS.APPDIR + "VotingPublishReceipt"));
                        SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null, 
                                new ByteArrayInputStream(responseBytes), 
                                "VotingPublishReceipt");
                        dnieMimeMessage.verify(
                                ContextoPruebas.INSTANCE.getSessionPKIXParameters());
                        logger.debug("--- dnieMimeMessage.getSignedContent(): " + dnieMimeMessage.getSignedContent());
                        evento = Evento.parse(dnieMimeMessage.getSignedContent());
                        logger.debug("Respuesta - Evento ID: " + evento.getEventoId());
                        ContextoPruebas.INSTANCE.setEvento(evento);
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                try {
                                    dispose();
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        });
                        
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        MensajeDialog errorDialog = new MensajeDialog(parentFrame, true);
                        errorDialog.setMessage(ex.getMessage(), "Error");
                        dispose();
                    }
                } else {
                    mostrarPantallaEnvio(false);
                    mostrarMensajeUsuario("ERROR - " + respuesta.getMensaje());
                } 
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        validacionPanel = new javax.swing.JPanel();
        mensajeValidacionLabel = new javax.swing.JLabel();
        closePanelLabel = new javax.swing.JLabel();
        formularioPanel = new javax.swing.JPanel();
        asuntoLabel = new javax.swing.JLabel();
        asuntoTextField = new javax.swing.JTextField();
        contenidoLabel = new javax.swing.JLabel();
        scrollPane = new javax.swing.JScrollPane();
        editorPane = new javax.swing.JEditorPane();
        fechaInicioLabel = new javax.swing.JLabel();
        fechaInicioDatePicker = new org.jdesktop.swingx.JXDatePicker();
        fechaFinalLabel = new javax.swing.JLabel();
        fechaFinalDatePicker = new org.jdesktop.swingx.JXDatePicker();
        opcionesVotacionPanel = new javax.swing.JPanel();
        crearOpcionVotacionButton = new javax.swing.JButton();
        opcionesPanel = new org.sistemavotacion.test.panel.OpcionesPanel();
        etiquetasLabel = new javax.swing.JLabel();
        etiquetasTextField = new javax.swing.JTextField();
        progressBarPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        progressLabel = new javax.swing.JLabel();
        cerrarButton = new javax.swing.JButton();
        publicarButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Publicar convocatoria de elección");

        validacionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        mensajeValidacionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        closePanelLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/close.gif"))); // NOI18N
        closePanelLabel.setText(" ");
        closePanelLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                closeMensajeUsuario(evt);
            }
        });

        javax.swing.GroupLayout validacionPanelLayout = new javax.swing.GroupLayout(validacionPanel);
        validacionPanel.setLayout(validacionPanelLayout);
        validacionPanelLayout.setHorizontalGroup(
            validacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(validacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mensajeValidacionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(closePanelLabel))
        );
        validacionPanelLayout.setVerticalGroup(
            validacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mensajeValidacionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
            .addGroup(validacionPanelLayout.createSequentialGroup()
                .addComponent(closePanelLabel)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        asuntoLabel.setText("Asunto");

        contenidoLabel.setText("Contenido");

        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        editorPane.setBackground(java.awt.Color.white);
        scrollPane.setViewportView(editorPane);

        fechaInicioLabel.setText("Fecha de inicio:");

        fechaFinalLabel.setText("Fecha final:");

        fechaFinalDatePicker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fechaFinalDatePickerActionPerformed(evt);
            }
        });

        opcionesVotacionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Opciones de Votación"));

        crearOpcionVotacionButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/edit_add_16x16.png"))); // NOI18N
        crearOpcionVotacionButton.setText("Añadir Opción de Votación");
        crearOpcionVotacionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                crearOpcionVotacionButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout opcionesVotacionPanelLayout = new javax.swing.GroupLayout(opcionesVotacionPanel);
        opcionesVotacionPanel.setLayout(opcionesVotacionPanelLayout);
        opcionesVotacionPanelLayout.setHorizontalGroup(
            opcionesVotacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(opcionesVotacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(opcionesVotacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(opcionesVotacionPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(crearOpcionVotacionButton))
                    .addComponent(opcionesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        opcionesVotacionPanelLayout.setVerticalGroup(
            opcionesVotacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(opcionesVotacionPanelLayout.createSequentialGroup()
                .addComponent(crearOpcionVotacionButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(opcionesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                .addContainerGap())
        );

        etiquetasLabel.setText("Etiquetas (separadas por comas):");

        javax.swing.GroupLayout formularioPanelLayout = new javax.swing.GroupLayout(formularioPanel);
        formularioPanel.setLayout(formularioPanelLayout);
        formularioPanelLayout.setHorizontalGroup(
            formularioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(formularioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(formularioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(opcionesVotacionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(etiquetasTextField)
                    .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(asuntoTextField)
                    .addGroup(formularioPanelLayout.createSequentialGroup()
                        .addGroup(formularioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(etiquetasLabel)
                            .addComponent(asuntoLabel)
                            .addComponent(contenidoLabel))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, formularioPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(fechaInicioLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fechaInicioDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(fechaFinalLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fechaFinalDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        formularioPanelLayout.setVerticalGroup(
            formularioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, formularioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(asuntoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(asuntoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(contenidoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(formularioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fechaInicioLabel)
                    .addComponent(fechaInicioDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fechaFinalLabel)
                    .addComponent(fechaFinalDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(opcionesVotacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(etiquetasLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(etiquetasTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        progressLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        progressLabel.setText("<html><b>Enviando la información ... </b><html>");

        javax.swing.GroupLayout progressBarPanelLayout = new javax.swing.GroupLayout(progressBarPanel);
        progressBarPanel.setLayout(progressBarPanelLayout);
        progressBarPanelLayout.setHorizontalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        progressBarPanelLayout.setVerticalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cerrarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/cancel_16x16.png"))); // NOI18N
        cerrarButton.setText("Cerrar");
        cerrarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cerrarButtonActionPerformed(evt);
            }
        });

        publicarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/mail_send_16x16.png"))); // NOI18N
        publicarButton.setText("Publicar");
        publicarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                publicarButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(formularioPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(validacionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressBarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(publicarButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cerrarButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(validacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(formularioPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cerrarButton)
                    .addComponent(publicarButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fechaFinalDatePickerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fechaFinalDatePickerActionPerformed

}//GEN-LAST:event_fechaFinalDatePickerActionPerformed

        private void crearOpcionVotacionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_crearOpcionVotacionButtonActionPerformed
        OpcionVotacionDialog opcionVotacionDialog = 
                new OpcionVotacionDialog(parentFrame, true);
        opcionVotacionDialog.setVisible(true);
        OpcionEvento opcionEvento = opcionVotacionDialog.getOpcionEvento();
        if (opcionEvento != null) {
            opcionesPanel.setBorder(normalTextBorder);
            opcionesPanel.anyadirOpcion(opcionEvento.getContenido());
            this.pack();
        }
    }//GEN-LAST:event_crearOpcionVotacionButtonActionPerformed

    private void cerrarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cerrarButtonActionPerformed
        if(mostrandoPantallaEnvio) {
            tareaEnEjecucion.cancel(true);
            mostrandoPantallaEnvio = false;
            formularioPanel.setVisible(true);
            progressBarPanel.setVisible(false);
            pack();
        } else dispose();
    }//GEN-LAST:event_cerrarButtonActionPerformed

    private void publicarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_publicarButtonActionPerformed
        if (!validarFormulario()) return;
        try {
            SignedMailGenerator signedMailGenerator = null;
            evento.setAsunto(asuntoTextField.getText());
            evento.setContenido(editorPane.getText());
            evento.setFechaInicio(fechaInicioDatePicker.getDate());
            evento.setFechaFin(fechaFinalDatePicker.getDate());
            evento.setOpciones(opcionesPanel.obtenerOpciones());
            evento.setTipo(Tipo.VOTACION);
            String[] etiquetas = etiquetasTextField.getText().split(",");
            evento.setEtiquetas(etiquetas);
            evento.setCentroControl(ContextoPruebas.INSTANCE.getControlCenter());
            signedMailGenerator = new SignedMailGenerator(
                ContextoPruebas.INSTANCE.getUserTest().getKeyStore(),
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.DEFAULTS.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
            
            String eventoParaPublicar = evento.toJSON().toString();
            SMIMEMessageWrapper smimeDocument = signedMailGenerator.genMimeMessage(
                    ContextoPruebas.INSTANCE.getUserTest().getEmail(), 
                    Contexto.INSTANCE.getAccessControl().getNombreNormalizado(), 
                    eventoParaPublicar, "Solicitud Publicación convocatoria",
                    null);
            
            SMIMESignedSender worker = new SMIMESignedSender(null, 
                smimeDocument, ContextoPruebas.INSTANCE. getURLGuardarEventoParaVotar(), 
                null, null);
            Future<Respuesta> future = ContextoPruebas.INSTANCE.submit(worker);
            mostrarPantallaEnvio(true);
            tareaEnEjecucion = future;
            queue.put(future);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }//GEN-LAST:event_publicarButtonActionPerformed

    private void closeMensajeUsuario(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_closeMensajeUsuario
        mostrarMensajeUsuario(null);
    }//GEN-LAST:event_closeMensajeUsuario

    @Override
    public void keyTyped(KeyEvent ke) { }

    @Override
    public void keyPressed(KeyEvent ke) {
        int key = ke.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            Toolkit.getDefaultToolkit().beep();
            publicarButton.doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) { }
    
    public boolean validarFormulario() {
        boolean errores = false;
        Date fechaInicio = fechaInicioDatePicker.getDate();
        Date fechaFinal = fechaFinalDatePicker.getDate();
        asuntoTextField.setBorder(normalTextBorder);
        editorPane.setBorder(normalTextBorder);
        fechaInicioDatePicker.setBorder(normalTextBorder);
        fechaFinalDatePicker.setBorder(normalTextBorder);
        opcionesPanel.setBorder(opcionesBorder);
        if (asuntoTextField.getText().trim() == null ||
                "".equals(asuntoTextField.getText().trim())) {
            mensajeValidacionLabel.setText("<html>El campo <b>Asunto</b> no puede ir vacío</html>");
            asuntoTextField.setBorder(new LineBorder(Color.RED,2));
            errores = true;
        } else if (asuntoTextField.getText().length() > Contexto.MAXIMALONGITUDCAMPO) {
                mensajeValidacionLabel.setText("<html>El campo <b>Asunto</b> "
                        + "no puede tener una tamaño de más de "
                        + Contexto.MAXIMALONGITUDCAMPO + " caracteres</html>");
                asuntoTextField.setBorder(new LineBorder(Color.RED,2));
                errores = true;
        }
        else if (!(fechaFinal.compareTo(fechaInicio)> 0)) {
            if (fechaFinal.compareTo(fechaInicio) == 0) {
                mensajeValidacionLabel.setText("<html>Las fechas de comienzo y final de votación no pueden coincidir</html>");
                fechaInicioDatePicker.setBorder(new LineBorder(Color.RED,2));
                fechaFinalDatePicker.setBorder(new LineBorder(Color.RED,2));
                errores = true;
            }
            if (fechaFinal.compareTo(fechaInicio) < 0) {
                mensajeValidacionLabel.setText("<html>Las fecha de final de votación no puede ser menor que la fecha de inicio</html>");
                fechaInicioDatePicker.setBorder(new LineBorder(Color.RED,2));
                fechaFinalDatePicker.setBorder(new LineBorder(Color.RED,2));
                errores = true;
            }
        }
        if (!errores && (editorPane.getText().trim() == null ||
                "".equals(editorPane.getText().trim()))) {
            mensajeValidacionLabel.setText(
                    "<html>El campo no puede ir vacío</html>");
            editorPane.setBorder(new LineBorder(Color.RED,2));
            errores = true;
        }
        if (!errores && opcionesPanel.obtenerOpciones().size() < 2) {
             mensajeValidacionLabel.setText("<html>Para que haya una votación "
                     + "debe haber al menos <b>2 opciones</b></html>");
             opcionesPanel.setBorder(new LineBorder(Color.RED,2));
             errores = true;
        }
        if (errores)validacionPanel.setVisible(true);
        else validacionPanel.setVisible(false);
        pack();
        return !errores;
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CrearVotacionDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CrearVotacionDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CrearVotacionDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CrearVotacionDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the dialog
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                CrearVotacionDialog dialog = new CrearVotacionDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel asuntoLabel;
    private javax.swing.JTextField asuntoTextField;
    private javax.swing.JButton cerrarButton;
    private javax.swing.JLabel closePanelLabel;
    private javax.swing.JLabel contenidoLabel;
    private javax.swing.JButton crearOpcionVotacionButton;
    private javax.swing.JEditorPane editorPane;
    private javax.swing.JLabel etiquetasLabel;
    private javax.swing.JTextField etiquetasTextField;
    private org.jdesktop.swingx.JXDatePicker fechaFinalDatePicker;
    private javax.swing.JLabel fechaFinalLabel;
    private org.jdesktop.swingx.JXDatePicker fechaInicioDatePicker;
    private javax.swing.JLabel fechaInicioLabel;
    private javax.swing.JPanel formularioPanel;
    private javax.swing.JLabel mensajeValidacionLabel;
    private org.sistemavotacion.test.panel.OpcionesPanel opcionesPanel;
    private javax.swing.JPanel opcionesVotacionPanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel progressBarPanel;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JButton publicarButton;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JPanel validacionPanel;
    // End of variables declaration//GEN-END:variables

    
    private void mostrarPantallaEnvio(boolean mostrar) {
        mostrandoPantallaEnvio = mostrar;
        formularioPanel.setVisible(!mostrar);
        progressBarPanel.setVisible(mostrar);
        publicarButton.setVisible(!mostrar);
        pack();
    }
    
    public void mostrarMensajeUsuario(String mensaje) {
        if(mensaje == null) {
            validacionPanel.setVisible(false);
        }else {
            mensajeValidacionLabel.setText(mensaje);
            validacionPanel.setVisible(true);
        }
        pack();
    }

}
