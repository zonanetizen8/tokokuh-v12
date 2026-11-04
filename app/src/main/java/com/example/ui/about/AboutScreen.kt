package com.example.ui.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.TokokuhTopBar
import com.example.ui.theme.TokokuhAccent
import com.example.ui.theme.TokokuhBackground
import com.example.ui.theme.TokokuhCard
import com.example.ui.theme.TokokuhSecondary
import com.example.ui.theme.TokokuhTextPrimary
import com.example.ui.theme.TokokuhTextSecondary

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TokokuhBackground)
    ) {
        TokokuhTopBar(
            title = "Tentang Aplikasi",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Official Header Logo Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                border = BorderStroke(1.dp, TokokuhSecondary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_header_atas),
                        contentDescription = "Logo TOKOKUH",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(65.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Aplikasi Kasir Offline Profesional (POS)",
                        color = TokokuhAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Versi 1.0.0 (Offline Native Build)",
                        color = TokokuhTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Official Identity Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                border = BorderStroke(1.dp, TokokuhSecondary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Identitas Produk & Pengembang",
                        color = TokokuhTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Divider(color = TokokuhSecondary)

                    AboutInfoRow(
                        icon = Icons.Default.Language,
                        label = "Produk Resmi Dari",
                        value = "www.tokokuh.com"
                    )
                    AboutInfoRow(
                        icon = Icons.Default.Person,
                        label = "Developer",
                        value = "Logis Denis Prabowo"
                    )
                    AboutInfoRow(
                        icon = Icons.Default.Work,
                        label = "Jabatan",
                        value = "Prompt Developer"
                    )
                    AboutInfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Lokasi",
                        value = "Indramayu - Jawa Barat"
                    )
                }
            }

            // Official Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TokokuhCard),
                border = BorderStroke(1.dp, TokokuhSecondary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Deskripsi Resmi",
                        color = TokokuhTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Divider(color = TokokuhSecondary)

                    Text(
                        text = "TOKOKUH merupakan aplikasi kasir digital yang dirancang untuk membantu pelaku usaha mengelola transaksi penjualan, inventaris produk, karyawan, dan laporan keuangan secara praktis.",
                        color = TokokuhTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Text(
                        text = "Aplikasi ini dibuat untuk beroperasi sepenuhnya secara offline. Seluruh data disimpan secara lokal pada perangkat masing-masing pengguna dan tidak menggunakan database server TOKOKUH.",
                        color = TokokuhTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Text(
                        text = "Aplikasi ini dikembangkan oleh Logis Denis Prabowo dengan tujuan menyediakan solusi kasir yang sederhana, efisien, dan dapat digunakan oleh berbagai jenis usaha.",
                        color = TokokuhTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Offline Guarantee Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TokokuhSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TokokuhAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "100% Offline: Data tersimpan secara aman di perangkat lokal menggunakan Room SQLite. Tidak memerlukan koneksi internet.",
                        color = TokokuhTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AboutInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TokokuhAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = TokokuhTextSecondary, fontSize = 11.sp)
            Text(value, color = TokokuhTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
