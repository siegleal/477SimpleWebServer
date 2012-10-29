/*
 * ConnectionHandler.java
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;

import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.Protocol;
import protocol.ProtocolException;

/**
 * This class is responsible for handling a incoming request by creating a
 * {@link HttpRequest} object and sending the appropriate response be creating a
 * {@link HttpResponse} object. It implements {@link Runnable} to be used in
 * multi-threaded environment.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class ConnectionHandler implements Runnable {
	private Server server;
	private SelectionKey selKey;
	private SocketChannel socket;
	private String authenticatedUser;

	public ConnectionHandler(Server server, SelectionKey selKey) {
		this.server = server;
		this.selKey = selKey;
		this.socket = (SocketChannel) selKey.channel();
		authenticatedUser = null;
	}

	/**
	 * @return the socket
	 */
	public SocketChannel getSocket() {
		return socket;
	}

	/**
	 * The entry point for connection handler. It first parses incoming request
	 * and creates a {@link HttpRequest} object, then it creates an appropriate
	 * {@link HttpResponse} object and sends the response back to the client
	 * (web browser).
	 */
	public void run() {
		// Get the start time
		long start = System.currentTimeMillis();

		InputStream inStream = null;
		OutputStream outStream = null;

		try {
			inStream = new ByteBufferInputStreamAdapter(this.socket);
			outStream = new ByteBufferOutputStreamAdapter(this.socket);
		} catch (Exception e) {
			// Cannot do anything if we have exception reading input or output
			// stream
			// May be have text to log this for further analysis?
			e.printStackTrace();

			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end - start);
			this.selKey.interestOps(this.selKey.interestOps()
					| SelectionKey.OP_READ);
			this.selKey.selector().wakeup();
			return;
		}

		// At this point we have the input and output stream of the socket
		// Now lets create a HttpRequest object
		HttpRequest request = null;
		HttpResponse response = null;
		try {
			request = HttpRequest.read(inStream);
			System.out.println(request);
		} catch (ProtocolException pe) {
			// We have some sort of protocol exception. Get its status code and
			// create response
			// We know only two kind of exception is possible inside
			// fromInputStream
			// Protocol.BAD_REQUEST_CODE and Protocol.NOT_SUPPORTED_CODE
			int status = pe.getStatus();
			if (status == Protocol.BAD_REQUEST_CODE) {
				response = HttpResponseFactory
						.create400BadRequest(Protocol.CLOSE);
			} else if (status == Protocol.NOT_SUPPORTED_CODE) {
				response = HttpResponseFactory
						.create505NotSupported(Protocol.CLOSE);
			}
			// TODO: Handle version not supported code as well
		} catch (Exception e) {
			e.printStackTrace();
			// For any other error, we will create bad request response as well
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}

		this.selKey.interestOps(this.selKey.interestOps()
				| SelectionKey.OP_READ);
		this.selKey.selector().wakeup();

		if (response != null) {
			// Means there was an error, now write the response object to the
			// socket
			try {
				response.write(outStream);
				// System.out.println(response);
			} catch (Exception e) {
				// We will ignore this exception
				e.printStackTrace();
			}

			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end - start);
			return;
		}

		// We reached here means no error so far, so lets process further
		try {
			// Fill in the code to create a response for version mismatch.
			// You may want to use constants such as Protocol.VERSION,
			// Protocol.NOT_SUPPORTED_CODE, and more.
			// You can check if the version matches as follows
			if (!request.getVersion().equalsIgnoreCase(Protocol.VERSION)) {
				// Here you checked that the "Protocol.VERSION" string is not
				// equal to the
				// "request.version" string ignoring the case of the letters in
				// both strings
				// TODO: Fill in the rest of the code here
			} else if (!request.getMethod().equalsIgnoreCase(Protocol.GET)) {
				response = HttpResponseFactory
						.create505NotSupported(Protocol.CLOSE);
			} else if (request.getMethod().equalsIgnoreCase(Protocol.GET)) {
				Map<String, String> header = request.getHeader();
				String date = header.get("if-modified-since");
				Date dateResult = null;
				if (date != null) {
					DateFormat df = new SimpleDateFormat(
							"EEE MMM dd yyyy kk:mm:ss z");
					dateResult = df.parse(date);
				}

				// authenticate user
				String authenString = header.get("authorization");
				if (authenticatedUser == null && authenString != null) {
					authenticateUser(authenString, request.getUri());
				}

				// check for needs authentication
				if (passedAuthentication(request.getUri())) {

					// String hostName = header.get("host");
					//
					// Handling GET request here
					// Get relative URI path from request
					String uri = request.getUri();
					// Get root directory path from server
					String rootDirectory = server.getRootDirectory();
					// Combine them together to form absolute file path
					File file = new File(rootDirectory + uri);

					// Check if the file exists
					if (file.exists()) {
						if (uri.contains("passwd") || uri.contains("permission")){
							response = HttpResponseFactory.create403Forbidden(Protocol.CLOSE);
						}
						else if (file.isDirectory()) {
							// Look for default index.html file in a directory
							String location = rootDirectory + uri
									+ System.getProperty("file.separator")
									+ Protocol.DEFAULT_FILE;
							file = new File(location);
							if (file.exists()) {
								if (date != null
										&& dateResult.after(new Date(file
												.lastModified())))// create a
																	// 304NotMOdified
									response = HttpResponseFactory
											.create304NotModified(Protocol.CLOSE);
								else
									// Lets create 200 OK response
									response = HttpResponseFactory.create200OK(
											file, Protocol.CLOSE);
							} else {
								// File does not exist so lets create 404 file
								// not
								// found code
								response = HttpResponseFactory
										.create404NotFound(Protocol.CLOSE);
							}
						} else { // Its a file
							if (date != null
									&& dateResult.after(new Date(file
											.lastModified())))// create a
																// 304NotMOdified
								response = HttpResponseFactory
										.create304NotModified(Protocol.CLOSE);
							else
								// Lets create 200 OK response
								response = HttpResponseFactory.create200OK(
										file, Protocol.CLOSE);
						}
					} else {
						// File does not exist so lets create 404 file not found
						// code
						response = HttpResponseFactory
								.create404NotFound(Protocol.CLOSE);
					}
				} else {
					// failed authentication
					response = HttpResponseFactory
							.create401Unauthorized(Protocol.CLOSE);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// TODO: So far response could be null for protocol version mismatch.
		// So this is a temporary patch for that problem and should be removed
		// after a response object is created for protocol version mismatch.
		if (response == null) {
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}

		try {
			// Write response and we are all done so close the socket
			response.write(outStream);
			// System.out.println(response);
			// socket.close();
		} catch (Exception e) {
			// We will ignore this exception
			e.printStackTrace();
		}

		// Increment number of connections by 1
		server.incrementConnections(1);
		// Get the end time
		long end = System.currentTimeMillis();
		this.server.incrementServiceTime(end - start);
	}

	private boolean passedAuthentication(String uri) {
		// check to see is the uri is controlled
		if (server.needsPermission(uri)) {
			String[] allowedUsers = server.getUsersForUri(uri);
			for (String u : allowedUsers) {
				if (u.equals(authenticatedUser))
					return true;
			}
			return false;
		} else {
			return true;
		}
	}

	private void authenticateUser(String authorizationString, String uri) {
		try {

			String username = parseString(authorizationString, "username");
			String realm = parseString(authorizationString, "realm");
			String password;

			password = server.getPasswd(username);

			String hashString = username + ":" + realm + ":" + password;

			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash1 = md.digest(hashString.getBytes("UTF-8"));
			String shash1 = new BigInteger(1, hash1).toString(16);

			String get = "GET";
			String hash2 = new BigInteger(1, md.digest((get + ":" + uri)
					.getBytes("UTF-8"))).toString(16);

			System.out.println("hash1 = " + shash1 + "\nhash2 = " + hash2);

			// build the final string for our MD5
			StringBuilder sb = new StringBuilder();
			sb.append(shash1);
			sb.append(":");
			sb.append(parseString(authorizationString, "nonce"));
			sb.append(":");
			sb.append(parseString(authorizationString, "nc"));
			sb.append(":");
			sb.append(parseString(authorizationString, "cnonce"));
			sb.append(":");
			sb.append(parseString(authorizationString, "qop"));
			sb.append(":");
			sb.append(hash2);

			String finalHash = new BigInteger(1, md.digest(sb.toString()
					.getBytes("UTF-8"))).toString(16);

			// System.out.println(finalHash + "\n" +
			// parseString(authorizationString, "response"));

			if (finalHash.equals(parseString(authorizationString, "response"))) {
				// authorize user
				this.authenticatedUser = username;
				System.out.println("User successfully authenticated");
			}

		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String parseString(String line, String key) {
		key = key + "=";
		int startIndex = line.indexOf(key) + key.length();
		String result = line.substring(startIndex);
		result = result.substring(
				0,
				(result.indexOf(",") == -1 ? result.length() - 1 : result
						.indexOf(",")));
		result = result.replace("\"", "");
		// System.out.println("Looking for '" + key + "', found '" + result +
		// "'");
		return result;
	}
}
