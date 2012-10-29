/*
 * WebServer.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 */

package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.Transient;
import java.net.InetAddress;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

import server.Server;

/**
 * The application window for the {@link Server}, where you can update some
 * parameters and start and stop the server.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class WebServer extends JFrame {
	private static final long serialVersionUID = 5042579745743827174L;

	private JPanel panelRunServer;
	private JLabel lblPortNumber;
	private JTextField txtPortNumber;
	private JLabel lblRootDirectory;
	private JTextField txtRootDirectory;
	private JButton butSelect;

	private JPanel panelInput;
	private JButton butStartServer;
	private JButton butStopServer;
	private JLabel lblServiceRate;
	private JTextField txtServiceRate;

	private JList<InetAddress> whiteListView;
	private JList<InetAddress> blackListView;
	private JButton butWhitelist;
	private JButton butBlacklist;
	private JButton butUnBlacklist;
	private JButton butUnWhitelist;
	private JLabel lblSampleSize;
	private JLabel lblThreshold;
	private JButton butChgSample;
	private JButton butChgThreshold;

	private DefaultListModel<InetAddress> whitelist;
	private DefaultListModel<InetAddress> blacklist;

	private Server server;
	private ServiceRateUpdater rateUpdater;

	/**
	 * For constantly updating the service rate in the GUI.
	 * 
	 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
	 */
	private class ServiceRateUpdater implements Runnable {
		public boolean stop = false;

		public void run() {
			while (!stop) {
				// Poll if server is not null and server is still accepting
				// connections
				if (server != null && !server.isStopped()) {
					double rate = server.getServiceRate();
					if (rate == Double.MIN_VALUE)
						WebServer.this.txtServiceRate.setText("Unknown");
					else
						WebServer.this.txtServiceRate.setText(Double
								.toString(rate));
				}

				// Poll at an interval of 500 milliseconds
				try {
					Thread.sleep(500);
				} catch (Exception e) {
				}
			}
		}
	}

	/** Creates new form WebServer */
	public WebServer() {
		initComponents();
		this.addListeners();
	}

	private void initComponents() {
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setTitle("Simple Web Server (SWS) Window");

		// Input panel widgets
		this.panelInput = new JPanel();
		this.lblPortNumber = new JLabel("Port Number");
		this.txtPortNumber = new JTextField("8080");
		this.lblRootDirectory = new JLabel("Select Root Directory");
		// Set the root directory to be the current working directory
		this.txtRootDirectory = new JTextField(System.getProperty("user.dir"));
		this.txtRootDirectory.setEditable(false);
		this.txtRootDirectory.setPreferredSize(new Dimension(400, 21));
		this.butSelect = new JButton("Select");

		this.panelInput.setBorder(BorderFactory
				.createTitledBorder("Input Parameters"));
		this.panelInput.setLayout(new SpringLayout());
		this.panelInput.add(this.lblPortNumber);
		this.panelInput.add(this.txtPortNumber);
		this.panelInput.add(this.lblRootDirectory);
		this.panelInput.add(this.txtRootDirectory);
		this.panelInput.add(new JLabel("")); // Empty label
		this.panelInput.add(this.butSelect);

		// Compact the grid
		SpringUtilities.makeCompactGrid(this.panelInput, 3, 2, 5, 5, 5, 5);

		// Run server widgets
		this.panelRunServer = new JPanel();
		this.butStartServer = new JButton("Start Simple Web Server");
		this.butStopServer = new JButton("Stop Simple Web Server");
		this.butStopServer.setEnabled(false);
		this.lblServiceRate = new JLabel(
				"Service Rate (Connections Serviced/Second)");
		this.txtServiceRate = new JTextField("Unknown");

		// panelRunServer uses FlowLayout by default
		this.panelRunServer.setBorder(BorderFactory
				.createTitledBorder("Run Server"));
		this.panelRunServer.setLayout(new SpringLayout());
		this.panelRunServer.add(this.butStartServer);
		this.panelRunServer.add(this.butStopServer);
		this.panelRunServer.add(this.lblServiceRate);
		this.panelRunServer.add(this.txtServiceRate);

		// Compact the grid
		SpringUtilities.makeCompactGrid(this.panelRunServer, 2, 2, 5, 5, 5, 5);

		whitelist = new DefaultListModel<InetAddress>();
		blacklist = new DefaultListModel<InetAddress>();
		butWhitelist = new JButton("<<");
		butWhitelist.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				WebServer.this.server
						.whitelistAddress(WebServer.this.blackListView
								.getSelectedValue());

			}
		});
		butBlacklist = new JButton(">>");
		butBlacklist.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				WebServer.this.server
						.whitelistAddress(WebServer.this.blackListView
								.getSelectedValue());

			}
		});
		butUnBlacklist = new JButton("Remove selected IP");
		butUnBlacklist.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				WebServer.this.server
						.unBlacklistAddress(WebServer.this.blackListView
								.getSelectedValue());

			}
		});
		butUnWhitelist = new JButton("Remove selected IP");
		butUnWhitelist.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				WebServer.this.server
						.unWhitelistAddress(WebServer.this.blackListView
								.getSelectedValue());

			}
		});

		// white and black list
		JPanel wbPanel = new JPanel();
		lblSampleSize = new JLabel("N/A");
		lblSampleSize.setPreferredSize(new Dimension(100, 20));
		lblSampleSize.setBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.RAISED));

		lblThreshold = new JLabel("N/A");
		lblThreshold.setPreferredSize(new Dimension(100, 20));
		lblThreshold.setBorder(BorderFactory
				.createEtchedBorder(EtchedBorder.RAISED));
		butChgSample = new JButton("Change");
		butChgSample.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String s = (String) JOptionPane
						.showInputDialog("Input a new sample size\n The sample size is the number of connection histories to store while analyzing time between connections");
				try {
					Integer i = Integer.parseInt(s);
					server.setSampleSize(i);
					updateView();
				} catch (NumberFormatException e) {
					
				}
			}

		});

		butChgThreshold = new JButton("Change");
		butChgThreshold.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String s = (String) JOptionPane
						.showInputDialog("Input a time threshold in milliseconds\n The time threshold is the lowest average time between connections an IP can have before it is blacklisted.");
				try {
					Integer i = Integer.parseInt(s);
					server.setTimeThreshold(i);
					updateView();
				} catch (NumberFormatException e) {
					
				}
			}

		});

		JLabel lblWhite = new JLabel("Whitelisted IPs");
		JLabel lblBlack = new JLabel("Blacklisted IPs");
		whiteListView = new JList<InetAddress>(whitelist) {
			@Override
			@Transient
			public Dimension getPreferredSize() {
				return new Dimension(250, 100);
			}
		};
		blackListView = new JList<InetAddress>(blacklist) {
			@Override
			@Transient
			public Dimension getPreferredSize() {
				return new Dimension(250, 100);
			}
		};
		blackListView.setMinimumSize(new Dimension(100, 100));

		wbPanel.setBorder(BorderFactory.createTitledBorder("IP Restrictions"));
		wbPanel.setLayout(new SpringLayout());

		JPanel upperleft = new JPanel();
		upperleft.add(new JLabel("Sample Size:"));
		upperleft.add(lblSampleSize);
		upperleft.add(butChgSample);

		JPanel upperright = new JPanel();
		upperright.add(new JLabel("Time threshold:"));
		upperright.add(lblThreshold);
		upperright.add(butChgThreshold);

		wbPanel.add(upperleft);
		wbPanel.add(upperright);

		wbPanel.add(lblWhite);
		wbPanel.add(lblBlack);
		wbPanel.add(whiteListView);
		wbPanel.add(blackListView);

		JPanel leftPanel = new JPanel();
		leftPanel.add(butUnBlacklist);
		leftPanel.add(butBlacklist);

		JPanel rightPanel = new JPanel();
		rightPanel.add(butWhitelist);
		rightPanel.add(butUnWhitelist);

		wbPanel.add(leftPanel);
		wbPanel.add(rightPanel);

		SpringUtilities.makeCompactGrid(wbPanel, 4, 2, 5, 5, 5, 5);

		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.add(this.panelInput, BorderLayout.NORTH);
		contentPane.add(this.panelRunServer, BorderLayout.CENTER);
		contentPane.add(wbPanel, BorderLayout.SOUTH);

		pack();
	}

	private void addListeners() {
		// Add the action to be done when select directory button is pressed
		this.butSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get hold of the current directory
				String currentDirectory = WebServer.this.txtRootDirectory
						.getText();
				JFileChooser fileChooser = new JFileChooser(currentDirectory);
				fileChooser.setDialogTitle("Chose Web Server Root Directory");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setAcceptAllFileFilterUsed(false);
				if (fileChooser.showOpenDialog(WebServer.this) == JFileChooser.APPROVE_OPTION) {
					// A folder has been chosen
					currentDirectory = fileChooser.getSelectedFile()
							.getAbsolutePath();
					WebServer.this.txtRootDirectory.setText(currentDirectory);
				}
			}
		});

		// Add action for run server
		this.butStartServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (server != null && !server.isStopped()) {
					JOptionPane
							.showMessageDialog(
									WebServer.this,
									"The web server is still running, try again later.",
									"Server Still Running Error",
									JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Read port number
				int port = 80;
				try {
					port = Integer.parseInt(WebServer.this.txtPortNumber
							.getText());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(WebServer.this,
							"Invalid Port Number!", "Web Server Input Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Get hold of the root directory
				String rootDirectory = WebServer.this.txtRootDirectory
						.getText();

				// Now run the server in non-gui thread
				server = new Server(rootDirectory, port, WebServer.this);
				server.setWhitelist(whitelist);
				server.setBlacklist(blacklist);
				WebServer.this.updateView();

				rateUpdater = new ServiceRateUpdater();

				// Disable widgets
				WebServer.this.disableWidgets();

				// Now run the server in a separate thread
				new Thread(server).start();

				// Also run the service rate updater thread
				new Thread(rateUpdater).start();
			}
		});

		// Add action for stop button
		this.butStopServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (server != null && !server.isStopped())
					server.stop();
				if (rateUpdater != null)
					rateUpdater.stop = true;
				WebServer.this.enableWidgets();
				WebServer.this.lblSampleSize.setText("N/A");
				WebServer.this.lblThreshold.setText("N/A");
			}
		});

		// Make sure the web server is stopped before closing the window
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (server != null && !server.isStopped())
					server.stop();
				if (rateUpdater != null)
					rateUpdater.stop = true;
			}
		});
	}

	public void updateView() {
		lblSampleSize.setText(new Integer(server.getSampleSize()).toString());
		lblThreshold.setText(new Long(server.getTimeThreshold()).toString());
	}

	private void disableWidgets() {
		this.txtPortNumber.setEnabled(false);
		this.butSelect.setEnabled(false);
		this.butStartServer.setEnabled(false);
		this.butStopServer.setEnabled(true);
	}

	private void enableWidgets() {
		this.txtPortNumber.setEnabled(true);
		this.butSelect.setEnabled(true);
		this.butStartServer.setEnabled(true);
		this.butStopServer.setEnabled(false);
	}

	/**
	 * For displaying exception.
	 * 
	 * @param e
	 */
	public void showSocketException(Exception e) {
		JOptionPane.showMessageDialog(this, e.getMessage(),
				"Web Server Socket Problem", JOptionPane.ERROR_MESSAGE);
		if (this.server != null)
			this.server.stop();
		this.server = null;

		if (this.rateUpdater != null)
			this.rateUpdater.stop = true;
		this.rateUpdater = null;
		this.enableWidgets();
	}

	/**
	 * The application start point.
	 * 
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new WebServer().setVisible(true);
			}
		});
	}
}
