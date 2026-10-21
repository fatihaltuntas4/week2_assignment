package com.example.week2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.week2.ui.theme.Week2_assignmentTheme

/**
 * A. VERİ KATMANI (DATA LAYER) VE DERLEYİCİ SÖZLEŞMESİ
 *
 * 1. Değişmezlik (Immutability): Çok iş parçacıklı asenkron süreçlerde Race Condition
 *    hatalarını engellemek için tüm alanlar 'val' anahtar kelimesiyle tanımlanmıştır.
 * 2. Kararlılık Sözleşmesi (Stability Contract): Jetpack Compose derleyicisinin akıllı
 *    atlama (Smart Recomposition Skipping) motorunu tetiklemek amacıyla @Immutable ile işaretlenmiştir.
 */
@Immutable
data class HardwareItem(
    val id: String,
    val name: String,
    val category: String,
    val cost: Double,
    val stockCount: Int
)

/**
 * TEST VERİ KÜMESİ (Sabit Veri Seti)
 */
val SAMPLE_HARDWARE_LIST = listOf(
    HardwareItem(id = "101", name = "MacBook Pro M3 Max", category = "Geliştirici Bilgisayarı", cost = 3499.0, stockCount = 5),
    HardwareItem(id = "102", name = "NVIDIA RTX 4090 Sunucu", category = "Yapay Zeka / GPU", cost = 1599.0, stockCount = 2),
    HardwareItem(id = "103", name = "Google Pixel 8 Pro", category = "Mobil Test Cihazı", cost = 999.0, stockCount = 8),
    HardwareItem(id = "104", name = "Dell UltraSharp 49\"", category = "Monitör", cost = 1199.0, stockCount = 4),
    HardwareItem(id = "105", name = "Sony WH-1000XM5 Kulaklık", category = "Odaklanma Ekipmanı", cost = 399.0, stockCount = 12),
    HardwareItem(id = "106", name = "Raspberry Pi 5 8GB", category = "IoT Geliştirme Kiti", cost = 85.0, stockCount = 25),
    HardwareItem(id = "107", name = "HHKB Professional Keyboard", category = "Ergonomik Klavye", cost = 299.0, stockCount = 0), // Stok sınırı test verisi
    HardwareItem(id = "108", name = "AMD Ryzen Threadripper", category = "Yapay Zeka / GPU", cost = 4999.0, stockCount = 1)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Week2_assignmentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevClusterScreen()
                }
            }
        }
    }
}

/**
 * B & D. MERKEZİ STATEFUL KAPSAYICI (DevClusterScreen)
 *
 * - Çalışma Zamanı Hafıza Kilidi: Toplam harcanan ödenek Slot Table'a 'remember' ile kilitlenir.
 * - Konfigürasyon Değişim Koruması: Ekran döndüğünde (Portrait -> Landscape) arama kaybını
 *   önlemek için 'rememberSaveable' kullanılır.
 * - Turnike Mantığı (Memoization): Filtreleme algoritması gereksiz CPU tüketimini önlemek için
 *   'remember(searchQuery, hardwareList)' arkasına alınmıştır.
 * - State Hoisting: Alt bileşenler durumları parametre alır (State Down), olayları lambda ile bildirir (Events Up).
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun DevClusterPreview() {
    Week2_assignmentTheme {
        DevClusterScreen()
    }
}

@Composable
fun DevClusterScreen(modifier: Modifier = Modifier) {
    // 1. Slot Table içerisine kilitlenen dinamik durumlar
    var totalSpent by remember { mutableDoubleStateOf(0.0) }
    var hardwareList by remember { mutableStateOf(SAMPLE_HARDWARE_LIST) }

    // 2. Konfigürasyon Değişim Koruması (Ekran dönünce veri kaybını önler)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // 3. Turnike Mantığı (Memoization) - Sadece searchQuery veya hardwareList değiştiğinde çalışır
    val filteredList = remember(searchQuery, hardwareList) {
        if (searchQuery.isBlank()) {
            hardwareList
        } else {
            val query = searchQuery.trim()
            hardwareList.filter { item ->
                item.name.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            DevClusterHeader(totalSpent = totalSpent)
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // D. Stateless Arama Çubuğu (State Down, Events Up)
            HardwareSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Liste Başlığı & Sonuç Sayısı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Envanter ve Donanımlar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredList.size} kaynak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // C. TEMBEL YÜKLEME VE NOKTA ATIŞI RECOMPOSITION
            // LazyColumn kullanılır ve items bloğuna key = { it.id } zorunlu olarak geçilir.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredList,
                    key = { it.id } // Nokta Atışı Recomposition Sözleşmesi
                ) { item ->
                    HardwareCard(
                        item = item,
                        onItemClick = { clickedItem ->
                            if (clickedItem.stockCount > 0) {
                                // Slot Table'daki toplam harcanan ödenek güncellenir
                                totalSpent += clickedItem.cost

                                // Stok 1 adet düşürülür (Immutability korunarak yeni liste üretilir)
                                hardwareList = hardwareList.map { currentItem ->
                                    if (currentItem.id == clickedItem.id) {
                                        currentItem.copy(stockCount = currentItem.stockCount - 1)
                                    } else {
                                        currentItem
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Üst Bilgi Başlığı (Header) - Toplam Harcanan Ödeneği Gösterir
 */
@Composable
fun DevClusterHeader(
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "DevCluster: Donanım ve Sunucu Yönetimi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toplam Harcanan Ödenek: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = String.format("$%.2f", totalSpent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * D. MİMARİ AYRIŞMA DİSİPLİNİ: Stateless Arama Çubuğu (HardwareSearchBar)
 *
 * İçinde hiçbir yerel remember hücresi barındırmaz.
 * 'query' durumunu yukarıdan alır (State Down), metin değişimini lambda ile fırlatır (Events Up).
 */
@Composable
fun HardwareSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Donanım adı veya kategori ara...") },
        placeholder = { Text("Örn: RTX 4090, GPU, Monitör") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * D. MİMARİ AYRIŞMA DİSİPLİNİ: Stateless Donanım Kartı (HardwareCard)
 *
 * - Saf Fonksiyon Tasarımı: İçerisinde hiçbir yerel remember hücresi barındırmaz.
 * - Modifier Sıralaması: Görsel margin hatalarını önlemek amacıyla önce .clickable(),
 *   ardından .padding() hiyerarşisi işletilmiştir.
 * - Stok Sınırı Koruması: stockCount == 0 olan ürünlerde tıklama devre dışı bırakılır.
 */
@Composable
fun HardwareCard(
    item: HardwareItem,
    onItemClick: (HardwareItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = item.stockCount == 0

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            // ÖDEV KURALI: Önce clickable, ardından padding hiyerarşisi
            .clickable(enabled = !isOutOfStock) {
                onItemClick(item)
            }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Başlık ve Fiyat Satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutOfStock) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("$%.2f", item.cost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutOfStock) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Kategori Rozeti ve Stok Durumu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kategori Rozeti
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stok Sayısı / Stok Sınırı Göstergesi
                if (isOutOfStock) {
                    Text(
                        text = "Stok Tükendi (0)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Kalan Stok: ${item.stockCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
