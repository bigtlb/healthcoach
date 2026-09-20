package com.lbthomas.healthcoach.core.di


import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.koin.dsl.module
import java.io.File


val previewAppModule = module {


    factory { SettingsViewModel(settings = MutableStateFlow(
        SettingsData()
    )) }

    factory {
        WeightViewModel(
            previewEntries = MutableStateFlow(
                listOf(
                    WeightEntryData(1, LocalDate(2022, 12, 1), 140.0),
                    WeightEntryData(2, LocalDate(2022, 12, 2), 145.0),
                    WeightEntryData(3, LocalDate(2023, 1, 3), 140.0),
                    WeightEntryData(4, LocalDate(2023, 1, 4), 142.0),
                    WeightEntryData(5, LocalDate(2023, 1, 5), 130.0),
                    WeightEntryData(6, LocalDate(2023, 2, 6), 125.0),
                    WeightEntryData(7, LocalDate(2023, 2, 7), 120.0)
                )
            )
        )
    }
}

