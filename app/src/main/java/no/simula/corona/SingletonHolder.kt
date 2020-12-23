package no.simula.corona

open class SingletonHolder<out T: Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val obj = instance
        if (obj != null) {
            return obj
        }

        return synchronized(this) {
            val obj2 = instance
            if (obj2 != null) {
                obj2
            } else {
                val createdObj = creator!!(arg)
                instance = createdObj
                creator = null
                createdObj
            }
        }
    }
}