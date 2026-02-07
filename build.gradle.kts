// Top-level build file

plugins {
    alias(libs.plugins.android.application) apply false
    // Korrekte Plugin-ID direkt verwenden, um Alias-Probleme zu umgehen
    id("com.google.gms.google-services") version "4.4.2" apply false
}
