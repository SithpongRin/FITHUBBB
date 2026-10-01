package com.example.calculations

object UnitsConverter {

    const val KG_TO_LBS = 2.2046226218
    const val CM_TO_INCH = 0.3937007874
    const val METERS_TO_MILES = 0.000621371192
    const val KM_TO_MILES = 0.621371192
    const val KM_PER_MILE = 1.609344

    fun kgToLbs(kg: Double): Double = kg * KG_TO_LBS
    fun lbsToKg(lbs: Double): Double = lbs / KG_TO_LBS

    fun cmToInches(cm: Double): Double = cm * CM_TO_INCH
    fun inchesToCm(inches: Double): Double = inches / CM_TO_INCH

    fun metersToKilometers(meters: Double): Double = meters / 1000.0
    fun metersToMiles(meters: Double): Double = meters * METERS_TO_MILES

    fun paceKmToPaceMile(paceSecPerKm: Double): Double = paceSecPerKm * KM_PER_MILE
}
