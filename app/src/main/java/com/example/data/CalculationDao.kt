package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<Calculation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: Calculation)

    @Delete
    suspend fun deleteCalculation(calculation: Calculation)

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()
}
