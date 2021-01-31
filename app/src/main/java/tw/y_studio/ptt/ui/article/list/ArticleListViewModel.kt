package tw.y_studio.ptt.ui.article.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tw.y_studio.ptt.model.PartialPost
import tw.y_studio.ptt.source.remote.post.IPostRemoteDataSource
import tw.y_studio.ptt.utils.Log
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ArticleListViewModel(
    private val postRemoteDataSource: IPostRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    val data: MutableList<PartialPost> = ArrayList()
    private val page = AtomicInteger(1)

    private val loadingState = MutableLiveData<Boolean>()
    private val errorLiveData = MutableLiveData<Throwable>()

    fun getLoadingStateLiveData(): LiveData<Boolean> = loadingState

    fun getErrorLiveData(): LiveData<Throwable> = errorLiveData

    fun loadData(boardName: String) {
        viewModelScope.launch {
            if (loadingState.value == true) {
                return@launch
            }
            data.clear()
            page.set(1)
            loadingState.value = true
            getDataFromApi(boardName)
            loadingState.value = false
        }
    }

    fun loadNextData(boardName: String) {
        viewModelScope.launch {
            if (loadingState.value == true) {
                return@launch
            }
            loadingState.value = true
            getDataFromApi(boardName)
            loadingState.value = false
        }
    }

    private suspend fun getDataFromApi(boardName: String) = withContext(ioDispatcher) {
        try {
            val temp = mutableListOf<PartialPost>()
            for (i in 0..2) {
                try {
                    temp.addAll(postRemoteDataSource.getPostList(boardName, page.get()))
                } catch (e: Exception) {
                    if (page.get() > 1) {
                        throw e
                    }
                }
                page.incrementAndGet()
            }
            Log("ArticleListFragment", "get data from web success")
            data.addAll(temp)
        } catch (t: Throwable) {
            errorLiveData.postValue(t)
        }
    }

    override fun onCleared() {
        super.onCleared()
        postRemoteDataSource.disposeAll()
    }
}
