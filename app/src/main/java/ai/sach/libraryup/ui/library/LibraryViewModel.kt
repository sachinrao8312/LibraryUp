package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.Payment
import ai.sach.libraryup.data.repository.LibraryRepository
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.data.repository.UserError
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A one-off message for the snackbar. */
data class UiMessage(@StringRes val res: Int, val arg: String? = null)

/**
 * Shared by every library screen (scoped to the activity): one observed [LibraryState],
 * and a [run] helper that executes a repository operation and turns failures into a
 * snackbar message instead of a crash.
 */
class LibraryViewModel(val repository: LibraryRepository, val staff: StaffMode) : ViewModel() {

    /** Null until the database has emitted once, so screens can tell "loading" from "empty". */
    val state: StateFlow<LibraryState?> = repository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    fun message(@StringRes res: Int, arg: String? = null) {
        messageChannel.trySend(UiMessage(res, arg))
    }

    fun run(@StringRes success: Int? = null, onDone: () -> Unit = {}, block: suspend LibraryRepository.() -> Unit) {
        viewModelScope.launch {
            try {
                repository.block()
                success?.let { message(it) }
                onDone()
            } catch (e: UserError) {
                message(e.messageRes)
            } catch (e: Exception) {
                message(R.string.error_generic, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /** Like [run] but hands the new payment to [onPaid] (to offer sharing the receipt). */
    fun collect(block: suspend LibraryRepository.() -> Payment, onPaid: (Payment) -> Unit) {
        viewModelScope.launch {
            try {
                val p = repository.block()
                message(R.string.msg_payment_saved, p.receiptNo)
                onPaid(p)
            } catch (e: UserError) {
                message(e.messageRes)
            } catch (e: Exception) {
                message(R.string.error_generic, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    class Factory(private val repository: LibraryRepository, private val staff: StaffMode) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryViewModel(repository, staff) as T
    }
}
