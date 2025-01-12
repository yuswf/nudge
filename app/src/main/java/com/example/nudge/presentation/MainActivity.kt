package com.example.nudge.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.nudge.presentation.theme.NudgeTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        // firebase: save user
        saveAndroidIdToFirebase(androidId)

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(androidId)
        }
    }
}

fun saveAndroidIdToFirebase(androidId: String) {
    val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    database.child(androidId).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                println("[$androidId] Device already saved.")
            } else {
                val deviceData = mapOf(
                    "androidId" to androidId,
                    "timestamp" to System.currentTimeMillis(),
                )

                database.child(androidId).setValue(deviceData)
                    .addOnSuccessListener {
                        println("[$androidId] Device successfully saved.")
                    }
                    .addOnFailureListener { e ->
                        println("Something went wrong! [Error]:${e.message}")
                    }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            println("[DatabaseError]: ${error.message}")
        }
    })
}

@Composable
fun WearApp(androidId: String) {
    NudgeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        ) {
            CheckAndEnterName(androidId)
        }
    }
}

@SuppressLint("WearRecents")
@Composable
fun CheckAndEnterName(androidId: String) {
    val database = FirebaseDatabase.getInstance().reference
    val context = LocalContext.current

    var userName by remember { mutableStateOf("") }
    var isNameExists by remember { mutableStateOf(false) }
    var isNameSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        database.child("users").child(androidId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").getValue(String::class.java)
                if (!name.isNullOrEmpty()) {
                    userName = name
                    isNameExists = true
                } else {
                    isNameExists = false
                }
            }
        }
    }

    if (isNameExists) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hello, $userName",
                fontSize = 16.sp
            )
        }

        Text(
            text = "Your ID: $androidId",
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 48.dp, start = 54.dp)
        )
    } else {
        if (isNameSaved) {
            Text(
                text = "Name saved successfully!",
                modifier = Modifier.padding(128.dp),
            )

            Handler(Looper.getMainLooper()).postDelayed({
                val restartIntent =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                restartIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(restartIntent)
            }, 3000)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter your name",
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
                TextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (userName.isNotEmpty()) {
                            val userData = mapOf(
                                "androidId" to androidId,
                                "name" to userName,
                                "friends" to listOf<String>(),
                                "nudges" to listOf<String>(),
                                "timestamp" to System.currentTimeMillis()
                            )
                            database.child("users").child(androidId).setValue(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Name saved!", Toast.LENGTH_SHORT)
                                        .show()
                                    isNameSaved =
                                        true
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Save Name")
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("your_android_id")
}
