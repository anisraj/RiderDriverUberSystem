package me.anisjamadar.ridebookinguberpartnerapp.utils

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PartnerAlertDialog(
    title: String,
    text: String,
    okButtonText: String,
    onOkClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(5.dp),
                onClick = {
                    onOkClick()
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = Color.Black
                )
            ) {
                Text(okButtonText)
            }
        },
        dismissButton = {
            Button(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(5.dp),
                onClick = {
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black,
                    containerColor = Color.White
                )
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = title,
                color = Color.Black
            )
        },
        text = {
            Text(text)
        }
    )
}
