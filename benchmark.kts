import java.util.Calendar
import kotlin.system.measureTimeMillis

val duration = 10000
val startDate = System.currentTimeMillis()

// Warmup
for(i in 1..1000) {
    val c = Calendar.getInstance()
}

// Unoptimized
val unoptimizedTime = measureTimeMillis {
    for (i in 1..duration) {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = startDate
        calendar.add(java.util.Calendar.MONTH, i - 1)
        val dueDate = calendar.timeInMillis
    }
}

// Optimized
val optimizedTime = measureTimeMillis {
    val calendar = java.util.Calendar.getInstance()
    for (i in 1..duration) {
        calendar.timeInMillis = startDate
        calendar.add(java.util.Calendar.MONTH, i - 1)
        val dueDate = calendar.timeInMillis
    }
}

println("Unoptimized time: $unoptimizedTime ms")
println("Optimized time: $optimizedTime ms")
println("Improvement: ${unoptimizedTime - optimizedTime} ms (${((unoptimizedTime - optimizedTime).toDouble() / unoptimizedTime * 100).toInt()}%)")
