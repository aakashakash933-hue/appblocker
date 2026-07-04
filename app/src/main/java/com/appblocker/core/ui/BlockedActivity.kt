package com.appblocker.core.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.appblocker.core.data.AppRepository
import com.appblocker.core.security.PasswordManager
import com.appblocker.core.ui.theme.AppBlockerTheme
import com.appblocker.core.ui.theme.Dimensions
import com.appblocker.core.vpn.ContentFilterVpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockedActivity : ComponentActivity() {
    @Inject lateinit var passwordManager: PasswordManager
    @Inject lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val domain = intent.getStringExtra(EXTRA_DOMAIN).orEmpty()
        setContent {
            AppBlockerTheme {
                BlockedScreen(
                    domain = domain,
                    verifyPassword = { password ->
                        val result = passwordManager.verifyPassword(password)
                        if (result is PasswordManager.AuthResult.Success) {
                            lifecycleScope.launch {
                                repository.setBoolean(AppRepository.CONTENT_FILTER_ENABLED, false)
                                ContentFilterVpnService.stop(this@BlockedActivity)
                                finish()
                            }
                            true
                        } else {
                            false
                        }
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_DOMAIN = "domain"
    }
}

@Composable
private fun BlockedScreen(
    domain: String,
    verifyPassword: (String) -> Boolean,
    onClose: () -> Unit
) {
    var isOverrideVisible by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    // Deep crimson/black radial backdrop
    val backdropBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF421010), Color(0xFF140505)),
        radius = 1200f
    )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropBrush)
                .padding(Dimensions.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing warning/restricted icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3D71).copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFFF3D71),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            Text(
                text = "Website Blocked",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "This site has been restricted by parent protection controls.",
                color = Color(0xFFE4E7EB),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Dimensions.PaddingExtraSmall, bottom = Dimensions.PaddingLarge),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Domain Info Card (Glassmorphic look)
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2C1919).copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.PaddingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Restricted Domain",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFCFCF)
                    )
                    Spacer(modifier = Modifier.height(Dimensions.PaddingExtraSmall))
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingSmall)
            ) {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3D71),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Window")
                }

                if (!isOverrideVisible) {
                    OutlinedButton(
                        onClick = { isOverrideVisible = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Parent Override")
                    }
                }
            }

            // Inline Password Entry (Parent Override)
            AnimatedVisibility(visible = isOverrideVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimensions.PaddingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            isError = false
                        },
                        label = { Text("Parent Password", color = Color(0xFFE4E7EB)) },
                        isError = isError,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (verifyPassword(password)) {
                                    password = ""
                                } else {
                                    isError = true
                                }
                            }
                        ),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, tint = Color.White, contentDescription = null)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF3D71),
                            unfocusedBorderColor = Color(0xFFE4E7EB)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            text = "Incorrect password. Please try again.",
                            color = Color(0xFFFF3D71),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
                    Button(
                        onClick = {
                            if (verifyPassword(password)) {
                                password = ""
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF140505)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock Filter")
                    }
                }
            }
        }
    }
}
