// Author: Harshal R. Bisen
// Base interface for document text extractors/readers.

package com.ml.hrb.h.domain.readers

import java.io.InputStream

abstract class Reader {
    abstract fun readFromInputStream(inputStream: InputStream): String?
}
