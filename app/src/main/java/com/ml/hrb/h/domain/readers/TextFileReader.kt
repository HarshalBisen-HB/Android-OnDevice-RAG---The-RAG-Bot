// Author: Harshal R. Bisen
// Implementation for reading and extracting text from plain text files.

package com.ml.hrb.h.domain.readers


import java.io.InputStream

class TextFileReader : Reader() {
    override fun readFromInputStream(inputStream: InputStream): String {
        return inputStream.bufferedReader().use { it.readText() }
    }
}
