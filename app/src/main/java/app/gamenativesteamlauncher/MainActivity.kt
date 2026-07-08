package app.gamenativesteamlauncher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_ADD_FOLDER = 1001
        private const val REQUEST_CODE_EXPORT_JSON = 1002
        private const val JSON_ASSET_NAME = "Steam.json"
    }

    private lateinit var folderContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentAction = intent.action
        val hasFileUri = (Intent.ACTION_VIEW == intentAction && intent.data != null) ||
                (Intent.ACTION_SEND == intentAction && intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) != null)

        if (hasFileUri) {
            // Background flow: Process immediately and close
            processIncomingIntent()
        } else {
            // Interactive flow: Setup UI
            setContentView(R.layout.activity_main)
            folderContainer = findViewById(R.id.folderContainer)

            findViewById<Button>(R.id.btnAddFolder).setOnClickListener { requestFolderPermission() }
            findViewById<Button>(R.id.btnExportJson).setOnClickListener { openJsonSavePicker() }

            refreshAllowedFolders()
        }
    }

    private fun refreshAllowedFolders() {
        folderContainer.removeAllViews()
        val permissions = contentResolver.persistedUriPermissions

        if (permissions.isEmpty()) {
            val emptyTv = TextView(this).apply { text = "No folder permissions granted yet." }
            folderContainer.addView(emptyTv)
            return
        }

        for (perm in permissions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val pathTv = TextView(this).apply {
                text = perm.uri.path
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val removeBtn = Button(this).apply {
                text = "Remove"
                setOnClickListener {
                    val revokeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.releasePersistableUriPermission(perm.uri, revokeFlags)
                    refreshAllowedFolders()
                }
            }

            row.addView(pathTv)
            row.addView(removeBtn)
            folderContainer.addView(row)
        }
    }

    private fun requestFolderPermission() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_ADD_FOLDER)
    }

    private fun openJsonSavePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "Steam.patched.json")
        }
        startActivityForResult(intent, REQUEST_CODE_EXPORT_JSON)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {
            REQUEST_CODE_ADD_FOLDER -> {
                data.data?.let { treeUri ->
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(treeUri, takeFlags)
                    refreshAllowedFolders()
                }
            }
            REQUEST_CODE_EXPORT_JSON -> {
                data.data?.let { targetUri ->
                    writeJsonFromAssetsToUri(targetUri)
                }
            }
        }
    }

    private fun writeJsonFromAssetsToUri(targetUri: Uri) {
        try {
            assets.open(JSON_ASSET_NAME).use { inputStream ->
                contentResolver.openOutputStream(targetUri).use { outputStream ->
                    if (outputStream != null) {
                        inputStream.copyTo(outputStream)
                        Toast.makeText(this, "JSON exported successfully!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export JSON file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processIncomingIntent() {
        val intentAction = intent.action
        var fileUri: Uri? = null

        if (Intent.ACTION_VIEW == intentAction) {
            fileUri = intent.data
        } else if (Intent.ACTION_SEND == intentAction) {
            fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        if (fileUri != null) {
            val appId = readFileContentAsInt(fileUri)
            if (appId != null) {
                launchGameIntent(appId)
            } else {
                Toast.makeText(this, "Failed to parse App ID from file.", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun readFileContentAsInt(uri: Uri): Int? {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine()?.trim()?.toInt()
                }
            }
        }.getOrNull()
    }

    private fun launchGameIntent(appId: Int) {
        val gameIntent = Intent("app.gamenative.LAUNCH_GAME").apply {
            setClassName("app.gamenative", "app.gamenative.MainActivity")
            putExtra("app_id", appId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        runCatching {
            startActivity(gameIntent)
        }.onFailure {
            Toast.makeText(this, "Target game app not found.", Toast.LENGTH_SHORT).show()
        }
    }
}