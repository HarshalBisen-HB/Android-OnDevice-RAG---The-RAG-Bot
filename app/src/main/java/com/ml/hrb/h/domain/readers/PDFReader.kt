// Author: Harshal R. Bisen
// Implementation for reading and extracting text from PDF documents.

package com.ml.hrb.h.domain.readers

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import java.io.InputStream

class PDFReader : Reader() {
    override fun readFromInputStream(inputStream: InputStream): String {
        val pdfReader = PdfReader(inputStream)
        var pdfText = ""
        for (i in 1..pdfReader.numberOfPages) {
            pdfText += "\n" + PdfTextExtractor.getTextFromPage(pdfReader, i)
        }
        return pdfText
    }
}
