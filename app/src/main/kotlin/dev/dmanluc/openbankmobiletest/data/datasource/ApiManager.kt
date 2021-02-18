package dev.dmanluc.openbankmobiletest.data.datasource

import arrow.core.left
import arrow.core.right
import arrow.core.rightIfNotNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.dmanluc.openbankmobiletest.data.model.MarvelCharactersApiResponse
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException

class ApiManager(private val gson: Gson) {

    inline fun <ResultType> performNetworkRequest(
        crossinline localDataQuery: () -> Flow<ResultType>,
        crossinline fetchFromNetwork: suspend () -> ResultType,
        crossinline saveFetchResult: suspend (ResultType) -> Unit,
        crossinline shouldFetch: (ResultType) -> Boolean = { true },
    ) = flow {
        val localData = localDataQuery().firstOrNull()

        val resultAsFlow = flow {
            if (localData == null || shouldFetch(localData)) {
                val networkResult = fetchFromNetwork()
                saveFetchResult(networkResult)
                emit(networkResult.right())
            } else {
                emit(localData.right())
            }
        }.catch {
            emit(handleException(it).left())
        }

        emitAll(resultAsFlow)
    }

    fun handleException(throwable: Throwable): ApiError {
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val errorResponse = convertErrorBody(throwable)
                ApiError.HttpError(code, errorResponse)
            }
            is IOException -> {
                ApiError.NetworkError(throwable)
            }
            else -> {
                ApiError.UnknownApiError(throwable)
            }
        }
    }

    private fun convertErrorBody(throwable: HttpException): String {
        val standardErrorMessage = throwable.response()?.errorBody()?.string().orEmpty()

        return try {
            throwable.response()?.errorBody()?.charStream()?.let {
                val apiResponse = gson.fromJson<MarvelCharactersApiResponse>(
                    it,
                    object : TypeToken<MarvelCharactersApiResponse>() {}.type
                )
                apiResponse.status
            } ?: standardErrorMessage
        } catch (exception: Exception) {
            standardErrorMessage
        }
    }

}