package no.simula.corona.ui.main

interface MainFragmentConnection {
    fun onServiceEnable(enabled:Boolean)
    fun onFeature(gps:Boolean, bluetooth:Boolean)
}