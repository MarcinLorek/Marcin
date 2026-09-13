package pl.mlorek.gpwshort.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * KNF nie udostępnia REST/JSON API - poniższe metody pobierają surową treść pod wskazanym,
 * pełnym adresem URL (@Url), zamiast modelować "zasoby" typowe dla Retrofit+REST.
 * Retrofit jest tu użyty głównie jako wygodna warstwa nad OkHttp (interceptory, timeouty,
 * łatwe podmienianie klienta w testach) - patrz [KnfRemoteDataSource].
 */
interface KnfApiService {

    @GET
    suspend fun fetchHtml(@Url url: String): Response<String>

    @GET
    suspend fun fetchBinary(@Url url: String): Response<ResponseBody>
}
