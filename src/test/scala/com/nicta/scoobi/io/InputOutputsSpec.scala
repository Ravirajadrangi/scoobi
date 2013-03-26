package com.nicta.scoobi
package io

import testing.mutable.NictaSimpleJobs
import testing.TestFiles._
import testing.{TestFiles, InputStringTestFile, TempFiles}
import core.InputConverter
import org.apache.hadoop.io.{Text, LongWritable}
import text.TextInput.TextSource
import com.nicta.scoobi.Scoobi._
import org.apache.hadoop.mapreduce.RecordReader
import impl.plan.DListImpl
import core.InputOutputContext
import impl.plan.comp.CompNodeData._
import java.io.File

class InputOutputsSpec extends NictaSimpleJobs {

  "1. an input must only be read once" >> { implicit sc: SC =>
    if (sc.isInMemory) {
      var checks = 0
      var reads = 0
      lazy val file = createTempFile("test.input")

      TempFiles.writeLines(file, Seq("1", "2", "3"), isRemote)

      val converter = new InputConverter[LongWritable, Text, String] {
        def fromKeyValue(context: InputContext, k: LongWritable, v: Text) = v.toString
      }
      val source = new TextSource[String](Seq(file.getPath), converter) {
        override def inputCheck(implicit sc: ScoobiConfiguration) {
          checks = checks + 1
          super.inputCheck(sc)
        }
        override def read(reader: RecordReader[_,_], mapContext: InputOutputContext, read: Any => Unit) {
          super.read(reader, mapContext, (a: Any) => { reads = reads + 1; read(a) })
        }

      }
      val input1 = DListImpl[String](source)
      val (input2, input3) = (input1.map(identity), input1.map(identity))
      run(input2, input3)

      "there is only one check" ==> { checks === 1 }
      "there are only 3 reads"  ==> { reads === 3 }
    } else success
  }

  "2. it is possible to read from a file or a directory of files" >> { implicit sc: SC =>
    val singleFile = TempFiles.createTempFile("test")
    TempFiles.writeLines(singleFile, Seq("a", "b"), isRemote)
    fromTextFile(singleFile.getPath).run.normalise === "Vector(a, b)"

    val directory = TempFiles.createTempDir("test")
    TempFiles.writeLines(new File(path(directory.getPath+"/file1.part")), Seq("a1"), isRemote)
    TempFiles.writeLines(new File(path(directory.getPath+"/file2.part")), Seq("b1"), isRemote)

    fromTextFile(directory.getPath).run.normalise === "Vector(a1, b1)"
  }
}
