package com.example.filepickertest

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.filepickertest.ui.theme.FilePickerTestTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilePickerTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val localContext= LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    val mimeTypeFilter = arrayOf("*/*") // Allow all file types
                    val MAX_FILE_SIZE_BYTES = 4 * 1024 * 1024 // 4 MB in bytes

                    // multi file picker
                    val selectedFiles = remember { mutableStateListOf<Uri>() }
                    val selectFileActivity =
                        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) { result ->
                            result.take(5).forEach { fileUri ->
                                // Check file size before adding to selectedFiles list
                                if (fileUri.getFileSize(localContext) <= MAX_FILE_SIZE_BYTES) {
                                    selectedFiles.add(fileUri)
                                } else {
                                    // Handle file size exceeding limit
                                    Toast.makeText(
                                        applicationContext,
                                        "File ${fileUri.getFileName(applicationContext)} exceeds size limit",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                    // UI
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = {
                            coroutineScope.launch {
                                selectFileActivity.launch(mimeTypeFilter)
                            }
                        }) {
                            Text(text = "Select Files")
                        }

                        selectedFiles.forEach { fileUri ->
                            Text(text = fileUri.toString())
                        }
                    }
                }
            }
        }
    }
}


fun Uri.getFileSize(context: Context): Long {
    val cursor = context.contentResolver.query(this, null, null, null, null)
    val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)
    cursor?.moveToFirst()
    val size = cursor?.getLong(sizeIndex ?: -1) ?: 0
    cursor?.close()
    return size
}



// extension functions
fun Uri.getFileName(context: Context): String? {
    val cursor = context.contentResolver.query(this, null, null, null, null)
    if (cursor == null || !cursor.moveToFirst()) return null

    val indexName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val fileName = cursor.getString(indexName)
    cursor.close()

    return fileName
}

fun Uri.getFile(context: Context): File? {
    val fileDescriptor = context.contentResolver.openFileDescriptor(this, "r", null)
    if (fileDescriptor == null) return null

    val file = File(context.cacheDir, getFileName(context)!!)
    val fileOutputStream = FileOutputStream(file)

    val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
    fileInputStream.copyTo(fileOutputStream)
    fileDescriptor.close()

    return file
}