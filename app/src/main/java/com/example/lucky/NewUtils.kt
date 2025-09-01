package com.example.lucky

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

object NewUtils {
    var isAppActive = true
    var private_key: PrivateKey? = null
    var public_key: PublicKey? = null

    fun String.addDashesToCardNumber(): String {
        // input: "1234567890123456"
        // output: "1234-5678-9012-3456"
        val formattedText = StringBuilder()
        for ((index, char) in this.withIndex()) {
            if (index % 4 == 0 && index != 0) {
                formattedText.append("-")
            }
            formattedText.append(char)
        }
        return formattedText.toString()
    }


    fun String.toSafeLong(): Long {
        return this.toLongOrNull() ?: 0L
    }


    // ... (rest of the code remains the same)

    fun String.copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Device ID", this)
        clipboard.setPrimaryClip(clip)
    }

    // -----------------------------
    // RSA-OAEP (SHA-256) utilities
    // -----------------------------

    // Load PKCS#8 private key from PEM file placed in res/raw (e.g., R.raw.private_key)
    fun loadPrivateKeyFromRaw(context: Context, rawResId: Int): PrivateKey {
        val pem = StringBuilder()
        context.resources.openRawResource(rawResId).use { ins ->
            BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).use { br ->
                var line: String? = br.readLine()
                while (line != null) {
                    if (!line.startsWith("-----")) pem.append(line.trim())
                    line = br.readLine()
                }
            }
        }
        val der = Base64.decode(pem.toString(), Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(der)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(keySpec)
    }

    // Decrypt a Base64-encoded RSA-OAEP(SHA-256) ciphertext to UTF-8 string
    fun decryptRsaOaepSha256Base64(privateKey: PrivateKey?, b64Cipher: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams)
        val plaintext = cipher.doFinal(Base64.decode(b64Cipher, Base64.DEFAULT))
        return String(plaintext, StandardCharsets.UTF_8)
    }

    // Safe helper that returns null on any failure or blank input
    fun String.decrypt(): String {
        if (this.isEmpty()) return this
        return try {
            decryptRsaOaepSha256Base64(privateKey = private_key, this)
        } catch (_: Exception) {
            this
        }
    }




    fun showProgressDialog(context: Context, message: String): Dialog {
        var progressDialog = Dialog(context)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.setCancelable(false)

        val view = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val messageTextView = view.findViewById<TextView>(R.id.messageTextView)
        messageTextView.text = message
        progressDialog.setContentView(view)
        progressDialog.show()
        return progressDialog
    }



    fun EditText.scrollByY(scrollView: ScrollView, byY: Int = 400) {
        this.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                scrollView.scrollBy(0, byY)
            }
        }
    }




    fun Context.showToast(message: Any, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message.toString(), duration).show()
    }



    private fun formatCardNumber(text: String): String {
        var formattedText = ""
        for ((index, char) in text.replace(" ", "").withIndex()) {
            if (index % 4 == 0 && index != 0) {
                formattedText += " "
            }
            formattedText += char
        }
        return formattedText
    }

    inline fun <reified T : ViewBinding> Activity.viewBinding(
        crossinline bindingInflater: (LayoutInflater) -> T
    ): Lazy<T> {
        return lazy(LazyThreadSafetyMode.NONE) {
            bindingInflater.invoke(layoutInflater).also {
                setContentView(it.root)
            }
        }
    }

    fun EditText.onTextChange(callback: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                callback(s.toString())


            }
        })
    }


    // Load X.509 SubjectPublicKeyInfo public key from PEM placed in res/raw (e.g., R.raw.public_key)
    fun loadPublicKeyFromRaw(context: Context, rawResId: Int): PublicKey {
        val pem = StringBuilder()
        context.resources.openRawResource(rawResId).use { ins ->
            BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).use { br ->
                var line: String? = br.readLine()
                while (line != null) {
                    if (!line.startsWith("-----")) pem.append(line.trim())
                    line = br.readLine()
                }
            }
        }
        val der = Base64.decode(pem.toString(), Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(der)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(keySpec)
    }


    // Encrypt UTF-8 string to Base64-encoded RSA-OAEP(SHA-256) ciphertext
    fun encryptRsaOaepSha256ToBase64(publicKey: PublicKey?, plaintext: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)
        val ct = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(ct, Base64.NO_WRAP)
    }


    // Convenience: encrypt a string with a given public key; returns original if blank or on failure
    fun String.encrypt(): String {
        if (this.isEmpty()) return this
        return try {
            encryptRsaOaepSha256ToBase64(public_key, this)
        } catch (_: Exception) {
            this
        }
    }


        @Volatile private var cachedPublicKey: PublicKey? = null

        fun getPublicKey(context: Context): PublicKey? {
            cachedPublicKey?.let { return it }
            return try {
                val pk = loadPublicKeyFromRaw(context.applicationContext, R.raw.public_key)
                cachedPublicKey = pk
                pk
            } catch (_: Exception) {
                null
            }
        }

        fun clearCache() { cachedPublicKey = null }







}