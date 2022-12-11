package hdl

import org.scalatest.freespec.AnyFreeSpec

object HDL {
  // sealed trait Circuit[+I, +O]

  sealed abstract class UInt {
    def +(op2: UInt): UInt = {
      add(this, op2)
    }
  }

  case class UIntLit(lit: Int) extends UInt

  class UIntVar(val width: Int, val name: Option[String] = None) extends UInt
  object UIntVar {
    def unapply(v: UIntVar): Some[(Int, Option[String])] = {
      Some(v.width, v.name)
    }
  }

  class Add(val arg1: UInt, val arg2: UInt) extends UInt
  object Add {
    def unapply(a: Add): Option[(UInt, UInt)] = {
      Some((a.arg1, a.arg2))
    }
  }

  def add(arg1: UInt, arg2: UInt): UInt = {
    new Add(arg1, arg2)
  }

  type NameMap = Map[UInt, (String, Int)]

  def toVerilog(hw: UInt, nameGen: NameGen, nameMap: NameMap): (String, NameMap) = {
    hw match {
      case l @ UIntLit(lit) => (s"'d$lit", nameMap)//math.ceil(math.log(lit) / math.log(2)).toInt)
      case v @ UIntVar(width, name) =>
        val finalName = if (name.isDefined) {
          name.get
        } else {
          nameGen.nextName()
        }
        //val name = nameGen.nextName()
        (s"wire [${width-1}:0] ${finalName};\n", nameMap + (v -> (finalName, width)))
      case a @ Add(arg1, arg2) =>
        val (vlog1, nameMap1) = toVerilog(arg1, nameGen, nameMap)
        val (vlog2, nameMap2) = toVerilog(arg2, nameGen, nameMap1)
        val name = nameGen.nextName()
        val adderWidth = nameMap2(arg1)._2 + 1
        val newNameMap = nameMap2 + (a -> (name, adderWidth))
        (vlog1 + vlog2 +
          s"wire [${adderWidth-1}:0] ${name};\n" +
          s"assign ${name} = ${nameMap2(arg1)._1} + ${nameMap2(arg2)._1};\n", newNameMap)
    }
  }

  def treeAdd(s: Seq[Int]): Int = {
    if (s.isEmpty) {
      0
    } else if (s.length == 1) {
      s.head
    } else {
      treeAdd(s.grouped(2).map { summands =>
        if (summands.length == 1)
          summands(0)
        else
          summands(0) + summands(1)
      }.toSeq)
    }
  }

  def hwTreeAdd(s: Seq[UInt]): UInt = {
    if (s.isEmpty) {
      UIntLit(0)
    } else if (s.length == 1) {
      s.head
    } else {
      hwTreeAdd(s.grouped(2).map { summands =>
        if (summands.length == 1)
          summands(0)
        else
          summands(0) + summands(1)
      }.toSeq)
    }
  }

  class NameGen() {
    var id = 0
    def nextName(): String = {
      val name = s"__GEN_$id"
      id = id + 1
      name
    }
  }
}

class HDLSpec extends AnyFreeSpec {
  "the hdl should do the thing" in {
    //println(HDL.treeAdd(Seq(1, 2, 3, 4, 5)))

    import HDL._
    /*
    val var1 = new UIntVar(16)
    val var2 = new UIntVar(16)

    println(toVerilog(add(var1, var2), new NameGen(), Map[UInt, (String, Int)]())._1)
     */

    val vars = Seq.tabulate(9)(i => new UIntVar(16, Some(s"inp_${i.toString}")))
    println(toVerilog(hwTreeAdd(vars), new NameGen(), Map[UInt, (String, Int)]())._1)
  }
}
