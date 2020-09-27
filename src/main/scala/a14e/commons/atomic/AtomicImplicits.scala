package a14e.commons.atomic

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}


object AtomicImplicits {


  // хелперы чтобы поменьше использовать моникс

  implicit class RichAtomic[T](val atomic: AtomicReference[T]) extends AnyVal {
    def apply(): T = atomic.get()


    // фактически копия того, что делает моникс
    // тут особенность, что моникс кэширует функцию для нечистых функций
    // для нас кажется, что это бессмысленно
    def transformAndExtract[U](cb: T => (U, T)): U = {
      var current = atomic.get()
      var (resultVar, updateVar) = cb(current)

      while (!atomic.compareAndSet(current, updateVar)) {
        current = atomic.get
        val (resultTmp, updateTmp) = cb(current)
        updateVar = updateTmp
        resultVar = resultTmp
      }

      resultVar
    }

    def transformAndGet(f: T => T): T = atomic.updateAndGet(x => f(x))
    def getAndTransform(f: T => T): T = atomic.getAndUpdate(x => f(x))
  }

}
