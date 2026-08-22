package com.snitrix.snitify.ui.screens.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snitrix.snitify.R
import com.snitrix.snitify.ui.screens.onboarding.components.ArtistCard
import com.snitrix.snitify.ui.screens.onboarding.components.BottomActionBar
import com.snitrix.snitify.ui.screens.onboarding.components.StepHeader

data class OnboardingArtist(
    val name: String,
    val imageUrl: Any,
    val category: String
)

val HINDI_ARTISTS = listOf(
    OnboardingArtist("Arijit Singh", R.drawable.artist_arijit_singh, "Hindi"),
    OnboardingArtist("Shreya Ghoshal", R.drawable.artist_shreya_ghoshal, "Hindi"),
    OnboardingArtist("Sonu Nigam", R.drawable.artist_sonu_nigam, "Hindi"),
    OnboardingArtist("KK", R.drawable.artist_kk, "Hindi"),
    OnboardingArtist("Jubin Nautiyal", R.drawable.artist_jubin_nautiyal, "Hindi"),
    OnboardingArtist("Sunidhi Chauhan", R.drawable.artist_sunidhi_chauhan, "Hindi"),
    OnboardingArtist("Pritam", R.drawable.artist_pritam, "Hindi"),
    OnboardingArtist("A. R. Rahman", R.drawable.artist_ar_rahman, "Hindi"),
    OnboardingArtist("Badshah", R.drawable.artist_badshah, "Hindi"),
    OnboardingArtist("Neha Kakkar", R.drawable.artist_neha_kakkar, "Hindi")
)

val ENGLISH_ARTISTS = listOf(
    OnboardingArtist("Taylor Swift", R.drawable.artist_taylor_swift, "English"),
    OnboardingArtist("Ed Sheeran", R.drawable.artist_ed_sheeran, "English"),
    OnboardingArtist("Coldplay", R.drawable.artist_coldplay, "English"),
    OnboardingArtist("Imagine Dragons", R.drawable.artist_imagine_dragons, "English"),
    OnboardingArtist("Bruno Mars", R.drawable.artist_bruno_mars, "English"),
    OnboardingArtist("The Weeknd", R.drawable.artist_the_weeknd, "English"),
    OnboardingArtist("Billie Eilish", R.drawable.artist_billie_eilish, "English"),
    OnboardingArtist("Dua Lipa", R.drawable.artist_dua_lipa, "English"),
    OnboardingArtist("Adele", R.drawable.artist_adele, "English"),
    OnboardingArtist("Post Malone", R.drawable.artist_post_malone, "English")
)

@Composable
fun ArtistStep(
    selectedArtists: Set<String>,
    selectedLanguages: Set<String> = emptySet(),
    onToggleArtist: (String) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val onboardingDataStore = remember(context) { com.snitrix.snitify.data.datastore.OnboardingDataStore(context) }
    val artistImagesMap by onboardingDataStore.artistImagesMap.collectAsState(initial = emptyMap())

    val isContinueEnabled = selectedArtists.size >= 3
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var activeLanguageForAdd by remember { mutableStateOf<String?>(null) }
    var customLanguageArtistsMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var localCustomArtistImagesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var expandedSections by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Preserve user language selection order (or fallback to Hindi, English)
    val orderedLanguages = remember(selectedLanguages) {
        if (selectedLanguages.isEmpty()) listOf("Hindi", "English")
        else selectedLanguages.toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
    ) {
        StepHeader(
            currentStep = 2,
            totalSteps = 3,
            onBackClick = onBackClick
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title & Subtitle
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Choose artists\nyou enjoy",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Pick at least 3 artists.",
                        color = Color(0xFFA7A7A7),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Language Sections in Order of Selection
            orderedLanguages.forEach { lang ->
                val baseList = when (lang) {
                    "Hindi" -> HINDI_ARTISTS
                    "English" -> ENGLISH_ARTISTS
                    else -> emptyList()
                }
                val customNamesForLang = customLanguageArtistsMap[lang] ?: emptyList()
                val customArtistObjects = customNamesForLang.map { name ->
                    val path = localCustomArtistImagesMap[name] ?: artistImagesMap[name]
                    val imgModel: Any = if (!path.isNullOrBlank()) java.io.File(path) else R.drawable.ic_user
                    OnboardingArtist(name, imgModel, lang)
                }
                val artistList = baseList + customArtistObjects
                val title = if (lang == "Hindi" || lang == "English") "$lang Artists" else "$lang Artists"

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                val isExpanded = expandedSections.contains(lang)
                val displayedArtists = if (isExpanded) artistList else artistList.take(5)

                items(displayedArtists, key = { "${lang}_${it.name}" }) { artist ->
                    ArtistCard(
                        name = artist.name,
                        imageUrl = artist.imageUrl,
                        isSelected = selectedArtists.contains(artist.name),
                        onSelect = { onToggleArtist(artist.name) }
                    )
                }

                // Slot 6: View All card if section not expanded
                if (!isExpanded && artistList.size > 5) {
                    item(key = "${lang}_view_all") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { expandedSections = expandedSections + lang }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color(0xFF242424))
                                    .border(1.5.dp, Color(0xFF3B3B3B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_down),
                                    contentDescription = "View All",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "View All",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // "+ Add New" Artist card at the end of this language section
                item(key = "${lang}_add_new") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                activeLanguageForAdd = lang
                                showAddCustomDialog = true
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E1E))
                                .border(1.5.dp, Color(0xFF333333), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_plus),
                                contentDescription = "Add New Artist",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Add New",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        BottomActionBar(
            buttonText = "Continue",
            enabled = isContinueEnabled,
            onContinueClick = onContinueClick,
            selectionCountText = "${selectedArtists.size} selected"
        )
    }

    if (showAddCustomDialog) {
        com.snitrix.snitify.ui.component.CreateArtistBottomSheet(
            onSave = { name, imagePath ->
                if (name.isNotBlank()) {
                    val targetLang = activeLanguageForAdd ?: orderedLanguages.firstOrNull() ?: "Hindi"
                    val currentList = customLanguageArtistsMap[targetLang] ?: emptyList()
                    if (!currentList.contains(name)) {
                        customLanguageArtistsMap = customLanguageArtistsMap + (targetLang to (currentList + name))
                    }
                    onToggleArtist(name)
                    if (imagePath != null) {
                        localCustomArtistImagesMap = localCustomArtistImagesMap + (name to imagePath)
                        coroutineScope.launch {
                            onboardingDataStore.saveArtistImagePath(name, imagePath)
                        }
                    }
                    showAddCustomDialog = false
                    activeLanguageForAdd = null
                }
            },
            onDismiss = {
                showAddCustomDialog = false
                activeLanguageForAdd = null
            }
        )
    }
}
