package com.dongtronic.diabot.logic.conversion

import com.dongtronic.diabot.converters.GlucoseUnit
import com.dongtronic.diabot.data.ConversionDTO
import com.dongtronic.diabot.exceptions.UnknownUnitException
import org.apache.commons.lang3.math.NumberUtils

object GlucoseConversionFacadeBean : GlucoseConversionFacade {
    override fun convert(value: String, unit: String?): ConversionDTO {
        if (!NumberUtils.isCreatable(value)) {
            throw IllegalArgumentException("value must be numeric")
        }

        val input = java.lang.Double.valueOf(value)

        if (input < 0 || input > 999) {
            throw IllegalArgumentException("value must be between 0 and 999")
        }

        return if (unit != null && unit.length > 1) {
            convert(input, unit)
        } else {
            convert(input)
        }
    }

    override fun convert(value: Double): ConversionDTO {
        return try {
            when {
                value < 25 -> convert(value, "mmol")
                value > 50 -> convert(value, "mgdl")
                else -> convertAmbiguous(value)
            }
        } catch (ex: UnknownUnitException) {
            convertAmbiguous(value)
        }
    }

    @Throws(UnknownUnitException::class)
    private fun convert(originalValue: Double, unit: String): ConversionDTO {

        return when {
            unit.toUpperCase().contains("MMOL") -> {
                val result = originalValue * 18.016
                ConversionDTO(originalValue, result, GlucoseUnit.MMOL)
            }
            unit.toUpperCase().contains("MG") -> {
                val result = originalValue / 18.016
                ConversionDTO(originalValue, result, GlucoseUnit.MGDL)
            }
            else -> throw UnknownUnitException()
        }
    }

    private fun convertAmbiguous(originalValue: Double): ConversionDTO {

        val toMgdl = originalValue * 18.016
        val toMmol = originalValue / 18.016

        return ConversionDTO(originalValue, toMmol, toMgdl)

    }
}