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


package com.github.knockshore.wsstreamlib;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;

import com.github.knockshore.wsstreamlib.handlers.*;

public class Main {

	// setup a simple HTTP server using Sockets
	public static void setupServer2(int port) throws IOException {

		ServerSocket httpServer = new ServerSocket(port);
		
		while (true) {
		
			final Socket client = httpServer.accept();

			new Thread() {

				@Override
				public void run() {

					try {

						HttpSocketStreamHandler h = new HttpSocketStreamHandler();
						h.handleStream(client.getInputStream(),
								client.getOutputStream());
						h.getWebSocketStreamHandler().sendTextMessage("Hello!");


					} catch (IOException e) {

						e.printStackTrace();
					}
				}

			}.start();

		}

	}

	public static void main(String[] args) throws Exception {

		setupServer2(49152);
	}
}
