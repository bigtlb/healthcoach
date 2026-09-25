package com.lbthomas.healthcoach.core.di


import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureViewModel
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.koin.dsl.module


val previewAppModule = module {


    factory { SettingsViewModel(settings = MutableStateFlow(
        SettingsData()
    )) }

    factory {
        WeightViewModel(
            previewEntries = MutableStateFlow(
                listOf(
                    WeightEntryData("1", LocalDate(2022, 12, 1), 140.0),
                    WeightEntryData("2", LocalDate(2022, 12, 2), 145.0),
                    WeightEntryData("3", LocalDate(2023, 1, 3), 140.0),
                    WeightEntryData("4", LocalDate(2023, 1, 4), 142.0),
                    WeightEntryData("5", LocalDate(2023, 1, 5), 130.0),
                    WeightEntryData("6", LocalDate(2023, 2, 6), 125.0),
                    WeightEntryData("7", LocalDate(2023, 2, 7), 120.0)
                )
            )
        )
    }

    factory {
        BloodPressureViewModel(
            previewEntries = MutableStateFlow(
                listOf(
                    BloodPressureEntryData("1", "2023-01-03T08:30:00Z", 118, 76, 68),
                    BloodPressureEntryData("2", "2023-01-04T12:15:00Z", 124, 78, 72),
                    BloodPressureEntryData("3", "2023-01-05T19:45:00Z", 134, 84, 75),
                    BloodPressureEntryData("4", "2023-02-06", 142, 92, 80),
                    BloodPressureEntryData("5", "2023-02-07T09:00:00Z", 115, 75, 65)
                )
            )
        )
    }
}

