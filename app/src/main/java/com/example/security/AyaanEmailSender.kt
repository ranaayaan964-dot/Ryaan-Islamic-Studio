package com.example.security

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.net.ssl.SSLSocketFactory

object AyaanEmailSender {
    suspend fun sendPasswordResetOtpEmail(email: String, otpCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sender = BuildConfig.GMAIL_SENDER_EMAIL.trim()
            val password = BuildConfig.GMAIL_16_DIGIT_APP_PASSWORD.replace(" ", "")
            require(sender.isNotBlank()) { "Gmail sender email is not configured." }
            require(password.isNotBlank()) { "Gmail app password is not configured." }
            require(email.contains("@") && otpCode.matches(Regex("\\d{6}"))) { "Invalid email or verification code." }

            val socket = SSLSocketFactory.getDefault().createSocket("smtp.gmail.com", 465)
            socket.use {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
                fun readResponse(): String {
                    val lines = mutableListOf<String>()
                    do {
                        val line = reader.readLine() ?: return lines.joinToString("\n")
                        lines += line
                    } while (line.length >= 4 && line[3] == '-')
                    return lines.joinToString("\n")
                }
                fun command(value: String) {
                    writer.write(value + "\r\n")
                    writer.flush()
                    val response = readResponse()
                    if (!response.startsWith("220") && !response.startsWith("250") && !response.startsWith("235") && !response.startsWith("334") && !response.startsWith("354")) {
                        throw IllegalStateException("SMTP command failed: $response")
                    }
                }

                readResponse().also { if (!it.startsWith("220")) throw IllegalStateException("SMTP connection failed: $it") }
                command("EHLO ayaan-islamic-studio")
                command("AUTH LOGIN")
                command(Base64.getEncoder().encodeToString(sender.toByteArray(StandardCharsets.UTF_8)))
                command(Base64.getEncoder().encodeToString(password.toByteArray(StandardCharsets.UTF_8)))
                command("MAIL FROM:<$sender>")
                command("RCPT TO:<$email>")
                command("DATA")

                val body = buildString {
                    append("From: Ayaan Islamic Studio <$sender>\r\n")
                    append("To: <$email>\r\n")
                    append("Subject: Ayaan Islamic Studio Password Reset Code\r\n")
                    append("MIME-Version: 1.0\r\n")
                    append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
                    append("Your Ayaan Islamic Studio password reset verification code is: $otpCode\r\n")
                    append("This code expires in 10 minutes. Do not share it with anyone.\r\n")
                    append(".\r\n")
                }
                writer.write(body)
                writer.flush()
                val dataResponse = readResponse()
                if (!dataResponse.startsWith("250")) throw IllegalStateException("SMTP DATA failed: $dataResponse")
                writer.write("QUIT\r\n")
                writer.flush()
                readResponse()
            }
        }
    }
}
