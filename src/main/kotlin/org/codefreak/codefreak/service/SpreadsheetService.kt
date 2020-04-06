package org.codefreak.codefreak.service

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Service

@Service
class SpreadsheetService : BaseService() {

  fun generateCsv(headers: List<String>, rows: Iterable<Iterable<String>>): String {
    val output = StringBuilder()
    val outWriter = CSVPrinter(output, CSVFormat.EXCEL)
    outWriter.printRecord(headers)
    rows.forEach(outWriter::printRecord)
    return output.toString()
  }
}
