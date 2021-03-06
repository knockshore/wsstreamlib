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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.knockshore.wsstreamlib.WebSocketListener;

public class WebSocketStreamHandler {

	public final static int WSF_OPCODE_CONTIN = 0x0;
	public final static int WSF_OPCODE_TEXT = 0x1;
	public final static int WSF_OPCODE_BIN = 0x2;
	public final static int WSF_OPCODE_PING = 0x9;
	public final static int WSF_OPCODE_CLOSE = 0x8;
	public final static int WSF_OPCODE_PONG = 0xA;

	private void debug(Object ...args) {
	}

	private void output(Object ...args) {

		for (Object arg : args) {
			System.out.print(arg);
			System.out.print(", ");
		}
		System.out.println();	
	}

	public static class WebSocketFrame {

		boolean	 isFinal;
		boolean	 reserved1;
		boolean	 reserved2;
		boolean	 reserved3;
		int	 opcode;
		boolean	 isMask;
		long	 payloadLen;
		byte[]	 maskingKey = new byte[4];
		byte[]	payload;
		String	textPayload;

		@Override
		public String toString() {
			return	
			"isFinal;           	: " + isFinal + "\n" +
			"reserved1;         	: " + reserved1 + "\n" +
			"reserved2;         	: " + reserved2 + "\n" +
			"reserved3;         	: " + reserved3 + "\n" +
			"opcode;            	: " + opcode + "\n" +
			"isMask;            	: " + isMask + "\n" +
			"payloadLen;        	: " + payloadLen + "\n" +
			"textPayload;   	: " + textPayload + "\n" +
			"maskingKey;        	: " + maskingKey;
	
		}

	}

