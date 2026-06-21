package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculatorRepository(private val calculationDao: CalculationDao) {
    val allCalculations: Flow<List<Calculation>> = calculationDao.getAllCalculations()

    suspend fun insert(calculation: Calculation) {
        calculationDao.insertCalculation(calculation)
    }

    suspend fun delete(calculation: Calculation) {
        calculationDao.deleteCalculation(calculation)
    }

    suspend fun clearAll() {
        calculationDao.clearHistory()
    }
}
