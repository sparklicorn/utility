package com.sparklicorn.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Files {

	/**
	 * Reads all content from the InputStream to a String. Decodes the bytes
	 * to UTF-8 characters.
	 * @param in - the InputStream to read from.
	 * @return A String containing the content from the stream.
	 * @throws IOException If an I/O error occurs while reading.
	 */
	public static String readString(InputStream in) throws IOException {
		InputStreamReader reader = new InputStreamReader(in);
		StringBuffer strBuffer = new StringBuffer();
		char[] buffer = new char[1<<14];
		int numRead;
		while ((numRead = reader.read(buffer)) > -1) {
			strBuffer.append(buffer, 0, numRead);
		}
		return strBuffer.toString();
	}
}
