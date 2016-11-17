package com.jarvanmo.slientpackageinstaller

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.text.format.DateFormat
import okio.BufferedSink
import okio.BufferedSource
import okio.Okio
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by mo on 16-11-17.
 * @author mo
 */
class PmService : Service() {

    val pmExecutor: ExecutorService by lazy {
      Executors.newSingleThreadExecutor()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null || intent.action == null || intent.data == null) {
            return super.onStartCommand(intent, Service.START_FLAG_REDELIVERY, startId)
        }

        val uri = intent.data
        val action = intent.action

        val cmd :Array<String>

        when (action) {
            "com.jarvanmo.install_packages" -> cmd = install(uri)
            "com.jarvanmo.delete_packages" -> cmd = uninstall(uri)
            else -> return super.onStartCommand(intent, flags, startId)
        }


        pmExecutor.execute(PmRunnable(cmd))

        return super.onStartCommand(intent, Service.START_FLAG_REDELIVERY, startId)
    }

    private fun install(data: Uri): Array<String> {
        if ("file" == data.scheme) {
            return arrayOf("pm", "install", "-r", data.path)
        } else {
            return arrayOf()
        }
    }

    private fun uninstall(data: Uri): Array<String> {
        if ("file" == data.scheme) {
            return arrayOf("pm", "uninstall", data.path)
        } else {
            return arrayOf()
        }
    }


    private inner class PmRunnable internal constructor(args: Array<String>):Runnable {

        private val processBuilder: ProcessBuilder
        private var process:Process? = null
        private var command = ""

        init {
            processBuilder = ProcessBuilder(*args)
            for (s in args)
            {
                command = command + " " + s + ""
            }
        }

        override fun run() {
            try {
                process = processBuilder.start()
                handleProcessInfo(process!!)
            }catch (e : Exception){
                handleExceptionFromProcess(e)
            }finally {
                finallyDo()
            }

        }


        private fun handleExceptionFromProcess(e: Exception) {
            val time = DateFormat.format("yyyy年MM月dd日,kk:mm", System.currentTimeMillis()).toString()
            val log = time + "\n" + "Exception:" + e.javaClass.simpleName + " message:" + e.message + "\n"
            try {
                writeLog(log)
            } catch (e1: IOException) {
                e1.printStackTrace()
            }

        }

        private fun handleProcessInfo(process: Process){

            val time  = DateFormat.format("yyyy年MM月dd日,kk:mm",System.currentTimeMillis()).toString()
            var source:BufferedSource

            source = Okio.buffer(Okio.source(process.inputStream))
            var txtToWrite = "--->\n$time:try to  $command\n"
            txtToWrite = txtToWrite + "success info:" + source.readUtf8() + "\n"

            source = Okio.buffer(Okio.source(process.errorStream))
            txtToWrite = txtToWrite + "error info:" + source.readUtf8() + "<---\n"
            source.close()
            writeLog(txtToWrite)
        }


        private fun writeLog(log : String){
            val writeLog : Boolean
            val sd = this@PmService.externalCacheDir
            val file = File(sd.absolutePath + "/log")
            writeLog = file.exists()  || file.mkdirs()

            if (!writeLog){
                return
            }

            val logFile = File(file.absolutePath + "/pminstaller.log")
            val sink:BufferedSink
            if(logFile.createNewFile()){
                sink = Okio.buffer(Okio.sink(logFile))
            }else{
                sink = Okio.buffer(Okio.appendingSink(logFile))
            }
            sink.writeUtf8(log)
            sink.close()
        }

        private fun finallyDo(){
            process?.destroy()

            if (pmExecutor.isTerminated || pmExecutor.isShutdown) {
                stopSelf()
            }
        }
    }
}



