package com.sans.finance.data.util

import android.content.Context
import android.util.Base64
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

object GcsPortfolioSyncer {

    private const val BUCKET_NAME = "ichsanul-portfolio-snapshots"
    private const val OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"

    // Load credentials from assets
    private fun loadCredentials(context: Context): JSONObject {
        val jsonString = context.assets.open("SA_cred_general.json").use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
        return JSONObject(jsonString)
    }

    // Generate JWT Assertion for Google OAuth2
    private fun generateJwt(clientEmail: String, privateKeyPem: String): String {
        val iat = System.currentTimeMillis() / 1000
        val exp = iat + 3600

        val header = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }

        val claims = JSONObject().apply {
            put("iss", clientEmail)
            put("scope", "https://www.googleapis.com/auth/devstorage.read_only")
            put("aud", OAUTH_TOKEN_URL)
            put("exp", exp)
            put("iat", iat)
        }

        val headerBase64 = base64UrlEncode(header.toString().toByteArray(Charsets.UTF_8))
        val claimsBase64 = base64UrlEncode(claims.toString().toByteArray(Charsets.UTF_8))
        val stringToSign = "$headerBase64.$claimsBase64"

        val privateKey = parsePrivateKey(privateKeyPem)
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(stringToSign.toByteArray(Charsets.UTF_8))
        }
        val signatureBytes = signature.sign()
        val signatureBase64 = base64UrlEncode(signatureBytes)

        return "$stringToSign.$signatureBase64"
    }

    private fun base64UrlEncode(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE).trim()
    }

    private fun parsePrivateKey(pem: String): java.security.PrivateKey {
        val privateKeyDer = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .replace("\n", "")

        val keyBytes = Base64.decode(privateKeyDer, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    // Request Access Token from Google OAuth2
    private suspend fun getAccessToken(context: Context): String = withContext(Dispatchers.IO) {
        val creds = loadCredentials(context)
        val clientEmail = creds.getString("client_email")
        val privateKey = creds.getString("private_key")

        val assertion = generateJwt(clientEmail, privateKey)
        val params = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
                "&assertion=" + URLEncoder.encode(assertion, "UTF-8")

        val url = URL(OAUTH_TOKEN_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        conn.outputStream.use { os ->
            os.write(params.toByteArray(Charsets.UTF_8))
        }

        if (conn.responseCode != 200) {
            val errStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Failed to get OAuth token: ${conn.responseCode} - $errStream")
        }

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        json.getString("access_token")
    }

    // List and Fetch snapshot content from GCS
    suspend fun downloadLatestSnapshot(context: Context): Triple<Long, List<PortfolioHoldingEntity>, Double?> = withContext(Dispatchers.IO) {
        val token = getAccessToken(context)

        // 1. List GCS objects to find the latest snapshot file
        val listUrl = URL("https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o?prefix=snapshots/")
        val listConn = listUrl.openConnection() as HttpURLConnection
        listConn.setRequestProperty("Authorization", "Bearer $token")
        listConn.setRequestProperty("Accept", "application/json")

        if (listConn.responseCode != 200) {
            val errStream = listConn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Failed to list snapshots: ${listConn.responseCode} - $errStream")
        }

        val listResponse = listConn.inputStream.bufferedReader().use { it.readText() }
        val listJson = JSONObject(listResponse)
        val items = listJson.optJSONArray("items") ?: throw Exception("No snapshots found in GCS bucket.")

        // Find the latest snapshot by sorting name (which starts with date)
        var latestSnapshotName: String? = null
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val name = item.getString("name")
            if (name.endsWith("_snapshot.json")) {
                if (latestSnapshotName == null || name > latestSnapshotName) {
                    latestSnapshotName = name
                }
            }
        }

        if (latestSnapshotName == null) {
            throw Exception("No snapshot JSON file found in bucket.")
        }

        // 2. Download the contents of the latest snapshot file
        val encodedName = URLEncoder.encode(latestSnapshotName, "UTF-8")
        val downloadUrl = URL("https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o/$encodedName?alt=media")
        val downloadConn = downloadUrl.openConnection() as HttpURLConnection
        downloadConn.setRequestProperty("Authorization", "Bearer $token")

        if (downloadConn.responseCode != 200) {
            val errStream = downloadConn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("Failed to download snapshot: ${downloadConn.responseCode} - $errStream")
        }

        val jsonString = downloadConn.inputStream.bufferedReader().use { it.readText() }
        
        // Parse it using our existing importer
        PortfolioJsonImporter.parseContent(jsonString)
    }
}
