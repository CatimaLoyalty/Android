package protect.card_locker.async

import java.util.concurrent.Callable

interface CompatCallable<T> : Callable<T?> {
    fun onPostExecute(result: Any?)

    fun onPreExecute()
}
