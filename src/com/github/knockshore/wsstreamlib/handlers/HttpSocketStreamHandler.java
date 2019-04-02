/**
 * Copyright 2019 Karthick S (yuvikarti[at]gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.knockshore.wsstreamlib.handlers;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.sun.net.httpserver.Headers;

import com.github.knockshore.wsstreamlib.utils.Utils;

public class HttpSocketStreamHandler {

	private static void output(Object ...args) {

		StringBuilder sb = new StringBuilder();

		for (Object obj : args) {
			
			sb.append(obj.toString());
			sb.append(" ");
		}

		//logger.info(sb.toString());
		System.out.println(sb);
	}

	private static void info(Object ...args) {
		output(args);
	}

	private static void debug(Object ...args) {
		// output(args);
	}

	public final static String WS_KEY_HEADER = "Sec-WebSocket-Key";
	public final static String WS_PROTOCOL_HEADER = "Sec-WebSocket-Protocol";
	public final static String WS_VERSION_HEADER = "Sec-WebSocket-Version";
	public final static String WS = "WebSocket";
	public final static String WS_ACCEPT_HEADER = "Sec-WebSocket-Accept";

	public final static String HTTP_STATUS_101 = "HTTP/1.1 101 Switching Protocols";
	public final static String HTTP_UPGRADE_HEADER = "Upgrade";

	public final static String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	public final static int WS_TEST_PORT = 49152;
	public final static int HTTP_MAX_HEADER_SIZE = 8192;

	protected WebSocketStreamHandler wsHandler;

	public void handleStream(InputStream is, OutputStream os) throws IOException {

		byte[] buf = null;
		List<byte[]> buffers = new ArrayList<byte[]>();
		int markHeaderEnd = 0;
		Headers headers = new Headers();
		Headers respHeaders = new Headers();

		while (true) {
			
			buf = new byte[HTTP_MAX_HEADER_SIZE];
			int read = is.read(buf);

			// possibly, could've prepared the sb stuff here
			for (int i = 0; i < buf.length - 3; i++) {
				
				if (
				
					buf[i] == '\r'
					&& buf[i + 1] == '\n'
					&& buf[i + 2] == '\r'
					&& buf[i + 3] == '\n'
				) {

					markHeaderEnd = i;
					break;
				}
			}

			if (markHeaderEnd > 0) {
				/// parse headers
				//
				buffers.add(buf);
				// TODO: move remaining data to data buffer if there's post data
				// not urgent

				parseHeaders(buffers, headers, markHeaderEnd);
				
				// debug("headers: " + headers);
				printHeaders(headers);
				break;

			} else {

				buffers.add(buf);
				
				debug("Added buffer: " 
						+ new String(buf)
						.replaceAll("\\r","\\r")
						.replaceAll("\\n", "\\n"));
				debug("Length: " + buffers.size() * HTTP_MAX_HEADER_SIZE);

			}

		}

		// send back response
		respHeaders.add(null, HTTP_STATUS_101);
		respHeaders.add("Connection", "Upgrade");
		respHeaders.add(HTTP_UPGRADE_HEADER, "websocket");
		
		String wsKey = headers.getFirst(WS_KEY_HEADER);
		String wsAcceptHash = Utils.encodeAsBase64(Utils.hashWithSHA1(wsKey + WS_GUID));
		
		respHeaders.add(WS_ACCEPT_HEADER, wsAcceptHash);
		sendHeaders(respHeaders, os);	

		wsHandler = new WebSocketStreamHandler();
		wsHandler.handleStream(is, os);

	}

	public WebSocketStreamHandler getWebSocketStreamHandler() {
	
		return this.wsHandler;
	}


	private void sendHeaders(Headers headers, OutputStream stream) throws IOException {

		byte[] colonSpace = ": ".getBytes();
		byte[] crlf = "\r\n".getBytes();
		
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			
			String keyStr = entry.getKey();
			byte[] key = null;

			if (keyStr == null) {
				
				stream.write(entry.getValue().get(0).getBytes());
				stream.write(crlf);
				continue;
			
			} else {
				
				key = keyStr.getBytes();

			}

			for (String value : entry.getValue()) {

				stream.write(key);
				stream.write(colonSpace);
				stream.write(value.getBytes());
				stream.write(crlf);

			}
		}
		
		// because websocket complains
		// stream.write(crlf);

	}

	private void printHeaders(Headers headers) {

		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String key = entry.getKey();
			for (String value : entry.getValue()) {
				debug("k: " + key + ": " + value);
			}
		}
	}

	private Headers parseHeaders(List<byte[]> buffers, Headers headers, int headerEnd) {
		
		byte[] prevBuf = null;
		int prevOffset = -1;
		StringBuilder lineSb = new StringBuilder();
		boolean isLastByteReturn = false;

		for (byte[] buf : buffers) {

			int offset = 0;
			boolean containsLineBreak = false;

			for (int i = 0; i < buf.length - 1; i++) {

				if (i < buf.length - 3
						&& buf[i] == 13
						&& buf[i + 1] == 10
						&& buf[i + 2] == 13
						&& buf[i + 3] == 10) {
					return headers;
				}

				if ((buf[i] == 13 && buf[i + 1] == 10) 
					|| (isLastByteReturn && buf[i] == 10 && i == 0)) {
					
					// out.printf("before creating buf: i:%d, off:%d\n", i, offset);

					String part = new String(buf, offset, i - offset);
					lineSb.append(part);

					String line = lineSb.toString();
					lineSb = new StringBuilder();
					
					debug("line: " + line);

					if (line.contains(":")) {
					
						headers.add(
							line.substring(0, 
								line.indexOf(":")), 
							line.substring(line.indexOf(":") + 1)
										.trim());
					} else {
					
						headers.add(null, line);
					}

					offset = !isLastByteReturn ? i + 2 : i + 1;
					containsLineBreak = true;

				}
			}
			
			isLastByteReturn = buf[buf.length - 1] == 13;


			if (!containsLineBreak) {

				lineSb.append(new String(buf));
			} else if (offset != buf.length) {

				lineSb.append(new String(buf, offset, buf.length - offset));
			}

		}

		return headers;

	}
}

