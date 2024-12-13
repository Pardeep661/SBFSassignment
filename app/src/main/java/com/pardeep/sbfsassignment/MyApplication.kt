package com.pardeep.sbfsassignment

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

class MyApplication : Application() {
    var supabaseClient: SupabaseClient?=null

    override fun onCreate() {
        super.onCreate()
        supabaseClient = createSupabaseClient(
            supabaseUrl ="https://lecfqdaoullqqrkximvq.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxlY2ZxZGFvdWxscXFya3hpbXZxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI2ODY4MDQsImV4cCI6MjA0ODI2MjgwNH0.Myd_8-Exs9LkCpNITCbL5mnz2548nsJkFooslTkmW2Y",
        ){
            install(Storage)
        }
        val bucket = supabaseClient?.storage?.from("Fs_assignment")

    }
}