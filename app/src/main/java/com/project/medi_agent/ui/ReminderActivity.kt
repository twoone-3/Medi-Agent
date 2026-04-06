package com.project.medi_agent.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.medi_agent.ui.theme.MediAgentTheme

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupLockScreenFlags()

        val medicineName = intent.getStringExtra("EXTRA_MEDICINE_NAME") ?: "药品"
        val dosage = intent.getStringExtra("EXTRA_DOSAGE") ?: ""

        setContent {
            MediAgentTheme(isSeniorMode = true) { // 强制开启大字号模式
                val viewModel: MainViewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    ReminderContent(
                        medicineName = medicineName,
                        dosage = dosage,
                        onConfirm = {
                            // 记录服药日志
                            viewModel.recordMedicationTaken(medicineName)
                            Toast.makeText(this@ReminderActivity, "已记录服药：$medicineName", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun ReminderContent(medicineName: String, dosage: String, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MedicalServices,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "爷爷/奶奶，该吃药啦！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = medicineName,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                if (dosage.isNotBlank()) {
                    Text(
                        text = "剂量：$dosage",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.DarkGray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(16.dp))
                Text("我已吃完", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
