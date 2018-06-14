package com.inmotionsoftware.promisekt

import java.util.concurrent.Executor

object PMKConfiguration : PMKConfig {

    override var Q: PMKConfig.Value = PMKConfig.Value(map = DispatchExecutor.main, `return` = DispatchExecutor.main)
    override var catchPolicy: CatchPolicy = CatchPolicy.allErrorsExceptCancellation
}

var conf: PMKConfig = PMKConfiguration

interface PMKConfig {
    data class Value(val map: Executor?, val `return`: Executor?)

    var Q: Value
    var catchPolicy: CatchPolicy
}