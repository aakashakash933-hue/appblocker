package com.appblocker.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appblocker.core.ui.theme.Dimensions

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) colorScheme.primary.copy(alpha = 0.3f) else colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.PaddingMedium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) colorScheme.primary else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimensions.IconLarge)
                )
                Spacer(modifier = Modifier.width(Dimensions.PaddingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }
                
                // Status pill
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (isGranted) {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFFFF3E0)
                    },
                    modifier = Modifier.padding(start = Dimensions.PaddingSmall)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Dimensions.PaddingSmall, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isGranted) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGranted) "Granted" else "Required",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
            
            if (!isGranted) {
                Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Grant Permission")
                }
            } else {
                Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
                OutlinedButton(
                    onClick = onRequestPermission,
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reverify", color = colorScheme.primary)
                }
            }
        }
    }
}
