import scala.collection.immutable.Queue
import scala.io.StdIn

case class State(
  index: Int,
  m: Queue[Boolean],
  output: List[IndexedSeq[Boolean]]
) {
  lazy val code: Int = m.foldLeft(0)((acc, v) => (acc << 1) + (if (v) 1 else 0))
}

class Encoder(n: Int, k: Int, g: IndexedSeq[IndexedSeq[Boolean]]) {
  def encode(source: IndexedSeq[Boolean], state: State = State(0, Queue.fill(k)(false), Nil)): IndexedSeq[Boolean] = {
    def inner(index: Int, state: State): State = 
      if (index < source.length + k) inner(index + 1, singleStep(state, source.isDefinedAt(index) && source(index))) 
      else state
    
    inner(0, state).output.reverse.flatten.toIndexedSeq
  }

  def singleStep(state: State, inputBit: Boolean): State = {
    val nextM = state.m.tail.enqueue(inputBit)
    val t = (0 until n).map(i => g(i).zip(nextM.reverse).map{ case (a, b) => a && b}.reduce(_ ^ _))
    State(state.index + 1, nextM, t :: state.output)
  }
}

class Decoder(n: Int, k: Int, g: IndexedSeq[IndexedSeq[Boolean]]) {
  val encoder = new Encoder(n, k, g)

  def decode(encoded: IndexedSeq[Boolean]): IndexedSeq[Boolean] = {
    val groupedSource = encoded.grouped(n).toIndexedSeq
    val zeroM = Queue.fill(k)(false)
    val initialItems = List(Item(State(0, zeroM, Nil), false, None, 0))

    @scala.annotation.tailrec
    def inner(index: Int, items: List[Item]): List[Item] =
      if (index < groupedSource.length) {
        val nextItems = items.flatMap { item =>
          Seq(false, true).map(inputBit => Item(encoder.singleStep(item.state, inputBit), inputBit, Some(item), item.penalty))
        }.groupBy(_.state.code).values.map(_.minBy(_.penalty)).toList
        inner(index + 1, nextItems)
      } else items

    val finalItems = inner(0, initialItems)
    val finalItem = finalItems.minBy(_.penalty)

    @scala.annotation.tailrec
    def backtrack(item: Item, acc: IndexedSeq[Boolean] = IndexedSeq()): IndexedSeq[Boolean] =
      if (item.origin.isEmpty) acc else backtrack(item.origin.get, item.inputBit +: acc)

    backtrack(finalItem).take(encoded.length / n - k)
  }

  case class Item(state: State, inputBit: Boolean, origin: Option[Item], penalty: Int)
}

object Solution extends App {
  val Array(n, k) = StdIn.readLine().split(" ").map(_.toInt)
  val g = IndexedSeq.fill(n)(IndexedSeq(StdIn.readLine().split(" ").map(_ == "1"): _*))
  val Array(n1, k1) = StdIn.readLine().split(" ").map(_.toInt)
  val g1 = IndexedSeq.fill(n1)(IndexedSeq(StdIn.readLine().split(" ").map(_ == "1"): _*))

  val encodedString = Iterator.continually(StdIn.readLine()).takeWhile(_ != null)
    .mkString.filter(c => c == '0' || c == '1').map(_ == '1')

  val decoder = new Decoder(n, k, g)
  val decodedMessage = decoder.decode(encodedString)

  val encoder = new Encoder(n1, k1, g1)
  val encodedMessage = encoder.encode(decodedMessage)

  println(encodedMessage.map(if(_) '1' else '0').mkString)
}
