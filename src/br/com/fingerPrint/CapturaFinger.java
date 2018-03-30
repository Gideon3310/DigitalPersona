package br.com.fingerPrint;
	
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPErrorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPErrorEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;

/**
 *
 * @author max davis
 */

public class CapturaFinger extends javax.swing.JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Creates new form CapturaFinger
	 */

	// Variaveis de Digital Persona
	private DPFPCapture leitor = DPFPGlobal.getCaptureFactory().createCapture();
	private DPFPEnrollment recrutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
	private DPFPVerification verificador = DPFPGlobal.getVerificationFactory().createVerification();
	private DPFPTemplate template;
	public static String TEMPLATE_PROPERTY = "template";

	public DPFPFeatureSet featuresIncripcion;
	public DPFPFeatureSet featuresVerificacion;
	
	
	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnGuardar;
	private javax.swing.JButton btnIdentificar;
	private javax.swing.JButton btnSair;
	private javax.swing.JButton btnVerificar;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JLabel lblImagenDigital;
	private javax.swing.JPanel panBtns;
	private javax.swing.JPanel panDigital;
	private javax.swing.JTextArea textArea;
	// End of variables declaration//GEN-END:variables

	ConexionBD cn = new ConexionBD();

	public DPFPFeatureSet extrairCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose) {
		DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
		try {
			return extractor.createFeatureSet(sample, purpose);
		} catch (DPFPImageQualityException e) {
			return null;
		}
	}

	public Image criarImagemDigital(DPFPSample sample) {
		return DPFPGlobal.getSampleConversionFactory().createImage(sample);
	}

	public void desenharDigital(Image image) {
		lblImagenDigital.setIcon(new ImageIcon(
				image.getScaledInstance(lblImagenDigital.getWidth(), lblImagenDigital.getHeight(), image.SCALE_DEFAULT)));
		repaint();
	}

	public void estadoDaDigital() {
		enviarTexto("É necessário colocar o seu finger para salvar a impressão" + recrutador.getFeaturesNeeded());
	}

	public void enviarTexto(String string) {
		textArea.append(string + "\n");
	}

	public void start() {
		leitor.startCapture();
		enviarTexto("Usando o leitor de impressão digital");
	}

	public void stop() {
		leitor.stopCapture();
		enviarTexto("O leitor de impressões digitais não está sendo usado");
	}

	public DPFPTemplate getTemplate() {
		return template;
	}

	public void setTemplate(DPFPTemplate template) {
		DPFPTemplate old = this.template;
		this.template = template;
		firePropertyChange(TEMPLATE_PROPERTY, old, template);
	}

	// Metodo para procesar a captura da digital
	public void ProcesarCaptura(DPFPSample sample) {
		
		featuresIncripcion = extrairCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);

		featuresVerificacion = extrairCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);

		if (featuresIncripcion != null)
			try {
				System.out.println("As características da digital foram criadas");
				recrutador.addFeatures(featuresIncripcion);
				
				// Desenhar Digital
				Image image = criarImagemDigital(sample);
				desenharDigital(image);

				btnVerificar.setEnabled(true);
				btnIdentificar.setEnabled(true);

			} catch (DPFPImageQualityException ex) {
				System.out.println("Error: " + ex.getMessage());
			} finally {
				estadoDaDigital();
				
				switch (recrutador.getTemplateStatus()) {
				case TEMPLATE_STATUS_READY: 
					stop();
					setTemplate(recrutador.getTemplate());
					enviarTexto("O modelo da digital foi criado, você pode verificá-lo ou identificá-lo");
					btnIdentificar.setEnabled(false);
					btnVerificar.setEnabled(false);
					btnGuardar.setEnabled(true);
					btnGuardar.grabFocus();
					break;

				case TEMPLATE_STATUS_FAILED: 
					recrutador.clear();
					stop();
					estadoDaDigital();
					setTemplate(null);
					JOptionPane.showMessageDialog(CapturaFinger.this,
							"O modelo da digital não pôde ser criado, repita o processo");
					start();
					break;

				}

			}

	}

	protected void Iniciar() {
		leitor.addDataListener(new DPFPDataAdapter() {
			@Override
			public void dataAcquired(final DPFPDataEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						enviarTexto("A impressão digital foi capturada");
						ProcesarCaptura(e.getSample());
					}
				});
			}
		});

		leitor.addReaderStatusListener(new DPFPReaderStatusAdapter() {
			@Override
			public void readerConnected(final DPFPReaderStatusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						enviarTexto("O sensor de impressão digital está ligado ou conectado");
					}
				});
			}

			@Override
			public void readerDisconnected(final DPFPReaderStatusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						enviarTexto("O sensor de impressão digital está ligado ou conectado");
					}
				});
			}
		});

		leitor.addSensorListener(new DPFPSensorAdapter() {
			@Override
			public void fingerTouched(final DPFPSensorEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						enviarTexto("O dedo foi colocado no leitor de impressões digitais");
					}
				});
			}

			@Override
			public void fingerGone(final DPFPSensorEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						enviarTexto("O dedo foi removido do leitor de impressões digitais");
					}
				});
			}
		});

		leitor.addErrorListener(new DPFPErrorAdapter() {
			public void errorReader(final DPFPErrorEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						enviarTexto("Error: " + e.getError());
					}
				});
			}
		});
	}

	public CapturaFinger() {
	        try {
	            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	            
	        } catch(Exception e){
	            JOptionPane.showMessageDialog(null,"Impossível modificar o tema visual.","LookandFeel Invalidos.",JOptionPane.ERROR_MESSAGE);
	        }
	            
	        initComponents();
	    }

	public void guardarDigital() throws SQLException {
		
		ByteArrayInputStream dadosDigital = new ByteArrayInputStream(template.serialize());
		Integer tamaDigital = template.serialize().length;

		String nome = JOptionPane.showInputDialog("Nome:");
		try {
			
			Connection c = cn.connectar();
			System.out.println(c.getSchema());
			PreparedStatement guardarStmt = c.prepareStatement("INSERT INTO finger(nome, digital) values(?,?)");
			guardarStmt.setString(1, nome);
			guardarStmt.setBinaryStream(2, dadosDigital, tamaDigital);

			guardarStmt.execute();
			guardarStmt.close();
			JOptionPane.showMessageDialog(null, "Digital salva corretamente.");
			cn.deconectar();
			btnGuardar.setEnabled(false);
			btnVerificar.grabFocus();

		} catch (SQLException ex) {
			
			System.out.println("Erro ao salvar os dados da impressão digital.");
			System.out.println(ex);
			JOptionPane.showMessageDialog(null, "Erro ao salvar os dados da impressão digital.");
		} finally {
			cn.deconectar();
		}
	}

	public void verificarDigital(String nom) {
		
		try {
			
			Connection c = cn.connectar();
			
			PreparedStatement verificarStmt = c.prepareStatement("SELECT digital FROM finger WHERE nome = ?");
			verificarStmt.setString(1, nom);
			ResultSet rs = verificarStmt.executeQuery();

			if (rs.next()) {
				
				byte templateBuffer[] = rs.getBytes("digital");
				
				DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
				
				setTemplate(referenceTemplate);

				DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());

				if (result.isVerified())
					JOptionPane.showMessageDialog(null, "Os traços capturados coincidem com os de " + nom,
							"Verificação da Digital", JOptionPane.INFORMATION_MESSAGE);
				else
					JOptionPane.showMessageDialog(null, "A Digital não corresponde " + nom, "Verificação de Digital",
							JOptionPane.ERROR_MESSAGE);
				
			} else {
				JOptionPane.showMessageDialog(null, "Não existe un registro da digital para " + nom,
						"Verificação da Digital", JOptionPane.ERROR_MESSAGE);
			}
		} catch (SQLException ex) {
			System.out.println("Error: " + ex);
			JOptionPane.showMessageDialog(null, "Erro ao verificar dados da Digital.");
		} finally {
			cn.deconectar();
		}
	}

	public void identificarDigital() throws IOException {
		try {
			Connection c = cn.connectar();

			PreparedStatement identificarStmt = c.prepareStatement("SELECT nome, digital FROM finger");
			ResultSet rs = identificarStmt.executeQuery();

			while (rs.next()) {
				byte templateBuffer[] = rs.getBytes("digital");
				String nome = rs.getString("nome");
				
				DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);

				setTemplate(referenceTemplate);
				DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
				
				if (result.isVerified()) {
					
					JOptionPane.showMessageDialog(null, "A Digital capturada " + nome, "Verificação da Digital",
							JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}
			
			JOptionPane.showMessageDialog(null, "Não há registro que corresponda com a Digital.",
					"Verificar a Digital", JOptionPane.ERROR_MESSAGE);
			setTemplate(null);
		} catch (SQLException e) {
			System.out.println("Error: " + e.getMessage());
			JOptionPane.showMessageDialog(null, "Erro ao identificar a impressão digital. " + e.getMessage());
		} finally {
			cn.deconectar();
		}

	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		panDigital = new javax.swing.JPanel();
		lblImagenDigital = new javax.swing.JLabel();
		panBtns = new javax.swing.JPanel();
		btnVerificar = new javax.swing.JButton();
		btnGuardar = new javax.swing.JButton();
		btnIdentificar = new javax.swing.JButton();
		btnSair = new javax.swing.JButton();
		jPanel1 = new javax.swing.JPanel();
		jScrollPane1 = new javax.swing.JScrollPane();
		textArea = new javax.swing.JTextArea();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				formWindowClosing(evt);
			}

			public void windowOpened(java.awt.event.WindowEvent evt) {
				formWindowOpened(evt);
			}
		});

		panDigital.setBorder(javax.swing.BorderFactory.createTitledBorder(
				new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Impressão Digital",
				javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

		lblImagenDigital.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

		javax.swing.GroupLayout panDigitalLayout = new javax.swing.GroupLayout(panDigital);
		panDigital.setLayout(panDigitalLayout);
		panDigitalLayout.setHorizontalGroup(panDigitalLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panDigitalLayout.createSequentialGroup().addContainerGap()
						.addComponent(lblImagenDigital, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
						.addContainerGap()));
		panDigitalLayout.setVerticalGroup(panDigitalLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panDigitalLayout.createSequentialGroup().addContainerGap()
						.addComponent(lblImagenDigital, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
						.addContainerGap()));

		panBtns.setBorder(javax.swing.BorderFactory.createTitledBorder(
				new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Ações",
				javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

		btnVerificar.setText("Verificar");
		btnVerificar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
		btnVerificar.setContentAreaFilled(false);
		btnVerificar.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnVerificarActionPerformed(evt);
			}
		});

		btnGuardar.setText("Salvar");
		btnGuardar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
		btnGuardar.setContentAreaFilled(false);
		btnGuardar.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnGuardarActionPerformed(evt);
			}
		});

		btnIdentificar.setText("Identificar");
		btnIdentificar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
		btnIdentificar.setContentAreaFilled(false);
		btnIdentificar.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnIdentificarActionPerformed(evt);
			}
		});

		btnSair.setText("Sair");
		btnSair.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
		btnSair.setContentAreaFilled(false);
		btnSair.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSairActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout panBtnsLayout = new javax.swing.GroupLayout(panBtns);
		panBtns.setLayout(panBtnsLayout);
		panBtnsLayout.setHorizontalGroup(panBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panBtnsLayout.createSequentialGroup().addContainerGap()
						.addComponent(btnVerificar, javax.swing.GroupLayout.PREFERRED_SIZE, 55,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(48, 48, 48).addComponent(btnIdentificar)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 142, Short.MAX_VALUE)
						.addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 53,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(45, 45, 45).addComponent(btnSair, javax.swing.GroupLayout.PREFERRED_SIZE, 53,
								javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(24, 24, 24)));
		panBtnsLayout.setVerticalGroup(panBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panBtnsLayout.createSequentialGroup().addGap(24, 24, 24)
						.addGroup(panBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(btnVerificar, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnIdentificar, javax.swing.GroupLayout.PREFERRED_SIZE, 29,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnSair, javax.swing.GroupLayout.PREFERRED_SIZE, 27,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap(20, Short.MAX_VALUE)));

		textArea.setColumns(20);
		textArea.setRows(5);
		jScrollPane1.setViewportView(textArea);

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup().addComponent(jScrollPane1).addContainerGap()));
		jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
						jPanel1Layout.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE).addComponent(jScrollPane1,
								javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
								.addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(panBtns, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addContainerGap())
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
						layout.createSequentialGroup()
								.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(panDigital, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addGap(187, 187, 187)));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(20, 20, 20)
						.addComponent(panDigital, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18)
						.addComponent(panBtns, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addContainerGap()));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void btnSairActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSairActionPerformed
		System.exit(0);
	}// GEN-LAST:event_btnSairActionPerformed

	private void btnVerificarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnVerificarActionPerformed
		String nome = JOptionPane.showInputDialog("Nome para verificar: ");
		verificarDigital(nome);
		recrutador.clear();
	}// GEN-LAST:event_btnVerificarActionPerformed

	private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnGuardarActionPerformed
		try {
			guardarDigital();
			recrutador.clear();
			lblImagenDigital.setIcon(null);
			start();
		} catch (SQLException ex) {
			Logger.getLogger(CapturaFinger.class.getName()).log(Level.SEVERE, null, ex);
		}
	}// GEN-LAST:event_btnGuardarActionPerformed

	private void btnIdentificarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnIdentificarActionPerformed
		try {
			identificarDigital();
			recrutador.clear();
		} catch (IOException ex) {
			Logger.getLogger(CapturaFinger.class.getName()).log(Level.SEVERE, null, ex);
		}
	}// GEN-LAST:event_btnIdentificarActionPerformed

	private void formWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosing
		stop();
	}// GEN-LAST:event_formWindowClosing

	private void formWindowOpened(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowOpened
		Iniciar();
		start();
		estadoDaDigital();
		btnGuardar.setEnabled(false);
		btnIdentificar.setEnabled(false);
		btnVerificar.setEnabled(false);
		btnSair.grabFocus();
	}// GEN-LAST:event_formWindowOpened

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
		// (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the default
		 * look and feel. For details see
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
			java.util.logging.Logger.getLogger(CapturaFinger.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(CapturaFinger.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(CapturaFinger.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(CapturaFinger.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		}
		// </editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new CapturaFinger().setVisible(true);
			}
		});
	}


}