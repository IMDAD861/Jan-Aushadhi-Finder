package com.example.janaushadhifindernew

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

// ---------------- DATA MODELS ----------------

data class Medicine(
    val brand: String,
    val generic: String,
    val bPrice: Double,
    val gPrice: Double,
    val info: String
)

data class Store(
    val name: String,
    val distance: Double,
    val status: String
)

data class Reminder(
    val medName: String,
    val date: String
)

// ---------------- LOAD JSON ----------------

fun loadMedicinesFromJson(
    context: android.content.Context
): List<Medicine> {

    val jsonString = context.assets
        .open("medicine_list.json")
        .bufferedReader()
        .use { it.readText() }

    val listType =
        object : TypeToken<List<Medicine>>() {}.type

    return Gson().fromJson(jsonString, listType)
}

// ---------------- MAIN ACTIVITY ----------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            var currentTab by remember {
                mutableStateOf("Search")
            }

            var isKannada by remember {
                mutableStateOf(false)
            }

            MaterialTheme(

                colorScheme = lightColorScheme(
                    primary = Color(0xFF0056B3),
                    secondary = Color(0xFF28A745),
                    surface = Color.White
                )

            ) {

                Scaffold(

                    bottomBar = {

                        NavigationBar(
                            containerColor = Color.White
                        ) {

                            NavigationBarItem(

                                selected =
                                    currentTab == "Search",

                                onClick = {
                                    currentTab = "Search"
                                },

                                label = {

                                    Text(
                                        if (isKannada)
                                            "ಹುಡುಕಿ"
                                        else
                                            "Search"
                                    )
                                },

                                icon = {
                                    Icon(
                                        Icons.Default.Search,
                                        null
                                    )
                                }
                            )

                            NavigationBarItem(

                                selected =
                                    currentTab == "Map",

                                onClick = {
                                    currentTab = "Map"
                                },

                                label = {

                                    Text(
                                        if (isKannada)
                                            "ಸ್ಥಳ"
                                        else
                                            "Locator"
                                    )
                                },

                                icon = {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        null
                                    )
                                }
                            )

                            NavigationBarItem(

                                selected =
                                    currentTab == "Reminders",

                                onClick = {
                                    currentTab = "Reminders"
                                },

                                label = {

                                    Text(
                                        if (isKannada)
                                            "ನೆನಪು"
                                        else
                                            "Refill"
                                    )
                                },

                                icon = {
                                    Icon(
                                        Icons.Default.DateRange,
                                        null
                                    )
                                }
                            )
                        }
                    }

                ) { innerPadding ->

                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        when (currentTab) {

                            "Search" -> {
                                MedicineSearchScreen(
                                    isKannada,
                                    onLanguageToggle = {
                                        isKannada = !isKannada
                                    }
                                )
                            }

                            "Map" -> {
                                StoreLocatorScreen(isKannada)
                            }

                            "Reminders" -> {
                                ReminderScreen(isKannada)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- FUZZY SEARCH ----------------

fun fuzzyMatch(
    query: String,
    target: String
): Boolean {

    if (query.isEmpty()) return false

    val q = query.lowercase().trim()
    val t = target.lowercase().trim()

    return t.contains(q) ||
            (q.length > 3 && t.startsWith(q.take(3)))
}

// ---------------- SEARCH SCREEN ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineSearchScreen(
    isKannada: Boolean,
    onLanguageToggle: () -> Unit
) {

    val context = LocalContext.current

    var searchQuery by remember {
        mutableStateOf("")
    }

    // LOAD FROM JSON
    val medicineDatabase = remember {
        loadMedicinesFromJson(context)
    }

    val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        uri?.let {

            try {

                val image =
                    InputImage.fromFilePath(context, it)

                recognizer.process(image)

                    .addOnSuccessListener { visionText ->

                        val extractedText = visionText.text

                        val detectedMedicine =
                            medicineDatabase.firstOrNull { med ->

                                extractedText.contains(
                                    med.brand,
                                    ignoreCase = true
                                ) ||

                                        extractedText.contains(
                                            med.generic,
                                            ignoreCase = true
                                        )
                            }

                        searchQuery =
                            detectedMedicine?.brand ?: ""

                        if (searchQuery.isEmpty()) {

                            Toast.makeText(
                                context,
                                "No matching medicine found",
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            Toast.makeText(
                                context,
                                "Prescription scanned successfully",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    .addOnFailureListener {

                        Toast.makeText(
                            context,
                            "Failed to scan prescription",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    context,
                    "Error reading image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val filteredList = medicineDatabase.filter {

        fuzzyMatch(searchQuery, it.brand) ||
                fuzzyMatch(searchQuery, it.generic)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                if (isKannada)
                    "ಜನ-ಔಷಧಿ ಫೈಂಡರ್ ನ್ಯೂ"
                else
                    "Jan Aushadhi Finder New",

                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0056B3)
            )

            TextButton(
                onClick = onLanguageToggle
            ) {

                Text(
                    if (isKannada)
                        "English"
                    else
                        "ಕನ್ನಡ"
                )
            }
        }

        // OCR BUTTON
        Button(

            onClick = {
                launcher.launch("image/*")
            },

            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),

            shape = RoundedCornerShape(12.dp)

        ) {

            Icon(Icons.Default.AddCircle, null)

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                if (isKannada)
                    "ಪ್ರಿಸ್ಕ್ರಿಪ್ಷನ್ ಸ್ಕ್ಯಾನ್ ಮಾಡಿ (GenAI)"
                else
                    "Scan Prescription (GenAI)"
            )
        }

        // SEARCH FIELD
        OutlinedTextField(

            value = searchQuery,

            onValueChange = {
                searchQuery = it
            },

            modifier = Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    if (isKannada)
                        "ಔಷಧಿ ಹುಡುಕಿ..."
                    else
                        "Search 500+ medicines..."
                )
            },

            leadingIcon = {
                Icon(Icons.Default.Search, null)
            },

            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // MEDICINE LIST
        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            items(filteredList) { med ->

                MedicineSavingsCard(
                    med,
                    isKannada
                )
            }
        }
    }
}

// ---------------- MEDICINE CARD ----------------

@Composable
fun MedicineSavingsCard(
    med: Medicine,
    isKannada: Boolean
) {

    val context = LocalContext.current

    var quantity by remember {
        mutableStateOf("1")
    }

    val qtyInt = quantity.toIntOrNull() ?: 0

    val totalGeneric = med.gPrice * qtyInt

    val savings =
        (med.bPrice - med.gPrice) * qtyInt

    Card(

        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            Color.White
        ),

        border = BorderStroke(
            1.dp,
            Color(0xFFEEEEEE)
        )

    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        med.brand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        "${if (isKannada) "ಜೆನೆರಿಕ್ ಸಾಲ್ಟ್" else "Generic Salt"}: ${med.generic}",
                        color = Color(0xFF28A745)
                    )
                }

                IconButton(
                    onClick = {

                        Toast.makeText(
                            context,
                            med.info,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                ) {

                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        "₹${med.bPrice}",

                        style = TextStyle(
                            textDecoration =
                                TextDecoration.LineThrough
                        ),

                        color = Color.LightGray
                    )

                    Text(
                        "₹${med.gPrice}",

                        fontWeight =
                            FontWeight.ExtraBold,

                        fontSize = 22.sp,

                        color = Color(0xFF0056B3)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 12.dp
                ),

                thickness = 0.5.dp
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedTextField(

                    value = quantity,

                    onValueChange = {

                        if (it.length <= 3)
                            quantity = it
                    },

                    label = {
                        Text("Qty")
                    },

                    modifier = Modifier.width(75.dp),

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        )
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        if (isKannada)
                            "ಒಟ್ಟು ಜೆನೆರಿಕ್ ಬೆಲೆ:"
                        else
                            "Total Generic Cost:",

                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Text(
                        "₹${
                            String.format(
                                Locale.US,
                                "%.2f",
                                totalGeneric
                            )
                        }",

                        fontWeight =
                            FontWeight.Bold,

                        color = Color(0xFF0056B3)
                    )
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        if (isKannada)
                            "ಒಟ್ಟು ಉಳಿತಾಯ:"
                        else
                            "Potential Savings:",

                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Text(
                        "₹${
                            String.format(
                                Locale.US,
                                "%.2f",
                                savings
                            )
                        }",

                        fontWeight =
                            FontWeight.Bold,

                        color = Color(0xFF28A745),

                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

// ---------------- STORE LOCATOR ----------------

@Composable
fun StoreLocatorScreen(
    isKannada: Boolean
) {

    val context = LocalContext.current

    val nearbyStores = listOf(

        Store(
            "Kendra #102 - Hope Farm",
            0.8,
            "Open Now"
        ),

        Store(
            "Kendra #115 - Kadugodi",
            3.2,
            "Open Now"
        ),

        Store(
            "Kendra #88 - Marathahalli",
            5.9,
            "Open Now"
        ),

        Store(
            "Kendra #22 - Hoodi Junction",
            4.5,
            "Open Now"
        )
    )

    Column(
        modifier = Modifier.padding(20.dp)
    ) {

        Text(
            if (isKannada)
                "ಸ್ಥಳ ಪತ್ತೆಕಾರಕ"
            else
                "Store Locator",

            fontSize = 24.sp,

            fontWeight =
                FontWeight.Bold,

            color = Color(0xFF0056B3)
        )

        Text(
            if (isKannada)
                "ವೈಟ್‌ಫೀಲ್ಡ್‌ನಿಂದ 10 ಕಿಮೀ ಒಳಗೆ"
            else
                "Within 10km of Whitefield",

            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyColumn {

            items(nearbyStores) { store ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {

                    Row(
                        modifier = Modifier.padding(16.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = Color(0xFF0056B3)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {

                            Text(
                                store.name,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                "${store.distance} km • ${store.status}",

                                color = Color(0xFF28A745),

                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {

                                Toast.makeText(
                                    context,
                                    "Stock checked at ${store.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {

                            Text(
                                if (isKannada)
                                    "ದಾಸ್ತಾನು?"
                                else
                                    "Stock?",

                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- REMINDER SCREEN ----------------

@Composable
fun ReminderScreen(
    isKannada: Boolean
) {

    val reminders = listOf(

        Reminder(
            "Thyronorm 50",
            "May 15"
        ),

        Reminder(
            "Azithral 500",
            "May 22"
        )
    )

    Column(
        modifier = Modifier.padding(20.dp)
    ) {

        Text(
            if (isKannada)
                "ಮರುಪೂರಣ ಟ್ರ್ಯಾಕರ್"
            else
                "Refill Tracker",

            fontSize = 24.sp,

            fontWeight =
                FontWeight.Bold,

            color = Color(0xFF0056B3)
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        reminders.forEach { rem ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),

                colors = CardDefaults.cardColors(
                    Color(0xFFFFF3E0)
                )
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.DateRange,
                        null,
                        tint = Color(0xFFE65100)
                    )

                    Spacer(
                        modifier = Modifier.width(16.dp)
                    )

                    Column {

                        Text(
                            rem.medName,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize = 18.sp
                        )

                        Text(
                            "${if (isKannada) "ಮುಂದಿನ ರೀಫಿಲ್" else "Next Refill"}: ${rem.date}"
                        )
                    }
                }
            }
        }
    }
}