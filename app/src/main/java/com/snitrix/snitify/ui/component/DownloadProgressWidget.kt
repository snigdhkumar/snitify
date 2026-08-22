package com.snitrix.snitify.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.R
import com.snitrix.snitify.ui.theme.LocalAppThemeColors

@Composable
fun DownloadProgressWidget(
    percent: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppThemeColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(end = 8.dp)
    ) {
        Text(
            text = "${percent}%",
            color = appColors.primaryAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = "Cancel Download",
            tint = Color(0xFFFF5252),
            modifier = Modifier
                .size(18.dp)
                .clickable { onCancel() }
        )
    }
}
