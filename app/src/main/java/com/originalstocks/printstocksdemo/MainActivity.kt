package com.originalstocks.printstocksdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.originalstocks.printstocks.PrintStocks
import com.originalstocks.printstocks.data.printable.ImagePrintable
import com.originalstocks.printstocks.data.printable.Printable
import com.originalstocks.printstocks.data.printable.RawPrintable
import com.originalstocks.printstocks.data.printable.TextPrintable
import com.originalstocks.printstocks.data.printer.DefaultPrinter
import com.originalstocks.printstocks.ui.ScannerActivity
import com.originalstocks.printstocks.utilities.Printing
import com.originalstocks.printstocks.utilities.PrintingCallback
import com.originalstocks.printstocksdemo.databinding.ActivityMainBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

class MainActivity : AppCompatActivity(), PrintingCallback {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private var printing: Printing? = null
    private val imageLink = "https://img.icons8.com/pastel-glyph/2x/hamburger.png"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (printing != null) {
            printing?.printingCallback = this
        }

        /*Checking if printer is already paired or not ! */
        if (PrintStocks.getPairedPrinter() != null){
            binding.bluetoothToggleSwitch.text = StringBuilder("Disconnect with ${PrintStocks.getPairedPrinter()?.name}")
            binding.bluetoothToggleSwitch.isChecked = true
            Log.i(TAG, "onCreate has already a paired printer name = ${PrintStocks.getPairedPrinter()?.name} \n Mac address = ${PrintStocks.getPairedPrinter()?.address}")
        } else {
            Log.i(TAG, "onCreate no already paired printers found !")
            binding.bluetoothToggleSwitch.isChecked = false
            binding.bluetoothToggleSwitch.text = StringBuilder(getString(R.string.pair_with_bluetooth))
        }

