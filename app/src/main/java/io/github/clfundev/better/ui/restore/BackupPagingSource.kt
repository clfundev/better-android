package io.github.clfundev.better.ui.restore

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.clfundev.better.data.network.api.BackupApi
import io.github.clfundev.better.data.network.model.Backup

class BackupPagingSource(
    private val backupApi: BackupApi
) : PagingSource<Int, Backup>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Backup> {
        try {
            val pageNum = params.key ?: 1 // Start refresh at page 1 if undefined
            val pageSize = params.loadSize
            val response = backupApi.page(pageNum, pageSize)
            val page = response.data
            if (!response.isSuccessful() || page == null) {
                Log.e(TAG, "getBackups error: code = ${response.code}, msg = ${response.message}")
                return LoadResult.Error(Exception("getBackups error: ${response.message}"))
            }
            return LoadResult.Page(
                data = page.records,
                prevKey = null,
                nextKey = if (pageNum < page.pages) (pageNum + 1) else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "load error: $e")
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Backup>): Int? {
        // Try to find the page key of the closest page to anchorPosition, from
        // either the prevKey or the nextKey, but you need to handle nullability
        // here:
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey null -> anchorPage is the initial page, so
        //    just return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    companion object {
        private const val TAG = "BackupPagingSource"
    }

}