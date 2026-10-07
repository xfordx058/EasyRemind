package com.app.easyremind.debug

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.app.easyremind.EasyRemindApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImportTestActivity : Activity() {

    private lateinit var label: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        label = TextView(this).apply { textSize = 14f; setPadding(32, 64, 32, 32) }
        setContentView(label)
        val path = intent.getStringExtra(EXTRA_PATH)
        label.text = "Importing from $path …"
        CoroutineScope(Dispatchers.IO).launch {
            val result = try {
                val repo = (application as EasyRemindApp).container.repository
                val text = contentResolver.openInputStream(android.net.Uri.parse(path))?.use {
                    it.bufferedReader().readText()
                }
                if (text == null) {
                    Log.e(TAG, "IMPORT_TEST: could not read $path")
                    "FAIL: could not read file"
                } else {
                    val r = repo.importJson(text)
                    val all = repo.getAllClasses()
                    Log.i(TAG, "IMPORT_TEST result: $r")
                    Log.i(TAG, "IMPORT_TEST count: ${all.size}")
                    all.sortedBy { it.startMin }.forEach {
                        Log.i(
                            TAG,
                            "IMPORT_TEST class: ${it.subjectName} | days=${it.daysBitmask} " +
                                "start=${it.startMin} end=${it.endMin} enabled=${it.isEnabled}",
                        )
                    }
                    "DONE: $r | total classes in DB: ${all.size}"
                }
            } catch (t: Throwable) {
                Log.e(TAG, "IMPORT_TEST failed", t)
                "FAIL: ${t.message}"
            }
            runOnUiThread {
                label.text = result
                Log.i(TAG, "IMPORT_TEST finished: $result")
            }
        }
    }

    companion object {
        private const val TAG = "ImportTest"
        const val EXTRA_PATH = "path"
    }
}
