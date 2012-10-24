/*
 * Server.java
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

package server;

import gui.WebServer;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.print.attribute.standard.Severity;
import javax.swing.DefaultListModel;

/**
 * This represents a welcoming server for the incoming TCP request from a HTTP
 * client such as a web browser.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
	private String rootDirectory;
	private int port;
	private boolean stop;
	private ServerSocket welcomeSocket;

	private long connections;
	private long serviceTime;

	private WebServer window;

	private static final int NTHREDS = 10;

	private int sampleSize = 5;
	private long timeThreshold = 100;
	private List<ServerConnection> latestConnections;
	private DefaultListModel<InetAddress> blackList;
	private DefaultListModel<InetAddress> whiteList;

	private class ServerConnection {
		Date connTime;
		InetAddress address;
		long timeSinceLastConnection;
		int connNumber;

		public int getConnNumber() {
			return connNumber;
		}

		public long getTimeSinceLastConnection() {
			return timeSinceLastConnection;
		}

		public void setTimeSinceLastConnection(long timeSinceLastConnection) {
			this.timeSinceLastConnection = timeSinceLastConnection;
		}

		public Date getConnTime() {
			return connTime;
		}

		public InetAddress getAddress() {
			return address;
		}

		public void incrementConnNumber() {
			this.connNumber++;
		}

		public ServerConnection(Date time, InetAddress addr) {
			address = addr;
			connTime = time;
			connNumber = 1;
			timeSinceLastConnection = -1;
		}

		/**
		 * @param other
		 * @return the time between the connections if the addresses is the
		 *         same, if not then returns -1
		 */
		public long isSameAddress(ServerConnection other) {
			if (address.equals(other.address)) {
				return other.connTime.getTime() - connTime.getTime();
			}
			return -1;
		}
	}

	public int getSampleSize() {
		return sampleSize;
	}

	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}

	public long getTimeThreshold() {
		return timeThreshold;
	}

	public void setTimeThreshold(long timeThreshold) {
		this.timeThreshold = timeThreshold;
	}

	public void setWhitelist(DefaultListModel<InetAddress> wl) {
		whiteList = wl;
	}

	public void setBlacklist(DefaultListModel<InetAddress> wl) {
		blackList = wl;
	}

	/**
	 * @param rootDirectory
	 * @param port
	 */
	public Server(String rootDirectory, int port, WebServer window) {
		this.rootDirectory = rootDirectory;
		this.port = port;
		this.stop = false;
		this.connections = 0;
		this.serviceTime = 0;
		this.window = window;

		this.latestConnections = new ArrayList<ServerConnection>();
		blackList = new DefaultListModel<InetAddress>();
		whiteList = new DefaultListModel<InetAddress>();
	}

	/**
	 * Gets the root directory for this web server.
	 * 
	 * @return the rootDirectory
	 */
	public String getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Gets the port number for this web server.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns connections serviced per second. Synchronized to be used in
	 * threaded environment.
	 * 
	 * @return
	 */
	public synchronized double getServiceRate() {
		if (this.serviceTime == 0)
			return Long.MIN_VALUE;
		double rate = this.connections / (double) this.serviceTime;
		rate = rate * 1000;
		return rate;
	}

	/**
	 * Increments number of connection by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementConnections(long value) {
		this.connections += value;
	}

	/**
	 * Increments the service time by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementServiceTime(long value) {
		this.serviceTime += value;
	}

	/**
	 * The entry method for the main server thread that accepts incoming TCP
	 * connection request and creates a {@link ConnectionHandler} for the
	 * request.
	 */
	public void run() {
		try {
			final Executor executor = Executors.newFixedThreadPool(NTHREDS);
			this.welcomeSocket = new ServerSocket(port);

			// Now keep welcoming new connections until stop flag is set to true
			while (true) {
				// Listen for incoming socket connection
				// This method block until somebody makes a request
				Socket connectionSocket = this.welcomeSocket.accept();

				// Come out of the loop if the stop flag is set
				if (this.stop)
					break;

				InetAddress address = connectionSocket.getInetAddress();
				if (whiteList.contains(address)
						|| (!blackList.contains(address) && allowConnection(new ServerConnection(
								new Date(), address)))) {

					// Create a handler for this incoming connection and start
					// the handler in a new thread
					ConnectionHandler handler = new ConnectionHandler(this,
							connectionSocket);
					executor.execute(new Thread(handler));
				}
			}
			this.welcomeSocket.close();
		} catch (Exception e) {
			window.showSocketException(e);
		}
	}

	/**
	 * Stops the server from listening further.
	 */
	public synchronized void stop() {
		if (this.stop)
			return;

		// Set the stop flag to be true
		this.stop = true;
		try {
			// This will force welcomeSocket to come out of the blocked accept()
			// method
			// in the main loop of the start() method
			Socket socket = new Socket(InetAddress.getLocalHost(), port);

			// We do not have any other job for this socket so just close it
			socket.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Checks if the server is stopped or not.
	 * 
	 * @return
	 */
	public boolean isStoped() {
		if (this.welcomeSocket != null)
			return this.welcomeSocket.isClosed();
		return true;
	}

	public void whitelistAddress(InetAddress addr) {
		whiteList.addElement(addr);
		blackList.removeElement(addr);
		System.out.println("Address " + addr.getHostAddress()
				+ " has been whitelisted");
	}

	public void blacklistAddress(InetAddress addr) {
		blackList.addElement(addr);
		whiteList.removeElement(addr);
		System.out.println("Address " + addr.getHostAddress()
				+ " has been blacklisted");
	}

	public void unBlacklistAddress(InetAddress addr) {
		blackList.removeElement(addr);
	}

	public void unWhitelistAddress(InetAddress addr) {
		blackList.removeElement(addr);
	}

	private boolean allowConnection(ServerConnection c) {
		long timeSum = 0;
		int numConnections = 1;
		int indexToRemove = -1;

		for (int i = 0; i < latestConnections.size(); i++) {
			ServerConnection conn = latestConnections.get(i);
			long result = conn.isSameAddress(c);
			if (result != -1) {
				// keep the sample size the same
				conn.incrementConnNumber();
				if (conn.getConnNumber() > sampleSize) {
					// flag this item for removal
					indexToRemove = i;
					continue;
				} else if (conn.getConnNumber() == 2) { // latest connection
					c.timeSinceLastConnection = result;
					timeSum += result;
				}

				// make sure we dont add the first connction in the average
				if (conn.getTimeSinceLastConnection() != -1) {
					timeSum += conn.timeSinceLastConnection;
					numConnections++;
				}
			}
		}

		if (indexToRemove != -1)
			latestConnections.remove(indexToRemove);

		latestConnections.add(c);

		// calculate average time between connection
		long avg = timeSum / (long) numConnections;

		System.out.println("Average time between connections: " + avg
				+ " milliseconds");

		/* possibly add a minimum number of connections to get a good sample */
		if (numConnections != 1 && avg <= timeThreshold) {
			// limit connections from this ip
			blacklistAddress(c.address);

			return false;
		}
		return true;
	}
}
