package com.originalstocks.printstocksdemo

import android.app.Application
import com.originalstocks.printstocks.PrintStocks

public class ApplicationRoot : Application() {

    override fun onCreate() {
        super.onCreate()
        PrintStocks.init(this)
    }
}