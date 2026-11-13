package org.usvm.machine
/** Audit-side observable record of discarded-step sites (jvm-only-audit-v4). */
object CxAuditLog {
    private val sites = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    fun record(site: String) { sites.add(site) }
    fun snapshot(): List<String> = sites.toList()
    fun clear() { sites.clear() }
}
