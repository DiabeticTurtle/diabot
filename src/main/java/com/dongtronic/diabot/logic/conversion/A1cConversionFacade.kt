package com.dongtronic.diabot.logic.conversion

import com.dongtronic.diabot.data.A1cDTO
import com.dongtronic.diabot.exceptions.UnknownUnitException

interface A1cConversionFacade {
    @Throws(UnknownUnitException::class)
    fun estimateA1c(value: String, unit: String?): A1cDTO
    fun estimateAverage(value: String): A1cDTO
}