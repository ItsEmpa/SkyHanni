package at.hannibal2.skyhanni.utils.collection

import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeFirstMatches
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeSource

class TimeLimitedMap<K, V>(
    expireAfterWrite: Duration
) : MutableMap<K, V> {

    private data class Entry<K, V>(
        override val key: K,
        override var value: V
    ) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            // TODO: setValue needs to change it in tree and keyIndex
            val oldValue = value
            value = newValue
            return oldValue
        }
    }

    private val expireAfterWrite = expireAfterWrite.inWholeNanoseconds

    private val tree = TreeMap<Long, MutableList<Entry<K, V>>>()
    private val keyIndex = HashMap<K, Long>()

    private val timeSource = TimeSource.Monotonic

    private fun now(): Long = timeSource.markNow().elapsedNow().inWholeNanoseconds

    private fun purgeExpired(current: Long = now()) {
        while (tree.isNotEmpty() && tree.firstKey() <= current) {
            val entry = tree.pollFirstEntry() ?: break // not sure if this can be null
            val list = entry.value
            list.forEach { (key, _) ->
                keyIndex.remove(key)
            }
        }
    }

    override val size: Int
        get() {
            purgeExpired()
            return keyIndex.size
        }

    override fun isEmpty(): Boolean = keyIndex.isEmpty() || size == 0

    override fun containsKey(key: K): Boolean {
        val time = keyIndex[key] ?: return false
        val now = now()
        if (time > now) return true
        purgeExpired(now)
        return keyIndex.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        purgeExpired()
        return tree.values.any { list -> list.any { it.value == value } }
    }

    override fun get(key: K): V? {
        val time = keyIndex[key] ?: return null
        val now = now()
        if (time <= now) {
            purgeExpired(now)
            return null
        }
        return tree[time]?.find { it.key == key }?.value
    }

    override fun put(key: K, value: V): V? {
        val now = now()
        val newTime = now + expireAfterWrite

        fun returnNewEntry(): V? {
            val newEntry = Entry(key, value)
            tree.getOrPut(newTime, ::ArrayList).add(newEntry)
            keyIndex[key] = newTime
            return null
        }

        val previousTime = keyIndex[key] ?: return returnNewEntry()
        if (previousTime <= now) {
            purgeExpired(now)
            return returnNewEntry()
        }
        val oldEntry = tree[previousTime]?.removeFirstMatches { it.key == key } ?: return returnNewEntry()
        val oldValue = oldEntry.setValue(value)
        tree.getOrPut(newTime, ::ArrayList).add(oldEntry)
        keyIndex[key] = newTime
        return oldValue
    }

    override fun remove(key: K): V? {
        val time = keyIndex.remove(key) ?: return null
        val now = now()
        if (time <= now) {
            purgeExpired(now)
            return null
        }
        val list = tree[time] ?: return null
        val entry = list.removeFirstMatches { it.key == key } ?: return null
        if (list.isEmpty()) tree.remove(time)
        return entry.value
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (k, v) -> put(k, v) }
    }

    override fun clear() {
        tree.clear()
        keyIndex.clear()
    }

    override val keys: MutableSet<K> = KeySet<K, V>(this)

    override val values: MutableCollection<V>
        get() {
            purgeExpired()
            return tree.values.map { it.value }.toMutableList()
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            purgeExpired()
            return keyIndex.map { (k, _) ->
                object : MutableMap.MutableEntry<K, V> {
                    override val key: K = k
                    override val value: V = get(k)!!

                    override fun setValue(newValue: V): V {
                        return put(k, newValue) ?: value
                    }
                }
            }.toMutableSet()
        }

    private class KeySet<K, V>(private val m: TimeLimitedMap<K, V>) : MutableSet<K> {
        override val size: Int get() = m.size
        override fun contains(element: K): Boolean = m.containsKey(element)

        override fun containsAll(elements: Collection<K>): Boolean {
            return elements.all { m.containsKey(it) } // TODO: optimize
        }

        override fun isEmpty(): Boolean = m.isEmpty()

        override fun add(element: K): Boolean = throw UnsupportedOperationException()

        override fun addAll(elements: Collection<K>): Boolean = throw UnsupportedOperationException()

        override fun clear() = m.clear()

        override fun iterator(): MutableIterator<K> {
            TODO("Not yet implemented")
        }

        override fun remove(element: K): Boolean = m.remove(element) != null

        override fun removeAll(elements: Collection<K>): Boolean {
            var anyRemoved = false
            for (element in elements) {
                if (m.remove(element) != null) anyRemoved = true
            }
            return anyRemoved
        }

        override fun retainAll(elements: Collection<K>): Boolean {
            TODO("Not yet implemented")
        }

    }
}

