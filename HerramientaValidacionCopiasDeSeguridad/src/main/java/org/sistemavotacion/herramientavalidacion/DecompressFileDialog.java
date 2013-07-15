package org.sistemavotacion.herramientavalidacion;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.swing.UIManager;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DecompressFileDialog extends javax.swing.JDialog {
    
    private static Logger logger = LoggerFactory.getLogger(DecompressFileDialog.class);

    private Future<Respuesta> runningTask;
    private final AtomicBoolean unzipCancelled = new  AtomicBoolean(false);

    public DecompressFileDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
            }
        });
        setTitle(Contexto.INSTANCE.getString("decompressBackupCaption"));
        progressBar.setStringPainted(true);
        setLocationRelativeTo(null);
    }

    public boolean unZipBackup(final String zipFilePath, final String outputFolder){
        runningTask = Contexto.INSTANCE.submit(new Callable() {
            @Override
            public Respuesta call() throws Exception {
                return unZip(zipFilePath, outputFolder);
            }
        });
        setVisible(true);
        boolean backupUnzipped = !unzipCancelled.get();
        logger.debug("unZipBackup - backupUnzipped: " + backupUnzipped);
        return backupUnzipped;
    }
    
    /**
    * Unzip it
    * @param zipFile input zip file
    * @param output zip file output folder
    */
    private Respuesta unZip(String zipFilePath, String outputFolder){
        logger.debug("unZipIt: " + zipFilePath + " - outputFolder: " + 
                outputFolder);
        backupFileLabel.setText(Contexto.INSTANCE.getString(
                "decompressProgressBarLabel", zipFilePath));
        byte[] buffer = new byte[2048];
        try{
            File folder = new File(outputFolder);
            if(!folder.exists()) folder.mkdir();
            ZipFile zipFile = new ZipFile(zipFilePath);
            int zipFileSize = zipFile.size();
            progressBar.setMaximum(zipFileSize);
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry zipEntry = zis.getNextEntry();
            int fileIndex = 0;
            while( zipEntry != null && !unzipCancelled.get()){
                fileIndex++;
                progressBar.setValue(fileIndex);
                String msg = Contexto.INSTANCE.getString(
                        "decompressProgressBarMsg", fileIndex, zipFileSize);
                progressBar.setString(msg);
                String fileName = zipEntry.getName();
                File newFile = new File(outputFolder + File.separator + fileName);
                if(zipEntry.isDirectory()) {
                    newFile.mkdirs();
                    logger.debug("mkdirs : "+ newFile.getAbsoluteFile());
                } else {
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);             
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();   
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch(IOException ex){
            logger.error(ex.getMessage(), ex);
            setVisible(false);
            return new Respuesta(Respuesta.SC_ERROR);
        } 
        setVisible(false);
        return new Respuesta(Respuesta.SC_OK);
    }   
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        backupFileLabel = new javax.swing.JLabel();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/herramientavalidacion/Bundle"); // NOI18N
        backupFileLabel.setText(bundle.getString("DecompressFileDialog.backupFileLabel.text")); // NOI18N

        cancelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/cancel_16x16.png"))); // NOI18N
        cancelButton.setText(bundle.getString("DecompressFileDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLayout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 483, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(backupFileLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 483, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cancelButton, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                .addContainerGap(16, Short.MAX_VALUE)
                .addComponent(backupFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        logger.debug(" - runningTask.cancel: " + runningTask.cancel(true));
        unzipCancelled.set(true);
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        final DecompressFileDialog dialog = new DecompressFileDialog(new javax.swing.JFrame(), true);
        Contexto.INSTANCE.init();
        String zipFile = "./representative_00000001R.zip";
        String outputFolder = Contexto.DEFAULTS.APPTEMPDIR + 
            File.separator + UUID.randomUUID();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    logger.debug("run");
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    dialog.setVisible(true);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        dialog.unZipBackup(zipFile, outputFolder);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backupFileLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel panel;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}