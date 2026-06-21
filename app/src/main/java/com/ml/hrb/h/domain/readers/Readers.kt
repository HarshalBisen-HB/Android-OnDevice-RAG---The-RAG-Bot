// Author: Harshal R. Bisen
// Utility classes and factory for instantiating different document readers.

package com.ml.hrb.h.domain.readers



class Readers {
    enum class DocumentType {
        PDF,
        MS_DOCX,
        PLAIN_TEXT,
        MARKDOWN,
    }

    companion object {
        fun getReaderForDocType(docType: DocumentType): Reader =
            when (docType) {
                DocumentType.PDF -> PDFReader()
                DocumentType.MS_DOCX -> DOCXReader()
                DocumentType.MARKDOWN -> MarkdownReader()
                DocumentType.PLAIN_TEXT -> TextFileReader()
            }
    }
}

fun Readers.DocumentType.getMimeType(): String = when (this) {
    Readers.DocumentType.PDF -> "application/pdf"
    Readers.DocumentType.MS_DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    Readers.DocumentType.PLAIN_TEXT -> "text/plain"
    Readers.DocumentType.MARKDOWN -> "text/markdown"
}