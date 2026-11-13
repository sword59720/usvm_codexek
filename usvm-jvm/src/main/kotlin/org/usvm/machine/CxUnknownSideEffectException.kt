package org.usvm.machine
class CxUnknownSideEffectException(site: String) : RuntimeException(site), org.usvm.CxStepDiscard