        binding.bluetoothToggleSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                showToast(this, "Bluetooth is ON Connecting with Nearby Printers")
                if (PrintStocks.hasPairedPrinter()) {
                    PrintStocks.removeCurrentPrinter()
                } else {
                    // if there are no printers found nearby, this activity will launch a screen where you can select new printer.
                    startActivityForResult(
                        Intent(this, ScannerActivity::class.java),
                        ScannerActivity.SCANNING_FOR_PRINTER
                    )
                    changePairAndUnPairStatusUpdate()
                }
            } else {
                showToast(this, "Bluetooth is OFF & Disconnecting with Printer")
                if (PrintStocks.hasPairedPrinter()) {
                    PrintStocks.removeCurrentPrinter()
                    binding.bluetoothToggleSwitch.text = StringBuilder(getString(R.string.pair_with_bluetooth))
                }
            }

        }

        binding.printTextButton.setOnClickListener {
            val textToPrint = binding.editTextToPrint.text.toString()
            if (textToPrint.isNotEmpty()){
                Log.i(TAG, "onCreate_printTextButton_OnClick $textToPrint")
                if (PrintStocks.hasPairedPrinter()) {
                    Log.i(TAG, "onCreate_printTextButton_OnClick = hasPairedPrinter")
                    printTextOnlyOnPrinter(textToPrint)
                } else {
                    Log.e(TAG, "onCreate_printTextButton_OnClick = No Paired Printer")
                    startActivityForResult(
                        Intent(this, ScannerActivity::class.java),
                        ScannerActivity.SCANNING_FOR_PRINTER
                    )
                }
            } else {
                showToast(this, "Enter some text to print.")
                Log.e(TAG, "onCreate_printTextButton_OnClick No text found to print")
            }
        }

        binding.printImageButton.setOnClickListener {
            if (PrintStocks.hasPairedPrinter()) {
                printImageOnPrinter()
            } else {
                startActivityForResult(
                    Intent(this, ScannerActivity::class.java),
                    ScannerActivity.SCANNING_FOR_PRINTER
                )
            }
        }


    }// onCreate closes

    /* Callbacks for printer connectivity & data transfer*/
    override fun connectingWithPrinter() {
        showToast(this, "Connecting with Printer...")
    }

    override fun printingOrderSentSuccessfully() {
        showToast(this, "Package is sent to Printer for printing.")
    }

    override fun connectionFailed(error: String) {
        showToast(this, "Problem found while connection - $error")
    }

    override fun onError(error: String) {
        showToast(this, error)
    }

    override fun onMessage(message: String) {
        showToast(this, message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ScannerActivity.SCANNING_FOR_PRINTER && resultCode == Activity.RESULT_OK) {
            //Printer is ready now, initiate printing process !
            initPrintingProcess()
            changePairAndUnPairStatusUpdate()
        } else {
            showToast(this, "We're unable to process the printing request at this time. Try again in a bit...")
            Log.e(TAG, "onActivityResult_requestCode doesn't match ")
        }
    }

    private fun initPrintingProcess() {
        if (PrintStocks.hasPairedPrinter()) {
            printing = PrintStocks.printer()
        }
        if (printing != null) {
            printing?.printingCallback = this
        }
    }

    private fun changePairAndUnPairStatusUpdate() {
        if (PrintStocks.hasPairedPrinter()) {
            binding.bluetoothToggleSwitch.text =
                StringBuilder("Disconnect with ${PrintStocks.getPairedPrinter()?.name}")
        } else {
            binding.bluetoothToggleSwitch.text = StringBuilder(getString(R.string.pair_with_bluetooth))
        }
    }

    private fun printImageOnPrinter() {
        val printableArrayList: ArrayList<Printable> = ArrayList()
        /*fetching a dummy image from net to print via Picasso*/

        // can accept the URI, too
        Picasso.get().load(R.drawable.dummy_slip).resize(100, 100).into(object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                if (bitmap != null) {
                    binding.printableImageView.setImageBitmap(bitmap)
                    printableArrayList.add(ImagePrintable.Builder(bitmap).build())
                    printing?.print(printableArrayList)
                    initPrintingProcess()
                } else {
                    Log.e(TAG, "onBitmapLoaded Some error occurred while loading bitmap.")
                }
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                Log.e(TAG, "onBitmapLoaded Exception = $e")
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                Log.e(TAG, "onBitmapLoaded Loading bitmap...")
            }
        })

    }

    private fun printTextOnlyOnPrinter(textToPrint: String) {
        Log.i(TAG, "printTextOnlyOnPrinter process initiated")
        val printableArrayList: ArrayList<Printable> = ArrayList()
        printableArrayList.add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build())

        val textBoldHeader = "SUR PLACE\nESP123"
        val textNormalContent = "RESTAURANT NAME\n5 rue sala,\nTel.04.74.98.22.22\nSTREET 43201425400035 - APE 5610C\n" +
                "  RCS LYON TVA INTRA FR27432078939"

        val textTableContent = "#254896-11     10/12/2020 12:56:13\n" +
                "\n" +
                "QTE PRODUIT     UNIT   TOTAL\n" +
                "1X Burger BIO   7.50   8.50\n" +
                "    Bacon\n" +
                "    Chili Sauce\n" +
                "    Sup Oeuf    1.00\n" +
                "2X Green Burger 8.00   16.00\n" +
                "    Barbecue\n" +
                "    Sup Oeuf\n" +
                "\n" +
                "Total TTC              28.30\n" +
                "TVA                     8.60\n" +
                "Total HT               19.70"


        // adding text to printable
        val printableHeader = TextPrintable.Builder()
            .setText(textBoldHeader) //The text you want to print
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD) //Bold or normal
            .setFontSize(DefaultPrinter.FONT_SIZE_LARGE)
            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF) // Underline on/off
            .setCharacterCode(DefaultPrinter.CHARCODE_PC437) // Character code to support languages /** CHARCODE_PC863 for Canadian-French*/
            .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
            .setNewLinesAfter(2) // To provide n lines after sentence
            .build()

        val printableContent = TextPrintable.Builder()
            .setText(textNormalContent) //The text you want to print
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_NORMAL) //Bold or normal
            .setFontSize(DefaultPrinter.FONT_SIZE_NORMAL)
            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF) // Underline on/off
            .setCharacterCode(DefaultPrinter.CHARCODE_PC437) // Character code to support languages /** CHARCODE_PC863 for Canadian-French*/
            .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
            .setNewLinesAfter(1) // To provide n lines after sentence
            .build()

        val printableTable = TextPrintable.Builder()
            .setText(textTableContent) //The text you want to print
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_NORMAL) //Bold or normal
            .setFontSize(DefaultPrinter.FONT_SIZE_NORMAL)
            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF) // Underline on/off
            .setCharacterCode(DefaultPrinter.CHARCODE_PC437) // Character code to support languages /** CHARCODE_PC863 for Canadian-French*/
            .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
            .setNewLinesAfter(1) // To provide n lines after sentence
            .build()

        printableArrayList.add(printableHeader)
        printableArrayList.add(printableContent)
        printableArrayList.add(printableTable)

        printing?.print(printableArrayList)
        initPrintingProcess()
    }


}