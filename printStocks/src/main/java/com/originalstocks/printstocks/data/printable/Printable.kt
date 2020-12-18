package com.originalstocks.printstocks.data.printable

import com.originalstocks.printstocks.data.printer.Printer

/**
 * Interface for conversion any string into ByteArray
 * */
interface Printable {
    fun getPrintableByteArray(printer: Printer): List<ByteArray>
}