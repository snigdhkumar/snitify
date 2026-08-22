package com.snitrix.snitify.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.R
import com.snitrix.snitify.ui.screens.settings.components.DeveloperInfoCard
import com.snitrix.snitify.ui.screens.settings.components.DeveloperInfoRow
import com.snitrix.snitify.ui.screens.settings.components.SettingsSection

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "App & Developer Details",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // 1. Top Card (Logo & App Summary)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF181818))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logosnitify),
                            contentDescription = "Snitify Logo",
                            modifier = Modifier.height(44.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Version 6",
                            color = com.snitrix.snitify.ui.theme.LocalAppThemeColors.current.primaryAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Snitify is a minimal, fast, and feature-rich music player designed for high quality local and streaming audio.",
                            color = Color(0xFFA7A7A7),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Section 1: APP DETAILS
            item {
                SettingsSection(title = "APP DETAILS") {
                    DeveloperInfoCard {
                        Column {
                            DeveloperInfoRow(label = "Version", value = "6")
                            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
                            DeveloperInfoRow(label = "Build Date", value = "09-08-2026")
                            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
                            DeveloperInfoRow(label = "Package Name", value = "com.snitrix.snitify")
                        }
                    }
                }
            }

            // Section 2: DEVELOPER
            item {
                SettingsSection(title = "DEVELOPER") {
                    DeveloperInfoCard {
                        Column {
                            DeveloperInfoRow(label = "Developer", value = "Snigdh", onClick = null)
                            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
                            DeveloperInfoRow(
                                label = "Website",
                                value = "View",
                                onClick = { openUrl("https://developer.snitrix.in") }
                            )
                            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
                            DeveloperInfoRow(
                                label = "GitHub",
                                value = "View",
                                onClick = { openUrl("https://github.com/snigdhkumar/snitify") }
                            )
                        }
                    }
                }
            }

            // Section 3: TERMS & CONDITIONS
            item {
                SettingsSection(title = "TERMS & CONDITIONS") {
                    DeveloperInfoCard {
                        Column {
                            DeveloperInfoRow(
                                label = "Privacy Policy",
                                value = "View",
                                onClick = { openUrl("https://snitify.snitrix.in/privacy") }
                            )
                            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
                            DeveloperInfoRow(
                                label = "Terms of Service",
                                value = "View",
                                onClick = { openUrl("https://snitify.snitrix.in/terms") }
                            )
                        }
                    }
                }
            }

            // Section 4: OPEN SOURCE
            item {
                SettingsSection(title = "OPEN SOURCE") {
                    DeveloperInfoCard {
                        Column {
                            DeveloperInfoRow(
                                label = "Source Code",
                                value = "View",
                                onClick = { openUrl("https://github.com/snigdhkumar/snitify") }
                            )
                            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
                            DeveloperInfoRow(
                                label = "Open Source Licenses",
                                value = "View",
                                onClick = { openUrl("https://snitify.snitrix.in/licenses") }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}
