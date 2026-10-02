    package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun GenderGuideDialog(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Gender & Metabolic Calculations Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TL;DR Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TL;DR Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "• Male: Base Mifflin-St Jeor formula with +5 kcal offset.\n" +
                                    "• Female: Base Mifflin-St Jeor formula with -161 kcal offset.\n" +
                                    "• Other: Midpoint offset of -78 kcal, splitting the difference between male and female baselines to account for intermediate lean body mass averages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Biological Driver
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Why Biological Differences Exist in BMR",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "The numerical difference (+5 vs. -161) in resting metabolic rate equations reflects differences in Fat-Free Mass (FFM) / Lean Body Mass (LBM), skeletal muscle proportions, and circulating hormone levels (testosterone vs. estrogen/progesterone), rather than chromosomes directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Clinical Considerations for Transgender & Gender-Diverse Individuals
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Clinical Guidance for Transgender & Gender-Diverse Individuals",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "According to clinical endocrinology consensus and nutrition practice guidelines (Endocrine Society, WPATH):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "1. On Established Hormone Therapy (> 1–2 years): Gender-Affirming Hormone Therapy (GAHT) shifts muscle mass, body fat distribution, and metabolic expenditure to closely align with affirmed sex hormone ranges. Selecting your affirmed gender is generally recommended.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Pre-Hormone Therapy / Not on GAHT: The formula for sex assigned at birth typically provides the closest reflection of baseline lean body mass.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. During Transition (first 6–12 months) or Non-Binary: Selecting 'Other' applies the -78 kcal midpoint offset, providing a balanced intermediate estimate as body composition evolves.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // References & Citations
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Scientific Literature & Citations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    ReferenceLinkItem(
                        title = "Mifflin et al. (1990) - AJCN",
                        description = "A new predictive equation for resting energy expenditure in healthy individuals (DOI: 10.1093/ajcn/51.2.241)",
                        url = "https://doi.org/10.1093/ajcn/51.2.241",
                        onClick = { uriHandler.openUri("https://doi.org/10.1093/ajcn/51.2.241") }
                    )

                    ReferenceLinkItem(
                        title = "Klaver et al. (2018) - Journal of Sexual Medicine",
                        description = "Cross-sex hormone therapy affects total body & visceral fat mass and muscle mass (DOI: 10.1111/jsm.12827)",
                        url = "https://doi.org/10.1111/jsm.12827",
                        onClick = { uriHandler.openUri("https://doi.org/10.1111/jsm.12827") }
                    )

                    ReferenceLinkItem(
                        title = "Endocrine Society Clinical Guidelines",
                        description = "Endocrine Treatment of Gender-Dysphoric/Gender-Incongruent Persons (DOI: 10.1210/jc.2017-01658)",
                        url = "https://doi.org/10.1210/jc.2017-01658",
                        onClick = { uriHandler.openUri("https://doi.org/10.1210/jc.2017-01658") }
                    )

                    ReferenceLinkItem(
                        title = "WPATH Standards of Care (SOC-8)",
                        description = "World Professional Association for Transgender Health Standards of Care Version 8 (wpath.org/publications/soc8)",
                        url = "https://www.wpath.org/publications/soc8",
                        onClick = { uriHandler.openUri("https://www.wpath.org/publications/soc8") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun ReferenceLinkItem(
    title: String,
    description: String,
    url: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open link in browser",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(name = "Gender & Metabolic Guide Dialog")
@Composable
fun GenderGuideDialogPreview() {
    MaterialTheme {
        GenderGuideDialog(onDismiss = {})
    }
}