	protected WebSocketListener websocketListener = new WebSocketListener() {

		public void onOpen(WebSocketStreamHandler wsStreamHandler) { output("got open"); }
		public void onTextMessage(WebSocketStreamHandler wsStreamHandler, String message) { 
			output("got message", message);
			try {

				wsStreamHandler.sendTextMessage(message);
				wsStreamHandler.sendTextMessage("eom");
				wsStreamHandler.close();

			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		public void onClose(WebSocketStreamHandler wsStreamHandler) { output("got close"); }
		public void onPing(WebSocketStreamHandler wsStreamHandler) { output("got ping"); }

	};

	protected OutputStream os;
	protected InputStream is;

	public void setWebSocketMessageListener(WebSocketListener websocketListener) {
		
		this.websocketListener = websocketListener;

	}

	public InputStream getInputStream() {
		
		return is;
	}

	public OutputStream getOutputStream() {

		return os;
	}

	public void sendTextMessage(String text) throws IOException {
		
		if (text == null) return;
		if (text.length() == 0) return;

		// prepare text frame
		WebSocketFrame wsf = new WebSocketFrame();
		wsf.isFinal = true;
		wsf.reserved1 = false;
		wsf.reserved2 = false;
		wsf.reserved3 = false;
		wsf.opcode = 1;
		wsf.isMask = false;
		wsf.payloadLen = text.length();
		wsf.payload = text.getBytes();
		// no need to set text payload

		// convert to bytes
		byte[] frame = getFrameBytes(wsf);
		// send em
		this.os.write(frame, 0, frame[1] < 126 ? 2 : (frame[1] == 126 ? 4 : 10));
		this.os.write(wsf.payload, 0, (int)wsf.payloadLen);
	}

	public void close() throws IOException {
		
		// prepare frame
		// convert to bytes
		// send emi
		this.os.write(getCloseFrame());
	}

	private byte[] getCloseFrame() {

		return new byte[] { (byte)0x88 };
	}

	private byte[] getFrameBytes(WebSocketFrame wsf) {
		
		// no masking key here
		byte[] buf = new byte[10];
		int strLen = (int)wsf.payloadLen;
		int finalLen = 0;

		debug("sending frame:", wsf);

		buf[0] = (byte)(wsf.isFinal ? 0x080 : 0x00);
		buf[0] |= (byte)(!wsf.reserved1 ? 0x00 : 0x40);
		buf[0] |= (byte)(!wsf.reserved2 ? 0x00 : 0x20);
		buf[0] |= (byte)(!wsf.reserved3 ? 0x00 : 0x10);
		buf[0] |= wsf.opcode;

		debug("first: ", String.format("%x", buf[0]));

		buf[1] = 0x00; // no mask 

		if (strLen > 65535) {

			buf[1] = 127;
			buf[2] = (byte)((0xFF00000000000000L & strLen) >>> 56);
			buf[3] = (byte)((0x00FF000000000000L & strLen) >>> 48);
			buf[4] = (byte)((0x0000FF0000000000L & strLen) >>> 40);
			buf[5] = (byte)((0x000000FF00000000L & strLen) >>> 32);
			buf[6] = (byte)((0x00000000FF000000L & strLen) >>> 24);
			buf[7] = (byte)((0x0000000000FF0000L & strLen) >>> 16);
			buf[8] = (byte)((0x000000000000FF00L & strLen) >>> 8);
			buf[9] = (byte)(0x00000000000000FFL & strLen);

		} else if (strLen < 126) {
			
			buf[1] = (byte)strLen;

		} else {

			buf[1] = 126;
			buf[2] = (byte)((0xFF00 & strLen) >>> 8);
			buf[3] = (byte)(0xFF & strLen);

		}


		/*try {
			
			this.os.write(buf, 0, finalLen);
		} catch (IOException e) {
			
			e.printStackTrace();
		}*/

		return buf;
	}

	public void ping() throws Exception{
		throw new Exception("Unimplemented");
	}

	public void handleStream(InputStream is, OutputStream os) throws IOException {
		
		this.is = is;
		this.os = os;

		debug("In websocket stream handler");
		while (true) {

			byte[] buf = new byte[512];
			int read = is.read(buf);

			debug("buf: " + new String(buf));

			// read first 16 bytes
			WebSocketFrame wsf = new WebSocketFrame();
			byte first = buf[0];

			wsf.isFinal = (first & 0x80) == 0x80;
			wsf.reserved1 = (first & 0x40) == 0x40;
			wsf.reserved2 = (first & 0x20) == 0x20;
			wsf.reserved3 = (first & 0x10) == 0x10;
			wsf.opcode = first & 0x0F;

			byte second = buf[1];
			wsf.isMask = (second & 0x80) == 0x80;
			wsf.payloadLen = second & 0x7F;
			
			int maskingKeyOffset = 0;
			int payloadLenIndicator = (int) wsf.payloadLen;

			if (wsf.payloadLen == 126) {

				maskingKeyOffset = 2;
				wsf.payloadLen = (short)((buf[2] << 8) | (0x00FF & buf[3]));
				// wsf.payloadLen |= buf[3];

				// out.printf("payload bytes: %x, %x, shifted: %x, actual: %x\n", buf[2], buf[3], buf[2] << 0x8, (buf[2] << 0x8) | (0x00FF & buf[3]));

			} else if (wsf.payloadLen == 127) {
				
				maskingKeyOffset = 8;
				wsf.payloadLen =
					(buf[2] << 56) | (buf[3] << 48)	+ (buf[4] << 40)
					+ (buf[5] << 32) + (buf[6] << 24) + (buf[7] << 16)
					+ (buf[8] << 8) + buf[9];

			}
			
			if (wsf.isMask) {
				
				wsf.maskingKey[0] = buf[maskingKeyOffset + 2];
				wsf.maskingKey[1] = buf[maskingKeyOffset + 3];
				wsf.maskingKey[2] = buf[maskingKeyOffset + 4];
				wsf.maskingKey[3] = buf[maskingKeyOffset + 5];

			}

			int dataOffset = 0;
			dataOffset += (wsf.isMask ? 4 : 0);
			dataOffset += (payloadLenIndicator == 126 ? 2
					: (payloadLenIndicator == 127 ? 8 : 0));
			dataOffset += 2;

			
			if (wsf.opcode == WSF_OPCODE_TEXT) {

				if (wsf.isMask) {
					
					// unmask
					buf = unmaskBytes(buf, wsf, dataOffset);

				}
	
				wsf.textPayload = new String(buf,
						dataOffset,
						(int) Math.min(
							buf.length - dataOffset, 
							wsf.payloadLen)
						);

				debug("first part: ", wsf.textPayload);
				
				if (wsf.payloadLen >= 512 - dataOffset) {

					byte[] remainingBytes =
						new byte[(int) wsf.payloadLen - (512 - dataOffset)];
					
					read = is.read(remainingBytes);
					debug("read bytes: ", read);
					
					if (wsf.isMask) {
					
						unmaskBytes(remainingBytes, wsf, 0);
					}
					
					wsf.textPayload += new String(remainingBytes);

				}
				this.websocketListener.onTextMessage(this, wsf.textPayload);

			} else if (wsf.opcode == WSF_OPCODE_CLOSE) {

				this.websocketListener.onClose(this);
			} else if (wsf.opcode == WSF_OPCODE_PING) {

				this.websocketListener.onPing(this);
			}
					

			debug("frame: \n" + wsf);


			if (read == -1) break;
		}
	}

	private byte[] unmaskBytes(byte[] buf, WebSocketFrame wsf, int dataOffset) {
	
		for (int i = 0; i < buf.length - dataOffset; i++) {
				
			byte a = buf[dataOffset + i];
		      	byte b = wsf.maskingKey[i % 4];
			buf[dataOffset + i] = (byte)((a | b) & (~a | ~b));
			// because too simple to xor
		}

		return buf;
	}
}

