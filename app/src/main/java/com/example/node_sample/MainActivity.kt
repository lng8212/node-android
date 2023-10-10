package com.example.node_sample

import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.node_sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var _startedNodeAlready = false

    private lateinit var apiService: ApiService

    companion object {
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("node")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getIP()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:3000") // Replace with your server's IP or domain
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
        if (!_startedNodeAlready) {
            _startedNodeAlready = true
            Thread {
                //The path where we expect the node project to be at runtime.
                //The path where we expect the node project to be at runtime.
                val nodeDir = applicationContext.filesDir.absolutePath + "/nodejs-project"
                if (wasAPKUpdated()) {
                    //Recursively delete any existing nodejs-project.
                    val nodeDirReference = File(nodeDir)
                    if (nodeDirReference.exists()) {
                        deleteFolderRecursively(File(nodeDir))
                    }
                    //Copy the node project from assets into the application's data path.
                    copyAssetFolder(applicationContext.assets, "nodejs-project", nodeDir)
                    saveLastUpdateTime()
                }
                startNodeWithArguments(
                    arrayOf(
                        "node",
                        "$nodeDir/main.js"
                    )
                )
            }.start()
        }
        handleClick()
    }

    private fun wasAPKUpdated(): Boolean {
        val prefs = applicationContext.getSharedPreferences("NODEJS_MOBILE_PREFS", MODE_PRIVATE)
        val previousLastUpdateTime = prefs.getLong("NODEJS_MOBILE_APK_LastUpdateTime", 0)
        var lastUpdateTime: Long = 1
        try {
            val packageInfo = applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName, 0
            )
            lastUpdateTime = packageInfo.lastUpdateTime
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return lastUpdateTime != previousLastUpdateTime
    }

    private fun saveLastUpdateTime() {
        var lastUpdateTime: Long = 1
        try {
            val packageInfo = applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName, 0
            )
            lastUpdateTime = packageInfo.lastUpdateTime
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val prefs = applicationContext.getSharedPreferences("NODEJS_MOBILE_PREFS", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong("NODEJS_MOBILE_APK_LastUpdateTime", lastUpdateTime)
        editor.apply()
    }

    private fun handleClick() {
        binding.btnVolPlus.setOnClickListener { //Network operations should be done in the background.
            lifecycleScope.launch {
                val data = DataModel("+", "value +") // Replace with your data

                val res = apiService.sendData(data)
                if (res.isSuccessful) {
                    Log.e("NODEJS-MOBILE", "onResponse:${res.body()?.message} ")
                }
            }
        }
        binding.btnVolMinus.setOnClickListener {
            lifecycleScope.launch {
                val data = DataModel("-", "value -") // Replace with your data

                val res = apiService.sendData(data)
                if (res.isSuccessful) {
                    Log.e("NODEJS-MOBILE", "onResponse:${res.body()?.message} ")
                }
            }
        }
        binding.btnHome.setOnClickListener {
            lifecycleScope.launch {
                val data = DataModel("home", "value home") // Replace with your data

                val res = apiService.sendData(data)
                if (res.isSuccessful) {
                    Log.e("NODEJS-MOBILE", "onResponse:${res.body()?.message} ")
                }
            }
        }
        binding.btnMenu.setOnClickListener {
            lifecycleScope.launch {
                val data = DataModel("menu", "value menu") // Replace with your data

                val res = apiService.sendData(data)
                if (res.isSuccessful) {
                    Log.e("NODEJS-MOBILE", "onResponse:${res.body()?.message} ")
                }
            }
        }
    }

    private fun getIP() {
        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        val ipAddress: String = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        binding.txtIp.text = ipAddress
    }

    private external fun startNodeWithArguments(arguments: Array<String>): Int

    private fun deleteFolderRecursively(file: File): Boolean {
        return try {
            var res = true
            for (childFile in file.listFiles()) {
                res = if (childFile.isDirectory) {
                    res and deleteFolderRecursively(childFile)
                } else {
                    res and childFile.delete()
                }
            }
            res = res and file.delete()
            res
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyAssetFolder(
        assetManager: AssetManager,
        fromAssetPath: String,
        toPath: String
    ): Boolean {
        return try {
            val files = assetManager.list(fromAssetPath)
            var res = true
            if (files.isNullOrEmpty()) {
                //If it's a file, it won't have any assets "inside" it.
                res = res and copyAsset(
                    assetManager,
                    fromAssetPath,
                    toPath
                )
            } else {
                File(toPath).mkdirs()
                for (file in files) res = res and copyAssetFolder(
                    assetManager,
                    "$fromAssetPath/$file",
                    "$toPath/$file"
                )
            }
            res
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyAsset(
        assetManager: AssetManager,
        fromAssetPath: String,
        toPath: String
    ): Boolean {
        var `in`: InputStream? = null
        var out: OutputStream? = null
        return try {
            `in` = assetManager.open(fromAssetPath)
            File(toPath).createNewFile()
            out = FileOutputStream(toPath)
            copyFile(`in`, out)
            `in`.close()
            `in` = null
            out.flush()
            out.close()
            out = null
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }
}