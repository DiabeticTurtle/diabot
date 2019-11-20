package com.dongtronic.diabot.logic.conversion

import com.dongtronic.diabot.data.ConversionDTO

interface GlucoseConversionFacade {
    fun convert(value: String, unit: String?): ConversionDTO
    fun convert(value: Double): ConversionDTO
}