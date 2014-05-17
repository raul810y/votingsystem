package org.votingsystem.client.dialog;

import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;

import javax.swing.*;
import java.awt.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MessageDialog extends javax.swing.JDialog {

    private static Logger logger = Logger.getLogger(MessageDialog.class); 
    
    
    private Container container;
    private JEditorPane editorPane;
    
    public MessageDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        pack();
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        logger.debug("initComponents");
        container = getContentPane();   
        container.setLayout(new MigLayout("fill"));
        JScrollPane scrollPane = new JScrollPane();
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        //editorPane.setLineWrap(true);
        //editorPane.setWrapStyleWord(true);
        editorPane.setContentType("text/html");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        editorPane.setBackground(java.awt.Color.white);
        scrollPane.setViewportView(editorPane);

        JButton cancelButton = new JButton(ContextVS.getMessage("closeLbl"));
        cancelButton.setIcon(ContextVS.getIcon(this, "cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) { dispose();}
        });
        
        container.add(scrollPane, "width 600::, height 400::, grow, wrap");
        container.add(cancelButton, "width :150:, align right");
    }
    
    public void showMessage (String message, String caption) {
        logger.debug("--- showMessage - showMessage: " + message + " - caption: " + caption);
        if (caption != null) setTitle(caption);
        if (message != null) editorPane.setText(message);
        editorPane.updateUI();
        setVisible(true);
    }
    
    public static void main(String args[]) {

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    ContextVS.init(null, "log4j.properties", "messages_", "es");
                    final MessageDialog dialog = new MessageDialog(new javax.swing.JFrame(), true);
                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            dialog.dispose();
                        }
                    });
                    dialog.showMessage("Message", "Caption");
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }

    
}