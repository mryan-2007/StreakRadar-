package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RadarStateRepository
import com.example.model.CropRegion
import kotlin.random.Random

@Composable
fun CropSimulatorComponent(
    cropRegion: CropRegion,
    onOpenCropSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("crop_simulator_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Score Bar Crop Region",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Area analyzed for Aviator multipliers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenCropSelector,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("open_crop_overlay_button")
                ) {
                    Text("✂ Set Crop Area", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated browser top bar & Aviator multiplier strip
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column {
                    // Chrome-like address bar simulation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "https://aviator-game.com/play",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulated Horizontal Multiplier Ribbon
                    Text(
                        text = "SIMULATED SCORE RIBBON (CHROME HUD)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF070B14), RoundedCornerShape(6.dp))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leftmost multiplier (newest round)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF004D40), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "1.34x ◄ (Leftmost)",
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Older rounds
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF3E2723), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "4.20x",
                                color = Color(0xFFFFB74D),
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF004D40), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "1.89x",
                                color = Color(0xFF00E676),
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2E1065), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "15.00x",
                                color = Color(0xFFC084FC),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Coordinates details
            val wPercent = ((cropRegion.rightRatio - cropRegion.leftRatio) * 100).toInt()
            val hPercent = ((cropRegion.bottomRatio - cropRegion.topRatio) * 100).toInt()
            val topPercent = (cropRegion.topRatio * 100).toInt()
            val leftPercent = (cropRegion.leftRatio * 100).toInt()

            Text(
                text = "Crop Bounds: Left ${leftPercent}% • Top ${topPercent}% • Width ${wPercent}% • Height ${hPercent}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick In-App Testing Buttons
            Text(
                text = "Simulate Aviator Rounds (Test without opening game):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val randomUnder = 1.05 + Random.nextDouble(0.0, 0.90)
                        RadarStateRepository.processDetectedScore(
                            multiplier = (randomUnder * 100).toInt() / 100.0,
                            rawText = String.format("%.2fx", randomUnder),
                            forceNewRound = true
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_under_threshold_button")
                ) {
                    Text("< 2.00x", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val randomOver = 2.10 + Random.nextDouble(0.0, 15.0)
                        RadarStateRepository.processDetectedScore(
                            multiplier = (randomOver * 100).toInt() / 100.0,
                            rawText = String.format("%.2fx", randomOver),
                            forceNewRound = true
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_over_threshold_button")
                ) {
                    Text(">= 2.00x", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        // Simulate consecutive streak up to target to test alert!
                        val target = RadarStateRepository.config.value.targetStreak
                        for (i in 1..target) {
                            val v = 1.10 + (i * 0.1)
                            RadarStateRepository.processDetectedScore(
                                multiplier = (v * 100).toInt() / 100.0,
                                rawText = String.format("%.2fx", v),
                                forceNewRound = true
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00B4D8),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("simulate_target_streak_button")
                ) {
                    Text("7x Streak", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
