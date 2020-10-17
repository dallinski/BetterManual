package com.obsidium.bettermanual

import com.github.ma1co.pmcademo.app.Logger
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

class CustomExceptionHandler : Thread.UncaughtExceptionHandler {
    private val defaultUEH: Thread.UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    override fun uncaughtException(t: Thread, e: Throwable) {
        val result: Writer = StringWriter()
        val printWriter = PrintWriter(result)
        e.printStackTrace(printWriter)
        val stacktrace = result.toString()
        printWriter.close()
        Logger.error(stacktrace)
        defaultUEH.uncaughtException(t, e)
    }

}