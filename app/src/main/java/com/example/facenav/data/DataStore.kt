package com.example.facenav.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// Create a singleton DataStore using extension property
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "facenav_preferences")